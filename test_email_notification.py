#!/usr/bin/env python3
"""
Skrypt do testowania powiadomień email i pipeline'u do analizy emaili.
"""
import os
import sys
import time
import json
import argparse
from pathlib import Path

# Dodanie katalogu nadrzędnego do ścieżki, aby umożliwić import modułów
current_dir = Path(__file__).parent
sys.path.append(str(current_dir))

# Import modułów
try:
    from advanced_logging import setup_logger, log_exception
    logger = setup_logger("test_email")
except ImportError:
    import logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    logger = logging.getLogger("test_email")
    logger.warning("Moduł advanced_logging nie jest dostępny. Używam standardowego logowania.")

# Import modułów do testowania
from notification_service import send_email_notification, ensure_config as ensure_notification_config
from email_pipeline import EmailProcessor, ensure_config as ensure_email_config

def parse_arguments():
    """Parsuje argumenty wiersza poleceń."""
    parser = argparse.ArgumentParser(description="Testowanie powiadomień email i pipeline'u do analizy emaili.")
    
    # Główne opcje
    parser.add_argument("--test-notification", action="store_true", help="Testuj wysyłanie powiadomień email")
    parser.add_argument("--test-pipeline", action="store_true", help="Testuj pipeline do analizy emaili")
    parser.add_argument("--test-all", action="store_true", help="Testuj wszystkie funkcje")
    
    # Opcje dla powiadomień
    parser.add_argument("--subject", type=str, default="Test powiadomienia", help="Temat wiadomości")
    parser.add_argument("--message", type=str, default="To jest testowa wiadomość z systemu taskinity.", help="Treść wiadomości")
    
    # Opcje dla pipeline'u
    parser.add_argument("--check-interval", type=int, default=10, help="Interwał sprawdzania nowych emaili (w sekundach)")
    parser.add_argument("--max-emails", type=int, default=5, help="Maksymalna liczba emaili do przetworzenia")
    parser.add_argument("--run-time", type=int, default=60, help="Czas działania testu pipeline'u (w sekundach)")
    
    return parser.parse_args()

def test_notification(subject, message):
    """Testuje wysyłanie powiadomień email."""
    logger.info("Rozpoczynam test powiadomień email")
    
    try:
        # Załaduj konfigurację powiadomień
        config = ensure_notification_config()
        
        # Sprawdź, czy powiadomienia email są włączone
        if not config.get("enabled", False) or not config.get("email", {}).get("enabled", False):
            logger.error("Powiadomienia email są wyłączone w konfiguracji")
            return False
        
        # Sprawdź, czy wszystkie wymagane parametry są ustawione
        email_config = config.get("email", {})
        required_params = ["smtp_server", "smtp_port", "username", "password", "from_email", "recipients"]
        missing_params = [param for param in required_params if not email_config.get(param)]
        
        if missing_params:
            logger.error(f"Brakujące parametry konfiguracji email: {missing_params}")
            return False
        
        # Wyślij powiadomienie
        logger.info(f"Wysyłam powiadomienie email: {subject}")
        result = send_email_notification(subject, message, config)
        
        if result:
            logger.info("Powiadomienie email zostało wysłane pomyślnie")
            return True
        else:
            logger.error("Nie udało się wysłać powiadomienia email")
            return False
    
    except Exception as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "test_notification"})
        else:
            logger.error(f"Błąd podczas testowania powiadomień email: {str(e)}")
        return False

def test_pipeline(check_interval, max_emails, run_time):
    """Testuje pipeline do analizy emaili."""
    logger.info("Rozpoczynam test pipeline'u do analizy emaili")
    
    try:
        # Załaduj konfigurację email
        config = ensure_email_config()
        
        # Zmodyfikuj konfigurację na potrzeby testu
        config["processing"]["check_interval_seconds"] = check_interval
        config["processing"]["max_emails_per_batch"] = max_emails
        
        # Utwórz procesor emaili
        processor = EmailProcessor(config)
        
        # Uruchom test przez określony czas
        start_time = time.time()
        end_time = start_time + run_time
        
        logger.info(f"Pipeline do analizy emaili będzie działać przez {run_time} sekund")
        
        cycles = 0
        while time.time() < end_time:
            logger.info(f"Cykl {cycles + 1}: Sprawdzanie nowych emaili")
            processor.process_emails()
            cycles += 1
            
            # Oblicz pozostały czas
            remaining_time = end_time - time.time()
            if remaining_time <= 0:
                break
            
            # Oblicz czas do następnego cyklu
            sleep_time = min(check_interval, remaining_time)
            logger.debug(f"Oczekiwanie {sleep_time:.1f} sekund do następnego cyklu")
            time.sleep(sleep_time)
        
        logger.info(f"Test pipeline'u zakończony. Wykonano {cycles} cykli sprawdzania emaili.")
        return True
    
    except Exception as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "test_pipeline"})
        else:
            logger.error(f"Błąd podczas testowania pipeline'u do analizy emaili: {str(e)}")
        return False

def main():
    """Główna funkcja programu."""
    args = parse_arguments()
    
    # Sprawdź, czy wybrano jakiś test
    if not (args.test_notification or args.test_pipeline or args.test_all):
        logger.error("Nie wybrano żadnego testu. Użyj --help, aby zobaczyć dostępne opcje.")
        return 1
    
    # Wykonaj wybrane testy
    results = []
    
    if args.test_notification or args.test_all:
        notification_result = test_notification(args.subject, args.message)
        results.append(("Powiadomienia email", notification_result))
    
    if args.test_pipeline or args.test_all:
        pipeline_result = test_pipeline(args.check_interval, args.max_emails, args.run_time)
        results.append(("Pipeline do analizy emaili", pipeline_result))
    
    # Wyświetl podsumowanie
    logger.info("=== Podsumowanie testów ===")
    all_passed = True
    
    for test_name, result in results:
        status = "SUKCES" if result else "BŁĄD"
        logger.info(f"{test_name}: {status}")
        all_passed = all_passed and result
    
    if all_passed:
        logger.info("Wszystkie testy zakończone pomyślnie")
        return 0
    else:
        logger.error("Niektóre testy zakończyły się błędem")
        return 1

if __name__ == "__main__":
    sys.exit(main())
