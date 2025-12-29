#!/bin/bash

# 微服务电商系统 - 停止脚本
# 使用方法: ./stop-all.sh

PROJECT_DIR=$(dirname "$0")
cd "$PROJECT_DIR"

echo "========================================"
echo "  微服务电商系统 - 停止中..."
echo "========================================"

# 读取保存的 PID
if [ -f ".service-pids" ]; then
    PIDS=$(cat .service-pids)
    for PID in $PIDS; do
        if ps -p $PID > /dev/null 2>&1; then
            echo "  停止进程: $PID"
            kill $PID 2>/dev/null
        fi
    done
    rm -f .service-pids
fi

# 确保杀掉所有 Spring Boot 进程
pkill -f "spring-boot:run" 2>/dev/null
pkill -f "mall-gateway" 2>/dev/null
pkill -f "user-service" 2>/dev/null
pkill -f "product-service" 2>/dev/null
pkill -f "inventory-service" 2>/dev/null
pkill -f "order-service" 2>/dev/null

echo ""
echo "✅ 所有服务已停止"
