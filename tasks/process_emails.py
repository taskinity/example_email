#!/usr/bin/env python3
"""
Email processing functionality for Taskinity.
This module provides tasks for processing emails of different categories.
"""
import os
import time
from typing import Any, Dict, List, Optional
from pathlib import Path
from dotenv import load_dotenv

# Import Taskinity core functionality
from taskinity.core.taskinity_core import task

@task(name="Process Urgent Emails", description="Processes urgent emails")
def process_urgent_emails(urgent_emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Processes urgent emails.
    
    Args:
        urgent_emails: List of urgent email data dictionaries
    
    Returns:
        List of processed urgent emails with responses
    """
    print(f"Processing {len(urgent_emails)} urgent emails")
    
    # Simulate processing time
    time.sleep(0.5)
    
    responses = []
    for email in urgent_emails:
        # Create response
        response = {
            "id": email["id"],
            "original_subject": email["subject"],
            "original_from": email["from"],
            "response_subject": f"Re: {email['subject']}",
            "response_body": _generate_urgent_response(email),
            "priority": "high",
            "response_time": "within 1 hour"
        }
        
        responses.append(response)
    
    return responses

@task(name="Process Emails with Attachments", description="Processes emails with attachments")
def process_emails_with_attachments(emails_with_attachments: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Processes emails with attachments.
    
    Args:
        emails_with_attachments: List of email data dictionaries with attachments
    
    Returns:
        List of processed emails with responses
    """
    print(f"Processing {len(emails_with_attachments)} emails with attachments")
    
    # Simulate processing time
    time.sleep(0.7)
    
    responses = []
    for email in emails_with_attachments:
        # Create response
        response = {
            "id": email["id"],
            "original_subject": email["subject"],
            "original_from": email["from"],
            "response_subject": f"Re: {email['subject']}",
            "response_body": _generate_attachment_response(email),
            "priority": "medium",
            "response_time": "within 24 hours"
        }
        
        responses.append(response)
    
    return responses

@task(name="Process Support Emails", description="Processes support request emails")
def process_support_emails(support_emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Processes support request emails.
    
    Args:
        support_emails: List of support request email data dictionaries
    
    Returns:
        List of processed support emails with responses
    """
    print(f"Processing {len(support_emails)} support emails")
    
    # Simulate processing time
    time.sleep(0.6)
    
    responses = []
    for email in support_emails:
        # Create response
        response = {
            "id": email["id"],
            "original_subject": email["subject"],
            "original_from": email["from"],
            "response_subject": f"Re: {email['subject']}",
            "response_body": _generate_support_response(email),
            "priority": "medium",
            "response_time": "within 24 hours",
            "ticket_number": f"SUP-{int(time.time())}-{email['id']}"
        }
        
        responses.append(response)
    
    return responses

@task(name="Process Order Emails", description="Processes order-related emails")
def process_order_emails(order_emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Processes order-related emails.
    
    Args:
        order_emails: List of order-related email data dictionaries
    
    Returns:
        List of processed order emails with responses
    """
    print(f"Processing {len(order_emails)} order emails")
    
    # Simulate processing time
    time.sleep(0.5)
    
    responses = []
    for email in order_emails:
        # Create response
        response = {
            "id": email["id"],
            "original_subject": email["subject"],
            "original_from": email["from"],
            "response_subject": f"Re: {email['subject']}",
            "response_body": _generate_order_response(email),
            "priority": "medium",
            "response_time": "within 24 hours",
            "order_number": _extract_order_number(email)
        }
        
        responses.append(response)
    
    return responses

@task(name="Process Regular Emails", description="Processes regular emails")
def process_regular_emails(regular_emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Processes regular emails.
    
    Args:
        regular_emails: List of regular email data dictionaries
    
    Returns:
        List of processed regular emails with responses
    """
    print(f"Processing {len(regular_emails)} regular emails")
    
    # Simulate processing time
    time.sleep(0.5)
    
    responses = []
    for email in regular_emails:
        # Create response
        response = {
            "id": email["id"],
            "original_subject": email["subject"],
            "original_from": email["from"],
            "response_subject": f"Re: {email['subject']}",
            "response_body": _generate_regular_response(email),
            "priority": "low",
            "response_time": "within 48 hours"
        }
        
        responses.append(response)
    
    return responses

def _generate_urgent_response(email: Dict[str, Any]) -> str:
    """Generate response for urgent email."""
    return f"""Dear {_extract_name(email['from'])},

Thank you for your urgent message regarding "{email['subject']}".

We have prioritized your request and our team is working on it immediately. 
You can expect a detailed response within the next hour.

If you need immediate assistance, please call our urgent support line at +1-555-123-4567.

Best regards,
The Taskinity Support Team
"""

def _generate_attachment_response(email: Dict[str, Any]) -> str:
    """Generate response for email with attachments."""
    attachments = ", ".join(email.get("attachments", []))
    return f"""Dear {_extract_name(email['from'])},

Thank you for your email and for sending us the following attachment(s): {attachments}.

We have received your files and they are being processed by our team. 
We will get back to you within 24 hours with our feedback.

Best regards,
The Taskinity Support Team
"""

def _generate_support_response(email: Dict[str, Any]) -> str:
    """Generate response for support request email."""
    return f"""Dear {_extract_name(email['from'])},

Thank you for contacting Taskinity Support regarding "{email['subject']}".

We have received your support request and created a ticket for you. Your ticket number is SUP-{int(time.time())}-{email['id']}.
Our support team will analyze your issue and respond within 24 hours.

For future reference, please include your ticket number in any follow-up communications.

Best regards,
The Taskinity Support Team
"""

def _generate_order_response(email: Dict[str, Any]) -> str:
    """Generate response for order-related email."""
    order_number = _extract_order_number(email)
    return f"""Dear {_extract_name(email['from'])},

Thank you for your message regarding your order {order_number}.

We have received your inquiry and our customer service team is reviewing the details.
We will get back to you with more information within 24 hours.

You can also check the status of your order at any time by visiting our website and entering your order number.

Best regards,
The Taskinity Customer Service Team
"""

def _generate_regular_response(email: Dict[str, Any]) -> str:
    """Generate response for regular email."""
    return f"""Dear {_extract_name(email['from'])},

Thank you for your email regarding "{email['subject']}".

We have received your message and will respond to your inquiry within 48 hours.
We appreciate your patience.

Best regards,
The Taskinity Team
"""

def _extract_name(email_address: str) -> str:
    """Extract name from email address."""
    if not email_address:
        return "Customer"
    
    # Try to extract name from "Name <email>" format
    if "<" in email_address and ">" in email_address:
        name_part = email_address.split("<")[0].strip()
        if name_part:
            # Return first name only
            return name_part.split()[0]
    
    # If no name found, use the part before @ in the email
    try:
        username = email_address.split("@")[0]
        # Remove special characters and numbers
        username = ''.join(c for c in username if c.isalpha() or c == '.')
        # Split by dots and get first part
        return username.split(".")[0].capitalize()
    except:
        return "Customer"

def _extract_order_number(email: Dict[str, Any]) -> str:
    """Extract order number from email."""
    import re
    
    # Check subject and body for order number pattern
    subject = email.get("subject", "")
    body = email.get("body", "")
    
    # Look for patterns like "order #12345" or "order number: 12345"
    patterns = [
        r'order\s*#\s*(\d+)',
        r'order\s*number\s*[:#]\s*(\d+)',
        r'order\s*id\s*[:#]\s*(\d+)',
        r'#(\d{4,})',
        r'zamówienie\s*nr\s*(\d+)'
    ]
    
    for pattern in patterns:
        # Check subject
        match = re.search(pattern, subject, re.IGNORECASE)
        if match:
            return f"#{match.group(1)}"
        
        # Check body
        match = re.search(pattern, body, re.IGNORECASE)
        if match:
            return f"#{match.group(1)}"
    
    # If no order number found, generate a placeholder
    return f"#ORDER-{email['id']}"

if __name__ == "__main__":
    # Example usage
    from fetch_emails import _get_mock_emails
    from classify_emails import classify_emails
    
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
    
    # Print results
    print("\nProcessing Results:")
    print(f"Urgent responses: {len(urgent_responses)}")
    print(f"Attachment responses: {len(attachment_responses)}")
    print(f"Support responses: {len(support_responses)}")
    print(f"Order responses: {len(order_responses)}")
    print(f"Regular responses: {len(regular_responses)}")
    
    # Print sample response
    if urgent_responses:
        print("\nSample Urgent Response:")
        print(f"Subject: {urgent_responses[0]['response_subject']}")
        print(f"Body:\n{urgent_responses[0]['response_body']}")
