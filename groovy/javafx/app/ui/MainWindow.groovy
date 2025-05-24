// app/ui/MainWindow.groovy - layout głównego okna
package app.ui

import groovy.swing.SwingBuilder
import java.awt.*
import java.awt.event.*
import javax.swing.*

class MainWindow {
    def frame
    def outputArea
    def statusLabel
    def logManager
    def routeManager
    def pipelineManager
    def swingBuilder

    MainWindow(logManager, routeManager, pipelineManager) {
        this.logManager = logManager
        this.routeManager = routeManager
        this.pipelineManager = pipelineManager
        this.swingBuilder = new SwingBuilder()

        // Connect log manager to GUI
        logManager.setOutputArea { message -> appendToOutput(message) }
        logManager.setStatusUpdater { status -> updateStatus(status) }

        createGUI()
    }

    def createGUI() {
        SwingUtilities.invokeLater {
            swingBuilder.edt {
                frame = frame(
                    title: 'Groovy Pipeline Desktop App v2.0',
                    size: [1000, 800],
                    defaultCloseOperation: JFrame.EXIT_ON_CLOSE
                ) {
                    borderLayout()

                    // Menu bar
                    menuBar {
                        menu(text: 'File') {
                            menuItem(text: 'New Pipeline', actionPerformed: { newPipeline() })
                            menuItem(text: 'Load Routes', actionPerformed: { loadRoutes() })
                            separator()
                            menuItem(text: 'Exit', actionPerformed: { System.exit(0) })
                        }
                        menu(text: 'Pipeline') {
                            menuItem(text: 'Start All', actionPerformed: { startAllPipelines() })
                            menuItem(text: 'Stop All', actionPerformed: { stopAllPipelines() })
                            menuItem(text: 'Reset', actionPerformed: { resetPipelines() })
                        }
                        menu(text: 'Routes') {
                            menuItem(text: 'HTTP Routes', actionPerformed: { showHttpRoutes() })
                            menuItem(text: 'Data Routes', actionPerformed: { showDataRoutes() })
                            menuItem(text: 'Custom Routes', actionPerformed: { showCustomRoutes() })
                        }
                        menu(text: 'Help') {
                            menuItem(text: 'About', actionPerformed: { showAbout() })
                        }
                    }

                    // Toolbar
                    toolBar(constraints: BorderLayout.NORTH) {
                        button(text: '🚀 Start Pipeline',
                               tooltip: 'Start main data pipeline',
                               actionPerformed: { pipelineManager.startMainPipeline() })

                        button(text: '🌐 HTTP Request',
                               tooltip: 'Execute HTTP route',
                               actionPerformed: { routeManager.executeHttpRoute() })

                        button(text: '⚙️ Process Data',
                               tooltip: 'Run data processing',
                               actionPerformed: { pipelineManager.processData() })

                        button(text: '📊 Analytics',
                               tooltip: 'Run analytics pipeline',
                               actionPerformed: { pipelineManager.runAnalytics() })

                        separator()

                        button(text: '🛑 Stop All',
                               tooltip: 'Stop all processes',
                               actionPerformed: { stopAllPipelines() })

                        button(text: '🗑️ Clear Log',
                               tooltip: 'Clear output log',
                               actionPerformed: { clearOutput() })
                    }

                    // Main content area
                    splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT,
                             constraints: BorderLayout.CENTER,
                             dividerLocation: 250) {

                        // Left panel - Route/Pipeline tree
                        scrollPane {
                            tree(id: 'routeTree',
                                 rootVisible: true,
                                 showsRootHandles: true) {
                                createRouteTree()
                            }
                        }

                        // Right panel - Output and controls
                        splitPane(orientation: JSplitPane.VERTICAL_SPLIT,
                                 dividerLocation: 500) {

                            // Top right - Output area
                            scrollPane {
                                outputArea = textArea(
                                    rows: 30, columns: 80,
                                    font: new Font(Font.MONOSPACED, Font.PLAIN, 11),
                                    background: Color.BLACK,
                                    foreground: Color.GREEN,
                                    editable: false
                                )
                            }

                            // Bottom right - Control panel
                            panel {
                                borderLayout()

                                // Route controls
                                panel(constraints: BorderLayout.NORTH) {
                                    gridLayout(rows: 2, cols: 3, hgap: 5, vgap: 5)

                                    button(text: 'HTTP GET', actionPerformed: { routeManager.httpGet() })
                                    button(text: 'HTTP POST', actionPerformed: { routeManager.httpPost() })
                                    button(text: 'File Process', actionPerformed: { routeManager.processFile() })

                                    button(text: 'JSON Transform', actionPerformed: { pipelineManager.jsonTransform() })
                                    button(text: 'Batch Process', actionPerformed: { pipelineManager.batchProcess() })
                                    button(text: 'Stream Data', actionPerformed: { pipelineManager.streamData() })
                                }

                                // Configuration panel
                                panel(constraints: BorderLayout.CENTER) {
                                    borderLayout()

                                    // Config inputs
                                    panel(constraints: BorderLayout.NORTH) {
                                        gridBagLayout()

                                        label(text: 'URL:', constraints: gbc(gridx: 0, gridy: 0))
                                        textField(id: 'urlField',
                                                text: 'https://jsonplaceholder.typicode.com/posts/1',
                                                columns: 30,
                                                constraints: gbc(gridx: 1, gridy: 0, fill: GridBagConstraints.HORIZONTAL))

                                        label(text: 'Interval (ms):', constraints: gbc(gridx: 0, gridy: 1))
                                        textField(id: 'intervalField',
                                                text: '3000',
                                                columns: 10,
                                                constraints: gbc(gridx: 1, gridy: 1))

                                        label(text: 'Batch Size:', constraints: gbc(gridx: 0, gridy: 2))
                                        textField(id: 'batchSizeField',
                                                text: '100',
                                                columns: 10,
                                                constraints: gbc(gridx: 1, gridy: 2))
                                    }
                                }
                            }
                        }
                    }

                    // Status bar
                    panel(constraints: BorderLayout.SOUTH) {
                        borderLayout()

                        statusLabel = label(
                            text: 'Ready',
                            foreground: Color.BLUE,
                            constraints: BorderLayout.WEST
                        )

                        panel(constraints: BorderLayout.CENTER) {
                            flowLayout(alignment: FlowLayout.CENTER)

                            label(text: "Running: ")
                            label(id: 'runningCountLabel', text: '0', foreground: Color.GREEN)
                            label(text: " | Errors: ")
                            label(id: 'errorCountLabel', text: '0', foreground: Color.RED)
                            label(text: " | Processed: ")
                            label(id: 'processedCountLabel', text: '0', foreground: Color.BLUE)
                        }

                        label(
                            text: "Groovy ${GroovySystem.version} | Java ${System.getProperty('java.version')}",
                            constraints: BorderLayout.EAST
                        )
                    }
                }
            }

            frame.setLocationRelativeTo(null)
            logManager.info("🎨 GUI components initialized")
        }
    }

    def createRouteTree() {
        // Tree structure for routes and pipelines
        def root = swingBuilder.node('Pipeline App')

        def routesNode = swingBuilder.node(root, 'Routes')
        swingBuilder.node(routesNode, 'HTTP Routes')
        swingBuilder.node(routesNode, 'File Routes')
        swingBuilder.node(routesNode, 'Data Routes')
        swingBuilder.node(routesNode, 'Custom Routes')

        def pipelinesNode = swingBuilder.node(root, 'Pipelines')
        swingBuilder.node(pipelinesNode, 'Main Pipeline')
        swingBuilder.node(pipelinesNode, 'Analytics Pipeline')
        swingBuilder.node(pipelinesNode, 'ETL Pipeline')
        swingBuilder.node(pipelinesNode, 'Monitoring Pipeline')

        def configNode = swingBuilder.node(root, 'Configuration')
        swingBuilder.node(configNode, 'Endpoints')
        swingBuilder.node(configNode, 'Schedules')
        swingBuilder.node(configNode, 'Error Handling')
    }

    def show() {
        SwingUtilities.invokeLater {
            frame.visible = true
        }
    }

    def appendToOutput(String message) {
        SwingUtilities.invokeLater {
            if (outputArea) {
                outputArea.append("$message\n")
                outputArea.caretPosition = outputArea.document.length
            }
        }
    }

    def updateStatus(String status) {
        SwingUtilities.invokeLater {
            if (statusLabel) {
                statusLabel.text = status
            }
        }
    }

    def clearOutput() {
        SwingUtilities.invokeLater {
            if (outputArea) {
                outputArea.text = ''
                logManager.info("📋 Log cleared")
            }
        }
    }

    // Menu actions
    def newPipeline() {
        logManager.info("📝 Creating new pipeline...")
        // TODO: Open new pipeline dialog
    }

    def loadRoutes() {
        logManager.info("📂 Loading routes from file...")
        // TODO: File chooser for routes
    }

    def startAllPipelines() {
        logManager.info("🚀 Starting all pipelines...")
        pipelineManager.startAll()
    }

    def stopAllPipelines() {
        logManager.info("🛑 Stopping all pipelines...")
        pipelineManager.stopAll()
        routeManager.stopAll()
    }

    def resetPipelines() {
        logManager.info("🔄 Resetting all pipelines...")
        pipelineManager.reset()
        routeManager.reset()
    }

    def showHttpRoutes() {
        logManager.info("🌐 Showing HTTP routes configuration")
        // TODO: Show HTTP routes dialog
    }

    def showDataRoutes() {
        logManager.info("📊 Showing data routes configuration")
        // TODO: Show data routes dialog
    }

    def showCustomRoutes() {
        logManager.info("⚙️ Showing custom routes configuration")
        // TODO: Show custom routes dialog
    }

    def showAbout() {
        JOptionPane.showMessageDialog(
            frame,
            """Groovy Pipeline Desktop App v2.0

Built with:
• Groovy ${GroovySystem.version}
• Java ${System.getProperty('java.version')}
• Swing GUI

Features:
• Modular architecture
• HTTP routes
• Data pipelines
• Real-time monitoring""",
            "About",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}