@echo off
echo 🔍 Testing Customer Service Tracing...

echo.
echo 1. Checking if customer-service is running...
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo ✅ Customer service is running
) else (
    echo ❌ Customer service is not running
    echo Please start it with: customer-start-with-zipkin.sh
    pause
    exit /b 1
)

echo.
echo 2. Checking if Zipkin is running...
curl -s http://localhost:9411/health >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo ✅ Zipkin is running
) else (
    echo ❌ Zipkin is not running
    echo Please start it with: docker-compose up -d
    pause
    exit /b 1
)

echo.
echo 3. Making test API calls to generate traces...
echo 📡 Calling tracing test endpoint...
curl -s http://localhost:8080/api/tracing/test

echo.
echo 📡 Calling trace generation endpoint...
curl -s http://localhost:8080/api/tracing/generate-trace

echo.
echo 📡 Calling health endpoint...
curl -s http://localhost:8080/actuator/health

echo.
echo 📡 Calling info endpoint...
curl -s http://localhost:8080/actuator/info

echo.
echo 4. Waiting 5 seconds for traces to be sent to Zipkin...
timeout /t 5 /nobreak >nul

echo.
echo 5. Checking traces in Zipkin...
curl -s "http://localhost:9411/api/v2/traces?serviceName=customer-service&limit=10" > traces.json

for %%R in (traces.json) do (
    if %%~zR gtr 2 (
        echo ✅ Traces found in Zipkin!
        echo 🌐 View traces: http://localhost:9411/zipkin/?serviceName=customer-service
    ) else (
        echo ❌ No traces found in Zipkin
        echo.
        echo 🔧 Troubleshooting steps:
        echo    1. Check customer service logs for any errors
        echo    2. Verify OpenTelemetry agent is loaded
        echo    3. Check if traces are being exported
    )
)

echo.
echo 6. Available services in Zipkin:
curl -s "http://localhost:9411/api/v2/services"

echo.
echo Test completed!
del traces.json >nul 2>&1
pause 