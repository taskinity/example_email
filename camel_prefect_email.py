#!/usr/bin/env python3
"""
Prefect workflow for email processing.

This module provides a simple email classification and response workflow
using Prefect and SMTP for sending emails.
"""
import os
import socket
import smtplib
import imaplib
import email
import time
import logging
from typing import Dict, List, Any, Optional, Tuple, Union
from prefect import flow, task
from dotenv import load_dotenv

# Set up logging
log_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'email_processor.log')
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(log_file, encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger('email_processor')

# Load environment variables from .env file
load_dotenv()

logger.info("Starting email processor")

# Email configuration from environment variables
SMTP_SERVER = os.getenv("SMTP_SERVER")
SMTP_PORT = int(os.getenv("SMTP_PORT", 587))
SMTP_USERNAME = os.getenv("SMTP_USERNAME")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD")
FROM_EMAIL = os.getenv("FROM_EMAIL", SMTP_USERNAME)
REPLY_TO_EMAIL = os.getenv("REPLY_TO_EMAIL", FROM_EMAIL)
TEST_EMAIL = os.getenv("TEST_EMAIL")  # Default test email

# IMAP Configuration
IMAP_SERVER = os.getenv("IMAP_SERVER")
IMAP_PORT = int(os.getenv("IMAP_PORT", 993))
IMAP_USERNAME = os.getenv("IMAP_USERNAME", SMTP_USERNAME)
IMAP_PASSWORD = os.getenv("IMAP_PASSWORD", SMTP_PASSWORD)
IMAP_FOLDER = os.getenv("IMAP_FOLDER", "INBOX")


@task(retries=3)
def classify_email(content: str) -> str:
    """
    Classify email content to determine priority.

    Args:
        content: The email content to classify

    Returns:
        str: The priority level ("High Priority" or "Normal")
    """
    if "urgent" in content.lower():
        return "High Priority"
    return "Normal"


@task
def fetch_emails(limit: int = 5) -> List[Dict[str, str]]:
    """
    Fetch emails from IMAP server.

    Args:
        limit: Maximum number of emails to fetch

    Returns:
        List of dictionaries containing email data
    """
    try:
        # Connect to IMAP server
        mail = imaplib.IMAP4_SSL(IMAP_SERVER, IMAP_PORT)
        mail.login(IMAP_USERNAME, IMAP_PASSWORD)

        # Select mailbox
        mail.select(IMAP_FOLDER)

        # Search for all emails
        status, messages = mail.search(None, 'ALL')
        if status != 'OK':
            return []

        # Get the list of email IDs
        email_ids = messages[0].split()
        emails = []

        # Fetch the most recent emails (up to the limit)
        for e_id in email_ids[-limit:]:
            status, msg_data = mail.fetch(e_id, '(RFC822)')

            if status != 'OK':
                continue

            for response_part in msg_data:
                if isinstance(response_part, tuple):
                    msg = email.message_from_bytes(response_part[1])

                    # Get email details
                    email_data = {
                        'id': e_id.decode(),
                        'from': msg.get('From', ''),
                        'to': msg.get('To', ''),
                        'subject': msg.get('Subject', 'No Subject'),
                        'date': msg.get('Date', ''),
                        'body': ''
                    }

                    # Get email body
                    if msg.is_multipart():
                        for part in msg.walk():
                            content_type = part.get_content_type()
                            content_disposition = str(part.get('Content-Disposition'))

                            if content_type == 'text/plain' and 'attachment' not in content_disposition:
                                try:
                                    email_data['body'] = part.get_payload(decode=True).decode()
                                except:
                                    pass
                                break
                    else:
                        try:
                            email_data['body'] = msg.get_payload(decode=True).decode()
                        except:
                            pass

                    emails.append(email_data)

        mail.close()
        mail.logout()
        return emails

    except Exception as e:
        print(f"Error fetching emails: {str(e)}")
        return []


@task(retries=2, retry_delay_seconds=5)
def send_response(email: str, subject: str, content: str, timeout: int = 30) -> bool:
    """
    Send an email response using SMTP with timeout and better error handling.

    Args:
        email: Recipient email address
        subject: Email subject
        content: Email content
        timeout: Connection and operation timeout in seconds

    Returns:
        bool: True if email was sent successfully, False otherwise
    """
    if not all([SMTP_USERNAME, SMTP_PASSWORD]):
        print("❌ SMTP_USERNAME and SMTP_PASSWORD must be set in .env")
        return False

    message = f"""From: {FROM_EMAIL}
To: {email}
Reply-To: {REPLY_TO_EMAIL}
Subject: {subject}

{content}"""

    server = None
    try:
        print(f"🔌 Connecting to SMTP server: {SMTP_SERVER}:{SMTP_PORT}...")
        start_time = time.time()

        # Handle SSL vs non-SSL connections
        if SMTP_PORT == 465:  # SSL
            print("🔒 Using SSL connection...")
            server = smtplib.SMTP_SSL(SMTP_SERVER, SMTP_PORT, timeout=timeout)
        else:  # Non-SSL or STARTTLS
            server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT, timeout=timeout)
            if SMTP_PORT == 587:  # STARTTLS
                print("🔒 Starting TLS...")
                server.starttls()

        # Enable debug output
        server.set_debuglevel(1)

        # Login
        print(f"🔑 Authenticating as {SMTP_USERNAME}...")
        server.login(SMTP_USERNAME, SMTP_PASSWORD)

        # Send the email
        print(f"✉️  Sending email to {email}...")
        server.sendmail(FROM_EMAIL, email, message)

        elapsed = time.time() - start_time
        print(f"✅ Email sent successfully in {elapsed:.2f} seconds")
        return True

    except smtplib.SMTPConnectError as e:
        print(f"❌ Failed to connect to SMTP server: {str(e)}")
    except smtplib.SMTPAuthenticationError as e:
        print(f"❌ Authentication failed: {str(e)}")
    except smtplib.SMTPServerDisconnected as e:
        print(f"❌ Server disconnected: {str(e)}")
    except smtplib.SMTPException as e:
        print(f"❌ SMTP error: {str(e)}")
    except socket.timeout as e:
        print(f"⌛ Operation timed out after {timeout} seconds: {str(e)}")
    except Exception as e:
        print(f"❌ Unexpected error: {str(e)}")
        import traceback
        traceback.print_exc()
    finally:
        try:
            if server:
                server.quit()
        except Exception as e:
            print(f"⚠️  Error while closing connection: {str(e)}")

    return False


@flow(name="email_workflow")
def process_email(sender: str, content: str):
    """
    Process an email and send a response.

    Args:
        sender: Sender's email address
        content: Email content
    """
    # Classify the email
    priority = classify_email(content)

    # Prepare response based on priority
    if priority == "High Priority":
        response = "Your urgent message has been received and will be processed immediately."
    else:
        response = "Thank you for your message. We will get back to you soon."

    # Send the response
    send_response(sender, f"Re: Your message - {priority}", response)


def test_smtp_connection() -> bool:
    """Test SMTP connection with current settings."""
    logger.info("\n" + "=" * 50)
    logger.info("Testing SMTP connection...")
    logger.info(f"Server: {SMTP_SERVER}:{SMTP_PORT}")
    security = 'SSL' if SMTP_PORT == 465 else 'STARTTLS' if SMTP_PORT == 587 else 'None'
    logger.info(f"Security: {security}")
    logger.info(f"Username: {SMTP_USERNAME}")
    logger.info(f"Test email will be sent to: {TEST_EMAIL}")

    logger.info(f"\nAttempting to send test email to {TEST_EMAIL}...")

    try:
        # Test connection without sending an email first
        logger.info("Testing connection and authentication...")
        if SMTP_PORT == 465:  # SSL
            server = smtplib.SMTP_SSL(SMTP_SERVER, SMTP_PORT, timeout=10)
        else:  # Non-SSL or STARTTLS
            server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT, timeout=10)
            if SMTP_PORT == 587:  # STARTTLS
                server.starttls()

        server.login(SMTP_USERNAME, SMTP_PASSWORD)
        logger.info("Connection and authentication successful!")
        server.quit()

        # Now try sending a test email
        logger.info("\nSending test email...")
        success = send_response(
            email=TEST_EMAIL,
            subject="SMTP Test",
            content="This is a test email to verify SMTP connectivity.",
            timeout=15  # Slightly longer timeout for the full operation
        )

        if success:
            logger.info("SMTP test successful!")
        else:
            logger.error("Failed to send test email.")

        return success

    except smtplib.SMTPConnectError as e:
        logger.error(f"Failed to connect to SMTP server: {str(e)}")
    except smtplib.SMTPAuthenticationError as e:
        logger.error(f"Authentication failed: {str(e)}")
    except Exception as e:
        logger.error(f"Error during SMTP test: {str(e)}", exc_info=True)

    return False


def main():
    """Main function to run the email workflow."""
    logger.info("Starting email processor")
    logger.info(f"Log file: {os.path.abspath(log_file)}")

    # First test SMTP connection
    if not test_smtp_connection():
        logger.error("\nCannot proceed without a working SMTP connection.")
        logger.error("Please check your .env file and ensure your SMTP settings are correct.")
        logger.info(f"Check the log file for more details: {os.path.abspath(log_file)}")
        return

    logger.info("\n🚀 Starting email processing workflow...")

    # Uncomment these lines once SMTP is working
    """
    # Example 1: Process specific emails
    process_email("info@softreck.dev", "URGENT: Need immediate assistance!")
    process_email("info@softreck.dev", "Just following up on my request")

    # Example 2: Fetch and process emails from IMAP
    print("\n📥 Fetching emails from IMAP...")
    emails = fetch_emails(limit=3)  # Get 3 most recent emails

    for email_data in emails:
        print(f"\n📧 Processing email: {email_data['subject']}")
        print(f"👤 From: {email_data['from']}")
        print(f"📅 Date: {email_data['date']}")

        # Process the email
        process_email(
            sender=email_data['from'],
            content=f"{email_data['subject']}\n\n{email_data['body'][:200]}..."
        )
    """


if __name__ == "__main__":
    main()
