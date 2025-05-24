#!/usr/bin/env python3
"""
Email processor using dataclasses and type hints.
"""
from dataclasses import dataclass, field
import os
import smtplib
import imaplib
import email
import logging
from typing import List, Optional, Dict, Any
from prefect import flow, task
from dotenv import load_dotenv

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('email_processor.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger('email_processor')

# Load environment
load_dotenv()

@dataclass
class EmailConfig:
    """Email configuration from environment variables."""
    smtp_server: str = field(default_factory=lambda: os.getenv("SMTP_SERVER", ""))
    smtp_port: int = field(default_factory=lambda: int(os.getenv("SMTP_PORT", 587)))
    smtp_username: str = field(default_factory=lambda: os.getenv("SMTP_USERNAME", ""))
    smtp_password: str = field(default_factory=lambda: os.getenv("SMTP_PASSWORD", ""))
    from_email: str = field(init=False)
    reply_to: str = field(init=False)
    test_email: str = field(default_factory=lambda: os.getenv("TEST_EMAIL", "test@example.com"))
    imap_server: str = field(default_factory=lambda: os.getenv("IMAP_SERVER", ""))
    imap_port: int = field(default_factory=lambda: int(os.getenv("IMAP_PORT", 993)))
    imap_username: str = field(init=False)
    imap_password: str = field(init=False)
    imap_folder: str = field(default_factory=lambda: os.getenv("IMAP_FOLDER", "INBOX"))
    
    def __post_init__(self):
        self.from_email = os.getenv("FROM_EMAIL", self.smtp_username)
        self.reply_to = os.getenv("REPLY_TO_EMAIL", self.from_email)
        self.imap_username = os.getenv("IMAP_USERNAME", self.smtp_username)
        self.imap_password = os.getenv("IMAP_PASSWORD", self.smtp_password)

# Initialize config
config = EmailConfig()

@dataclass
class EmailMessage:
    """Email message data class."""
    id: str
    from_: str
    to: str
    subject: str
    date: str
    body: str = ""

@task
def classify_email(content: str) -> str:
    """Classify email content to determine priority."""
    return "High Priority" if "urgent" in content.lower() else "Normal"

def _extract_email_body(msg) -> str:
    """Extract plain text body from email message."""
    if msg.is_multipart():
        for part in msg.walk():
            if (part.get_content_type() == 'text/plain' and 
                'attachment' not in str(part.get('Content-Disposition'))):
                try: 
                    return part.get_payload(decode=True).decode()
                except Exception: 
                    continue
    try: 
        return msg.get_payload(decode=True).decode()
    except Exception: 
        return ""

@task
def fetch_emails(limit: int = 5) -> List[EmailMessage]:
    """Fetch emails from IMAP server."""
    try:
        mail = imaplib.IMAP4_SSL(config.imap_server, config.imap_port)
        mail.login(config.imap_username, config.imap_password)
        mail.select(config.imap_folder)
        
        _, messages = mail.search(None, 'ALL')
        if messages[0]:
            return [
                EmailMessage(
                    id=e_id.decode(),
                    from_=email.message_from_bytes(msg_data[0][1]).get('From', ''),
                    to=email.message_from_bytes(msg_data[0][1]).get('To', ''),
                    subject=email.message_from_bytes(msg_data[0][1]).get('Subject', 'No Subject'),
                    date=email.message_from_bytes(msg_data[0][1]).get('Date', ''),
                    body=_extract_email_body(email.message_from_bytes(msg_data[0][1]))
                )
                for e_id in messages[0].split()[-limit:]
                if (_status := mail.fetch(e_id, '(RFC822)'))[0] == 'OK' and _status[1]
            ]
        return []
    except Exception as e:
        logger.error(f"Error fetching emails: {e}")
        return []
    finally:
        try: 
            mail.close()
            mail.logout()
        except: 
            pass

@task
def send_email(to_email: str, subject: str, content: str, timeout: int = 30) -> bool:
    """Send an email using SMTP."""
    if not all([config.smtp_username, config.smtp_password]):
        logger.error("SMTP credentials not configured")
        return False

    message = f"""From: {config.from_email}
To: {to_email}
Reply-To: {config.reply_to}
Subject: {subject}

{content}"""
    
    try:
        if config.smtp_port == 465:  # SSL
            with smtplib.SMTP_SSL(config.smtp_server, config.smtp_port, timeout=timeout) as server:
                server.login(config.smtp_username, config.smtp_password)
                server.sendmail(config.from_email, to_email, message)
        else:  # STARTTLS or plain
            with smtplib.SMTP(config.smtp_server, config.smtp_port, timeout=timeout) as server:
                if config.smtp_port == 587:  # STARTTLS
                    server.starttls()
                server.login(config.smtp_username, config.smtp_password)
                server.sendmail(config.from_email, to_email, message)
        return True
    except Exception as e:
        logger.error(f"Error sending email: {e}")
        return False

@flow
def process_email(sender: str, content: str):
    """Process an email and send a response."""
    priority = classify_email(content)
    response = (
        "Your urgent message has been received and will be processed immediately."
        if priority == "High Priority" else 
        "Thank you for your message. We will get back to you soon."
    )
    send_email(sender, f"Re: Your message - {priority}", response)

def main():
    """Main function to run the email workflow."""
    logger.info("Starting email processor")
    
    # Test SMTP connection by sending a test email
    if send_email(config.test_email, "Test Email", "This is a test email"):
        logger.info("Test email sent successfully")
    else:
        logger.error("Failed to send test email")

if __name__ == "__main__":
    main()
