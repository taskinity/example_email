#!/usr/bin/env python3
"""
Prefect workflow for email processing.

This module provides a Prefect-based workflow for processing emails using the existing
email processor functionality. It defines tasks for fetching, classifying, and processing
emails with proper error handling and retries.
"""
import os
from typing import Dict, List, Any, Optional
from datetime import timedelta
from prefect import task, flow, get_run_logger
from prefect.tasks import task_input_hash
from dotenv import load_dotenv
from email_processor import (
    fetch_emails,
    classify_emails,
    process_urgent_emails,
    process_emails_with_attachments,
    process_regular_emails,
    send_responses
)

# Load environment variables
load_dotenv()

# Task configuration
TASK_RETRIES = 3
TASK_RETRY_DELAY = 5  # seconds

@task(
    name="fetch_emails_task",
    description="Fetches emails from IMAP server",
    retries=TASK_RETRIES,
    retry_delay_seconds=TASK_RETRY_DELAY,
    cache_key_fn=task_input_hash,
    cache_expiration=timedelta(minutes=5)
)
def fetch_emails_task(server: str, username: str, password: str) -> List[Dict[str, Any]]:
    """Task to fetch emails from IMAP server."""
    logger = get_run_logger()
    logger.info(f"Fetching emails from {server} for user {username}")
    return fetch_emails(server, username, password)

@task(
    name="classify_emails_task",
    description="Classifies emails into different categories"
)
def classify_emails_task(emails: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
    """Task to classify emails into different categories."""
    logger = get_run_logger()
    logger.info(f"Classifying {len(emails)} emails")
    return classify_emails(emails)

@task(
    name="process_urgent_emails_task",
    description="Processes urgent emails",
    retries=TASK_RETRIES,
    retry_delay_seconds=TASK_RETRY_DELAY
)
def process_urgent_emails_task(emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Task to process urgent emails."""
    logger = get_run_logger()
    if not emails:
        return []
    logger.info(f"Processing {len(emails)} urgent emails")
    return process_urgent_emails(emails)

@task(
    name="process_attachments_task",
    description="Processes emails with attachments",
    retries=TASK_RETRIES,
    retry_delay_seconds=TASK_RETRY_DELAY
)
def process_attachments_task(emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Task to process emails with attachments."""
    logger = get_run_logger()
    if not emails:
        return []
    logger.info(f"Processing {len(emails)} emails with attachments")
    return process_emails_with_attachments(emails)

@task(
    name="process_regular_emails_task",
    description="Processes regular emails"
)
def process_regular_emails_task(emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Task to process regular emails."""
    logger = get_run_logger()
    if not emails:
        return []
    logger.info(f"Processing {len(emails)} regular emails")
    return process_regular_emails(emails)

@task(
    name="send_responses_task",
    description="Sends email responses",
    retries=TASK_RETRIES,
    retry_delay_seconds=TASK_RETRY_DELAY
)
def send_responses_task(
    urgent_responses: List[Dict[str, Any]] = None,
    attachment_responses: List[Dict[str, Any]] = None,
    regular_responses: List[Dict[str, Any]] = None
) -> Dict[str, int]:
    """Task to send email responses."""
    logger = get_run_logger()
    logger.info("Sending email responses")
    
    send_responses(
        urgent_responses=urgent_responses or [],
        attachment_responses=attachment_responses or [],
        regular_responses=regular_responses or []
    )
    
    return {
        "urgent_responses_sent": len(urgent_responses) if urgent_responses else 0,
        "attachment_responses_sent": len(attachment_responses) if attachment_responses else 0,
        "regular_responses_sent": len(regular_responses) if regular_responses else 0
    }

@flow(
    name="email_processing_workflow",
    description="Orchestrates the email processing workflow"
)
def email_processing_workflow(
    imap_server: str,
    imap_username: str,
    imap_password: str,
    process_regular: bool = True,
    process_attachments: bool = True
) -> Dict[str, Any]:
    """
    Main workflow for processing emails.
    
    Args:
        imap_server: IMAP server address
        imap_username: IMAP username
        imap_password: IMAP password
        process_regular: Whether to process regular emails
        process_attachments: Whether to process emails with attachments
        
    Returns:
        Dict containing processing statistics
    """
    logger = get_run_logger()
    logger.info("Starting email processing workflow")
    
    # Fetch emails
    emails = fetch_emails_task(imap_server, imap_username, imap_password)
    
    if not emails:
        logger.info("No emails to process")
        return {"status": "completed", "message": "No emails to process"}
    
    # Classify emails
    classified = classify_emails_task(emails)
    
    # Process urgent emails (always processed)
    urgent_emails = classified.get("urgent_emails", [])
    urgent_responses = process_urgent_emails_task(urgent_emails) if urgent_emails else []
    
    # Process emails with attachments (if enabled)
    attachment_emails = classified.get("emails_with_attachments", [])
    attachment_responses = []
    if process_attachments and attachment_emails:
        attachment_responses = process_attachments_task(attachment_emails)
    
    # Process regular emails (if enabled)
    regular_emails = classified.get("regular_emails", [])
    regular_responses = []
    if process_regular and regular_emails:
        regular_responses = process_regular_emails_task(regular_emails)
    
    # Send responses
    response_stats = send_responses_task(
        urgent_responses=urgent_responses,
        attachment_responses=attachment_responses,
        regular_responses=regular_responses
    )
    
    # Prepare final statistics
    stats = {
        "status": "completed",
        "total_emails_processed": len(emails),
        "urgent_emails_processed": len(urgent_emails),
        "emails_with_attachments_processed": len(attachment_emails) if process_attachments else 0,
        "regular_emails_processed": len(regular_emails) if process_regular else 0,
        "responses_sent": sum(response_stats.values())
    }
    
    logger.info(f"Workflow completed: {stats}")
    return stats

def main():
    """Main function to run the workflow with environment variables."""
    # Get configuration from environment
    imap_server = os.getenv("IMAP_SERVER", "imap.example.com")
    imap_username = os.getenv("IMAP_USERNAME")
    imap_password = os.getenv("IMAP_PASSWORD")
    
    if not all([imap_username, imap_password]):
        raise ValueError("IMAP_USERNAME and IMAP_PASSWORD must be set in environment")
    
    # Run the workflow
    result = email_processing_workflow(
        imap_server=imap_server,
        imap_username=imap_username,
        imap_password=imap_password,
        process_regular=True,
        process_attachments=True
    )
    print(f"Workflow completed with result: {result}")

if __name__ == "__main__":
    main()
