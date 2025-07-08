#!/bin/bash

echo "🔍 Testing Customer Service Tracing..."

echo ""
echo "1. Checking if customer-service is running..."
curl -s http://localhost:8080/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Customer service is running"
else
    echo "❌ Customer service is not running"
    echo "Please start it with: ./customer-start-with-zipkin.sh"
    exit 1
fi

echo ""
echo "2. Checking if Zipkin is running..."
curl -s http://localhost:9411/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Zipkin is running"
else
    echo "❌ Zipkin is not running"
    echo "Please start it with: docker-compose up -d"
    exit 1
fi

echo ""
echo "3. Making test API calls to generate traces..."

# Call health endpoint
echo "📡 Calling health endpoint..."
curl -s http://localhost:8080/actuator/health

echo ""
echo "📡 Calling metrics endpoint..."
curl -s http://localhost:8080/actuator/metrics

echo ""
echo "📡 Calling info endpoint..."
curl -s http://localhost:8080/actuator/info

echo ""
echo "4. Waiting 5 seconds for traces to be sent to Zipkin..."
sleep 5

echo ""
echo "5. Checking traces in Zipkin..."
TRACES=$(curl -s "http://localhost:9411/api/v2/traces?serviceName=customer-service&limit=10")

if [ "$TRACES" != "[]" ]; then
    echo "✅ Traces found in Zipkin!"
    echo "🌐 View traces: http://localhost:9411/zipkin/?serviceName=customer-service"
else
    echo "❌ No traces found in Zipkin"
    echo ""
    echo "🔧 Troubleshooting steps:"
    echo "   1. Check customer service logs for any errors"
    echo "   2. Verify OpenTelemetry agent is loaded"
    echo "   3. Check if traces are being exported:"
    echo "      curl 'http://localhost:9411/api/v2/services'"
fi

echo ""
echo "6. Available services in Zipkin:"
curl -s "http://localhost:9411/api/v2/services" | jq .

echo ""
echo "Test completed!" 