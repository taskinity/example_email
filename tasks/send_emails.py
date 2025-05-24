#!/usr/bin/env python3
"""
Email sending functionality for Taskinity.
This module provides tasks for sending email responses.
"""
import os
import time
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from typing import Any, Dict, List, Optional, Union
from dotenv import load_dotenv

# Import Taskinity core functionality
from taskinity.core.taskinity_core import task

# Load environment variables
load_dotenv()

@task(name="Send Email Responses", description="Sends email responses")
def send_responses(urgent_responses: List[Dict[str, Any]] = None, 
                  attachment_responses: List[Dict[str, Any]] = None,
                  support_responses: List[Dict[str, Any]] = None,
                  order_responses: List[Dict[str, Any]] = None,
                  regular_responses: List[Dict[str, Any]] = None) -> Dict[str, int]:
    """
    Sends email responses for different categories.
    
    Args:
        urgent_responses: List of urgent email responses
        attachment_responses: List of responses for emails with attachments
        support_responses: List of responses for support emails
        order_responses: List of responses for order emails
        regular_responses: List of responses for regular emails
    
    Returns:
        Dictionary with counts of sent emails by category
    """
    # Initialize counts
    urgent_count = len(urgent_responses) if urgent_responses else 0
    attachment_count = len(attachment_responses) if attachment_responses else 0
    support_count = len(support_responses) if support_responses else 0
    order_count = len(order_responses) if order_responses else 0
    regular_count = len(regular_responses) if regular_responses else 0
    
    total_count = urgent_count + attachment_count + support_count + order_count + regular_count
    
    print(f"Sending responses: {urgent_count} urgent, {attachment_count} with attachments, "
          f"{support_count} support, {order_count} order, {regular_count} regular")
    
    # Combine all responses
    all_responses = []
    if urgent_responses:
        all_responses.extend(urgent_responses)
    if attachment_responses:
        all_responses.extend(attachment_responses)
    if support_responses:
        all_responses.extend(support_responses)
    if order_responses:
        all_responses.extend(order_responses)
    if regular_responses:
        all_responses.extend(regular_responses)
    
    # Send each response
    sent_count = 0
    for response in all_responses:
        if _send_email_response(response):
            sent_count += 1
    
    return {
        "sent_urgent": urgent_count,
        "sent_attachments": attachment_count,
        "sent_support": support_count,
        "sent_orders": order_count,
        "sent_regular": regular_count,
        "total_sent": sent_count,
        "total_attempted": total_count
    }

@task(name="Send Single Email", description="Sends a single email")
def send_email(to_email: str, subject: str, body: str, 
              from_email: Optional[str] = None, 
              cc: Optional[List[str]] = None,
              bcc: Optional[List[str]] = None,
              reply_to: Optional[str] = None,
              html_body: Optional[str] = None) -> bool:
    """
    Sends a single email.
    
    Args:
        to_email: Recipient email address
        subject: Email subject
        body: Email body (plain text)
        from_email: Sender email address (default: from environment)
        cc: List of CC recipients
        bcc: List of BCC recipients
        reply_to: Reply-to email address
        html_body: HTML version of the email body
    
    Returns:
        True if email was sent successfully, False otherwise
    """
    # Get SMTP configuration from environment
    smtp_server = os.getenv("SMTP_SERVER", "smtp.example.com")
    smtp_port = int(os.getenv("SMTP_PORT", 587))
    smtp_username = os.getenv("SMTP_USERNAME", "")
    smtp_password = os.getenv("SMTP_PASSWORD", "")
    default_from_email = os.getenv("FROM_EMAIL", "noreply@taskinity.org")
    
    # Use default from_email if not provided
    if not from_email:
        from_email = default_from_email
    
    # For testing without an actual SMTP server
    if os.getenv("MOCK_EMAILS", "false").lower() == "true":
        print(f"MOCK: Sending email to {to_email}")
        print(f"MOCK: Subject: {subject}")
        print(f"MOCK: From: {from_email}")
        if cc:
            print(f"MOCK: CC: {', '.join(cc)}")
        if bcc:
            print(f"MOCK: BCC: {', '.join(bcc)}")
        print(f"MOCK: Body: {body[:100]}...")
        time.sleep(0.2)  # Simulate sending delay
        return True
    
    try:
        # Create message
        msg = MIMEMultipart("alternative")
        msg["Subject"] = subject
        msg["From"] = from_email
        msg["To"] = to_email
        
        if cc:
            msg["Cc"] = ", ".join(cc)
        if reply_to:
            msg["Reply-To"] = reply_to
        
        # Add plain text body
        msg.attach(MIMEText(body, "plain"))
        
        # Add HTML body if provided
        if html_body:
            msg.attach(MIMEText(html_body, "html"))
        
        # Connect to SMTP server
        with smtplib.SMTP(smtp_server, smtp_port) as server:
            server.starttls()
            server.login(smtp_username, smtp_password)
            
            # Prepare recipients list
            recipients = [to_email]
            if cc:
                recipients.extend(cc)
            if bcc:
                recipients.extend(bcc)
            
            # Send email
            server.sendmail(from_email, recipients, msg.as_string())
        
        print(f"Email sent to {to_email}")
        return True
    
    except Exception as e:
        print(f"Error sending email: {str(e)}")
        return False

def _send_email_response(response: Dict[str, Any]) -> bool:
    """
    Send an email response based on the response data.
    
    Args:
        response: Response data dictionary
    
    Returns:
        True if email was sent successfully, False otherwise
    """
    # Extract recipient from the original sender
    to_email = response.get("original_from", "")
    if "<" in to_email and ">" in to_email:
        to_email = to_email.split("<")[1].split(">")[0]
    
    # Get subject and body
    subject = response.get("response_subject", "")
    body = response.get("response_body", "")
    
    # Add priority header for urgent emails
    headers = {}
    if response.get("priority") == "high":
        headers["X-Priority"] = "1"
        headers["X-MSMail-Priority"] = "High"
        headers["Importance"] = "High"
    
    # Send the email
    return send_email(
        to_email=to_email,
        subject=subject,
        body=body,
        reply_to=os.getenv("REPLY_TO_EMAIL", None)
    )

@task(name="Send Bulk Emails", description="Sends bulk emails to multiple recipients")
def send_bulk_emails(recipients: List[str], subject: str, body: str,
                    from_email: Optional[str] = None,
                    personalize: bool = True) -> Dict[str, int]:
    """
    Sends bulk emails to multiple recipients.
    
    Args:
        recipients: List of recipient email addresses
        subject: Email subject
        body: Email body template
        from_email: Sender email address (default: from environment)
        personalize: Whether to personalize emails for each recipient
    
    Returns:
        Dictionary with counts of sent and failed emails
    """
    print(f"Sending bulk emails to {len(recipients)} recipients")
    
    sent_count = 0
    failed_count = 0
    
    for recipient in recipients:
        # Personalize email if requested
        if personalize:
            personalized_body = _personalize_email(body, recipient)
            personalized_subject = _personalize_email(subject, recipient)
        else:
            personalized_body = body
            personalized_subject = subject
        
        # Send email
        if send_email(recipient, personalized_subject, personalized_body, from_email):
            sent_count += 1
        else:
            failed_count += 1
        
        # Add a small delay between emails to avoid rate limiting
        time.sleep(0.1)
    
    return {
        "total_recipients": len(recipients),
        "sent": sent_count,
        "failed": failed_count
    }

def _personalize_email(template: str, recipient: str) -> str:
    """
    Personalize an email template for a specific recipient.
    
    Args:
        template: Email template with placeholders
        recipient: Recipient email address
    
    Returns:
        Personalized email content
    """
    # Extract name from email
    name = recipient.split("@")[0]
    if "." in name:
        name = name.split(".")[0]
    name = name.capitalize()
    
    # Replace placeholders
    personalized = template.replace("{name}", name)
    personalized = personalized.replace("{email}", recipient)
    personalized = personalized.replace("{domain}", recipient.split("@")[1])
    
    return personalized

if __name__ == "__main__":
    # Example usage
    from fetch_emails import _get_mock_emails
    from classify_emails import classify_emails
    from process_emails import (
        process_urgent_emails, 
        process_emails_with_attachments,
        process_support_emails,
        process_order_emails,
        process_regular_emails
    )
    
    # Get mock emails
    emails = _get_mock_emails()
    
    # Classify emails
    classified = classify_emails(emails)
    
    # Process each category
    urgent_responses = process_urgent_emails(classified["urgent_emails"])
    attachment_responses = process_emails_with_attachments(classified["emails_with_attachments"])
    support_responses = process_support_emails(classified["support_emails"])
    order_responses = process_order_emails(classified["order_emails"])
    regular_responses = process_regular_emails(classified["regular_emails"])
    
    # Send responses
    results = send_responses(
        urgent_responses=urgent_responses,
        attachment_responses=attachment_responses,
        support_responses=support_responses,
        order_responses=order_responses,
        regular_responses=regular_responses
    )
    
    # Print results
    print("\nEmail Sending Results:")
    print(f"Total emails sent: {results['total_sent']}/{results['total_attempted']}")
    print(f"Urgent: {results['sent_urgent']}")
    print(f"With attachments: {results['sent_attachments']}")
    print(f"Support: {results['sent_support']}")
    print(f"Orders: {results['sent_orders']}")
    print(f"Regular: {results['sent_regular']}")
