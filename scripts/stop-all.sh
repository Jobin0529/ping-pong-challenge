#!/bin/bash
#
# 停止所有 Ping Pong 服务
#

echo "停止所有 Ping Pong 服务..."

PID_FILE="/tmp/ping-pong-pids.txt"

if [ -f "$PID_FILE" ]; then
    source "$PID_FILE"
    kill $PONG_PID $PING1_PID $PING2_PID $PING3_PID 2>/dev/null
    echo "已发送停止信号: Pong($PONG_PID), Ping-1($PING1_PID), Ping-2($PING2_PID), Ping-3($PING3_PID)"
    rm -f "$PID_FILE"
else
    echo "PID 文件不存在，尝试通过进程名查找..."
    pkill -f "pong-service" 2>/dev/null
    pkill -f "ping-service" 2>/dev/null
fi

# 清理锁文件
rm -f /tmp/ping-pong-rate.lock

echo "所有服务已停止"
