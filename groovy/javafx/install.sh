#!/bin/bash

# setup-project.sh - Skrypt do utworzenia struktury projektu

echo "🏗️ Creating modular Groovy Pipeline App structure..."

# Create directory structure
mkdir -p groovy-pipeline-app/{app/{core,ui,routes,pipelines},config,logs,data,scripts}

echo "📁 Directory structure created:"
tree groovy-pipeline-app/ 2>/dev/null || find groovy-pipeline-app -type d

# Create main application files
cat > groovy-pipeline-app/App.groovy << 'EOF'
// App.groovy - główny plik aplikacji
package app

import app.ui.MainWindow
import app.core.PipelineManager
import app.core.RouteManager
import app.core.LogManager

class App {
    static void main(String[] args) {
        println "🚀 Starting Groovy Pipeline Desktop App..."

        // Initialize core components
        def logManager = new LogManager()
        def routeManager = new RouteManager(logManager)
        def pipelineManager = new PipelineManager(logManager)

        // Create and show main window
        def mainWindow = new MainWindow(logManager, routeManager, pipelineManager)
        mainWindow.show()

        // Setup shutdown hook
        addShutdownHook {
            println "\n🛑 Shutting down application..."
            pipelineManager.shutdown()
            routeManager.shutdown()
            println "✅ Application closed cleanly"
        }

        logManager.info("🎉 Application initialized successfully")
        println "✅ App launched! Check the GUI window."
    }
}
EOF

# Create package structure indicator files
touch groovy-pipeline-app/app/core/.gitkeep
touch groovy-pipeline-app/app/ui/.gitkeep
touch groovy-pipeline-app/app/routes/.gitkeep
touch groovy-pipeline-app/app/pipelines/.gitkeep

# Create build.gradle for dependency management
cat > groovy-pipeline-app/build.gradle << 'EOF'
plugins {
    id 'groovy'
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.apache.groovy:groovy-all:4.0.24'
    implementation 'org.apache.groovy:groovy-swing:4.0.24'
    implementation 'org.apache.groovy:groovy-json:4.0.24'
    implementation 'org.apache.groovy:groovy-xml:4.0.24'

    // Optional: HTTP client
    implementation 'org.apache.httpcomponents:httpclient:4.5.14'

    // Optional: JSON processing
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
}

application {
    mainClass = 'app.App'
}

// Task to run with Groovy directly
task runGroovy(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = 'groovy.ui.GroovyMain'
    args = ['App.groovy']
}

// Package task
task packageApp(type: Zip) {
    from '.'
    include '**/*.groovy'
    include 'build.gradle'
    include 'README.md'
    archiveFileName = 'groovy-pipeline-app.zip'
    destinationDirectory = file('dist')
}
EOF

# Create configuration files
cat > groovy-pipeline-app/config/app.properties << 'EOF'
# Application Configuration
app.name=Groovy Pipeline App
app.version=2.0
app.description=Modular desktop pipeline application

# Logging Configuration
log.level=INFO
log.file.enabled=true
log.console.enabled=true

# Pipeline Configuration
pipeline.thread.pool.size=4
pipeline.max.concurrent=10

# Route Configuration
route.thread.pool.size=5
route.timeout.ms=30000

# UI Configuration
ui.theme=dark
ui.window.width=1000
ui.window.height=800
EOF

cat > groovy-pipeline-app/config/routes.json << 'EOF'
{
  "routes": {
    "http_endpoints": [
      {
        "name": "jsonplaceholder",
        "url": "https://jsonplaceholder.typicode.com/posts/1",
        "method": "GET",
        "timeout": 5000
      },
      {
        "name": "httpbin_post",
        "url": "https://httpbin.org/post",
        "method": "POST",
        "timeout": 10000