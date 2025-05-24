#!/usr/bin/env python3
"""
Mock email processing flow for Docker environment with MailHog.
This file imports the necessary tasks and runs an email processing flow with a mock SMTP server.
"""
import os
import sys
import time
from pathlib import Path

# Add parent directory to Python path to import tasks
sys.path.append(str(Path(__file__).parent.parent.parent))

# Import Taskinity core functionality
from taskinity.core.taskinity_core import run_flow_from_dsl, load_dsl

# Import tasks
from tasks import (
    fetch_emails,
    classify_emails,
    process_urgent_emails,
    process_emails_with_attachments,
    process_support_emails,
    process_order_emails,
    process_regular_emails,
    send_responses
)

def main():
    """Main function to run the email processing flow."""
    print("Starting email processing flow with MailHog mock server...")
    
    # Load the flow DSL
    flow_dsl_path = Path(__file__).parent / "flow.dsl"
    with open(flow_dsl_path, "r") as f:
        flow_dsl = f.read()
    
    # Set environment variables for SMTP
    os.environ["SMTP_SERVER"] = os.getenv("SMTP_SERVER", "mockserver")
    os.environ["SMTP_PORT"] = os.getenv("SMTP_PORT", "1025")
    
    # For mock mode, we'll use mock data for fetching but real SMTP for sending
    os.environ["MOCK_EMAILS"] = "true"
    
    # Prepare input data
    input_data = {
        "server": "mockserver",
        "username": "user@taskinity.org",
        "password": "password123",
        "folder": "INBOX",
        "limit": 5
    }
    
    # Wait for mock server to be ready
    print("Waiting for MailHog mock server to be ready...")
    time.sleep(3)
    
    # Run the flow
    print("Running email processing flow with mock data...")
    results = run_flow_from_dsl(flow_dsl, input_data)
    
    # Print results
    print("\nEmail Processing Results:")
    if "send_responses" in results:
        response_results = results["send_responses"]
        print(f"Total emails processed: {response_results.get('total_attempted', 0)}")
        print(f"Total emails sent: {response_results.get('total_sent', 0)}")
        print(f"  - Urgent: {response_results.get('sent_urgent', 0)}")
        print(f"  - With attachments: {response_results.get('sent_attachments', 0)}")
        print(f"  - Support: {response_results.get('sent_support', 0)}")
        print(f"  - Orders: {response_results.get('sent_orders', 0)}")
        print(f"  - Regular: {response_results.get('sent_regular', 0)}")
        print("\nYou can view sent emails in the MailHog web interface at: http://localhost:8025")
    else:
        print("No email responses were sent.")
    
    return results

def send_test_emails():
    """Send some test emails to the mock SMTP server."""
    from email.mime.text import MIMEText
    from email.mime.multipart import MIMEMultipart
    import smtplib
    
    print("Sending test emails to MailHog mock server...")
    
    # Test email templates
    test_emails = [
        {
            "subject": "Urgent: Support Request #12345",
            "body": "I need urgent help with my account. Please respond ASAP!",
            "from": "customer@example.com",
            "to": "support@taskinity.org"
        },
        {
            "subject": "Monthly Report - May 2025",
            "body": "Please find attached the monthly report for May 2025.",
            "from": "reports@example.com",
            "to": "admin@taskinity.org"
        },
        {
            "subject": "Question about my order #54321",
            "body": "I placed an order yesterday but haven't received a confirmation. Can you check the status?",
            "from": "customer@example.com",
            "to": "orders@taskinity.org"
        }
    ]
    
    try:
        # Connect to the mock SMTP server
        smtp_server = os.getenv("SMTP_SERVER", "mockserver")
        smtp_port = int(os.getenv("SMTP_PORT", 1025))
        
        with smtplib.SMTP(smtp_server, smtp_port) as server:
            # Send each test email
            for email_data in test_emails:
                msg = MIMEMultipart()
                msg["Subject"] = email_data["subject"]
                msg["From"] = email_data["from"]
                msg["To"] = email_data["to"]
                
                # Add body
                msg.attach(MIMEText(email_data["body"], "plain"))
                
                # Send email
                server.sendmail(email_data["from"], email_data["to"], msg.as_string())
                print(f"Sent test email: {email_data['subject']}")
        
        print("All test emails sent successfully!")
        print("You can view them in the MailHog web interface at: http://localhost:8025")
        
    except Exception as e:
        print(f"Error sending test emails: {str(e)}")

if __name__ == "__main__":
    # Check if we should send test emails
    if os.environ.get("SEND_TEST_EMAILS", "true").lower() == "true":
        send_test_emails()
    
    # Run the main flow
    main()
    
    # Keep container running for demonstration purposes
    if os.environ.get("KEEP_RUNNING", "true").lower() == "true":
        print("\nKeeping container running for demonstration. Press Ctrl+C to exit.")
        try:
            while True:
                time.sleep(60)
        except KeyboardInterrupt:
            print("Exiting...")
