"""
Taskinity Email Processing Tasks Package.
This package contains modular tasks for email processing.
"""

from tasks.fetch_emails import fetch_emails
from tasks.classify_emails import classify_emails, analyze_email_sentiment
from tasks.process_emails import (
    process_urgent_emails,
    process_emails_with_attachments,
    process_support_emails,
    process_order_emails,
    process_regular_emails
)
from tasks.send_emails import send_responses, send_email, send_bulk_emails

__all__ = [
    'fetch_emails',
    'classify_emails',
    'analyze_email_sentiment',
    'process_urgent_emails',
    'process_emails_with_attachments',
    'process_support_emails',
    'process_order_emails',
    'process_regular_emails',
    'send_responses',
    'send_email',
    'send_bulk_emails'
]
