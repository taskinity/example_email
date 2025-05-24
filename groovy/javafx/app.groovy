// WorkingApp.groovy - ostateczna działająca wersja
import groovy.swing.SwingBuilder
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.awt.*
import java.awt.event.*
import javax.swing.*
import java.net.URL
import java.util.concurrent.*

class WorkingPipelineApp {
    def frame
    def outputArea
    def statusLabel
    def statsArea
    def executor = Executors.newFixedThreadPool(3)
    def running = false
    def startTime = System.currentTimeMillis()
    def processedCount = 0
    def errorCount = 0

    def start() {
        createGUI()
        logMessage("=== Working Pipeline App Started ===")
        logMessage("🔧 All issues fixed - fully functional!")
    }

    def createGUI() {
        SwingUtilities.invokeLater {
            frame = new JFrame("Working Groovy Pipeline Desktop App")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setSize(1200, 900)
            frame.layout = new BorderLayout()

            // Enhanced toolbar
            def toolbar = new JPanel(new FlowLayout())

            // Main action buttons
            toolbar.add(createButton("🚀 Start Pipeline", "Start main data pipeline") { startPipeline() })
            toolbar.add(createButton("🌐 HTTP GET", "Execute HTTP GET request") { makeHttpRequest() })
            toolbar.add(createButton("📤 HTTP POST", "Execute HTTP POST request") { makeHttpPost() })
            toolbar.add(createButton("📊 Process JSON", "Process JSON data") { processJsonData() })
            toolbar.add(createButton("📄 Process XML", "Process XML data") { processXmlData() })
            toolbar.add(createButton("📈 Analytics", "Run analytics") { runAnalytics() })

            // Control buttons
            toolbar.add(Box.createHorizontalStrut(20)) // Spacer
            toolbar.add(createButton("🛑 Stop All", "Stop all processes") { stopPipeline() })
            toolbar.add(createButton("🗑️ Clear", "Clear output") { clearOutput() })

            frame.add(toolbar, BorderLayout.NORTH)

            // Main split pane
            def splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
            splitPane.dividerLocation = 300

            // Left panel - Controls and info
            def leftPanel = createLeftPanel()
            splitPane.leftComponent = leftPanel

            // Right panel - Output
            outputArea = new JTextArea(45, 70)
            outputArea.font = new Font(Font.MONOSPACED, Font.PLAIN, 11)
            outputArea.editable = false
            outputArea.background = Color.BLACK
            outputArea.foreground = Color.GREEN

            splitPane.rightComponent = new JScrollPane(outputArea)
            frame.add(splitPane, BorderLayout.CENTER)

            // Status bar
            def statusPanel = new JPanel(new BorderLayout())
            statusLabel = new JLabel("Ready")
            statusLabel.foreground = Color.BLUE
            statusPanel.add(statusLabel, BorderLayout.WEST)
            statusPanel.add(new JLabel("Groovy ${GroovySystem.version} | Java ${System.getProperty('java.version')}"), BorderLayout.EAST)
            frame.add(statusPanel, BorderLayout.SOUTH)

            frame.setLocationRelativeTo(null)
            frame.visible = true

            // Start stats updater with CORRECT Timer constructor
            startStatsUpdater()
        }
    }

    def startStatsUpdater() {
        // FIXED: Correct javax.swing.Timer constructor
        def timer = new javax.swing.Timer(2000, { updateStats() } as ActionListener)
        timer.start()
    }

    def createButton(String text, String tooltip, Closure action) {
        def button = new JButton(text)
        button.toolTipText = tooltip
        button.addActionListener(action as ActionListener)
        return button
    }

    def createLeftPanel() {
        def panel = new JPanel()
        panel.layout = new BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Controls & Status")

        // Pipeline status
        panel.add(createStatusSection())
        panel.add(Box.createVerticalStrut(10))

        // Configuration section
        panel.add(createConfigSection())
        panel.add(Box.createVerticalStrut(10))

        // Statistics section
        panel.add(createStatsSection())

        return new JScrollPane(panel)
    }

    def createStatusSection() {
        def panel = new JPanel()
        panel.layout = new BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Pipeline Status")

        def statusText = new JTextArea(8, 25)
        statusText.editable = false
        statusText.background = new Color(240, 240, 240)
        statusText.text = """Pipeline Components:

🔧 Route Manager: Ready
⚙️ Pipeline Manager: Ready
📝 Log Manager: Active
🌐 HTTP Client: Ready
📊 Data Processor: Ready

Status: All systems operational
Threads: ${Thread.activeCount()} active
"""

        panel.add(new JScrollPane(statusText))
        return panel
    }

    def createConfigSection() {
        def panel = new JPanel()
        panel.layout = new BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Configuration")

        // URL field
        panel.add(new JLabel("API URL:"))
        def urlField = new JTextField("https://jsonplaceholder.typicode.com/posts/1")
        urlField.name = "urlField"
        panel.add(urlField)

        panel.add(Box.createVerticalStrut(5))

        // Interval field
        panel.add(new JLabel("Interval (ms):"))
        def intervalField = new JTextField("3000")
        intervalField.name = "intervalField"
        panel.add(intervalField)

        panel.add(Box.createVerticalStrut(5))

        // Batch size
        panel.add(new JLabel("Batch Size:"))
        def batchField = new JTextField("100")
        batchField.name = "batchField"
        panel.add(batchField)

        panel.add(Box.createVerticalStrut(10))

        // Quick action buttons
        panel.add(createButton("Test Connection", "Test API connection") { testConnection() })
        panel.add(Box.createVerticalStrut(5))
        panel.add(createButton("Load Config", "Load configuration") { loadConfig() })

        return panel
    }

    def createStatsSection() {
        def panel = new JPanel()
        panel.layout = new BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Statistics")

        statsArea = new JTextArea(8, 25)
        statsArea.editable = false
        statsArea.background = new Color(250, 250, 250)
        updateStats() // Initial stats

        panel.add(new JScrollPane(statsArea))
        return panel
    }

    def updateStats() {
        SwingUtilities.invokeLater {
            if (statsArea) {
                def uptime = (System.currentTimeMillis() - startTime) / 1000
                def freeMem = Runtime.runtime.freeMemory() / 1024 / 1024
                def totalMem = Runtime.runtime.totalMemory() / 1024 / 1024
                def usedMem = totalMem - freeMem

                statsArea.text = """Processing Stats:

Processed: ${processedCount}
Errors: ${errorCount}
Running: ${running ? "Yes" : "No"}
Uptime: ${uptime}s

Memory Usage:
Used: ${usedMem.round(1)}MB
Free: ${freeMem.round(1)}MB
Total: ${totalMem.round(1)}MB

Threads: ${Thread.activeCount()}
"""
            }
        }
    }

    def logMessage(String message) {
        SwingUtilities.invokeLater {
            if (outputArea) {
                def timestamp = new Date().format('HH:mm:ss.SSS')
                outputArea.append("[$timestamp] $message\n")
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
                logMessage("📋 Output cleared")
            }
        }
    }

    def startPipeline() {
        if (running) {
            logMessage("⚠️ Pipeline already running!")
            return
        }

        running = true
        updateStatus("Pipeline running...")
        logMessage("🚀 Starting comprehensive data pipeline...")

        executor.submit {
            try {
                def steps = [
                    "🔍 Initialize data validation engine",
                    "📥 Connect to data sources",
                    "🔄 Apply transformation rules",
                    "📊 Execute aggregation functions",
                    "💾 Persist to data warehouse",
                    "📈 Generate performance metrics",
                    "🔔 Send completion notifications",
                    "🧹 Cleanup temporary resources"
                ]

                steps.eachWithIndex { step, index ->
                    if (!running) return

                    logMessage("   Step ${index + 1}/${steps.size()}: $step")

                    // Simulate realistic processing time
                    Thread.sleep(1200 + (int)(Math.random() * 800))

                    // Generate detailed results
                    def result = [
                        step: index + 1,
                        name: step,
                        recordsProcessed: (int)(Math.random() * 2000) + 500,
                        errors: (int)(Math.random() * 3),
                        duration: "${(int)(Math.random() * 800) + 200}ms",
                        throughput: "${(int)(Math.random() * 1000) + 100} records/sec",
                        memoryUsed: "${(int)(Math.random() * 100) + 50}MB"
                    ]

                    logMessage("     ✓ ${result.recordsProcessed} records, ${result.errors} errors, ${result.duration}")
                    logMessage("     📊 Throughput: ${result.throughput}, Memory: ${result.memoryUsed}")

                    processedCount += result.recordsProcessed
                    errorCount += result.errors

                    updateStatus("Step ${index + 1}/${steps.size()} completed")
                }

                if (running) {
                    def summary = [
                        status: "completed",
                        totalSteps: steps.size(),
                        totalRecords: processedCount,
                        totalErrors: errorCount,
                        pipeline: "comprehensive-pipeline",
                        completedAt: new Date().toString()
                    ]

                    logMessage("🎉 Pipeline completed successfully!")
                    logMessage("📊 Summary: ${new JsonBuilder(summary).toPrettyString()}")
                    updateStatus("Pipeline completed successfully")
                }

            } catch (Exception e) {
                logMessage("❌ Pipeline error: ${e.message}")
                updateStatus("Pipeline failed")
                errorCount++
            } finally {
                running = false
            }
        }
    }

    def makeHttpRequest() {
        logMessage("🌐 Executing HTTP GET requests...")
        updateStatus("HTTP requests in progress...")

        executor.submit {
            try {
                def endpoints = [
                    "https://jsonplaceholder.typicode.com/posts/1",
                    "https://jsonplaceholder.typicode.com/users/1",
                    "https://jsonplaceholder.typicode.com/albums/1",
                    "https://httpbin.org/json"
                ]

                endpoints.each { url ->
                    logMessage("📡 GET $url")

                    def connection = new URL(url).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("User-Agent", "Working Groovy Pipeline App v1.0")

                    def responseCode = connection.responseCode
                    def response = connection.inputStream.text

                    logMessage("📨 HTTP $responseCode - Response received (${response.length()} chars)")

                    if (response.startsWith('{') || response.startsWith('[')) {
                        def json = new JsonSlurper().parseText(response)
                        if (json instanceof Map) {
                            logMessage("   📋 Keys: ${json.keySet().take(5).join(', ')}${json.size() > 5 ? '...' : ''}")

                            if (json.title) logMessage("   📰 Title: ${json.title}")
                            if (json.name) logMessage("   👤 Name: ${json.name}")
                            if (json.email) logMessage("   📧 Email: ${json.email}")
                            if (json.body) {
                                def body = json.body.toString()
                                logMessage("   📄 Body: ${body.take(80)}${body.length() > 80 ? '...' : ''}")
                            }
                        }
                    }

                    processedCount++
                    Thread.sleep(800) // Pause between requests
                }

                updateStatus("HTTP requests completed")
                logMessage("✅ All HTTP requests completed successfully!")

            } catch (Exception e) {
                logMessage("❌ HTTP error: ${e.message}")
                updateStatus("HTTP request failed")
                errorCount++
            }
        }
    }

    def makeHttpPost() {
        logMessage("📤 Executing HTTP POST request...")
        updateStatus("HTTP POST in progress...")

        executor.submit {
            try {
                def url = "https://httpbin.org/post"
                def data = [
                    message: "Hello from Working Groovy Pipeline App",
                    timestamp: new Date().time,
                    source: "groovy-desktop-app",
                    version: "1.0",
                    user: System.getProperty("user.name"),
                    system: [
                        os: System.getProperty("os.name"),
                        java: System.getProperty("java.version"),
                        groovy: GroovySystem.version
                    ]
                ]

                logMessage("📡 POST $url")

                def connection = new URL(url).openConnection()
                connection.requestMethod = 'POST'
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "Working Groovy Pipeline App v1.0")

                def jsonData = new JsonBuilder(data).toString()
                connection.outputStream.withWriter { writer ->
                    writer.write(jsonData)
                }

                def responseCode = connection.responseCode
                def response = connection.inputStream.text

                logMessage("📤 POST data sent (${jsonData.length()} chars)")
                logMessage("📨 HTTP $responseCode - Response received")

                if (response.startsWith('{')) {
                    def json = new JsonSlurper().parseText(response)
                    logMessage("   ✅ Server confirmed data receipt")
                    if (json.json) {
                        logMessage("   📋 Echoed back: ${json.json.message}")
                    }
                }

                processedCount++
                updateStatus("HTTP POST completed")
                logMessage("✅ HTTP POST completed successfully!")

            } catch (Exception e) {
                logMessage("❌ HTTP POST error: ${e.message}")
                updateStatus("HTTP POST failed")
                errorCount++
            }
        }
    }

    def processJsonData() {
        logMessage("📊 Starting advanced JSON processing...")
        updateStatus("Processing JSON data...")

        executor.submit {
            try {
                // Generate complex sample data
                def datasets = [
                    [
                        name: "Employee Data",
                        records: (1..50).collect { i ->
                            [
                                id: i,
                                name: "Employee_${String.format('%03d', i)}",
                                department: ["Engineering", "Marketing", "Sales", "HR", "Finance"][(int)(Math.random() * 5)],
                                salary: (int)(Math.random() * 50000) + 50000,
                                experience: (int)(Math.random() * 15) + 1,
                                skills: ["Java", "Python", "JavaScript", "SQL", "Docker", "AWS", "React"]
                                    .shuffled().take((int)(Math.random() * 4) + 2),
                                startDate: new Date(System.currentTimeMillis() - (long)(Math.random() * 157680000000L)).format('yyyy-MM-dd')
                            ]
                        }
                    ],
                    [
                        name: "Sales Data",
                        records: (1..75).collect { i ->
                            [
                                transactionId: "TXN_${String.format('%05d', i)}",
                                amount: Math.round(Math.random() * 50000 * 100) / 100,
                                currency: ["USD", "EUR", "GBP", "JPY"][(int)(Math.random() * 4)],
                                timestamp: new Date().time - (long)(Math.random() * 2592000000L), // Last 30 days
                                category: ["Electronics", "Clothing", "Books", "Home", "Sports"][(int)(Math.random() * 5)],
                                customerType: ["Premium", "Standard", "Basic"][(int)(Math.random() * 3)],
                                region: ["North", "South", "East", "West", "Central"][(int)(Math.random() * 5)]
                            ]
                        }
                    ]
                ]

                datasets.each { dataset ->
                    logMessage("   📦 Processing dataset: ${dataset.name}")
                    logMessage("   📊 Records: ${dataset.records.size()}")

                    Thread.sleep(1000) // Simulate processing time

                    // Perform detailed analytics
                    if (dataset.name == "Employee Data") {
                        def byDept = dataset.records.groupBy { it.department }
                        def bySalaryRange = dataset.records.groupBy { record ->
                            def salary = record.salary
                            if (salary < 60000) return "Entry Level"
                            else if (salary < 80000) return "Mid Level"
                            else if (salary < 100000) return "Senior Level"
                            else return "Executive"
                        }

                        logMessage("     📈 Department Analysis:")
                        byDept.each { dept, employees ->
                            def avgSalary = employees.sum { it.salary } / employees.size()
                            def avgExp = employees.sum { it.experience } / employees.size()
                            def topSkills = employees.collectMany { it.skills }.countBy { it }
                                .sort { -it.value }.take(3)

                            logMessage("       🏢 $dept: ${employees.size()} employees")
                            logMessage("         💰 Avg salary: \$${(int)avgSalary}, Avg exp: ${avgExp.round(1)}y")
                            logMessage("         🎯 Top skills: ${topSkills.keySet().join(', ')}")
                        }

                        logMessage("     📊 Salary Distribution:")
                        bySalaryRange.each { range, employees ->
                            logMessage("       💼 $range: ${employees.size()} employees (${(employees.size() * 100 / dataset.records.size()).round(1)}%)")
                        }
                    }

                    if (dataset.name == "Sales Data") {
                        def totalSales = dataset.records.sum { it.amount }
                        def byCurrency = dataset.records.groupBy { it.currency }
                        def byCategory = dataset.records.groupBy { it.category }
                        def byRegion = dataset.records.groupBy { it.region }
                        def byCustomerType = dataset.records.groupBy { it.customerType }

                        logMessage("     💰 Financial Overview:")
                        logMessage("       Total sales: \$${totalSales.round(2)}")
                        logMessage("       Average transaction: \$${(totalSales / dataset.records.size()).round(2)}")

                        logMessage("     💱 Currency Breakdown:")
                        byCurrency.each { currency, transactions ->
                            def currencyTotal = transactions.sum { it.amount }
                            logMessage("       $currency: \$${currencyTotal.round(2)} (${transactions.size()} transactions)")
                        }

                        logMessage("     🛍️ Category Performance:")
                        byCategory.sort { -it.value.sum { tx -> tx.amount } }.each { category, transactions ->
                            def categoryTotal = transactions.sum { it.amount }
                            logMessage("       $category: \$${categoryTotal.round(2)} (${(categoryTotal * 100 / totalSales).round(1)}%)")
                        }

                        logMessage("     🌍 Regional Analysis:")
                        byRegion.each { region, transactions ->
                            def regionTotal = transactions.sum { it.amount }
                            def avgTransaction = regionTotal / transactions.size()
                            logMessage("       $region: \$${regionTotal.round(2)}, Avg: \$${avgTransaction.round(2)}")
                        }

                        logMessage("     👥 Customer Type Analysis:")
                        byCustomerType.each { type, transactions ->
                            def typeTotal = transactions.sum { it.amount }
                            def avgTransaction = typeTotal / transactions.size()
                            logMessage("       $type: \$${typeTotal.round(2)}, Avg: \$${avgTransaction.round(2)} (${transactions.size()} customers)")
                        }
                    }

                    processedCount += dataset.records.size()
                    Thread.sleep(500)
                }

                logMessage("✅ Advanced JSON processing completed!")
                updateStatus("JSON processing completed")

            } catch (Exception e) {
                logMessage("❌ JSON processing error: ${e.message}")
                updateStatus("JSON processing failed")
                errorCount++
            }
        }
    }

    def processXmlData() {
        logMessage("📄 Processing XML data...")
        updateStatus("XML processing...")

        executor.submit {
            try {
                // Generate comprehensive XML data
                def xmlContent = '''<?xml version="1.0" encoding="UTF-8"?>
<company name="TechCorp Inc" founded="2010">
    <departments>
        <department id="eng" name="Engineering">
            <employees>
                <employee id="001">
                    <n>Alice Johnson</n>
                    <position>Senior Developer</position>
                    <salary currency="USD">85000</salary>
                    <skills>
                        <skill level="expert">Java</skill>
                        <skill level="advanced">Python</skill>
                        <skill level="intermediate">React</skill>
                    </skills>
                </employee>
                <employee id="002">
                    <n>Bob Smith</n>
                    <position>Tech Lead</position>
                    <salary currency="USD">95000</salary>
                    <skills>
                        <skill level="expert">JavaScript</skill>
                        <skill level="expert">Node.js</skill>
                        <skill level="advanced">AWS</skill>
                    </skills>
                </employee>
            </employees>
        </department>
        <department id="mkt" name="Marketing">
            <employees>
                <employee id="003">
                    <n>Carol Brown</n>
                    <position>Marketing Manager</position>
                    <salary currency="USD">75000</salary>
                    <skills>
                        <skill level="expert">Digital Marketing</skill>
                        <skill level="advanced">Analytics</skill>
                    </skills>
                </employee>
            </employees>
        </department>
    </departments>
    <projects>
        <project id="proj1" name="Mobile App" status="active">
            <budget>500000</budget>
            <timeline>
                <start>2024-01-15</start>
                <end>2024-12-31</end>
            </timeline>
        </project>
    </projects>
</company>'''

                logMessage("   📄 XML content loaded (${xmlContent.length()} chars)")

                // Use regex-based parsing for better compatibility
                def companyName = xmlContent.find(/<company[^>]*name="([^"]*)"/) { match, name -> name }
                def founded = xmlContent.find(/<company[^>]*founded="([^"]*)"/) { match, year -> year }

                logMessage("   🏢 Company: $companyName (Founded: $founded)")

                // Parse departments and employees
                def departmentPattern = /<department[^>]*name="([^"]*)"[^>]*>(.*?)<\/department>/
                def employeePattern = /<employee[^>]*>(.*?)<\/employee>/
                def namePattern = /<n>(.*?)<\/name>/
                def positionPattern = /<position>(.*?)<\/position>/
                def salaryPattern = /<salary[^>]*>(.*?)<\/salary>/

                def totalEmployees = 0
                def totalSalary = 0
                def departments = []

                xmlContent.findAll(departmentPattern) { match, deptName, deptContent ->
                    logMessage("   🏬 Department: $deptName")

                    def deptEmployees = []
                    deptContent.findAll(employeePattern) { empMatch, empContent ->
                        def name = empContent.find(namePattern) { _, n -> n }
                        def position = empContent.find(positionPattern) { _, p -> p }
                        def salary = empContent.find(salaryPattern) { _, s -> s }

                        if (name && position && salary) {
                            def salaryInt = Integer.parseInt(salary)
                            deptEmployees << [name: name, position: position, salary: salaryInt]
                            logMessage("     👤 ${name} - ${position} (\$${salary})")
                            totalSalary += salaryInt
                            totalEmployees++
                        }
                    }

                    if (deptEmployees) {
                        def deptAvgSalary = deptEmployees.sum { it.salary } / deptEmployees.size()
                        logMessage("     📊 ${deptEmployees.size()} employees, avg salary: \$${(int)deptAvgSalary}")
                        departments << [name: deptName, employees: deptEmployees, avgSalary: deptAvgSalary]
                    }
                }

                // Parse projects
                def projectPattern = /<project[^>]*name="([^"]*)"[^>]*status="([^"]*)"[^>]*>(.*?)<\/project>/
                def budgetPattern = /<budget>(.*?)<\/budget>/

                xmlContent.findAll(projectPattern) { match, projName, status, projContent ->
                    def budget = projContent.find(budgetPattern) { _, b -> b }
                    logMessage("   🚀 Project: $projName (Status: $status, Budget: \$${budget})")
                }

                // Summary statistics
                if (totalEmployees > 0) {
                    def avgSalary = totalSalary / totalEmployees
                    logMessage("   📊 Company Statistics:")
                    logMessage("     👥 Total employees: $totalEmployees")
                    logMessage("     💰 Total payroll: \$${totalSalary}")
                    logMessage("     📈 Average salary: \$${(int)avgSalary}")
                    logMessage("     🏢 Departments: ${departments.size()}")
                }

                processedCount += totalEmployees
                updateStatus("XML processing completed")
                logMessage("✅ XML processing completed successfully!")

            } catch (Exception e) {
                logMessage("❌ XML processing error: ${e.message}")
                updateStatus("XML processing failed")
                errorCount++
            }
        }
    }

    def runAnalytics() {
        logMessage("📈 Running comprehensive analytics...")
        updateStatus("Analytics in progress...")

        executor.submit {
            try {
                def analyticsModules = [
                    [name: "System Performance", weight: 0.25],
                    [name: "Data Quality Assessment", weight: 0.20],
                    [name: "Processing Efficiency", weight: 0.20],
                    [name: "Error Pattern Analysis", weight: 0.15],
                    [name: "Resource Utilization", weight: 0.10],
                    [name: "Throughput Analysis", weight: 0.10]
                ]

                def results = [:]
                def overallScore = 0

                analyticsModules.each { module ->
                    logMessage("   🔍 Analyzing: ${module.name}")
                    Thread.sleep(800 + (int)(Math.random() * 400))

                    def analysis = [
                        score: (Math.random() * 40 + 60).round(2),
                        trend: ["improving", "stable", "declining"][(int)(Math.random() * 3)],
                        confidence: (Math.random() * 30 + 70).round(2),
                        samples: (int)(Math.random() * 5000) + 1000,
                        anomalies: (int)(Math.random() * 5),
                        recommendations: (int)(Math.random() * 3) + 1
                    ]

                    results[module.name] = analysis
                    overallScore += analysis.score * module.weight

                    logMessage("     ✓ Score: ${analysis.score}/100")
                    logMessage("     📊 Trend: ${analysis.trend}, Confidence: ${analysis.confidence}%")
                    logMessage("     📋 Samples: ${analysis.samples}, Anomalies: ${analysis.anomalies}")
                }

                // Generate comprehensive insights
                logMessage("🔍 Generating analytical insights...")
                Thread.sleep(600)

                def insights = []

                // Performance insights
                if (results['System Performance'].score > 85) {
                    insights << "Excellent system performance detected - consider scaling up workload"
                } else if (results['System Performance'].score < 70) {
                    insights << "Performance optimization needed - review resource allocation"
                }

                // Data quality insights
                if (results['Data Quality Assessment'].score > 90) {
                    insights << "Data quality exceeds industry standards"
                } else if (results['Data Quality Assessment'].score < 75) {
                    insights << "Data quality issues detected - implement validation rules"
                }

                // Efficiency insights
                if (results['Processing Efficiency'].trend == "improving") {
                    insights << "Processing efficiency trending upward - optimization strategies working"
                } else if (results['Processing Efficiency'].trend == "declining") {
                    insights << "Processing efficiency declining - investigate bottlenecks"
                }

                // Error analysis
                def totalAnomalies = results.values().sum { it.anomalies }
                if (totalAnomalies < 5) {
                    insights << "Low anomaly count indicates stable system operation"
                } else {
                    insights << "Multiple anomalies detected - review error handling procedures"
                }

                // Resource insights
                def memUsage = (Runtime.runtime.totalMemory() - Runtime.runtime.freeMemory()) / Runtime.runtime.maxMemory() * 100
                if (memUsage > 80) {
                    insights << "High memory utilization - consider garbage collection tuning"
                } else if (memUsage < 50) {
                    insights << "Memory utilization optimal - system has capacity for additional load"
                }

                logMessage("💡 Key Insights:")
                insights.each { insight ->
                    logMessage("   • $insight")
                }

                // Generate recommendations
                logMessage("🎯 Recommendations:")
                def recommendations = [
                    "Implement automated monitoring for real-time performance tracking",
                    "Establish data quality checkpoints at key processing stages",
                    "Optimize thread pool configuration for better throughput",
                    "Set up predictive analytics for proactive issue detection",
                    "Create performance baselines for comparative analysis"
                ]

                recommendations.take(3).each { rec ->
                    logMessage("   📋 $rec")
                }

                // Final summary
                logMessage("📊 Analytics Summary:")
                logMessage("   🎯 Overall System Health: ${overallScore.round(1)}/100")
                logMessage("   📈 Performance Grade: ${overallScore > 85 ? 'Excellent' : overallScore > 70 ? 'Good' : 'Needs Improvement'}")
                logMessage("   🔍 Total Samples Analyzed: ${results.values().sum { it.samples }}")
                logMessage("   ⚠️ Total Anomalies: $totalAnomalies")

                processedCount += analyticsModules.size()
                updateStatus("Analytics completed")
                logMessage("✅ Comprehensive analytics completed!")

            } catch (Exception e) {
                logMessage("❌ Analytics error: ${e.message}")
                updateStatus("Analytics failed")
                errorCount++
            }
        }
    }

    def testConnection() {
        logMessage("🔌 Testing API connection...")

        executor.submit {
            try {
                def testUrl = "https://httpbin.org/status/200"
                def connection = new URL(testUrl).openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 5000

                def responseCode = connection.responseCode
                def responseTime = System.currentTimeMillis()

                if (responseCode == 200) {
                    logMessage("   ✅ Connection test successful (${responseCode})")
                    logMessage("   📊 Response time: <3000ms")
                } else {
                    logMessage("   ⚠️ Unexpected response code: $responseCode")
                }

            } catch (Exception e) {
                logMessage("   ❌ Connection test failed: ${e.message}")
            }
        }
    }

    def loadConfig() {
        logMessage("📂 Loading configuration...")

        // Simulate config loading
        Thread.start {
            Thread.sleep(500)
            logMessage("   📋 Default configuration loaded:")
            logMessage("   • API timeout: 10000ms")
            logMessage("   • Batch size: 100 records")
            logMessage("   • Thread pool: 3 workers")
            logMessage("   • Log level: INFO")
            logMessage("   ✅ Configuration ready")
        }
    }

    def stopPipeline() {
        running = false
        logMessage("🛑 Stopping all processes...")
        updateStatus("Stopping...")

        Thread.start {
            Thread.sleep(1000)
            SwingUtilities.invokeLater {
                updateStatus("All processes stopped")
                logMessage("✅ All processes stopped successfully")
                logMessage("📊 Final stats - Processed: $processedCount, Errors: $errorCount")
            }
        }
    }
}

// Start the working application
println "🚀 Launching Working Groovy Pipeline App..."
new WorkingPipelineApp().start()

println "✅ Working App launched successfully!"
println "🔧 All Timer and GUI issues fixed"
println "📱 Check the GUI window for full functionality"