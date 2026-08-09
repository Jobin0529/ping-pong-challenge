#!/bin/bash
#
# Ping Pong Challenge - 一键启动脚本
# 启动 1 个 Pong 服务 + 3 个 Ping 服务实例
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "  Ping Pong Challenge - 启动所有服务"
echo "=========================================="

# 清理旧的锁文件
rm -f /tmp/ping-pong-rate.lock

# 创建日志目录
mkdir -p "$PROJECT_DIR/ping-service/logs"

# 1. 启动 Pong 服务
echo "[1/4] 启动 Pong 服务 (port: 8081)..."
cd "$PROJECT_DIR"
java -jar pong-service/target/pong-service-1.0.0-SNAPSHOT.jar &
PONG_PID=$!
echo "  Pong PID: $PONG_PID"
sleep 3

# 2. 启动 Ping 实例 1 (port: 8080)
echo "[2/4] 启动 Ping 实例 1 (port: 8080)..."
java -jar ping-service/target/ping-service-1.0.0-SNAPSHOT.jar \
    --server.port=8080 \
    > ping-service/logs/ping-8080.log 2>&1 &
PING1_PID=$!
echo "  Ping-1 PID: $PING1_PID"

# 3. 启动 Ping 实例 2 (port: 8082)
echo "[3/4] 启动 Ping 实例 2 (port: 8082)..."
java -jar ping-service/target/ping-service-1.0.0-SNAPSHOT.jar \
    --server.port=8082 \
    > ping-service/logs/ping-8082.log 2>&1 &
PING2_PID=$!
echo "  Ping-2 PID: $PING2_PID"

# 4. 启动 Ping 实例 3 (port: 8084)
echo "[4/4] 启动 Ping 实例 3 (port: 8084)..."
java -jar ping-service/target/ping-service-1.0.0-SNAPSHOT.jar \
    --server.port=8084 \
    > ping-service/logs/ping-8084.log 2>&1 &
PING3_PID=$!
echo "  Ping-3 PID: $PING3_PID"

echo ""
echo "=========================================="
echo "  所有服务已启动!"
echo "=========================================="
echo "  Pong:     PID=$PONG_PID  (port: 8081)"
echo "  Ping-1:   PID=$PING1_PID  (port: 8080)"
echo "  Ping-2:   PID=$PING2_PID  (port: 8082)"
echo "  Ping-3:   PID=$PING3_PID  (port: 8084)"
echo ""
echo "  查看日志:"
echo "    tail -f ping-service/logs/ping-8080.log"
echo "    tail -f ping-service/logs/ping-8082.log"
echo "    tail -f ping-service/logs/ping-8084.log"
echo ""
echo "  停止所有服务: kill $PONG_PID $PING1_PID $PING2_PID $PING3_PID"
echo "=========================================="

# 保存 PID 文件以便后续停止
cat > /tmp/ping-pong-pids.txt << EOF
PONG_PID=$PONG_PID
PING1_PID=$PING1_PID
PING2_PID=$PING2_PID
PING3_PID=$PING3_PID
EOF

# 等待所有后台进程
wait
