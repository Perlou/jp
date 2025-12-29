#!/bin/bash

# 微服务电商系统 - 一键启动脚本
# 使用方法: ./start-all.sh

PROJECT_DIR=$(dirname "$0")
cd "$PROJECT_DIR"

echo "========================================"
echo "  微服务电商系统 - 启动中..."
echo "========================================"

# 检查是否已编译
if [ ! -d "mall-common/target" ]; then
    echo "⚠️  首次运行，正在编译项目..."
    mvn clean install -DskipTests -q
fi

# 清理旧日志
rm -f logs/*.log 2>/dev/null
mkdir -p logs

echo ""
echo "🚀 启动服务..."

# 启动网关
echo "  [1/5] 启动 Gateway (8080)..."
cd mall-gateway
mvn spring-boot:run -q > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
cd ..

sleep 3

# 启动用户服务
echo "  [2/5] 启动 User Service (8081)..."
cd mall-service/user-service
mvn spring-boot:run -q > ../../logs/user.log 2>&1 &
USER_PID=$!
cd ../..

# 启动商品服务
echo "  [3/5] 启动 Product Service (8082)..."
cd mall-service/product-service
mvn spring-boot:run -q > ../../logs/product.log 2>&1 &
PRODUCT_PID=$!
cd ../..

# 启动库存服务
echo "  [4/5] 启动 Inventory Service (8083)..."
cd mall-service/inventory-service
mvn spring-boot:run -q > ../../logs/inventory.log 2>&1 &
INVENTORY_PID=$!
cd ../..

# 启动订单服务
echo "  [5/5] 启动 Order Service (8084)..."
cd mall-service/order-service
mvn spring-boot:run -q > ../../logs/order.log 2>&1 &
ORDER_PID=$!
cd ../..

# 保存 PID 用于停止
echo "$GATEWAY_PID $USER_PID $PRODUCT_PID $INVENTORY_PID $ORDER_PID" > .service-pids

echo ""
echo "========================================"
echo "  ✅ 所有服务启动中！"
echo "========================================"
echo ""
echo "服务端口:"
echo "  • Gateway:   http://localhost:8080"
echo "  • User:      http://localhost:8081"
echo "  • Product:   http://localhost:8082"
echo "  • Inventory: http://localhost:8083"
echo "  • Order:     http://localhost:8084"
echo ""
echo "日志目录: ./logs/"
echo "停止服务: ./stop-all.sh"
echo ""
echo "等待服务完全启动 (约30秒)..."
sleep 30
echo "✅ 服务已就绪！"
