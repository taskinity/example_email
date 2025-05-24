// fixed-camel-app.groovy - naprawiona wersja bez błędów
@Grab('org.apache.camel:camel-core:3.21.0')
@Grab('org.apache.camel:camel-http:3.21.0')
@Grab('org.slf4j:slf4j-simple:1.7.36')

import groovy.swing.SwingBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.Processor
import org.apache.camel.Exchange
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Color
import javax.swing.*

class DesktopCamelApp {
    // Declare GUI components as instance variables
    def frame
    def outputArea
    def statusLabel
    def context
    def swingBuilder

    def start() {
        println "🐪 Initializing Camel Desktop App..."

        // Initialize Camel context first
        context = new DefaultCamelContext()

        // Create GUI using SwingBuilder
        swingBuilder = new SwingBuilder()

        // Build GUI on EDT
        SwingUtilities.invokeLater {
            createGUI()
            setupCamelRoutes()
            logMessage("=== Camel Desktop App Started ===")
            logMessage("Ready to run routes...")
            println "✓ GUI created and ready"
        }
    }

    def createGUI() {
        swingBuilder.edt {
            frame = frame(title: 'Camel Desktop App', size: [900, 700],
                         defaultCloseOperation: JFrame.EXIT_ON_CLOSE, show: true) {

                borderLayout()

                // Top panel with buttons
                panel(constraints: BorderLayout.NORTH) {
                    flowLayout()
                    button('Start Timer Route', actionPerformed: { startTimerRoute() })
                    button('HTTP Route', actionPerformed: { startHttpRoute() })
                    button('Stop All', actionPerformed: { stopRoutes() })
                    button('Clear Log', actionPerformed: { clearLog() })
                    button('Test GUI', actionPerformed: { testGUI() })
                }

                // Center - log output area
                scrollPane(constraints: BorderLayout.CENTER) {
                    outputArea = textArea(rows: 30, columns: 80,
                                        font: new Font(Font.MONOSPACED, Font.PLAIN, 11),
                                        background: Color.BLACK,
                                        foreground: Color.GREEN,
                                        editable: false)
                }

                // Bottom - status panel
                panel(constraints: BorderLayout.SOUTH) {
                    borderLayout()
                    statusLabel = label(text: 'Initializing...',
                                       constraints: BorderLayout.WEST,
                                       foreground: Color.BLUE)
                    label(text: "Camel 3.21.0 | Groovy ${GroovySystem.version}",
                          constraints: BorderLayout.EAST)
                }
            }
        }

        // Update status after GUI is created
        SwingUtilities.invokeLater {
            updateStatus("Camel Context Ready")
        }
    }

    def setupCamelRoutes() {
        // Custom processor to log to GUI
        def guiLogger = [
            process: { Exchange exchange ->
                def message = exchange.in.getBody(String.class)
                logMessage("ROUTE: ${message}")
            }
        ] as Processor

        // Register processor with Camel
        context.registry.bind("guiLogger", guiLogger)

        logMessage("✓ Camel processors registered")
    }

    def startTimerRoute() {
        logMessage("🕐 Starting timer route...")

        try {
            context.addRoutes(new RouteBuilder() {
                void configure() {
                    from("timer://myTimer?period=3000")
                        .setBody { "Hello from Camel Timer! Time: ${new Date().format('HH:mm:ss')}" }
                        .to("bean:guiLogger")
                        .routeId("timerRoute")
                }
            })

            if (!context.started) {
                context.start()
                logMessage("✓ Camel context started")
            }

            updateStatus("Timer route running")
            logMessage("✓ Timer route activated - messages every 3 seconds")

        } catch (Exception e) {
            logMessage("❌ ERROR starting timer route: ${e.message}")
            updateStatus("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    def startHttpRoute() {
        logMessage("🌐 Starting HTTP route...")

        try {
            context.addRoutes(new RouteBuilder() {
                void configure() {
                    from("timer://httpTimer?period=10000")
                        .setHeader("CamelHttpMethod", constant("GET"))
                        .to("https://jsonplaceholder.typicode.com/posts/1")
                        .process { exchange ->
                            def body = exchange.in.getBody(String.class)
                            def shortBody = body.length() > 100 ?
                                body.substring(0, 100) + "..." : body
                            logMessage("📡 HTTP Response received: ${shortBody}")
                        }
                        .routeId("httpRoute")
                }
            })

            if (!context.started) {
                context.start()
                logMessage("✓ Camel context started")
            }

            updateStatus("HTTP route running")
            logMessage("✓ HTTP route activated - fetching data every 10 seconds")

        } catch (Exception e) {
            logMessage("❌ ERROR starting HTTP route: ${e.message}")
            updateStatus("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    def stopRoutes() {
        logMessage("🛑 Stopping all routes...")

        try {
            if (context.started) {
                context.stop()
                logMessage("✓ Camel context stopped")
            }

            updateStatus("All routes stopped")
            logMessage("✓ All routes stopped successfully")

            // Recreate context for next use
            context = new DefaultCamelContext()
            setupCamelRoutes()

        } catch (Exception e) {
            logMessage("❌ ERROR stopping routes: ${e.message}")
            e.printStackTrace()
        }
    }

    def clearLog() {
        if (outputArea) {
            SwingUtilities.invokeLater {
                outputArea.text = ""
                logMessage("📋 Log cleared")
            }
        }
    }

    def testGUI() {
        logMessage("🧪 Testing GUI components...")
        updateStatus("Running GUI test...")

        // Test threading
        Thread.start {
            for (int i = 1; i <= 5; i++) {
                logMessage("Test message ${i}/5")
                Thread.sleep(500)
            }
            updateStatus("GUI test completed")
            logMessage("✅ GUI test finished successfully")
        }
    }

    def logMessage(String message) {
        if (outputArea) {
            SwingUtilities.invokeLater {
                def timestamp = new Date().format('HH:mm:ss.SSS')
                outputArea.append("[${timestamp}] ${message}\n")
                outputArea.caretPosition = outputArea.document.length
            }
        } else {
            println "LOG (GUI not ready): $message"
        }
    }

    def updateStatus(String status) {
        if (statusLabel) {
            SwingUtilities.invokeLater {
                statusLabel.text = status
            }
        } else {
            println "STATUS (GUI not ready): $status"
        }
    }
}

// Add shutdown hook for clean exit
addShutdownHook {
    println "\n🛑 Shutting down Camel Desktop App..."
}

// Start the application
println "🚀 Launching Camel Desktop App..."
new DesktopCamelApp().start()

// Keep the script alive
println "✓ App launched. Check GUI window."
println "Press Ctrl+C to exit."

// Simple keep-alive loop
while (true) {
    Thread.sleep(1000)
}