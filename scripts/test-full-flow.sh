#!/bin/bash
# 全流程测试脚本
# 用法：./scripts/test-full-flow.sh

BASE_URL="http://localhost:8080/api/v1"

echo "======================================"
echo "AI Workflow Platform - 全流程测试"
echo "======================================"
echo ""

# 检查后端是否运行
echo "🔍 检查后端服务..."
if ! curl -s "$BASE_URL/workflows" > /dev/null 2>&1; then
    echo "❌ 后端服务未运行，请先启动："
    echo "   cd backend && mvn spring-boot:run"
    exit 1
fi
echo "✅ 后端服务运行中"
echo ""

# 1. 创建工作流
echo "1️⃣  创建可灵视频生成工作流..."
WORKFLOW_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ben-test" \
  -d '{
    "name": "可灵视频生成工作流",
    "description": "使用可灵模型生成视频",
    "nodes": [
      {
        "nodeId": "input-1",
        "type": "INPUT",
        "position": {"x": 100, "y": 100},
        "config": {"label": "输入提示词", "value": "a cat running"}
      },
      {
        "nodeId": "model-kling",
        "type": "MODEL",
        "modelProvider": "kling",
        "position": {"x": 300, "y": 100},
        "config": {
          "prompt": "a beautiful cat running",
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
        "sourceHandle": "output",
        "targetHandle": "input",
        "dataType": "text"
      },
      {
        "id": "e2-3",
        "source": "model-kling",
        "target": "output-1",
        "sourceHandle": "output",
        "targetHandle": "input",
        "dataType": "video"
      }
    ]
  }')

echo "$WORKFLOW_RESPONSE" | python3 -m json.tool 2>/dev/null

WORKFLOW_ID=$(echo "$WORKFLOW_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -z "$WORKFLOW_ID" ]; then
    echo "❌ 创建工作流失败"
    exit 1
fi

echo ""
echo "✅ 工作流创建成功：$WORKFLOW_ID"
echo ""

# 2. 获取工作流详情
echo "2️⃣  获取工作流详情..."
curl -s "$BASE_URL/workflows/$WORKFLOW_ID" | python3 -m json.tool 2>/dev/null
echo ""

# 3. 执行工作流
echo "3️⃣  执行工作流..."
EXECUTE_RESPONSE=$(curl -s -X POST "$BASE_URL/workflows/$WORKFLOW_ID/execute" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: ben-test" \
  -d '{}')

echo "$EXECUTE_RESPONSE" | python3 -m json.tool 2>/dev/null

EXECUTION_ID=$(echo "$EXECUTE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('executionId', ''))" 2>/dev/null)

if [ -z "$EXECUTION_ID" ]; then
    echo "❌ 执行工作流失败"
    exit 1
fi

echo ""
echo "✅ 执行已启动：$EXECUTION_ID"
echo ""

# 4. 等待执行完成
echo "4️⃣  等待执行完成..."
sleep 3

# 5. 查询执行状态
echo "5️⃣  查询执行状态..."
STATUS_RESPONSE=$(curl -s "$BASE_URL/executions/$EXECUTION_ID")
echo "$STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null

STATUS=$(echo "$STATUS_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', ''))" 2>/dev/null)

echo ""
echo "执行状态：$STATUS"
echo ""

# 6. 获取执行历史
echo "6️⃣  获取执行历史..."
curl -s "$BASE_URL/executions/history?userId=ben-test&limit=5" | python3 -m json.tool 2>/dev/null
echo ""

# 7. 获取工作流列表
echo "7️⃣  获取工作流列表..."
curl -s "$BASE_URL/workflows?createdBy=ben-test" | python3 -m json.tool 2>/dev/null
echo ""

echo "======================================"
if [ "$STATUS" = "SUCCESS" ]; then
    echo "✅ 全流程测试通过！"
else
    echo "⚠️  执行状态：$STATUS"
fi
echo "======================================"
