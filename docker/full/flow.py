#!/usr/bin/env python3
"""
Full email processing flow for Docker environment with real IMAP/SMTP servers.
This file imports the necessary tasks and runs a complete email processing flow.
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
    print("Starting full email processing flow with real IMAP/SMTP servers...")
    
    # Load the flow DSL
    flow_dsl_path = Path(__file__).parent / "flow.dsl"
    with open(flow_dsl_path, "r") as f:
        flow_dsl = f.read()
    
    # Get email server credentials from environment variables
    server = os.getenv("IMAP_SERVER", "mailserver")
    port = int(os.getenv("IMAP_PORT", 143))
    username = os.getenv("IMAP_USERNAME", "user@taskinity.org")
    password = os.getenv("IMAP_PASSWORD", "password123")
    folder = os.getenv("IMAP_FOLDER", "INBOX")
    limit = int(os.getenv("EMAIL_LIMIT", 10))
    
    # Prepare input data
    input_data = {
        "server": server,
        "username": username,
        "password": password,
        "folder": folder,
        "limit": limit
    }
    
    # Wait for mail server to be ready
    print(f"Waiting for mail server {server} to be ready...")
    time.sleep(5)
    
    # Run the flow
    print(f"Running email processing flow for {username} on {server}...")
    
    try:
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
        else:
            print("No email responses were sent.")
        
        return results
    
    except Exception as e:
        print(f"Error running email processing flow: {str(e)}")
        
        # If no emails are available, use mock data for demonstration
        if "No emails found" in str(e) or "Error fetching emails" in str(e):
            print("No emails found on server. Running with mock data for demonstration...")
            os.environ["MOCK_EMAILS"] = "true"
            results = run_flow_from_dsl(flow_dsl, input_data)
            print("Completed flow with mock data.")
            return results
        
        raise

def run_continuous():
    """Run the email processing flow continuously."""
    check_interval = int(os.getenv("CHECK_INTERVAL_SECONDS", 60))
    
    print(f"Starting continuous email processing (checking every {check_interval} seconds)...")
    
    while True:
        try:
            main()
        except Exception as e:
            print(f"Error in email processing cycle: {str(e)}")
        
        print(f"Waiting {check_interval} seconds before next check...")
        time.sleep(check_interval)

if __name__ == "__main__":
    # Check if continuous mode is enabled
    if os.environ.get("CONTINUOUS_MODE", "false").lower() == "true":
        run_continuous()
    else:
        main()
        
        # Keep container running for demonstration purposes
        if os.environ.get("KEEP_RUNNING", "true").lower() == "true":
            print("\nKeeping container running for demonstration. Press Ctrl+C to exit.")
            try:
                while True:
                    time.sleep(60)
            except KeyboardInterrupt:
                print("Exiting...")
