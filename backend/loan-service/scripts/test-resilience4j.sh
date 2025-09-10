#!/bin/bash

# Script kiểm thử Resilience4j cho Loan Service
# Author: Loan Service Team
# Version: 1.0

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
LOAN_SERVICE_URL="http://localhost:9827"
TOTAL_REQUESTS=50
CONCURRENT_REQUESTS=10

echo -e "${GREEN}⚡ Bắt đầu kiểm thử Resilience4j${NC}"

# Function to check service health
check_service_health() {
    echo -e "${YELLOW}🔍 Kiểm tra kết nối service...${NC}"
    
    if curl -s "$LOAN_SERVICE_URL/actuator/health" > /dev/null; then
        echo -e "${GREEN}✅ Loan Service đang chạy${NC}"
    else
        echo -e "${RED}❌ Loan Service không khả dụng${NC}"
        exit 1
    fi
}

# Function to test circuit breaker
test_circuit_breaker() {
    echo -e "${YELLOW}🔌 Test 1: Circuit Breaker${NC}"
    
    # Reset circuit breaker
    echo "Reset circuit breaker..."
    curl -s -X POST "$LOAN_SERVICE_URL/actuator/circuitbreakerevents/coreBanking/reset" > /dev/null || true
    
    # Test normal flow
    echo "Testing normal flow..."
    for i in {1..5}; do
        RESPONSE=$(curl -s -w "%{http_code}" "$LOAN_SERVICE_URL/api/test/circuit-breaker/normal")
        if [[ "$RESPONSE" == *"200" ]]; then
            echo -e "${GREEN}✅ Request $i thành công${NC}"
        else
            echo -e "${RED}❌ Request $i thất bại${NC}"
        fi
    done
    
    # Force failures to open circuit
    echo "Forcing failures to open circuit..."
    for i in {1..20}; do
        curl -s "$LOAN_SERVICE_URL/api/test/circuit-breaker/fail" > /dev/null &
    done
    wait
    
    sleep 5
    
    # Check circuit breaker state
    STATE=$(curl -s "$LOAN_SERVICE_URL/actuator/health/circuitbreakers" | jq -r '.details.coreBanking.details.state' 2>/dev/null || echo "UNKNOWN")
    echo "Circuit breaker state: $STATE"
    
    if [[ "$STATE" == "OPEN" ]]; then
        echo -e "${GREEN}✅ Circuit breaker đã mở đúng cách${NC}"
    else
        echo -e "${YELLOW}⚠️  Circuit breaker state: $STATE${NC}"
    fi
}

# Function to test rate limiter
test_rate_limiter() {
    echo -e "${YELLOW}🚦 Test 2: Rate Limiter${NC}"
    
    echo "Testing rate limit (10 requests/minute)..."
    
    # Send burst requests
    for i in {1..15}; do
        {
            RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
                "$LOAN_SERVICE_URL/api/test/rate-limit")
            echo "Request $i: $RESPONSE"
        } &
    done
    wait
    
    # Count 429 responses
    COUNT=$(curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.ratelimiter.available.permissions" | \
        jq -r '.measurements[0].value' 2>/dev/null || echo "0")
    echo "Available permissions: $COUNT"
}

# Function to test retry mechanism
test_retry() {
    echo -e "${YELLOW}🔄 Test 3: Retry Mechanism${NC}"
    
    echo "Testing retry with simulated failures..."
    
    # Test retry endpoint
    for i in {1..5}; do
        echo "Test retry $i..."
        RESPONSE=$(curl -s "$LOAN_SERVICE_URL/api/test/retry/fail-then-success")
        echo "Response: $RESPONSE"
    done
    
    # Check retry metrics
    RETRIES=$(curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.retry.calls" | \
        jq -r '.measurements[0].value' 2>/dev/null || echo "0")
    echo "Total retry attempts: $RETRIES"
}

# Function to test bulkhead
test_bulkhead() {
    echo -e "${YELLOW}🛡️  Test 4: Bulkhead${NC}"
    
    echo "Testing bulkhead isolation..."
    
    # Concurrent requests to test bulkhead
    for i in {1..20}; do
        {
            RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
                "$LOAN_SERVICE_URL/api/test/bulkhead/external-api")
            echo "Bulkhead test $i: $RESPONSE"
        } &
    done
    wait
    
    # Check bulkhead metrics
    CONCURRENT=$(curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls" | \
        jq -r '.measurements[0].value' 2>/dev/null || echo "0")
    echo "Available concurrent calls: $CONCURRENT"
}

# Function to test timeout
test_timeout() {
    echo -e "${YELLOW}⏱️  Test 5: TimeLimiter${NC}"
    
    echo "Testing timeout handling..."
    
    # Test timeout scenarios
    for i in {1..5}; do
        echo "Test timeout $i..."
        RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
            "$LOAN_SERVICE_URL/api/test/timeout/slow-service")
        echo "Timeout test $i: $RESPONSE"
    done
    
    # Check timeout metrics
    TIMEOUTS=$(curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.timelimiter.calls" | \
        jq -r '.measurements[0].value' 2>/dev/null || echo "0")
    echo "Total timeout calls: $TIMEOUTS"
}

# Function to test fallback
test_fallback() {
    echo -e "${YELLOW}🛡️  Test 6: Fallback Mechanism${NC}"
    
    echo "Testing fallback when service is down..."
    
    # Test fallback endpoint
    for i in {1..3}; do
        RESPONSE=$(curl -s "$LOAN_SERVICE_URL/api/test/fallback/service-down")
        echo "Fallback test $i: $RESPONSE"
    done
    
    # Check if fallback was triggered
    if grep -q "FALLBACK" logs/app.log 2>/dev/null; then
        echo -e "${GREEN}✅ Fallback được kích hoạt${NC}"
    else
        echo -e "${YELLOW}⚠️  Kiểm tra logs để xác nhận fallback${NC}"
    fi
}

# Function to load test
load_test() {
    echo -e "${YELLOW}🚀 Test 7: Load Testing${NC}"
    
    echo "Running load test with $TOTAL_REQUESTS requests..."
    
    # Use Apache Bench for load testing
    if command -v ab > /dev/null; then
        ab -n $TOTAL_REQUESTS -c $CONCURRENT_REQUESTS \
            "$LOAN_SERVICE_URL/api/loans/test-load" > /tmp/load_test.txt
        
        # Extract results
        RPS=$(grep "Requests per second" /tmp/load_test.txt | awk '{print $4}')
        MEAN_TIME=$(grep "Time per request" /tmp/load_test.txt | head -1 | awk '{print $4}')
        
        echo "Requests per second: $RPS"
        echo "Mean response time: ${MEAN_TIME}ms"
    else
        echo "Apache Bench không có sẵn, bỏ qua load test"
    fi
}

# Function to display metrics
display_metrics() {
    echo -e "\n${GREEN}📊 Tổng hợp metrics Resilience4j${NC}"
    echo "================================"
    
    # Circuit breaker metrics
    echo -e "${BLUE}Circuit Breaker:${NC}"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.circuitbreaker.calls" | jq '.' 2>/dev/null || echo "No data"
    
    # Rate limiter metrics
    echo -e "\n${BLUE}Rate Limiter:${NC}"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.ratelimiter.available.permissions" | jq '.' 2>/dev/null || echo "No data"
    
    # Retry metrics
    echo -e "\n${BLUE}Retry:${NC}"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.retry.calls" | jq '.' 2>/dev/null || echo "No data"
    
    # Bulkhead metrics
    echo -e "\n${BLUE}Bulkhead:${NC}"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.bulkhead.available.concurrent.calls" | jq '.' 2>/dev/null || echo "No data"
    
    # TimeLimiter metrics
    echo -
