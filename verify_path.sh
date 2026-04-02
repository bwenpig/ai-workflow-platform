#!/bin/bash
# ============================================
# 路径验证脚本 - 验收前必须执行
# ============================================

EXPECTED_PATH="/Users/ben/.openclaw/workspace-coder/ai-workflow/backend"
CURRENT_PATH="$(pwd)"

echo "========================================"
echo "  路径验证脚本 - Sprint 验收前检查"
echo "========================================"
echo ""

# 步骤 1: 路径确认
if [ "$CURRENT_PATH" != "$EXPECTED_PATH" ]; then
    echo "❌ 错误：当前路径不正确"
    echo "期望：$EXPECTED_PATH"
    echo "实际：$CURRENT_PATH"
    echo ""
    echo "请执行：cd $EXPECTED_PATH"
    exit 1
fi
echo "✅ 步骤 1: 路径确认通过"
echo "   当前路径：$CURRENT_PATH"
echo ""

# 步骤 2: Maven 项目验证
if [ ! -f "pom.xml" ]; then
    echo "❌ 错误：pom.xml 不存在，不是 Maven 项目"
    echo ""
    echo "请确认在正确的目录：$EXPECTED_PATH"
    exit 1
fi
echo "✅ 步骤 2: Maven 项目验证通过"
echo "   pom.xml: 存在"
echo ""

# 步骤 3: 源代码目录验证
if [ ! -d "src/main/java" ]; then
    echo "❌ 错误：src/main/java 目录不存在"
    exit 1
fi
echo "✅ 步骤 3: 源代码目录验证通过"
echo "   src/main/java: 存在"
echo ""

# 步骤 4: 测试代码目录验证
if [ ! -d "src/test/java" ]; then
    echo "❌ 错误：src/test/java 目录不存在"
    exit 1
fi
echo "✅ 步骤 4: 测试代码目录验证通过"
echo "   src/test/java: 存在"
echo ""

# 步骤 5: 资源文件目录验证
if [ ! -d "src/test/resources" ]; then
    echo "⚠️  警告：src/test/resources 目录不存在（可选）"
else
    echo "✅ 步骤 5: 资源文件目录验证通过"
    echo "   src/test/resources: 存在"
fi
echo ""

echo "========================================"
echo "  ✅ 所有验证通过，可以开始验收！"
echo "========================================"
echo ""
echo "项目路径：$CURRENT_PATH"
echo "继续执行验收测试..."
echo ""

exit 0
