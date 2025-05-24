// app/core/PipelineManager.groovy - zarządzanie pipelines
package app.core

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.util.concurrent.*

class PipelineManager {
    def logManager
    def executor = Executors.newFixedThreadPool(4)
    def pipelines = [:]
    def running = [:]
    def stats = [processed: 0, errors: 0, running: 0]

    PipelineManager(logManager) {
        this.logManager = logManager
        initializePipelines()
    }

    def initializePipelines() {
        // Core pipelines
        pipelines['main'] = new MainDataPipeline(this)
        pipelines['analytics'] = new AnalyticsPipeline(this)
        pipelines['etl'] = new ETLPipeline(this)
        pipelines['monitoring'] = new MonitoringPipeline(this)
        pipelines['batch'] = new BatchProcessingPipeline(this)
        pipelines['stream'] = new StreamDataPipeline(this)

        logManager.info("🔧 Pipelines initialized: ${pipelines.keySet().join(', ')}")
    }

    def executePipeline(String pipelineName, Map params = [:]) {
        if (!pipelines.containsKey(pipelineName)) {
            logManager.error("❌ Pipeline not found: $pipelineName")
            return
        }

        if (running[pipelineName]) {
            logManager.warning("⚠️ Pipeline $pipelineName already running")
            return
        }

        def pipeline = pipelines[pipelineName]
        logManager.info("🚀 Starting pipeline: $pipelineName")

        running[pipelineName] = true
        stats.running++

        executor.submit {
            try {
                pipeline.execute(params)
                logManager.info("✅ Pipeline $pipelineName completed")
                stats.processed++
            } catch (Exception e) {
                logManager.error("❌ Pipeline $pipelineName failed: ${e.message}")
                stats.errors++
            } finally {
                running[pipelineName] = false
                stats.running--
            }
        }
    }

    // Convenience methods for UI
    def startMainPipeline() {
        executePipeline('main')
    }

    def processData() {
        executePipeline('etl', [batchSize: 50])
    }

    def runAnalytics() {
        executePipeline('analytics')
    }

    def jsonTransform() {
        executePipeline('etl', [operation: 'transform'])
    }

    def batchProcess() {
        executePipeline('batch', [size: 100])
    }

    def streamData() {
        executePipeline('stream', [duration: 30])
    }

    def startAll() {
        logManager.info("🚀 Starting all pipelines...")
        pipelines.keySet().each { name ->
            if (!running[name]) {
                executePipeline(name)
                Thread.sleep(500) // Stagger starts
            }
        }
    }

    def stopAll() {
        logManager.info("🛑 Stopping all pipelines...")
        running.keySet().each { name ->
            if (running[name]) {
                pipelines[name].stop()
                running[name] = false
            }
        }
        stats.running = 0
    }

    def reset() {
        stopAll()
        Thread.sleep(100)
        stats = [processed: 0, errors: 0, running: 0]
        initializePipelines()
        logManager.info("🔄 Pipelines reset")
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
        logManager.info("✅ PipelineManager shutdown complete")
    }

    def getStats() {
        return stats.clone()
    }

    def log(String message) {
        logManager.info(message)
    }

    def error(String message) {
        logManager.error(message)
    }
}

// Main Data Pipeline
class MainDataPipeline {
    def pipelineManager
    def running = false

    MainDataPipeline(pipelineManager) {
        this.pipelineManager = pipelineManager
    }

    def execute(Map params = [:]) {
        running = true
        pipelineManager.log("🔄 Main Data Pipeline started")

        def steps = [
            "🔍 Data validation",
            "📥 Data ingestion",
            "🔄 Data transformation",
            "📊 Data aggregation",
            "💾 Data persistence",
            "📈 Metrics generation",
            "🔔 Notifications"
        ]

        steps.eachWithIndex { step, index ->
            if (!running) return

            pipelineManager.log("   Step ${index + 1}/${steps.size()}: $step")

            // Simulate processing time
            Thread.sleep(800 + (int)(Math.random() * 400))

            // Generate step results
            def result = [
                step: index + 1,
                name: step,
                recordsProcessed: (int)(Math.random() * 1000) + 100,
                errors: (int)(Math.random() * 3),
                duration: "${(int)(Math.random() * 500) + 200}ms",
                timestamp: new Date().time
            ]

            pipelineManager.log("     ✓ ${result.recordsProcessed} records, ${result.errors} errors, ${result.duration}")
        }

        if (running) {
            def summary = [
                status: "completed",
                totalSteps: steps.size(),
                totalRecords: (int)(Math.random() * 5000) + 1000,
                totalErrors: (int)(Math.random() * 10),
                pipeline: "main",
                completedAt: new Date().toString()
            ]

            pipelineManager.log("📊 Pipeline Summary: ${new JsonBuilder(summary).toString()}")
        }

        running = false
    }

    def stop() {
        running = false
        pipelineManager.log("🛑 Main Data Pipeline stopped")
    }
}

// Analytics Pipeline
class AnalyticsPipeline {
    def pipelineManager
    def running = false

    AnalyticsPipeline(pipelineManager) {
        this.pipelineManager = pipelineManager
    }

    def execute(Map params = [:]) {
        running = true
        pipelineManager.log("📊 Analytics Pipeline started")

        // Generate sample analytics data
        def datasets = [
            "User Behavior",
            "Sales Performance",
            "System Metrics",
            "Error Patterns",
            "Performance Trends"
        ]

        def results = [:]

        datasets.each { dataset ->
            if (!running) return

            pipelineManager.log("   📈 Analyzing: $dataset")
            Thread.sleep(600)

            def metrics = [
                mean: Math.random() * 100,
                median: Math.random() * 100,
                stdDev: Math.random() * 20,
                trend: Math.random() > 0.5 ? "increasing" : "decreasing",
                confidence: (Math.random() * 40 + 60).round(2)
            ]

            results[dataset] = metrics
            pipelineManager.log("     ✓ Mean: ${metrics.mean.round(2)}, Trend: ${metrics.trend}, Confidence: ${metrics.confidence}%")
        }

        // Generate insights
        if (running) {
            pipelineManager.log("🔍 Generating insights...")
            Thread.sleep(400)

            def insights = [
                "User engagement increased by 15%",
                "Peak performance at 14:00-16:00",
                "Error rate below threshold (0.2%)",
                "Revenue trending upward",
                "System efficiency: 94.3%"
            ]

            insights.each { insight ->
                pipelineManager.log("   💡 $insight")
            }
        }

        running = false
    }

    def stop() {
        running = false
        pipelineManager.log("🛑 Analytics Pipeline stopped")
    }
}

// ETL Pipeline
class ETLPipeline {
    def pipelineManager
    def running = false

    ETLPipeline(pipelineManager) {
        this.pipelineManager = pipelineManager
    }

    def execute(Map params = [:]) {
        running = true
        def operation = params.operation ?: 'full'
        def batchSize = params.batchSize ?: 100

        pipelineManager.log("🔄 ETL Pipeline started (${operation}, batch: ${batchSize})")

        // Extract phase
        if (running) {
            pipelineManager.log("   📥 EXTRACT: Reading from data sources...")
            Thread.sleep(500)

            def sources = ["Database", "API", "Files", "Streams"]
            sources.each { source ->
                if (!running) return
                def records = (int)(Math.random() * 1000) + 100
                pipelineManager.log("     📋 $source: $records records extracted")
                Thread.sleep(200)
            }
        }

        // Transform phase
        if (running) {
            pipelineManager.log("   🔄 TRANSFORM: Processing data...")
            Thread.sleep(600)

            def transformations = [
                "Data cleaning",
                "Format standardization",
                "Field mapping",
                "Validation rules",
                "Enrichment"
            ]

            transformations.each { transform ->
                if (!running) return
                pipelineManager.log("     ⚙️ $transform")
                Thread.sleep(150)
            }
        }

        // Load phase
        if (running) {
            pipelineManager.log("   💾 LOAD: Writing to destinations...")
            Thread.sleep(400)

            def destinations = ["Data Warehouse", "Search Index", "Cache", "Reports"]
            destinations.each { dest ->
                if (!running) return
                def loaded = (int)(Math.random() * 800) + 200
                pipelineManager.log("     📤 $dest: $loaded records loaded")
                Thread.sleep(200)
            }
        }

        running = false
    }

    def stop() {
        running = false
        pipelineManager.log("🛑 ETL Pipeline stopped")
    }
}

// Monitoring Pipeline
class MonitoringPipeline {
    def pipelineManager
    def running = false

    MonitoringPipeline(pipelineManager) {
        this.pipelineManager = pipelineManager
    }

    def execute(Map params = [:]) {
        running = true
        pipelineManager.log("📡 Monitoring Pipeline started")

        def duration = params.duration ?: 20 // seconds
        def interval = params.interval ?: 2000 // ms

        pipelineManager.log("   ⏰ Monitoring for ${duration}s, interval: ${interval}ms")

        def startTime = System.currentTimeMillis()
        def endTime = startTime + (duration * 1000)

        while (running && System.currentTimeMillis() < endTime) {
            def timestamp = new Date().format('HH:mm:ss')

            // Simulate system metrics
            def metrics = [
                cpu: (Math.random() * 100).round(1),
                memory: (Math.random() * 100).round(1),
                disk: (Math.random() * 100).round(1),
                network: (Math.random() * 1000).round(0),
                errors: (int)(Math.random() * 5)
            ]

            def status = metrics.cpu > 80 || metrics.memory > 85 ? "⚠️" : "✅"
            pipelineManager.log("   [$timestamp] $status CPU: ${metrics.cpu}%, MEM: ${metrics.memory}%, NET: ${metrics.network}KB/s")

            if (metrics.errors > 0) {
                pipelineManager.log("   [$timestamp] 🚨 ${metrics.errors} errors detected")
            }

            try {
                Thread.sleep(interval)
            } catch (InterruptedException e) {
                break
            }
        }

        running = false
        pipelineManager.log("📡 Monitoring Pipeline completed")
    }

    def stop() {
        running = false
        pipelineManager.log("🛑 Monitoring Pipeline stopped")
    }
}

// Batch Processing Pipeline
class BatchProcessingPipeline {
    def pipelineManager
    def running = false

    BatchProcessingPipeline(pipelineManager) {
        this.pipelineManager = pipelineManager
    }

    def execute(Map params = [:]) {
        running = true
        def batchSize = params.size ?: 100
        def totalRecords = params.total ?: 1000

        pipelineManager.log("📦 Batch Processing Pipeline started")
        pipelineManager.log("   📊 Batch size: $batchSize, Total records: $totalRecords")

        def batches = (totalRecords / batchSize).toInteger() + 1
        def processed = 0

        for (int i = 1; i <= batches && running; i++) {
            def currentBatchSize = Math.min(batchSize, totalRecords - processed)
            if (currentBatchSize <= 0) break

            pipelineManager.log("   📦 Processing batch $i/$batches ($currentBatchSize records)")

            // Simulate batch processing
            Thread.sleep(500 + (int)(Math.random() * 300))

            def batchResult = [
                batchId: i,
                records: currentBatchSize,
                processed: (int)(currentBatchSize * (0.95 + Math.random() * 0.05)),
                errors: (int)(currentBatchSize * Math.random() * 0.02),
                duration: "${(int)(Math.random() * 500) + 200}ms"
            ]

            processed += currentBatchSize

            pipelineManager.log("     ✓ Batch $i: ${batchResult.processed}/${batchResult.records} processed, ${batchResult.errors} errors")
        }

        if (running) {
            pipelineManager.log("📊 Batch processing completed: $processed/$totalRecords records")
        }

        running = false
    }

    def stop() {
        running = false
        pipelineManager.log("🛑 Batch Processing Pipeline stopped")
    }
}

// Stream Data Pipeline
class StreamDataPipeline {
    def pipelineManager
    def running = false

    StreamDataPipeline(pipelineManager) {
        this.pipelineManager = pipelineManager
    }

    def execute(Map params = [:]) {
        running = true
        def duration = params.duration ?: 15 // seconds

        pipelineManager.log("🌊 Stream Data Pipeline started")
        pipelineManager.log("   ⏰ Streaming for ${duration}s")

        def startTime = System.currentTimeMillis()
        def endTime = startTime + (duration * 1000)
        def messageCount = 0

        while (running && System.currentTimeMillis() < endTime) {
            messageCount++

            // Simulate streaming message
            def message = [
                id: messageCount,
                timestamp: new Date().time,
                type: ["user_action", "system_event", "data_update", "error_log"][((int)(Math.random() * 4))],
                value: Math.random() * 1000,
                source: ["web", "mobile", "api", "system"][((int)(Math.random() * 4))]
            ]

            if (messageCount % 50 == 0) {
                pipelineManager.log("   🌊 Processed $messageCount messages")
            }

            // Process high-value messages
            if (message.value > 900) {
                pipelineManager.log("   🔥 High-value message: ${message.type} = ${message.value.round(2)}")
            }

            // Random processing delay
            Thread.sleep(50 + (int)(Math.random() * 100))
        }

        if (running) {
            pipelineManager.log("🌊 Stream processing completed: $messageCount messages processed")
        }

        running = false
    }

    def stop() {
        running = false
        pipelineManager.log("🛑 Stream Data Pipeline stopped")
    }
}