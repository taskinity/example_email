#!/bin/bash

# quick-run.sh - Szybkie uruchomienie aplikacji pipeline

echo "🚀 Quick Launch - Groovy Pipeline App"
echo "====================================="

# Check if we're in the right directory
if [ ! -f "SingleApp.groovy" ]; then
    echo "📁 Creating SingleApp.groovy..."

    # Here you would save the SingleApp.groovy content
    echo "❌ Please save the SingleApp.groovy content first"
    echo ""
    echo "💡 To fix the package structure issue:"
    echo "   1. Save the 'Single File App' artifact as SingleApp.groovy"
    echo "   2. Run: groovy SingleApp.groovy"
    echo ""
    echo "🔧 Alternative solutions:"
    echo ""
    echo "OPTION 1: Single file approach (RECOMMENDED)"
    echo "   groovy SingleApp.groovy"
    echo ""
    echo "OPTION 2: Fix existing structure"
    echo "   mkdir -p app/core app/ui"
    echo "   # Move files to correct locations:"
    echo "   # LogManager.groovy → app/core/"
    echo "   # RouteManager.groovy → app/core/"
    echo "   # PipelineManager.groovy → app/core/"
    echo "   # MainWindow.groovy → app/ui/"
    echo ""
    echo "OPTION 3: Remove package declarations"
    echo "   # Remove all 'package app.core' and 'package app.ui' lines"
    echo "   # Remove all import statements for local classes"
    echo ""
    exit 1
fi

# Check Java and Groovy
echo "🔍 Checking environment..."

if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 11+"
    exit 1
fi

if ! command -v groovy &> /dev/null; then
    echo "❌ Groovy not found. Please install Groovy"
    exit 1
fi

echo "✓ Java: $(java -version 2>&1 | head -n 1)"
echo "✓ Groovy: $(groovy --version 2>&1 | head -n 1)"

# Run the application
echo ""
echo "🎬 Launching Pipeline App..."
echo "📱 GUI window should appear shortly..."

groovy SingleApp.groovy

echo ""
echo "✅ Application ended."