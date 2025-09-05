#!/bin/bash

# Script tổng hợp kiểm thử Temporal và Resilience4j cho Loan Service
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
TEMPORAL_SERVER="localhost:7233"
SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${GREEN}🚀 Bắt đầu kiểm thử toàn diện Loan Service${NC}"
echo "=============================================="

# Function to print_header
print_header() {
    echo -e "\n${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_header "Kiểm tra điều kiện tiên quyết"
    
    # Check if scripts exist
    if [[ ! -f "$SCRIPTS_DIR/test-temporal.sh" ]]; then
        echo -e "${RED}❌ test-temporal.sh không tồn tại${NC}"
        exit 1
    fi
    
    if [[ ! -f "$SCRIPTS_DIR/test-resilience4j.sh" ]]; then
        echo -e "${RED}❌ test-resilience4j.sh không tồn tại${NC}"
        exit 1
    fi
    
    # Check if scripts are executable
    chmod +x "$SCRIPTS_DIR/test-temporal.sh" "$SCRIPTS_DIR/test-resilience4j.sh"
    
    echo -e "${GREEN}✅ Tất cả scripts đã sẵn sàng${NC}"
}

# Function to check services
check_services() {
    print_header "Kiểm tra các dịch vụ"
    
    # Check Loan Service
    echo -e "${YELLOW}🔍 Kiểm tra Loan Service...${NC}"
    if curl -s "$LOAN_SERVICE_URL/actuator/health" > /dev/null; then
        echo -e "${GREEN}✅ Loan Service đang chạy${NC}"
    else
        echo -e "${RED}❌ Loan Service không khả dụng${NC}"
        echo "Hãy chạy: cd backend/loan-service && mvn spring-boot:run"
        exit 1
    fi
    
    # Check Temporal Server
    echo -e "${YELLOW}🔍 Kiểm tra Temporal Server...${NC}"
    if nc -z localhost 7233 2>/dev/null; then
        echo -e "${GREEN}✅ Temporal Server đang chạy${NC}"
    else
        echo -e "${RED}❌ Temporal Server không khả dụng${NC}"
        echo "Hãy chạy: docker run -d -p 7233:7233 temporalio/server:latest"
        exit 1
    fi
    
    # Check MySQL
    echo -e "${YELLOW}🔍 Kiểm tra MySQL...${NC}"
    if nc -z localhost 3306 2>/dev/null; then
        echo -e "${GREEN}✅ MySQL đang chạy${NC}"
    else
        echo -e "${RED}❌ MySQL không khả dụng${NC}"
        exit 1
    fi
}

# Function to setup test environment
setup_test_environment() {
    print_header "Thiết lập môi trường test"
    
    # Create test database
    echo -e "${YELLOW}📊 Tạo database test...${NC}"
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS loan_service_test;" 2>/dev/null || true
    
    # Create test data
    echo -e "${YELLOW}📝 Tạo dữ liệu test...${NC}"
    curl -s -X POST "$LOAN_SERVICE_URL/api/test/setup" > /dev/null || true
    
    echo -e "${GREEN}✅ Môi trường test đã sẵn sàng${NC}"
}

# Function to run temporal tests
run_temporal_tests() {
    print_header "Kiểm thử Temporal Workflow"
    
    echo -e "${YELLOW}🧪 Đang chạy test Temporal...${NC}"
    if "$SCRIPTS_DIR/test-temporal.sh"; then
        echo -e "${GREEN}✅ Temporal tests thành công${NC}"
    else
        echo -e "${RED}❌ Temporal tests thất bại${NC}"
        TEMPORAL_FAILED=true
    fi
}

# Function to run resilience4j tests
run_resilience4j_tests() {
    print_header "Kiểm thử Resilience4j"
    
    echo -e "${YELLOW}⚡ Đang chạy test Resilience4j...${NC}"
    if "$SCRIPTS_DIR/test-resilience4j.sh"; then
        echo -e "${GREEN}✅ Resilience4j tests thành công${NC}"
    else
        echo -e "${RED}❌ Resilience4j tests thất bại${NC}"
        RESILIENCE4J_FAILED=true
    fi
}

# Function to generate test report
generate_report() {
    print_header "Báo cáo kết quả kiểm thử"
    
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local report_file="/tmp/loan-service-test-report-$(date +%Y%m%d_%H%M%S).txt"
    
    cat > "$report_file" << EOF
# Báo cáo kiểm thử Loan Service
# Thời gian: $timestamp

## Tổng quan
- Loan Service URL: $LOAN_SERVICE_URL
- Temporal Server: $TEMPORAL_SERVER
- Thời gian test: $(date)

## Kết quả kiểm thử

### Temporal Workflow
$(if [[ "$TEMPORAL_FAILED" != "true" ]]; then echo "✅ PASS"; else echo "❌ FAIL"; fi)

### Resilience4j
$(if [[ "$RESILIENCE4J_FAILED" != "true" ]]; then echo "✅ PASS"; else echo "❌ FAIL"; fi)

## Metrics tổng hợp
EOF
    
    # Collect metrics
    echo "## Health Check" >> "$report_file"
    curl -s "$LOAN_SERVICE_URL/actuator/health" >> "$report_file"
    
    echo "## Circuit Breaker Metrics" >> "$report_file"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.circuitbreaker.calls" >> "$report_file"
    
    echo "## Rate Limiter Metrics" >> "$report_file"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics/resilience4j.ratelimiter.available.permissions" >> "$report_file"
    
    echo "## Temporal Metrics" >> "$report_file"
    curl -s "$LOAN_SERVICE_URL/actuator/metrics" | grep -i temporal >> "$report_file" || echo "No temporal metrics"
    
    echo -e "${GREEN}📊 Báo cáo đã được lưu: $report_file${NC}"
}

# Function to cleanup
cleanup() {
    print_header "Dọn dẹp sau kiểm thử"
    
    # Clean test data
    echo -e "${YELLOW}🧹 Dọn dẹp dữ liệu test...${NC}"
    curl -s -X DELETE "$LOAN_SERVICE_URL/api/test/cleanup" > /dev/null || true
    
    echo -e "${GREEN}✅ Dọn dẹp hoàn thành${NC}"
}

# Function to display summary
display_summary() {
    print_header "Tổng kết kiểm thử"
    
    echo -e "\n${GREEN}🎯 Kết quả tổng hợp:${NC}"
    echo "================================"
    
    if [[ "$TEMPORAL_FAILED" != "true" ]] && [[ "$RESILIENCE4J_FAILED" != "true" ]]; then
        echo -e "${GREEN}✅ TẤT CẢ TESTS ĐÃ PASS${NC}"
        echo -e "${GREEN}🎉 Loan Service đã sẵn sàng cho production${NC}"
    else
        echo -e "${RED}❌ CÓ TESTS THẤT BẠI${NC}"
        if [[ "$TEMPORAL_FAILED" == "true" ]]; then
            echo -e "${RED}  - Temporal tests thất bại${NC}"
        fi
        if [[ "$
