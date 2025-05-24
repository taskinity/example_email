// app/core/RouteManager.groovy - zarządzanie routes
package app.core

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.util.XmlSlurper
import java.net.URL
import java.util.concurrent.*

class RouteManager {
    def logManager
    def executor = Executors.newFixedThreadPool(5)
    def routes = [:]
    def running = false

    RouteManager(logManager) {
        this.logManager = logManager
        initializeRoutes()
    }

    def initializeRoutes() {
        // HTTP Routes
        routes['http_get'] = new HttpGetRoute(this)
        routes['http_post'] = new HttpPostRoute(this)
        routes['http_monitor'] = new HttpMonitorRoute(this)

        // File Routes
        routes['file_process'] = new FileProcessRoute(this)
        routes['file_watch'] = new FileWatchRoute(this)

        // Data Routes
        routes['json_transform'] = new JsonTransformRoute(this)
        routes['csv_process'] = new CsvProcessRoute(this)
        routes['xml_parse'] = new XmlParseRoute(this)

        logManager.info("🔧 Routes initialized: ${routes.keySet().join(', ')}")
    }

    def executeRoute(String routeName, Map params = [:]) {
        if (!routes.containsKey(routeName)) {
            logManager.error("❌ Route not found: $routeName")
            return
        }

        def route = routes[routeName]
        logManager.info("🚀 Executing route: $routeName")

        executor.submit {
            try {
                route.execute(params)
            } catch (Exception e) {
                logManager.error("❌ Route $routeName failed: ${e.message}")
            }
        }
    }

    // Convenience methods for UI
    def executeHttpRoute() {
        executeRoute('http_get', [url: 'https://jsonplaceholder.typicode.com/posts/1'])
    }

    def httpGet(String url = 'https://jsonplaceholder.typicode.com/posts/1') {
        executeRoute('http_get', [url: url])
    }

    def httpPost(String url = 'https://httpbin.org/post', Map data = [:]) {
        executeRoute('http_post', [url: url, data: data])
    }

    def processFile(String filename = 'sample.json') {
        executeRoute('file_process', [filename: filename])
    }

    def stopAll() {
        running = false
        routes.each { name, route ->
            if (route.respondsTo('stop')) {
                route.stop()
            }
        }
        logManager.info("🛑 All routes stopped")
    }

    def reset() {
        stopAll()
        Thread.sleep(100)
        initializeRoutes()
        logManager.info("🔄 Routes reset")
    }

    def shutdown() {
        stopAll()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (InterruptedException e) {
            executor.shutdownNow()
        }
        logManager.info("✅ RouteManager shutdown complete")
    }

    def log(String message) {
        logManager.info(message)
    }

    def error(String message) {
        logManager.error(message)
    }
}

// HTTP GET Route
class HttpGetRoute {
    def routeManager

    HttpGetRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        def url = params.url ?: 'https://jsonplaceholder.typicode.com/posts/1'

        routeManager.log("🌐 HTTP GET: $url")

        def connection = new URL(url).openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Groovy Pipeline App")

        def responseCode = connection.responseCode
        def response = connection.inputStream.text

        routeManager.log("📨 HTTP $responseCode - Response received (${response.length()} chars)")

        if (response.startsWith('{') || response.startsWith('[')) {
            def json = new JsonSlurper().parseText(response)
            if (json instanceof Map) {
                routeManager.log("   📋 Title: ${json.title ?: 'N/A'}")
                routeManager.log("   👤 User: ${json.userId ?: json.user ?: 'N/A'}")
                if (json.body) {
                    def body = json.body.toString()
                    routeManager.log("   📄 Body: ${body.take(100)}${body.length() > 100 ? '...' : ''}")
                }
            }
        } else {
            routeManager.log("   📄 Response: ${response.take(200)}${response.length() > 200 ? '...' : ''}")
        }

        return [status: responseCode, data: response]
    }
}

// HTTP POST Route
class HttpPostRoute {
    def routeManager

    HttpPostRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        def url = params.url ?: 'https://httpbin.org/post'
        def data = params.data ?: [message: "Hello from Groovy", timestamp: new Date().time]

        routeManager.log("🌐 HTTP POST: $url")

        def connection = new URL(url).openConnection()
        connection.requestMethod = 'POST'
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", "Groovy Pipeline App")

        def jsonData = new JsonBuilder(data).toString()
        connection.outputStream.withWriter { writer ->
            writer.write(jsonData)
        }

        def responseCode = connection.responseCode
        def response = connection.inputStream.text

        routeManager.log("📤 POST data sent: ${jsonData}")
        routeManager.log("📨 HTTP $responseCode - Response received")

        if (response.startsWith('{')) {
            def json = new JsonSlurper().parseText(response)
            routeManager.log("   ✅ Server confirmed data receipt")
        }

        return [status: responseCode, data: response]
    }
}

// HTTP Monitor Route (periodic checks)
class HttpMonitorRoute {
    def routeManager
    def timer

    HttpMonitorRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        def url = params.url ?: 'https://httpbin.org/status/200'
        def interval = params.interval ?: 10000

        routeManager.log("📡 Starting HTTP monitor for: $url")
        routeManager.log("   ⏰ Check interval: ${interval}ms")

        timer = new Timer(true)
        timer.scheduleAtFixedRate(
            {
                try {
                    def connection = new URL(url).openConnection()
                    connection.connectTimeout = 3000
                    connection.readTimeout = 5000

                    def responseCode = connection.responseCode
                    def timestamp = new Date().format('HH:mm:ss')

                    if (responseCode == 200) {
                        routeManager.log("[$timestamp] ✅ $url - Status: $responseCode")
                    } else {
                        routeManager.log("[$timestamp] ⚠️ $url - Status: $responseCode")
                    }
                } catch (Exception e) {
                    def timestamp = new Date().format('HH:mm:ss')
                    routeManager.error("[$timestamp] ❌ $url - Error: ${e.message}")
                }
            } as TimerTask,
            0,
            interval
        )
    }

    def stop() {
        if (timer) {
            timer.cancel()
            routeManager.log("🛑 HTTP monitor stopped")
        }
    }
}

// File Processing Route
class FileProcessRoute {
    def routeManager

    FileProcessRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        def filename = params.filename ?: 'sample.json'

        routeManager.log("📁 Processing file: $filename")

        // Create sample file if it doesn't exist
        def file = new File(filename)
        if (!file.exists()) {
            def sampleData = [
                [id: 1, name: "Alice", department: "Engineering"],
                [id: 2, name: "Bob", department: "Marketing"],
                [id: 3, name: "Charlie", department: "Sales"]
            ]
            file.text = new JsonBuilder(sampleData).toPrettyString()
            routeManager.log("   📝 Created sample file: $filename")
        }

        def content = file.text
        routeManager.log("   📊 File size: ${content.length()} bytes")

        if (filename.endsWith('.json')) {
            def json = new JsonSlurper().parseText(content)
            routeManager.log("   📋 JSON records: ${json.size()}")

            json.each { record ->
                routeManager.log("     - ${record.name}: ${record.department}")
            }
        } else {
            def lines = content.split('\n')
            routeManager.log("   📄 Text lines: ${lines.length}")
        }

        return [filename: filename, size: content.length()]
    }
}

// File Watch Route
class FileWatchRoute {
    def routeManager
    def watching = false

    FileWatchRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        def directory = params.directory ?: '.'

        routeManager.log("👁️ Starting file watch on: $directory")
        watching = true

        // Simple file monitoring simulation
        Thread.start {
            def lastModified = [:]

            while (watching) {
                try {
                    new File(directory).eachFile { file ->
                        def currentModified = file.lastModified()
                        def previousModified = lastModified[file.name]

                        if (previousModified && currentModified > previousModified) {
                            routeManager.log("📝 File changed: ${file.name}")
                        }

                        lastModified[file.name] = currentModified
                    }

                    Thread.sleep(2000)
                } catch (Exception e) {
                    routeManager.error("👁️ File watch error: ${e.message}")
                }
            }
        }
    }

    def stop() {
        watching = false
        routeManager.log("🛑 File watch stopped")
    }
}

// JSON Transform Route
class JsonTransformRoute {
    def routeManager

    JsonTransformRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        routeManager.log("🔄 JSON transformation started")

        def inputData = params.data ?: [
            [name: "Alice", age: 30, city: "New York"],
            [name: "Bob", age: 25, city: "San Francisco"],
            [name: "Charlie", age: 35, city: "Chicago"]
        ]

        routeManager.log("   📥 Input records: ${inputData.size()}")

        def transformed = inputData.collect { record ->
            [
                fullName: record.name,
                ageGroup: record.age < 30 ? 'Young' : 'Experienced',
                location: record.city,
                processedAt: new Date().format('yyyy-MM-dd HH:mm:ss'),
                id: UUID.randomUUID().toString().take(8)
            ]
        }

        routeManager.log("   📤 Transformed records: ${transformed.size()}")
        transformed.each { record ->
            routeManager.log("     - ${record.fullName} (${record.ageGroup}) from ${record.location}")
        }

        return transformed
    }
}

// CSV Process Route
class CsvProcessRoute {
    def routeManager

    CsvProcessRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        routeManager.log("📊 CSV processing started")

        // Create sample CSV data
        def csvData = """name,age,department,salary
Alice,30,Engineering,75000
Bob,25,Marketing,65000
Charlie,35,Engineering,80000
Diana,28,Sales,70000"""

        def lines = csvData.split('\n')
        def headers = lines[0].split(',')

        routeManager.log("   📋 Headers: ${headers.join(', ')}")
        routeManager.log("   📊 Data rows: ${lines.length - 1}")

        def records = []
        lines[1..-1].each { line ->
            def values = line.split(',')
            def record = [:]
            headers.eachWithIndex { header, index ->
                record[header] = values[index]
            }
            records << record
        }

        // Process records
        def totalSalary = records.sum { Integer.parseInt(it.salary) }
        def avgSalary = totalSalary / records.size()

        routeManager.log("   💰 Total salary: \$${totalSalary}")
        routeManager.log("   📈 Average salary: \$${(int)avgSalary}")

        def byDepartment = records.groupBy { it.department }
        byDepartment.each { dept, employees ->
            routeManager.log("   🏢 ${dept}: ${employees.size()} employees")
        }

        return records
    }
}

// XML Parse Route
class XmlParseRoute {
    def routeManager

    XmlParseRoute(routeManager) {
        this.routeManager = routeManager
    }

    def execute(Map params) {
        routeManager.log("📄 XML parsing started")

        def xmlData = '''<?xml version="1.0" encoding="UTF-8"?>
<employees>
    <employee id="1">
        <name>Alice</name>
        <department>Engineering</department>
        <salary>75000</salary>
    </employee>
    <employee id="2">
        <name>Bob</name>
        <department>Marketing</department>
        <salary>65000</salary>
    </employee>
</employees>'''

        def xml = new XmlSlurper().parseText(xmlData)

        routeManager.log("   📊 Total employees: ${xml.employee.size()}")

        xml.employee.each { employee ->
            routeManager.log("   👤 ${employee.name.text()} (${employee.department.text()}) - \${employee.salary.text()}")
        }

        return xml
    }
}