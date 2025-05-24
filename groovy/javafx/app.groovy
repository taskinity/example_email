// no-grab-app.groovy - bez zewnętrznych zależności
import groovy.swing.SwingBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.awt.*
import java.awt.event.*
import javax.swing.*
import java.net.URL
import java.util.concurrent.*

class PipelineApp {
    def frame
    def outputArea
    def statusLabel
    def executor = Executors.newFixedThreadPool(3)
    def running = false
    
    def start() {
        createGUI()
        logMessage("=== Pipeline App Started ===")
        logMessage("Pure Groovy + Swing - no external dependencies!")
    }
    
    def createGUI() {
        SwingUtilities.invokeLater {
            frame = new JFrame("Groovy Pipeline Desktop App")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setSize(900, 700)
            frame.layout = new BorderLayout()
            
            // Top panel
            def topPanel = new JPanel(new FlowLayout())
            topPanel.add(new JButton("Start Pipeline").with { 
                addActionListener({ startPipeline() } as ActionListener)
                return it
            })
            topPanel.add(new JButton("HTTP Request").with {
                addActionListener({ makeHttpRequest() } as ActionListener)
                return it
            })
            topPanel.add(new JButton("Process Data").with {
                addActionListener({ processData() } as ActionListener)
                return it
            })
            topPanel.add(new JButton("Stop All").with {
                addActionListener({ stopPipeline() } as ActionListener)
                return it
            })
            topPanel.add(new JButton("Clear").with {
                addActionListener({ outputArea.text = '' } as ActionListener)
                return it
            })
            
            frame.add(topPanel, BorderLayout.NORTH)
            
            // Center - output
            outputArea = new JTextArea(35, 80)
            outputArea.font = new Font(Font.MONOSPACED, Font.PLAIN, 11)
            outputArea.editable = false
            outputArea.background = Color.BLACK
            outputArea.foreground = Color.GREEN
            
            def scrollPane = new JScrollPane(outputArea)
            frame.add(scrollPane, BorderLayout.CENTER)
            
            // Bottom - status
            def bottomPanel = new JPanel(new BorderLayout())
            statusLabel = new JLabel("Ready")
            statusLabel.foreground = Color.BLUE
            bottomPanel.add(statusLabel, BorderLayout.WEST)
            bottomPanel.add(new JLabel("Groovy ${GroovySystem.version} | Java ${System.getProperty('java.version')}"), BorderLayout.EAST)
            frame.add(bottomPanel, BorderLayout.SOUTH)
            
            frame.setLocationRelativeTo(null)
            frame.visible = true
        }
    }
    
    def logMessage(String message) {
        SwingUtilities.invokeLater {
            def timestamp = new Date().format('HH:mm:ss.SSS')
            outputArea.append("[$timestamp] $message\n")
            outputArea.caretPosition = outputArea.document.length
        }
    }
    
    def updateStatus(String status) {
        SwingUtilities.invokeLater {
            statusLabel.text = status
        }
    }
    
    def startPipeline() {
        if (running) {
            logMessage("Pipeline already running!")
            return
        }
        
        running = true
        updateStatus("Pipeline running...")
        logMessage("🚀 Starting data pipeline...")
        
        // Simulate pipeline with multiple steps
        executor.submit {
            try {
                for (int i = 1; i <= 10; i++) {
                    if (!running) break
                    
                    logMessage("📊 Processing batch $i/10...")
                    
                    // Simulate work
                    Thread.sleep(1000)
                    
                    // Generate some data
                    def data = [
                        batch: i,
                        timestamp: new Date().time,
                        records: (int)(Math.random() * 1000),
                        status: "processed",
                        duration: "${(int)(Math.random() * 500)}ms"
                    ]
                    
                    def json = new JsonBuilder(data)
                    logMessage("📋 Batch $i: ${json.toString()}")
                    
                    updateStatus("Processed batch $i/10")
                }
                
                logMessage("✅ Pipeline completed successfully!")
                updateStatus("Pipeline completed")
                
            } catch (Exception e) {
                logMessage("❌ Pipeline error: ${e.message}")
                updateStatus("Pipeline failed")
            } finally {
                running = false
            }
        }
    }
    
    def makeHttpRequest() {
        logMessage("🌐 Making HTTP request...")
        updateStatus("HTTP request in progress...")
        
        executor.submit {
            try {
                def url = "https://jsonplaceholder.typicode.com/posts/1"
                logMessage("📡 GET $url")
                
                def connection = new URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                
                def response = connection.inputStream.text
                def json = new JsonSlurper().parseText(response)
                
                logMessage("📨 Response received:")
                logMessage("   Title: ${json.title}")
                logMessage("   User ID: ${json.userId}")
                logMessage("   Body: ${json.body.take(100)}...")
                
                updateStatus("HTTP request completed")
                
            } catch (Exception e) {
                logMessage("❌ HTTP error: ${e.message}")
                updateStatus("HTTP request failed")
            }
        }
    }
    
    def processData() {
        logMessage("⚙️ Processing data transformation...")
        updateStatus("Data processing...")
        
        executor.submit {
            try {
                // Simulate data processing pipeline
                def steps = [
                    "🔍 Data validation",
                    "🔄 Data transformation", 
                    "📊 Data aggregation",
                    "💾 Data persistence",
                    "📈 Generate metrics"
                ]
                
                steps.each { step ->
                    logMessage("   $step")
                    Thread.sleep(800)
                    
                    // Simulate some processing result
                    def result = [
                        step: step,
                        recordsProcessed: (int)(Math.random() * 500),
                        errors: (int)(Math.random() * 5),
                        duration: "${(int)(Math.random() * 200)}ms"
                    ]
                    
                    logMessage("     Result: ${new JsonBuilder(result).toString()}")
                }
                
                logMessage("✅ Data processing completed!")
                updateStatus("Data processing completed")
                
            } catch (Exception e) {
                logMessage("❌ Processing error: ${e.message}")
                updateStatus("Processing failed")
            }
        }
    }
    
    def stopPipeline() {
        running = false
        logMessage("🛑 Stopping all processes...")
        updateStatus("Stopping...")
        
        Thread.start {
            Thread.sleep(1000)
            SwingUtilities.invokeLater {
                updateStatus("Stopped")
                logMessage("✅ All processes stopped")
            }
        }
    }
}

// Start the application
new PipelineApp().start()

println "Groovy Pipeline App launched!"
println "Check the GUI window for pipeline operations."