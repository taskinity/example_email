#!/usr/bin/env python3
"""
Pipeline do analizy emaili przychodzących i automatycznej odpowiedzi.
Monitoruje skrzynkę pocztową, analizuje przychodzące emaile i automatycznie odpowiada na wybrane.
"""
import os
import re
import time
import json
import email
import imaplib
import smtplib
import sys
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple
import threading
from dotenv import load_dotenv

# Ładowanie zmiennych środowiskowych
load_dotenv()

# Konfiguracja katalogów
BASE_DIR = Path(__file__).parent
LOGS_DIR = BASE_DIR / "logs"
EMAILS_DIR = BASE_DIR / "emails"
CONFIG_DIR = BASE_DIR / "config"

# Tworzenie katalogów, jeśli nie istnieją
os.makedirs(LOGS_DIR, exist_ok=True)
os.makedirs(EMAILS_DIR, exist_ok=True)
os.makedirs(CONFIG_DIR, exist_ok=True)

# Import zaawansowanego logowania
try:
    from advanced_logging import setup_logger, log_exception, log_dependency_check, log_config_check, log_performance
    logger = setup_logger("email_pipeline")
    logger.info("Używam zaawansowanego logowania dla email_pipeline")
    
    # Sprawdzenie zależności
    log_dependency_check(logger, "imaplib", required=True)
    log_dependency_check(logger, "smtplib", required=True)
    log_dependency_check(logger, "email", required=True)
    log_dependency_check(logger, "dotenv", required=False)
    
except ImportError:
    # Fallback do standardowego logowania
    from loguru import logger
    LOG_FILE = LOGS_DIR / "email_pipeline.log"
    logger.remove()  # Usunięcie domyślnego handlera
    logger.add(sys.stderr, level="INFO")
    logger.add(LOG_FILE, rotation="1 day", retention="30 days", level="DEBUG")
    logger.warning("Moduł advanced_logging nie jest dostępny. Używam standardowego logowania.")

# Konfiguracja email
EMAIL_CONFIG_FILE = CONFIG_DIR / "email_config.json"

# Domyślna konfiguracja
DEFAULT_EMAIL_CONFIG = {
    "imap": {
        "server": os.getenv("IMAP_SERVER", "imap.gmail.com"),
        "port": int(os.getenv("IMAP_PORT", 993)),
        "username": os.getenv("IMAP_USERNAME", ""),
        "password": os.getenv("IMAP_PASSWORD", ""),
        "folder": "INBOX",
        "ssl": True
    },
    "smtp": {
        "server": os.getenv("SMTP_SERVER", "smtp.gmail.com"),
        "port": int(os.getenv("SMTP_PORT", 587)),
        "username": os.getenv("SMTP_USERNAME", ""),
        "password": os.getenv("SMTP_PASSWORD", ""),
        "from_email": os.getenv("FROM_EMAIL", ""),
        "use_tls": True
    },
    "auto_reply": {
        "enabled": True,
        "criteria": {
            "subject_contains": ["pytanie", "zapytanie", "pomoc", "wsparcie"],
            "from_domains": ["example.com", "gmail.com"],
            "priority_keywords": ["pilne", "ważne", "urgent", "asap"]
        },
        "templates": {
            "default": "Dziękujemy za wiadomość. Odpowiemy najszybciej jak to możliwe.",
            "priority": "Dziękujemy za pilną wiadomość. Zajmiemy się nią priorytetowo.",
            "support": "Dziękujemy za zgłoszenie. Nasz zespół wsparcia skontaktuje się z Tobą wkrótce."
        },
        "signature": "\n\nPozdrawiamy,\nZespół taskinity",
        "reply_to_all": False,
        "add_original_message": True,
        "cooldown_hours": 24  # Nie odpowiadaj ponownie do tego samego nadawcy przez 24h
    },
    "processing": {
        "check_interval_seconds": 60,
        "max_emails_per_batch": 10,
        "save_attachments": True,
        "attachments_folder": str(EMAILS_DIR / "attachments"),
        "archive_processed": True,
        "archive_folder": "Processed"
    },
    "flows": {
        "trigger_flow_on_email": True,
        "flow_mapping": {
            "support": "support_flow.dsl",
            "order": "order_processing.dsl",
            "complaint": "complaint_handling.dsl"
        }
    }
}

def ensure_config():
    """Upewnia się, że plik konfiguracyjny istnieje."""
    start_time = time.time()
    logger.debug("Sprawdzanie konfiguracji email pipeline...")
    
    try:
        # Sprawdź, czy katalog konfiguracyjny istnieje
        if not os.path.exists(CONFIG_DIR):
            logger.info(f"Tworzenie katalogu konfiguracyjnego: {CONFIG_DIR}")
            os.makedirs(CONFIG_DIR, exist_ok=True)
        
        # Sprawdź, czy plik konfiguracyjny istnieje
        if not EMAIL_CONFIG_FILE.exists():
            logger.info(f"Tworzenie domyślnego pliku konfiguracyjnego: {EMAIL_CONFIG_FILE}")
            with open(EMAIL_CONFIG_FILE, 'w') as f:
                json.dump(DEFAULT_EMAIL_CONFIG, f, indent=4)
        
        config = load_config()
        
        # Sprawdź, czy konfiguracja zawiera wszystkie wymagane klucze
        required_sections = ["imap", "smtp", "auto_reply", "processing", "flows"]
        if hasattr(log_config_check, "__call__"):
            log_config_check(logger, config, required_sections)
        else:
            missing_sections = [section for section in required_sections if section not in config]
            if missing_sections:
                logger.warning(f"Brakujące sekcje w konfiguracji: {missing_sections}")
        
        # Sprawdź wymagane parametry IMAP
        imap_required = ["server", "port", "username", "password", "folder"]
        if hasattr(log_config_check, "__call__"):
            log_config_check(logger, config.get("imap", {}), imap_required)
        
        # Sprawdź wymagane parametry SMTP
        smtp_required = ["server", "port", "username", "password", "from_email"]
        if hasattr(log_config_check, "__call__"):
            log_config_check(logger, config.get("smtp", {}), smtp_required)
        
        end_time = time.time()
        if hasattr(log_performance, "__call__"):
            log_performance(logger, start_time, end_time, "ensure_config")
        else:
            logger.debug(f"ensure_config zakończone w {end_time - start_time:.4f}s")
        
        return config
    
    except Exception as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "ensure_config"})
        else:
            logger.error(f"Błąd podczas sprawdzania konfiguracji: {str(e)}")
        
        # W przypadku błędu zwróć domyślną konfigurację
        return DEFAULT_EMAIL_CONFIG

def load_config() -> Dict[str, Any]:
    """Ładuje konfigurację email."""
    start_time = time.time()
    logger.debug(f"Ładowanie konfiguracji z: {EMAIL_CONFIG_FILE}")
    
    try:
        with open(EMAIL_CONFIG_FILE, 'r') as f:
            config = json.load(f)
        
        end_time = time.time()
        if hasattr(log_performance, "__call__"):
            log_performance(logger, start_time, end_time, "load_config")
        else:
            logger.debug(f"load_config zakończone w {end_time - start_time:.4f}s")
        
        return config
    except FileNotFoundError:
        logger.warning(f"Plik konfiguracyjny nie istnieje: {EMAIL_CONFIG_FILE}")
        logger.info("Używam domyślnej konfiguracji")
        return DEFAULT_EMAIL_CONFIG
    except json.JSONDecodeError as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "load_config", "file": str(EMAIL_CONFIG_FILE)})
        else:
            logger.error(f"Błąd parsowania JSON w pliku konfiguracyjnym: {str(e)}")
        return DEFAULT_EMAIL_CONFIG
    except Exception as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "load_config", "file": str(EMAIL_CONFIG_FILE)})
        else:
            logger.error(f"Błąd ładowania konfiguracji email: {str(e)}")
        return DEFAULT_EMAIL_CONFIG

def save_config(config: Dict[str, Any]):
    """Zapisuje konfigurację email."""
    start_time = time.time()
    logger.debug(f"Zapisywanie konfiguracji do: {EMAIL_CONFIG_FILE}")
    
    try:
        # Sprawdź, czy katalog konfiguracyjny istnieje
        if not os.path.exists(CONFIG_DIR):
            logger.info(f"Tworzenie katalogu konfiguracyjnego: {CONFIG_DIR}")
            os.makedirs(CONFIG_DIR, exist_ok=True)
        
        # Zapisz konfigurację
        with open(EMAIL_CONFIG_FILE, 'w') as f:
            json.dump(config, f, indent=4)
        
        end_time = time.time()
        if hasattr(log_performance, "__call__"):
            log_performance(logger, start_time, end_time, "save_config")
        else:
            logger.debug(f"save_config zakończone w {end_time - start_time:.4f}s")
        
        logger.info(f"Konfiguracja zapisana do: {EMAIL_CONFIG_FILE}")
        return True
    
    except PermissionError as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "save_config", "file": str(EMAIL_CONFIG_FILE)})
        else:
            logger.error(f"Brak uprawnień do zapisu pliku konfiguracyjnego: {str(e)}")
        return False
    
    except Exception as e:
        if hasattr(log_exception, "__call__"):
            log_exception(logger, e, {"function": "save_config", "file": str(EMAIL_CONFIG_FILE)})
        else:
            logger.error(f"Błąd zapisywania konfiguracji email: {str(e)}")
        return False

class EmailProcessor:
    """Klasa do przetwarzania emaili."""
    
    def __init__(self, config: Dict[str, Any] = None):
        """Inicjalizuje procesor emaili."""
        self.config = config or ensure_config()
        self.imap = None
        self.smtp = None
        self.replied_to = {}  # Słownik do śledzenia odpowiedzi (email -> timestamp)
        self.load_replied_to()
        
        # Tworzenie katalogu na załączniki
        attachments_folder = self.config["processing"]["attachments_folder"]
        os.makedirs(attachments_folder, exist_ok=True)
        
        logger.info("EmailProcessor zainicjalizowany.")
    
    def load_replied_to(self):
        """Ładuje historię odpowiedzi z pliku."""
        replied_to_file = EMAILS_DIR / "replied_to.json"
        if replied_to_file.exists():
            try:
                with open(replied_to_file, 'r') as f:
                    self.replied_to = json.load(f)
                logger.debug(f"Załadowano historię odpowiedzi: {len(self.replied_to)} wpisów.")
            except Exception as e:
                logger.error(f"Błąd ładowania historii odpowiedzi: {str(e)}")
    
    def save_replied_to(self):
        """Zapisuje historię odpowiedzi do pliku."""
        replied_to_file = EMAILS_DIR / "replied_to.json"
        try:
            with open(replied_to_file, 'w') as f:
                json.dump(self.replied_to, f, indent=4)
            logger.debug(f"Zapisano historię odpowiedzi: {len(self.replied_to)} wpisów.")
        except Exception as e:
            logger.error(f"Błąd zapisywania historii odpowiedzi: {str(e)}")
    
    def connect_imap(self) -> bool:
        """Łączy się z serwerem IMAP."""
        imap_config = self.config["imap"]
        
        try:
            if imap_config["ssl"]:
                self.imap = imaplib.IMAP4_SSL(imap_config["server"], imap_config["port"])
            else:
                self.imap = imaplib.IMAP4(imap_config["server"], imap_config["port"])
            
            self.imap.login(imap_config["username"], imap_config["password"])
            logger.info(f"Połączono z serwerem IMAP: {imap_config['server']}:{imap_config['port']}")
            return True
        except Exception as e:
            logger.error(f"Błąd połączenia z serwerem IMAP: {str(e)}")
            return False
    
    def connect_smtp(self) -> bool:
        """Łączy się z serwerem SMTP."""
        smtp_config = self.config["smtp"]
        
        try:
            if smtp_config["use_tls"]:
                self.smtp = smtplib.SMTP(smtp_config["server"], smtp_config["port"])
                self.smtp.starttls()
            else:
                self.smtp = smtplib.SMTP(smtp_config["server"], smtp_config["port"])
            
            self.smtp.login(smtp_config["username"], smtp_config["password"])
            logger.info(f"Połączono z serwerem SMTP: {smtp_config['server']}:{smtp_config['port']}")
            return True
        except Exception as e:
            logger.error(f"Błąd połączenia z serwerem SMTP: {str(e)}")
            return False
    
    def disconnect(self):
        """Rozłącza się z serwerami."""
        if self.imap:
            try:
                self.imap.logout()
                logger.info("Rozłączono z serwerem IMAP.")
            except Exception as e:
                logger.error(f"Błąd rozłączania z serwerem IMAP: {str(e)}")
        
        if self.smtp:
            try:
                self.smtp.quit()
                logger.info("Rozłączono z serwerem SMTP.")
            except Exception as e:
                logger.error(f"Błąd rozłączania z serwerem SMTP: {str(e)}")
    
    def fetch_emails(self) -> List[Dict[str, Any]]:
        """Pobiera nieprzeczytane emaile."""
        if not self.imap:
            if not self.connect_imap():
                return []
        
        emails = []
        imap_config = self.config["imap"]
        processing_config = self.config["processing"]
        
        try:
            # Wybierz folder
            self.imap.select(imap_config["folder"])
            
            # Szukaj nieprzeczytanych wiadomości
            status, messages = self.imap.search(None, "UNSEEN")
            
            if status != "OK":
                logger.error(f"Błąd wyszukiwania wiadomości: {status}")
                return []
            
            # Pobierz ID wiadomości
            message_ids = messages[0].split()
            logger.info(f"Znaleziono {len(message_ids)} nieprzeczytanych wiadomości.")
            
            # Ogranicz liczbę wiadomości do przetworzenia
            message_ids = message_ids[:processing_config["max_emails_per_batch"]]
            
            for msg_id in message_ids:
                try:
                    status, msg_data = self.imap.fetch(msg_id, "(RFC822)")
                    
                    if status != "OK":
                        logger.error(f"Błąd pobierania wiadomości {msg_id}: {status}")
                        continue
                    
                    raw_email = msg_data[0][1]
                    email_message = email.message_from_bytes(raw_email)
                    
                    # Przetwarzanie wiadomości
                    email_data = self.process_email_message(email_message, msg_id)
                    emails.append(email_data)
                    
                    # Oznacz jako przeczytane
                    self.imap.store(msg_id, "+FLAGS", "\\Seen")
                    
                    # Archiwizuj, jeśli skonfigurowano
                    if processing_config["archive_processed"]:
                        self.imap.copy(msg_id, processing_config["archive_folder"])
                        self.imap.store(msg_id, "+FLAGS", "\\Deleted")
                
                except Exception as e:
                    logger.error(f"Błąd przetwarzania wiadomości {msg_id}: {str(e)}")
            
            # Wykonaj usunięcie oznaczonych wiadomości
            if processing_config["archive_processed"]:
                self.imap.expunge()
            
            return emails
        
        except Exception as e:
            logger.error(f"Błąd pobierania wiadomości: {str(e)}")
            return []
    
    def process_email_message(self, email_message, msg_id) -> Dict[str, Any]:
        """Przetwarza wiadomość email."""
        processing_config = self.config["processing"]
        
        # Pobierz podstawowe informacje
        subject = email_message.get("Subject", "")
        from_email = email.utils.parseaddr(email_message.get("From", ""))[1]
        to_email = email.utils.parseaddr(email_message.get("To", ""))[1]
        date = email_message.get("Date", "")
        
        # Dekodowanie tematu, jeśli jest zakodowany
        if subject.startswith("=?"):
            subject = email.header.decode_header(subject)[0][0]
            if isinstance(subject, bytes):
                subject = subject.decode("utf-8", errors="ignore")
        
        # Pobierz treść wiadomości
        body = ""
        if email_message.is_multipart():
            for part in email_message.walk():
                content_type = part.get_content_type()
                content_disposition = str(part.get("Content-Disposition"))
                
                # Pobierz treść tekstową
                if content_type == "text/plain" and "attachment" not in content_disposition:
                    body = part.get_payload(decode=True).decode("utf-8", errors="ignore")
                    break
                
                # Zapisz załączniki
                if processing_config["save_attachments"] and "attachment" in content_disposition:
                    self.save_attachment(part)
        else:
            body = email_message.get_payload(decode=True).decode("utf-8", errors="ignore")
        
        # Zapisz pełną wiadomość do pliku
        email_file = EMAILS_DIR / f"{msg_id.decode('utf-8')}.eml"
        with open(email_file, "wb") as f:
            f.write(raw_email)
        
        logger.info(f"Przetworzono wiadomość: {subject} od {from_email}")
        
        return {
            "id": msg_id.decode("utf-8"),
            "subject": subject,
            "from": from_email,
            "to": to_email,
            "date": date,
            "body": body,
            "raw_email": email_message
        }
    
    def save_attachment(self, part):
        """Zapisuje załącznik."""
        processing_config = self.config["processing"]
        
        try:
            filename = part.get_filename()
            if filename:
                # Dekodowanie nazwy pliku, jeśli jest zakodowana
                if filename.startswith("=?"):
                    filename = email.header.decode_header(filename)[0][0]
                    if isinstance(filename, bytes):
                        filename = filename.decode("utf-8", errors="ignore")
                
                # Bezpieczna nazwa pliku
                filename = os.path.basename(filename)
                filename = re.sub(r'[^\w\.-]', '_', filename)
                
                # Dodanie timestampu, aby uniknąć nadpisywania
                base, ext = os.path.splitext(filename)
                timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
                filename = f"{base}_{timestamp}{ext}"
                
                # Ścieżka do zapisu
                filepath = os.path.join(processing_config["attachments_folder"], filename)
                
                # Zapisz załącznik
                with open(filepath, "wb") as f:
                    f.write(part.get_payload(decode=True))
                
                logger.info(f"Zapisano załącznik: {filename}")
        except Exception as e:
            logger.error(f"Błąd zapisywania załącznika: {str(e)}")
    
    def should_auto_reply(self, email_data: Dict[str, Any]) -> Tuple[bool, str]:
        """Sprawdza, czy należy automatycznie odpowiedzieć na email."""
        auto_reply_config = self.config["auto_reply"]
        
        if not auto_reply_config["enabled"]:
            return False, "auto_reply disabled"
        
        from_email = email_data["from"]
        subject = email_data["subject"]
        body = email_data["body"]
        
        # Sprawdź, czy już odpowiedziano
        if from_email in self.replied_to:
            last_reply_time = datetime.fromisoformat(self.replied_to[from_email])
            cooldown = timedelta(hours=auto_reply_config["cooldown_hours"])
            
            if datetime.now() - last_reply_time < cooldown:
                logger.info(f"Pomijanie odpowiedzi do {from_email} - w okresie cooldown.")
                return False, "cooldown period"
        
        # Sprawdź kryteria
        criteria = auto_reply_config["criteria"]
        
        # Sprawdź domenę nadawcy
        from_domain = from_email.split("@")[-1] if "@" in from_email else ""
        domain_match = any(domain == from_domain for domain in criteria["from_domains"])
        
        # Sprawdź temat
        subject_match = any(keyword.lower() in subject.lower() for keyword in criteria["subject_contains"])
        
        # Sprawdź słowa kluczowe priorytetu
        priority_match = any(keyword.lower() in subject.lower() or keyword.lower() in body.lower() 
                             for keyword in criteria["priority_keywords"])
        
        # Określ szablon odpowiedzi
        template_key = "default"
        if "support" in subject.lower() or "wsparcie" in subject.lower():
            template_key = "support"
        elif priority_match:
            template_key = "priority"
        
        # Decyzja
        if domain_match or subject_match:
            logger.info(f"Auto-odpowiedź do {from_email} z szablonem {template_key}.")
            return True, template_key
        
        return False, "criteria not met"
    
    def send_auto_reply(self, email_data: Dict[str, Any], template_key: str) -> bool:
        """Wysyła automatyczną odpowiedź."""
        if not self.smtp:
            if not self.connect_smtp():
                return False
        
        auto_reply_config = self.config["auto_reply"]
        smtp_config = self.config["smtp"]
        
        try:
            # Przygotuj wiadomość
            msg = MIMEMultipart()
            msg["From"] = smtp_config["from_email"]
            msg["To"] = email_data["from"]
            msg["Subject"] = f"Re: {email_data['subject']}"
            
            # Treść wiadomości
            template = auto_reply_config["templates"].get(template_key, auto_reply_config["templates"]["default"])
            body = template + auto_reply_config["signature"]
            
            # Dodaj oryginalną wiadomość
            if auto_reply_config["add_original_message"]:
                body += f"\n\n--- Oryginalna wiadomość ---\nOd: {email_data['from']}\nData: {email_data['date']}\nTemat: {email_data['subject']}\n\n{email_data['body']}"
            
            msg.attach(MIMEText(body, "plain", "utf-8"))
            
            # Wyślij wiadomość
            recipients = [email_data["from"]]
            if auto_reply_config["reply_to_all"] and "Cc" in email_data["raw_email"]:
                cc = email_data["raw_email"]["Cc"]
                if cc:
                    cc_emails = [email.utils.parseaddr(addr)[1] for addr in cc.split(",")]
                    recipients.extend(cc_emails)
            
            self.smtp.sendmail(smtp_config["from_email"], recipients, msg.as_string())
            
            # Zapisz informację o odpowiedzi
            self.replied_to[email_data["from"]] = datetime.now().isoformat()
            self.save_replied_to()
            
            logger.info(f"Wysłano automatyczną odpowiedź do {email_data['from']}")
            return True
        
        except Exception as e:
            logger.error(f"Błąd wysyłania automatycznej odpowiedzi: {str(e)}")
            return False
    
    def trigger_flow(self, email_data: Dict[str, Any]):
        """Uruchamia przepływ na podstawie emaila."""
        flows_config = self.config["flows"]
        
        if not flows_config["trigger_flow_on_email"]:
            return
        
        try:
            # Określ, który przepływ uruchomić
            flow_key = None
            subject = email_data["subject"].lower()
            body = email_data["body"].lower()
            
            for key in flows_config["flow_mapping"]:
                if key in subject or key in body:
                    flow_key = key
                    break
            
            if not flow_key:
                logger.debug(f"Nie znaleziono pasującego przepływu dla emaila: {email_data['subject']}")
                return
            
            flow_file = flows_config["flow_mapping"][flow_key]
            
            # Przygotuj dane wejściowe dla przepływu
            input_data = {
                "email": {
                    "id": email_data["id"],
                    "subject": email_data["subject"],
                    "from": email_data["from"],
                    "to": email_data["to"],
                    "body": email_data["body"],
                    "date": email_data["date"]
                }
            }
            
            # Uruchom przepływ
            logger.info(f"Uruchamianie przepływu {flow_file} dla emaila: {email_data['subject']}")
            
            # Import tutaj, aby uniknąć cyklicznych importów
            from flow_dsl import run_flow_from_dsl, load_dsl
            
            # Sprawdź, czy plik przepływu istnieje
            flow_path = Path(__file__).parent / "dsl_definitions" / flow_file
            if not flow_path.exists():
                logger.error(f"Plik przepływu nie istnieje: {flow_path}")
                return
            
            # Załaduj i uruchom przepływ
            dsl_content = load_dsl(str(flow_path))
            result = run_flow_from_dsl(dsl_content, input_data)
            
            logger.info(f"Przepływ {flow_file} zakończony: {result}")
        
        except Exception as e:
            logger.error(f"Błąd uruchamiania przepływu: {str(e)}")
    
    def process_emails(self):
        """Przetwarza emaile."""
        try:
            # Pobierz emaile
            emails = self.fetch_emails()
            
            if not emails:
                logger.debug("Brak nowych emaili do przetworzenia.")
                return
            
            # Przetwórz każdy email
            for email_data in emails:
                # Sprawdź, czy należy automatycznie odpowiedzieć
                should_reply, template_key = self.should_auto_reply(email_data)
                
                if should_reply:
                    self.send_auto_reply(email_data, template_key)
                
                # Uruchom przepływ, jeśli skonfigurowano
                self.trigger_flow(email_data)
        
        except Exception as e:
            logger.error(f"Błąd przetwarzania emaili: {str(e)}")
        
        finally:
            # Rozłącz się z serwerami
            self.disconnect()

def run_email_processor():
    """Uruchamia procesor emaili w pętli."""
    processor = EmailProcessor()
    processing_config = processor.config["processing"]
    
    logger.info("Uruchomiono procesor emaili.")
    
    try:
        while True:
            processor.process_emails()
            time.sleep(processing_config["check_interval_seconds"])
    
    except KeyboardInterrupt:
        logger.info("Zatrzymano procesor emaili.")
    
    except Exception as e:
        logger.error(f"Błąd procesora emaili: {str(e)}")

if __name__ == "__main__":
    run_email_processor()
