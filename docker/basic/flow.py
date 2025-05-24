#!/usr/bin/env python3
"""
Basic email processing flow for Docker environment.
This file imports the necessary tasks and runs a simplified email processing flow.
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
    process_regular_emails,
    send_responses
)

def main():
    """Main function to run the email processing flow."""
    print("Starting basic email processing flow...")
    
    # Load the flow DSL
    flow_dsl_path = Path(__file__).parent / "flow.dsl"
    with open(flow_dsl_path, "r") as f:
        flow_dsl = f.read()
    
    # Set environment variables for mock mode
    os.environ["MOCK_EMAILS"] = "true"
    
    # Prepare input data
    input_data = {
        "server": "mock.example.com",
        "username": "user@example.com",
        "password": "password123",
        "folder": "INBOX",
        "limit": 5
    }
    
    # Run the flow
    print(f"Running email processing flow with mock data...")
    results = run_flow_from_dsl(flow_dsl, input_data)
    
    # Print results
    print("\nEmail Processing Results:")
    if "send_responses" in results:
        response_results = results["send_responses"]
        print(f"Total emails processed: {response_results.get('total_attempted', 0)}")
        print(f"Total emails sent: {response_results.get('total_sent', 0)}")
        print(f"  - Urgent: {response_results.get('sent_urgent', 0)}")
        print(f"  - With attachments: {response_results.get('sent_attachments', 0)}")
        print(f"  - Regular: {response_results.get('sent_regular', 0)}")
    else:
        print("No email responses were sent.")
    
    return results

if __name__ == "__main__":
    main()
    
    # Keep container running for demonstration purposes
    if os.environ.get("KEEP_RUNNING", "false").lower() == "true":
        print("\nKeeping container running for demonstration. Press Ctrl+C to exit.")
        try:
            while True:
                time.sleep(60)
        except KeyboardInterrupt:
            print("Exiting...")
