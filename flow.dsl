flow email_processing {
    // Define input parameters
    input {
        imap_server: string,
        imap_username: string,
        imap_password: string,
        smtp_server: string,
        smtp_port: number,
        smtp_username: string,
        smtp_password: string,
        from_email: string
    }

    // Define the email processing flow
    task fetch_emails(imap_server, imap_username, imap_password) -> emails
    task classify_emails(emails) -> classified_emails
    
    // Process different types of emails
    task process_urgent_emails(classified_emails.urgent_emails) -> urgent_responses
    task process_emails_with_attachments(classified_emails.emails_with_attachments) -> attachment_responses
    task process_support_emails(classified_emails.support_emails) -> support_responses
    task process_order_emails(classified_emails.order_emails) -> order_responses
    task process_regular_emails(classified_emails.regular_emails) -> regular_responses
    
    // Send email responses
    task send_responses(
        urgent_responses,
        attachment_responses,
        support_responses,
        order_responses,
        regular_responses
    ) -> email_results

    // Define the output
    output {
        processed_emails: number,
        sent_emails: number,
        urgent_count: number,
        attachment_count: number,
        support_count: number,
        order_count: number,
        regular_count: number
    }
}
