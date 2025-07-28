#!/bin/bash

echo "🔍 Debug Customer Service OpenTelemetry Tracing..."

echo ""
echo "1. Checking OpenTelemetry agent..."
if [ -f "telemetry/opentelemetry-javaagent.jar" ]; then
    echo "✅ OpenTelemetry agent found"
    echo "📦 Agent size: $(ls -lh telemetry/opentelemetry-javaagent.jar | awk '{print $5}')"
else
    echo "❌ OpenTelemetry agent not found"
    echo "Downloading..."
    mkdir -p telemetry
    curl -L -o telemetry/opentelemetry-javaagent.jar \
        https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
fi

echo ""
echo "2. Checking Zipkin..."
ZIPKIN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9411/health)
if [ "$ZIPKIN_STATUS" = "200" ]; then
    echo "✅ Zipkin is running"
else
    echo "❌ Zipkin is not running (HTTP $ZIPKIN_STATUS)"
    echo "Start with: docker-compose up -d"
fi

echo ""
echo "3. Building customer-service with debug info..."
cd backend/customer-service
mvn clean package -DskipTests -q

echo ""
echo "4. Starting customer-service with DEBUG tracing..."
echo "⚙️  Configuration:"
echo "   - OpenTelemetry Agent: ENABLED"  
echo "   - Service Name: customer-service"
echo "   - Zipkin Endpoint: http://localhost:9411/api/v2/spans"
echo "   - MDC Injection: ENABLED"
echo "   - Debug Mode: ENABLED"
echo ""

# Start with debug enabled
java "-javaagent:../../telemetry/opentelemetry-javaagent.jar" \
     "-Dotel.service.name=customer-service" \
     "-Dotel.traces.exporter=zipkin" \
     "-Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans" \
     "-Dotel.metrics.exporter=none" \
     "-Dotel.logs.exporter=none" \
     "-Dotel.instrumentation.logback-mdc.enabled=true" \
     "-Dotel.propagators=tracecontext,baggage,b3" \
     "-Dotel.resource.attributes=service.name=customer-service,service.version=1.0.0" \
     "-Dotel.instrumentation.spring-webmvc.enabled=true" \
     "-Dotel.instrumentation.spring-web.enabled=true" \
     "-Dotel.instrumentation.jdbc.enabled=true" \
     "-Dotel.instrumentation.http.enabled=true" \
     "-Dotel.javaagent.debug=true" \
     "-Dotel.javaagent.logging=application" \
     -jar target/customer-service.jar 