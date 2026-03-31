#!/bin/bash
# Python 沙箱镜像构建脚本
# 用于构建和推送 Docker 镜像

set -e

IMAGE_NAME="ben/python-sandbox"
IMAGE_TAG="1.0.0"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

echo "🔨 开始构建 Docker 镜像：${FULL_IMAGE_NAME}"

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未运行，请先启动 Docker"
    exit 1
fi

# 构建镜像
echo "📦 构建镜像..."
docker build -t ${FULL_IMAGE_NAME} .

# 验证镜像
echo "✅ 镜像构建成功"
echo ""
echo "📋 镜像信息:"
docker images | grep ${IMAGE_NAME}

echo ""
echo "🚀 测试镜像:"
echo "   docker run --rm ${FULL_IMAGE_NAME} python3 -c \"print('Hello from sandbox')\""

echo ""
echo "📤 推送镜像到 Docker Hub (可选):"
echo "   docker push ${FULL_IMAGE_NAME}"
