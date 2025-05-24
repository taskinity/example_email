flow EmailProcessing:
    description: "Email processing flow with mock SMTP server (MailHog)"
    fetch_emails -> classify_emails
    classify_emails -> process_urgent_emails
    classify_emails -> process_emails_with_attachments
    classify_emails -> process_support_emails
    classify_emails -> process_order_emails
    classify_emails -> process_regular_emails
    process_urgent_emails -> send_responses
    process_emails_with_attachments -> send_responses
    process_support_emails -> send_responses
    process_order_emails -> send_responses
    process_regular_emails -> send_responses
