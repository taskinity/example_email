#!/usr/bin/env python3
"""
Email classification functionality for Taskinity.
This module provides tasks for classifying emails into different categories.
"""
import re
from typing import Any, Dict, List, Tuple

# Import Taskinity core functionality
from taskinity.core.taskinity_core import task

@task(name="Classify Emails", description="Classifies emails into different categories")
def classify_emails(emails: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
    """
    Classifies emails into different categories.
    
    Args:
        emails: List of email data dictionaries
    
    Returns:
        Dictionary with categorized emails
    """
    print(f"Classifying {len(emails)} emails")
    
    # Initialize categories
    urgent = []
    with_attachments = []
    support = []
    orders = []
    regular = []
    
    # Classify each email
    for email in emails:
        # Check if urgent
        if email.get("urgent", False):
            urgent.append(email)
        
        # Check if has attachments
        if email.get("has_attachments", False) or email.get("attachments", []):
            with_attachments.append(email)
        
        # Check if support request
        if _is_support_request(email):
            support.append(email)
        
        # Check if order related
        if _is_order_related(email):
            orders.append(email)
        
        # If not in any other category, add to regular
        if not (email.get("urgent", False) or 
                email.get("has_attachments", False) or 
                _is_support_request(email) or 
                _is_order_related(email)):
            regular.append(email)
    
    return {
        "urgent_emails": urgent,
        "emails_with_attachments": with_attachments,
        "support_emails": support,
        "order_emails": orders,
        "regular_emails": regular
    }

def _is_support_request(email: Dict[str, Any]) -> bool:
    """
    Determine if an email is a support request.
    
    Args:
        email: Email data dictionary
    
    Returns:
        True if the email is a support request, False otherwise
    """
    subject = email.get("subject", "").lower()
    body = email.get("body", "").lower()
    from_addr = email.get("from", "").lower()
    
    # Check for support keywords in subject
    support_keywords = ["help", "support", "assistance", "problem", "issue", 
                        "not working", "broken", "error", "bug", "pomoc", "wsparcie"]
    
    for keyword in support_keywords:
        if keyword in subject:
            return True
    
    # Check for support keywords in the first 200 characters of body
    body_preview = body[:200]
    for keyword in support_keywords:
        if keyword in body_preview:
            return True
    
    # Check if sent to support email address
    to_addr = email.get("to", "").lower()
    if "support" in to_addr or "help" in to_addr or "pomoc" in to_addr:
        return True
    
    # Check for question marks in subject
    if "?" in subject:
        return True
    
    return False

def _is_order_related(email: Dict[str, Any]) -> bool:
    """
    Determine if an email is related to an order.
    
    Args:
        email: Email data dictionary
    
    Returns:
        True if the email is order related, False otherwise
    """
    subject = email.get("subject", "").lower()
    body = email.get("body", "").lower()
    
    # Check for order keywords
    order_keywords = ["order", "purchase", "buy", "payment", "invoice", 
                      "shipping", "delivery", "tracking", "zamówienie", "zakup"]
    
    for keyword in order_keywords:
        if keyword in subject:
            return True
    
    # Check for order keywords in the first 200 characters of body
    body_preview = body[:200]
    for keyword in order_keywords:
        if keyword in body_preview:
            return True
    
    # Check for order number pattern (e.g., #12345)
    order_pattern = r'#\d{4,}'
    if re.search(order_pattern, subject) or re.search(order_pattern, body_preview):
        return True
    
    return False

@task(name="Analyze Email Sentiment", description="Analyzes the sentiment of emails")
def analyze_email_sentiment(emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Analyzes the sentiment of emails.
    
    Args:
        emails: List of email data dictionaries
    
    Returns:
        List of emails with sentiment analysis
    """
    print(f"Analyzing sentiment of {len(emails)} emails")
    
    # Simple sentiment analysis
    positive_words = ["happy", "pleased", "satisfied", "great", "excellent", "good", 
                      "thank", "appreciate", "zadowolony", "dziękuję", "dobry"]
    negative_words = ["unhappy", "disappointed", "unsatisfied", "bad", "terrible", "poor", 
                      "complaint", "issue", "problem", "niezadowolony", "problem", "zły"]
    
    for email in emails:
        subject = email.get("subject", "").lower()
        body = email.get("body", "").lower()
        
        # Count positive and negative words
        positive_count = sum(1 for word in positive_words if word in body or word in subject)
        negative_count = sum(1 for word in negative_words if word in body or word in subject)
        
        # Determine sentiment
        if positive_count > negative_count:
            sentiment = "positive"
        elif negative_count > positive_count:
            sentiment = "negative"
        else:
            sentiment = "neutral"
        
        # Add sentiment to email data
        email["sentiment"] = sentiment
        email["sentiment_score"] = positive_count - negative_count
    
    return emails

if __name__ == "__main__":
    # Example usage
    from fetch_emails import _get_mock_emails
    
    # Get mock emails
    emails = _get_mock_emails()
    
    # Classify emails
    classified = classify_emails(emails)
    
    # Print results
    print("\nClassification Results:")
    print(f"Urgent emails: {len(classified['urgent_emails'])}")
    print(f"Emails with attachments: {len(classified['emails_with_attachments'])}")
    print(f"Support emails: {len(classified['support_emails'])}")
    print(f"Order emails: {len(classified['order_emails'])}")
    print(f"Regular emails: {len(classified['regular_emails'])}")
    
    # Analyze sentiment
    analyzed = analyze_email_sentiment(emails)
    
    # Print sentiment analysis
    print("\nSentiment Analysis:")
    for email in analyzed:
        print(f"{email['subject']}: {email['sentiment']} (Score: {email['sentiment_score']})")
