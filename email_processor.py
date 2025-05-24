#!/usr/bin/env python3
"""
Przykład użycia taskinity do przetwarzania e-maili.
"""
import json
import os
import time
from typing import Any, Dict, List

from flow_dsl import task, flow, run_flow_from_dsl, save_dsl, load_dsl

# Definicje zadań

@task(name="Pobieranie e-maili", description="Pobiera e-maile z serwera IMAP")
def fetch_emails(server: str, username: str, password: str) -> List[Dict[str, Any]]:
    """Pobiera e-maile z serwera IMAP."""
    print(f"Pobieranie e-maili z {server} dla użytkownika {username}")
    # Symulacja pobierania e-maili
    time.sleep(1)
    return [
        {"id": "1", "subject": "Ważna wiadomość", "body": "Treść ważnej wiadomości", "urgent": True},
        {"id": "2", "subject": "Zwykła wiadomość", "body": "Treść zwykłej wiadomości", "urgent": False},
        {"id": "3", "subject": "Raport miesięczny", "body": "Treść raportu", "urgent": False, "attachment": True},
    ]

@task(name="Klasyfikacja e-maili", description="Klasyfikuje e-maile na różne kategorie")
def classify_emails(emails: List[Dict[str, Any]]) -> Dict[str, List[Dict[str, Any]]]:
    """Klasyfikuje e-maile na różne kategorie."""
    print(f"Klasyfikacja {len(emails)} e-maili")
    # Symulacja klasyfikacji
    time.sleep(0.5)
    urgent = [email for email in emails if email.get("urgent", False)]
    with_attachments = [email for email in emails if email.get("attachment", False)]
    regular = [email for email in emails if not email.get("urgent", False) and not email.get("attachment", False)]
    return {
        "urgent_emails": urgent, 
        "emails_with_attachments": with_attachments,
        "regular_emails": regular
    }

@task(name="Przetwarzanie pilnych e-maili", description="Przetwarza pilne e-maile")
def process_urgent_emails(urgent_emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Przetwarza pilne e-maile."""
    print(f"Przetwarzanie {len(urgent_emails)} pilnych e-maili")
    # Symulacja przetwarzania
    time.sleep(0.5)
    return [{"id": email["id"], "response": f"Pilna odpowiedź na: {email['subject']}"} for email in urgent_emails]

@task(name="Przetwarzanie e-maili z załącznikami", description="Przetwarza e-maile z załącznikami")
def process_emails_with_attachments(emails_with_attachments: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Przetwarza e-maile z załącznikami."""
    print(f"Przetwarzanie {len(emails_with_attachments)} e-maili z załącznikami")
    # Symulacja przetwarzania
    time.sleep(0.7)
    return [{"id": email["id"], "response": f"Potwierdzenie otrzymania załącznika: {email['subject']}"} for email in emails_with_attachments]

@task(name="Przetwarzanie zwykłych e-maili", description="Przetwarza zwykłe e-maile")
def process_regular_emails(regular_emails: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Przetwarza zwykłe e-maile."""
    print(f"Przetwarzanie {len(regular_emails)} zwykłych e-maili")
    # Symulacja przetwarzania
    time.sleep(0.5)
    return [{"id": email["id"], "response": f"Standardowa odpowiedź na: {email['subject']}"} for email in regular_emails]

@task(name="Wysyłanie odpowiedzi", description="Wysyła odpowiedzi na e-maile")
def send_responses(urgent_responses: List[Dict[str, Any]] = None, 
                   attachment_responses: List[Dict[str, Any]] = None,
                   regular_responses: List[Dict[str, Any]] = None) -> Dict[str, int]:
    """Wysyła odpowiedzi na e-maile."""
    urgent_count = len(urgent_responses) if urgent_responses else 0
    attachment_count = len(attachment_responses) if attachment_responses else 0
    regular_count = len(regular_responses) if regular_responses else 0
    
    print(f"Wysyłanie odpowiedzi: {urgent_count} pilnych, {attachment_count} z załącznikami, {regular_count} zwykłych")
    # Symulacja wysyłania
    time.sleep(1)
    
    return {
        "sent_urgent": urgent_count,
        "sent_attachments": attachment_count,
        "sent_regular": regular_count,
        "total_sent": urgent_count + attachment_count + regular_count
    }

# Definicja przepływu DSL
EMAIL_PROCESSING_DSL = """
flow EmailProcessing:
    description: "Przetwarzanie e-maili z różnymi kategoriami"
    fetch_emails -> classify_emails
    classify_emails -> process_urgent_emails
    classify_emails -> process_emails_with_attachments
    classify_emails -> process_regular_emails
    process_urgent_emails -> send_responses
    process_emails_with_attachments -> send_responses
    process_regular_emails -> send_responses
"""

def main():
    """Główna funkcja uruchamiająca przykładowy przepływ."""
    # Zapisanie DSL do pliku
    os.makedirs("dsl_definitions", exist_ok=True)
    save_dsl(EMAIL_PROCESSING_DSL, "email_processing.dsl")
    
    # Dane wejściowe
    input_data = {
        "server": "imap.example.com",
        "username": "test@example.com",
        "password": "password123",
    }
    
    # Uruchomienie przepływu
    print("Uruchamianie przepływu przetwarzania e-maili...")
    results = run_flow_from_dsl(EMAIL_PROCESSING_DSL, input_data)
    
    # Wyświetlenie wyników
    print("\nWyniki przepływu:")
    print(f"Łącznie wysłano {results['send_responses']['total_sent']} odpowiedzi")
    print(f"  - Pilne: {results['send_responses']['sent_urgent']}")
    print(f"  - Z załącznikami: {results['send_responses']['sent_attachments']}")
    print(f"  - Zwykłe: {results['send_responses']['sent_regular']}")

if __name__ == "__main__":
    main()
