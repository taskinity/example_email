@Grab('org.apache.camel:camel-core:4.4.0')
@Grab('org.apache.camel:camel-main:4.4.0')
@Grab('org.slf4j:slf4j-simple:2.0.9')

import org.apache.camel.main.Main
import org.apache.camel.builder.RouteBuilder
import java.lang.reflect.Method

println "🔍 CAMEL 4.4.0 API DISCOVERY"
println "============================="

// Sprawdź dostępne metody w Main
Main main = new Main()
println "📋 Available methods in org.apache.camel.main.Main:"

def methods = Main.class.getMethods()
    .findAll { it.name.contains("route") || it.name.contains("Route") || it.name.contains("builder") || it.name.contains("Builder") }
    .sort { it.name }

methods.each { method ->
    println "   ${method.name}(${method.parameterTypes.collect { it.simpleName }.join(', ')})"
}

println "\n🧪 Testing different API approaches..."

// Test 1: Direct addRouteBuilder
try {
    println "1️⃣ Testing: main.addRouteBuilder()"
    main.addRouteBuilder(new RouteBuilder() {
        void configure() {
            from("timer://test1?repeatCount=1").log("Test 1 works")
        }
    })
    println "✅ addRouteBuilder() - SUCCESS"
} catch (Exception e) {
    println "❌ addRouteBuilder() - FAILED: ${e.class.simpleName}"
}

// Test 2: Configure with routesBuilder
try {
    println "2️⃣ Testing: main.configure().routesBuilder()"
    def main2 = new Main()
    main2.configure().routesBuilder(new RouteBuilder() {
        void configure() {
            from("timer://test2?repeatCount=1").log("Test 2 works")
        }
    })
    println "✅ configure().routesBuilder() - SUCCESS"
} catch (Exception e) {
    println "❌ configure().routesBuilder() - FAILED: ${e.class.simpleName}"
}

// Test 3: Configure with addRoutesBuilder
try {
    println "3️⃣ Testing: main.configure().addRoutesBuilder()"
    def main3 = new Main()
    main3.configure().addRoutesBuilder(new RouteBuilder() {
        void configure() {
            from("timer://test3?repeatCount=1").log("Test 3 works")
        }
    })
    println "✅ configure().addRoutesBuilder() - SUCCESS"
} catch (Exception e) {
    println "❌ configure().addRoutesBuilder() - FAILED: ${e.class.simpleName}"
}

// Test 4: Configure with withRoutesBuilder
try {
    println "4️⃣ Testing: main.configure().withRoutesBuilder()"
    def main4 = new Main()
    main4.configure().withRoutesBuilder(new RouteBuilder() {
        void configure() {
            from("timer://test4?repeatCount=1").log("Test 4 works")
        }
    })
    println "✅ configure().withRoutesBuilder() - SUCCESS"
} catch (Exception e) {
    println "❌ configure().withRoutesBuilder() - FAILED: ${e.class.simpleName}"
}

// Test 5: Using bind
try {
    println "5️⃣ Testing: main.bind()"
    def main5 = new Main()
    def routeBuilder = new RouteBuilder() {
        void configure() {
            from("timer://test5?repeatCount=1").log("Test 5 works")
        }
    }
    main5.bind("myRoutes", routeBuilder)
    println "✅ bind() - SUCCESS"
} catch (Exception e) {
    println "❌ bind() - FAILED: ${e.class.simpleName}"
}

println "\n🔍 Checking MainConfigurationProperties methods..."
try {
    def config = main.configure()
    def configMethods = config.class.getMethods()
        .findAll { it.name.contains("route") || it.name.contains("Route") || it.name.contains("builder") || it.name.contains("Builder") }
        .sort { it.name }

    println "📋 MainConfigurationProperties route-related methods:"
    configMethods.each { method ->
        println "   ${method.name}(${method.parameterTypes.collect { it.simpleName }.join(', ')})"
    }
} catch (Exception e) {
    println "❌ Could not inspect MainConfigurationProperties: ${e.message}"
}

println "\n🎯 FINDING WORKING APPROACH..."