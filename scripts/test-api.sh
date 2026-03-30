#!/bin/bash
# API 测试脚本
# 用法：./scripts/test-api.sh

BASE_URL="http://localhost:8080/api/v1"

echo "======================================"
echo "AI Workflow Platform - API 测试"
echo "======================================"
echo ""

# 1. 创建工作流
echo "1️⃣  创建工作流..."
WORKFLOW_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{
    "name": "可灵视频生成工作流",
    "description": "使用可灵模型生成视频",
    "nodes": [
      {
        "nodeId": "input-1",
        "type": "INPUT",
        "position": {"x": 100, "y": 100},
        "config": {"label": "输入提示词"}
      },
      {
        "nodeId": "model-kling",
        "type": "MODEL",
        "modelProvider": "kling",
        "position": {"x": 300, "y": 100},
        "config": {
          "prompt": "a cat running",
          "duration": 5
        }
      },
      {
        "nodeId": "output-1",
        "type": "OUTPUT",
        "position": {"x": 500, "y": 100}
      }
    ],
    "edges": [
      {
        "id": "e1-2",
        "source": "input-1",
        "target": "model-kling",
        "dataType": "text"
      },
      {
        "id": "e2-3",
        "source": "model-kling",
        "target": "output-1",
        "dataType": "video"
      }
    ]
  }')

echo "$WORKFLOW_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$WORKFLOW_RESPONSE"

# 提取工作流 ID
WORKFLOW_ID=$(echo "$WORKFLOW_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -z "$WORKFLOW_ID" ]; then
  echo "❌ 创建工作流失败"
  exit 1
fi

echo ""
echo "✅ 工作流创建成功：$WORKFLOW_ID"
echo ""

# 2. 获取工作流列表
echo "2️⃣  获取工作流列表..."
curl -s "$BASE_URL/workflows" \
  -H "X-User-Id: test-user" | python3 -m json.tool 2>/dev/null || echo "获取失败"

echo ""

# 3. 获取工作流详情
echo "3️⃣  获取工作流详情..."
curl -s "$BASE_URL/workflows/$WORKFLOW_ID" | python3 -m json.tool 2>/dev/null || echo "获取失败"

echo ""

# 4. 执行工作流
echo "4️⃣  执行工作流..."
EXECUTE_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows/$WORKFLOW_ID/execute" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{
    "input-1": {
      "prompt": "a beautiful cat running in the grass"
    }
  }')

echo "$EXECUTE_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$EXECUTE_RESPONSE"

EXECUTION_ID=$(echo "$EXECUTE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('executionId', ''))" 2>/dev/null)

if [ -n "$EXECUTION_ID" ]; then
  echo ""
  echo "✅ 执行已启动：$EXECUTION_ID"
  echo ""
  
  # 5. 查询执行状态
  echo "5️⃣  查询执行状态..."
  sleep 2
  curl -s "$BASE_URL/executions/$EXECUTION_ID" | python3 -m json.tool 2>/dev/null || echo "查询失败"
fi

echo ""
echo "======================================"
echo "API 测试完成"
echo "======================================"
