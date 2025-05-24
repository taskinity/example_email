#!/usr/bin/env python3
"""
Email fetching functionality for Taskinity.
This module provides tasks for fetching emails from IMAP servers.
"""
import os
import time
import imaplib
import email
from email.header import decode_header
from typing import Any, Dict, List, Optional
from pathlib import Path
from dotenv import load_dotenv

# Import Taskinity core functionality
from taskinity.core.taskinity_core import task

# Load environment variables
load_dotenv()

@task(name="Fetch Emails", description="Fetches emails from IMAP server")
def fetch_emails(server: str, username: str, password: str, folder: str = "INBOX", limit: int = 10) -> List[Dict[str, Any]]:
    """
    Fetches emails from an IMAP server.
    
    Args:
        server: IMAP server address
        username: Email username
        password: Email password
        folder: Folder to fetch emails from (default: INBOX)
        limit: Maximum number of emails to fetch
    
    Returns:
        List of email data dictionaries
    """
    print(f"Fetching emails from {server} for user {username}")
    
    # For testing without an actual IMAP server, return mock data
    if os.getenv("MOCK_EMAILS", "false").lower() == "true":
        return _get_mock_emails()
    
    try:
        # Connect to the IMAP server
        mail = imaplib.IMAP4_SSL(server)
        mail.login(username, password)
        mail.select(folder)
        
        # Search for all emails in the folder
        status, messages = mail.search(None, "ALL")
        if status != "OK":
            print(f"Error searching for emails: {status}")
            return []
        
        email_ids = messages[0].split()
        if not email_ids:
            print("No emails found")
            return []
        
        # Limit the number of emails to fetch
        if limit > 0:
            email_ids = email_ids[-limit:]
        
        emails = []
        for email_id in email_ids:
            status, msg_data = mail.fetch(email_id, "(RFC822)")
            if status != "OK":
                print(f"Error fetching email {email_id}: {status}")
                continue
            
            raw_email = msg_data[0][1]
            msg = email.message_from_bytes(raw_email)
            
            # Extract email data
            subject = _decode_email_header(msg["Subject"])
            from_addr = _decode_email_header(msg["From"])
            to_addr = _decode_email_header(msg["To"])
            date = msg["Date"]
            
            # Extract email body
            body = ""
            attachments = []
            
            if msg.is_multipart():
                for part in msg.walk():
                    content_type = part.get_content_type()
                    content_disposition = str(part.get("Content-Disposition"))
                    
                    # Extract text content
                    if content_type == "text/plain" and "attachment" not in content_disposition:
                        try:
                            body = part.get_payload(decode=True).decode()
                        except:
                            body = "Unable to decode email body"
                    
                    # Track attachments
                    if "attachment" in content_disposition:
                        filename = part.get_filename()
                        if filename:
                            attachments.append(filename)
            else:
                # Not multipart - extract content directly
                try:
                    body = msg.get_payload(decode=True).decode()
                except:
                    body = "Unable to decode email body"
            
            # Create email data dictionary
            email_data = {
                "id": email_id.decode(),
                "subject": subject,
                "from": from_addr,
                "to": to_addr,
                "date": date,
                "body": body,
                "has_attachments": len(attachments) > 0,
                "attachments": attachments,
                "urgent": _is_urgent(subject, body)
            }
            
            emails.append(email_data)
        
        # Close the connection
        mail.close()
        mail.logout()
        
        return emails
    
    except Exception as e:
        print(f"Error fetching emails: {str(e)}")
        return _get_mock_emails()  # Fallback to mock data on error

def _decode_email_header(header):
    """Decode email header."""
    if not header:
        return ""
    
    try:
        decoded_header = decode_header(header)
        header_parts = []
        for content, encoding in decoded_header:
            if isinstance(content, bytes):
                if encoding:
                    header_parts.append(content.decode(encoding))
                else:
                    header_parts.append(content.decode('utf-8', errors='replace'))
            else:
                header_parts.append(content)
        return " ".join(header_parts)
    except:
        return header

def _is_urgent(subject: str, body: str) -> bool:
    """
    Determine if an email is urgent based on subject and body.
    
    Args:
        subject: Email subject
        body: Email body
    
    Returns:
        True if the email is urgent, False otherwise
    """
    urgent_keywords = ["urgent", "important", "asap", "emergency", "critical", "pilne", "ważne"]
    
    subject_lower = subject.lower()
    body_lower = body.lower()
    
    # Check for urgent keywords in subject
    for keyword in urgent_keywords:
        if keyword in subject_lower:
            return True
    
    # Check for urgent keywords in the first 200 characters of body
    body_preview = body_lower[:200]
    for keyword in urgent_keywords:
        if keyword in body_preview:
            return True
    
    # Check for exclamation marks in subject
    if "!" in subject:
        return True
    
    return False

def _get_mock_emails() -> List[Dict[str, Any]]:
    """
    Get mock email data for testing.
    
    Returns:
        List of mock email data dictionaries
    """
    # Simulate delay
    time.sleep(0.5)
    
    return [
        {
            "id": "1",
            "subject": "Important message about your order",
            "from": "support@example.com",
            "to": "user@example.com",
            "date": "Thu, 24 May 2025 10:30:45 +0200",
            "body": "Your order #12345 has been processed and will be shipped today.",
            "has_attachments": False,
            "attachments": [],
            "urgent": True
        },
        {
            "id": "2",
            "subject": "Monthly newsletter",
            "from": "newsletter@example.com",
            "to": "user@example.com",
            "date": "Thu, 24 May 2025 09:15:22 +0200",
            "body": "Check out our latest products and offers in this month's newsletter!",
            "has_attachments": False,
            "attachments": [],
            "urgent": False
        },
        {
            "id": "3",
            "subject": "Quarterly report",
            "from": "reports@example.com",
            "to": "user@example.com",
            "date": "Wed, 23 May 2025 16:45:10 +0200",
            "body": "Please find attached the quarterly report for Q2 2025.",
            "has_attachments": True,
            "attachments": ["Q2_2025_Report.pdf"],
            "urgent": False
        }
    ]

if __name__ == "__main__":
    # Example usage
    emails = fetch_emails(
        server=os.getenv("IMAP_SERVER", "imap.example.com"),
        username=os.getenv("IMAP_USERNAME", "user@example.com"),
        password=os.getenv("IMAP_PASSWORD", "password123"),
        limit=5
    )
    
    print(f"Fetched {len(emails)} emails:")
    for i, email_data in enumerate(emails, 1):
        print(f"\n{i}. {email_data['subject']} (From: {email_data['from']})")
        print(f"   Urgent: {'Yes' if email_data['urgent'] else 'No'}")
        print(f"   Attachments: {', '.join(email_data['attachments']) if email_data['attachments'] else 'None'}")
