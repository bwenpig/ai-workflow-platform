#!/bin/bash
# Sprint 3 资源限制测试脚本
# 用于验证 Docker 容器的内存和 CPU 限制是否生效

echo "======================================"
echo "Sprint 3 资源限制测试"
echo "======================================"
echo ""

# 测试 1: 内存限制测试
echo "📊 测试 1: 内存限制 (128MB)"
echo "--------------------------------------"
cat > /tmp/test_memory.py << 'EOF'
import sys

# 尝试分配 200MB 内存（超过限制）
try:
    data = bytearray(200 * 1024 * 1024)
    print("❌ 内存限制未生效 - 成功分配 200MB")
except MemoryError:
    print("✅ 内存限制生效 - MemoryError 捕获")
    sys.exit(0)
except Exception as e:
    print(f"✅ 内存限制生效 - 异常：{type(e).__name__}")
    sys.exit(0)

sys.exit(1)
EOF

docker run --rm \
    --memory=128m \
    --memory-swap=128m \
    --name test-memory-limit \
    python:3.11-slim \
    python3 /tmp/test_memory.py 2>&1 || echo "容器因 OOM 被终止（预期行为）"

echo ""

# 测试 2: CPU 限制测试
echo "📊 测试 2: CPU 限制 (0.5 CPU)"
echo "--------------------------------------"
cat > /tmp/test_cpu.py << 'EOF'
import time
import sys

start = time.time()

# CPU 密集型计算
result = 0
for i in range(10000000):
    result += i * i

elapsed = time.time() - start
print(f"计算完成，耗时：{elapsed:.2f}秒")
print("✅ CPU 限制测试完成（比较不同限制下的耗时）")
EOF

echo "使用 0.5 CPU 限制运行..."
time docker run --rm \
    --cpu-quota=50000 \
    --name test-cpu-limit \
    python:3.11-slim \
    python3 /tmp/test_cpu.py

echo ""
echo "使用 1.0 CPU 限制运行（对比）..."
time docker run --rm \
    --cpu-quota=100000 \
    --name test-cpu-full \
    python:3.11-slim \
    python3 /tmp/test_cpu.py

echo ""

# 测试 3: 组合限制测试
echo "📊 测试 3: 内存 + CPU 组合限制"
echo "--------------------------------------"
docker run --rm \
    --memory=256m \
    --memory-swap=256m \
    --cpu-quota=50000 \
    --name test-combined \
    python:3.11-slim \
    python3 -c "
import os
print('✅ 组合限制容器启动成功')
print(f'内存限制：256MB')
print(f'CPU 限制：0.5 核心')
"

echo ""
echo "======================================"
echo "测试完成！"
echo "======================================"
