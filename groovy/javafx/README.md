## **📁 Struktura projektu:**
```
groovy-pipeline-app/
├── App.groovy                 # 🚀 Główny punkt wejścia
├── app/
│   ├── core/                  # 🔧 Logika biznesowa
│   │   ├── LogManager.groovy      # 📝 Zarządzanie logami
│   │   ├── RouteManager.groovy    # 🌐 Zarządzanie routes
│   │   └── PipelineManager.groovy # ⚙️ Zarządzanie pipelines
│   └── ui/                    # 🎨 Interfejs użytkownika
│       └── MainWindow.groovy      # 🖥️ Główne okno
├── config/                    # ⚙️ Konfiguracja
├── data/                      # 📊 Przykładowe dane
├── logs/                      # 📋 Logi aplikacji
└── scripts/                   # 🔨 Skrypty pomocnicze
```

### **🚀 Jak uruchomić:**

1. **Uruchom skrypt struktury:**
```bash
chmod +x setup-project.sh
./setup-project.sh
```

2. **Zapisz pliki Groovy** (z poprzednich artefaktów):
   - `LogManager.groovy`
   - `RouteManager.groovy` 
   - `PipelineManager.groovy`
   - `MainWindow.groovy`

3. **Skopiuj pliki do struktur:**
```bash
./copy-files.sh
```

4. **Uruchom aplikację:**
```bash
cd groovy-pipeline-app
./scripts/run.sh
```

### **✨ Funkcjonalności:**

**🔧 Modularność:**
- Oddzielne managery dla różnych funkcji
- Łatwe dodawanie nowych routes i pipelines
- Konfiguracja przez pliki JSON/YAML

**🎨 Zaawansowane GUI:**
- Menu bar z opcjami
- Toolbar z szybkimi akcjami
- Tree view dla nawigacji
- Split panes dla organizacji
- Status bar z metrykami

**⚙️ Pipeline Types:**
- Main Data Pipeline
- Analytics Pipeline  
- ETL Pipeline
- Monitoring Pipeline
- Batch Processing
- Stream Data

**🌐 Route Types:**
- HTTP GET/POST
- File Processing
- JSON Transform
- CSV Processing
- XML Parsing
- Monitoring

**📝 Zaawansowane logowanie:**
- Structured logging
- File output
- Real-time GUI updates
- Performance metrics
- Error tracking

### **🎯 Zalety tej struktury:**

✅ **Łatwa rozbudowa** - dodaj nowy pipeline/route w jednym pliku
✅ **Czytelny kod** - każda funkcjonalność w osobnym pliku  
✅ **Konfigurowalność** - wszystko przez pliki config
✅ **Professional** - pełne GUI z menu, toolbar, statusbar
✅ **Monitoring** - real-time logi i metryki
✅ **Threading** - concurrent execution
✅ **Error handling** - proper exception management

