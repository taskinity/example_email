
# Install

1. **Zapisz skrypt** do pliku, np. `install1.sh`
2. **Nadaj uprawnienia wykonywalne**:
   ```bash
   chmod +x install1.sh
   ```
3. **Uruchom skrypt**:
   ```bash
   ./install1.sh
   ```
4. **Przeładuj środowisko**:
   ```bash
   source ~/.bashrc
   ```

## Co robi skrypt:

- **Aktualizuje system** Fedory
- **Instaluje OpenJDK 17** (wymagane dla wszystkich narzędzi)
- **Pobiera najnowsze wersje** wszystkich narzędzi z oficjalnych źródeł
- **Instaluje w katalogu** `~/tools/`
- **Konfiguruje zmienne środowiskowe** (JAVA_HOME, CAMEL_HOME, GROOVY_HOME)
- **Dodaje do PATH** wszystkie binaria
- **Tworzy przykładowe pliki** konfiguracyjne

## Po instalacji będziesz mógł:

- **Uruchomić Hawtio**: `hawtio` (interfejs webowy na http://localhost:8080/hawtio)
- **Używać Groovy**: `groovy --version` lub `groovy hello.groovy`
- **Uruchamiać Camel**: `camel run examples/simple-route.xml`

