@echo off
echo Starting Banking Services with Zipkin Tracing...

echo.
echo 1. Starting Docker services (Kafka, Zipkin, Vault, Zookeeper)...
docker-compose up -d

echo.
echo 2. Waiting for services to be ready...
timeout /t 10

echo.
echo 3. Checking Zipkin health...
curl -s http://localhost:9411/health || echo "Zipkin not ready yet, please wait..."

echo.
echo 4. Downloading OpenTelemetry agent if not exists...
if not exist "telemetry\opentelemetry-javaagent.jar" (
    echo Downloading OpenTelemetry Java Agent...
    mkdir telemetry 2>nul
    curl -L -o telemetry\opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
)

echo.
echo 5. Building account-service...
cd backend\account-service
call mvn clean package -DskipTests

echo.
echo 6. Starting account-service with Zipkin tracing...
java "-javaagent:..\..\telemetry\opentelemetry-javaagent.jar" ^
     "-Dotel.service.name=account-service" ^
     "-Dotel.traces.exporter=zipkin" ^
     "-Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans" ^
     "-Dotel.metrics.exporter=none" ^
     "-Dotel.logs.exporter=none" ^
     "-Dotel.instrumentation.logback-appender.enabled=true" ^
     "-Dotel.propagators=tracecontext,baggage,b3" ^
     -jar target\account-service.jar

echo.
echo Services started! 
echo Zipkin UI: http://localhost:9411
echo Account Service: http://localhost:8082/account/swagger-ui.html
pause 