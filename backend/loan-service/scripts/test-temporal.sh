#!/bin/bash

# Script kiểm thử Temporal Workflow cho Loan Service
# Author: Loan Service Team
# Version: 1.0

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
LOAN_SERVICE_URL="http://localhost:9827"
TEMPORAL_SERVER="localhost:7233"

echo -e "${GREEN}🚀 Bắt đầu kiểm thử Temporal Workflow${NC}"

# Function to check service health
check_service_health() {
    echo -e "${YELLOW}🔍 Kiểm tra kết nối service...${NC}"
    
    # Check Loan Service
    if curl -s "$LOAN_SERVICE_URL/actuator/health" > /dev/null; then
        echo -e "${GREEN}✅ Loan Service đang chạy${NC}"
    else
        echo -e "${RED}❌ Loan Service không khả dụng${NC}"
        exit 1
    fi
    
    # Check Temporal Server
    if nc -z localhost 7233 2>/dev/null; then
        echo -e "${GREEN}✅ Temporal Server đang chạy${NC}"
    else
        echo -e "${RED}❌ Temporal Server không khả dụng${NC}"
        exit 1
    fi
}

# Function to test happy path workflow
test_happy_path() {
    echo -e "${YELLOW}🎯 Test 1: Happy Path Workflow${NC}"
    
    LOAN_ID=$(date +%s)
    
    # Create test loan
    echo "Tạo loan test với ID: $LOAN_ID"
    curl -s -X POST "$LOAN_SERVICE_URL/api/loans/test-workflow" \
        -H "Content-Type: application/json" \
        -d "{\"loanId\": $LOAN_ID, \"amount\": 50000000}" > /tmp/workflow_response.json
    
    # Wait for workflow completion
    echo "Đang chờ workflow hoàn thành..."
    sleep 10
    
    # Check result
    RESULT=$(curl -s "$LOAN_SERVICE_URL/api/loans/$LOAN_ID/status")
    if echo "$RESULT" | grep -q "APPROVED"; then
        echo -e "${GREEN}✅ Workflow hoàn thành thành công${NC}"
    else
        echo -e "${RED}❌ Workflow thất bại: $RESULT${NC}"
        return 1
    fi
}

# Function to test retry mechanism
test_retry_mechanism() {
    echo -e "${YELLOW}🔄 Test 2: Retry Mechanism${NC}"
    
    LOAN_ID=$(date +%s)000
    
    # Test with simulated failure
    curl -s -X POST "$LOAN_SERVICE_URL/api/loans/test-retry" \
        -H "Content-Type: application/json" \
        -d "{\"loanId\": $LOAN_ID, \"simulateFailure\": true}" > /dev/null
    
    sleep 15
    
    # Check retry logs
    if grep -q "Retry attempt" logs/app.log 2>/dev/null; then
        echo -e "${GREEN}✅ Retry mechanism hoạt động${NC}"
    else
        echo -e "${YELLOW}⚠️  Không tìm thấy log retry (có thể cần kiểm tra thủ công)${NC}"
    fi
}

# Function to test timeout handling
test_timeout_handling() {
    echo -e "${YELLOW}⏰ Test 3: Timeout Handling${NC}"
    
    LOAN_ID=$(date +%s)111
    
    # Test timeout scenario
    curl -s -X POST "$LOAN_SERVICE_URL/api/loans/test-timeout" \
        -H "Content-Type: application/json" \
        -d "{\"loanId\": $LOAN_ID, \"simulateTimeout\": true}" > /dev/null
    
    sleep 20
    
    # Check timeout handling
    RESULT=$(curl -s "$LOAN_SERVICE_URL/api/loans/$LOAN_ID/status")
    if echo "$RESULT" | grep -q "FAILED"; then
        echo -e "${GREEN}✅ Timeout được xử lý đúng cách${NC}"
    else
        echo -e "${RED}❌ Timeout không được xử lý${NC}"
    fi
}

# Function to test workflow metrics
test_workflow_metrics() {
    echo -e "${YELLOW}📊 Test 4: Workflow Metrics${NC}"
    
    # Check Temporal metrics
    METRICS=$(curl -s "$LOAN_SERVICE_URL/actuator/metrics" | grep -i temporal || echo "No temporal metrics")
    echo "Temporal metrics: $METRICS"
    
    # Check workflow execution count
    EXECUTIONS=$(curl -s "$LOAN_SERVICE_URL/actuator/metrics/temporal.workflow.executions" | jq -r '.measurements[0].value' 2>/dev/null || echo "0")
    echo "Số workflow đã thực thi: $EXECUTIONS"
}

# Function to test rollback mechanism
test_rollback_mechanism() {
    echo -e "${YELLOW}🔄 Test 5: Rollback Mechanism${NC}"
    
    LOAN_ID=$(date +%s)222
    
    # Test failure scenario
    curl -s -X POST "$LOAN_SERVICE_URL/api/loans/test-failure" \
        -H "Content-Type: application/json" \
        -d "{\"loanId\": $LOAN_ID, \"simulateFailure\": true}" > /dev/null
    
    sleep 15
    
    # Check rollback
    RESULT=$(curl -s "$LOAN_SERVICE_URL/api/loans/$LOAN_ID/rollback-status")
    if echo "$RESULT" | grep -q "ROLLED_BACK"; then
        echo -e "${GREEN}✅ Rollback hoạt động${NC}"
    else
        echo -e "${RED}❌ Rollback không hoạt động${NC}"
    fi
}

# Function to display summary
display_summary() {
    echo -e "\n${GREEN}📋 Tổng kết kiểm thử Temporal${NC}"
    echo "================================"
    
    # Workflow statistics
    echo "Số workflow đã test: 5"
    echo "Thời gian test: $(date)"
    
    # Check logs for any errors
    if [ -f logs/app.log ]; then
        ERRORS=$(grep -c "ERROR" logs/app.log || echo "0")
        echo "Số lỗi trong logs: $ERRORS"
    fi
    
    echo -e "\n${GREEN}✨ Kiểm thử Temporal hoàn thành!${NC}"
    echo "Kiểm tra dashboard Temporal tại: http://localhost:8088"
}

# Main execution
main() {
    check_service_health
    
    echo -e "\n${GREEN}🧪 Bắt đầu các kịch bản kiểm thử...${NC}"
    
    test_happy_path
    test_retry_mechanism
    test_timeout_handling
    test_workflow_metrics
    test_rollback_mechanism
    
    display_summary
}

# Run main function
main "$@"
