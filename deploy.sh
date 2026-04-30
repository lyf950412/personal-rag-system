#!/bin/bash

# RAG 系统 Docker 部署脚本

set -e

echo "=========================================="
echo "  RAG 系统 Docker 部署脚本"
echo "=========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! command -v docker &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    exit 1
fi

# 检查环境变量文件
if [ ! -f .env ]; then
    echo "警告: .env 文件不存在，正在从 .env.example 创建..."
    cp .env.example .env
    echo "请编辑 .env 文件填入实际配置值"
    exit 1
fi

# 确认环境变量已配置
source .env
if [ -z "$OPENAI_API_KEY" ] || [ "$OPENAI_API_KEY" == "sk-your-openai-api-key" ]; then
    echo "错误: 请在 .env 中配置 OPENAI_API_KEY"
    exit 1
fi

if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" == "your-very-long-random-secret-key-at-least-32-chars" ]; then
    echo "错误: 请在 .env 中配置 JWT_SECRET"
    exit 1
fi

echo ""
echo "配置检查完成，开始部署..."
echo ""

# 创建必要的目录
mkdir -p volumes/etcd volumes/minio volumes/milvus volumes/qdrant
mkdir -p rag-backend/uploads

# 构建并启动服务
echo "正在构建镜像..."
docker-compose build

echo ""
echo "正在启动服务..."
docker-compose up -d

echo ""
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "检查服务状态..."
docker-compose ps

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  - 前端: http://localhost:8021"
echo "  - 后端: http://localhost:8022"
echo "  - API文档: http://localhost:8022/swagger-ui.html"
echo ""
echo "常用命令:"
echo "  - 查看日志: docker-compose logs -f"
echo "  - 停止服务: docker-compose down"
echo "  - 重启服务: docker-compose restart"
echo "  - 重新构建: docker-compose up -d --build"
echo ""
