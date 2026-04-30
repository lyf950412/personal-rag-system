@echo off
REM RAG 系统 Docker 部署脚本 (Windows)

echo ==========================================
echo   RAG 系统 Docker 部署脚本
echo ==========================================

REM 检查 Docker
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: Docker 未安装
    pause
    exit /b 1
)

REM 检查环境变量文件
if not exist .env (
    echo 警告: .env 文件不存在，正在从 .env.example 创建...
    copy .env.example .env
    echo 请编辑 .env 文件填入实际配置值
    pause
    exit /b 1
)

echo 配置检查完成，开始部署...
echo.

REM 创建必要的目录
if not exist volumes\etcd mkdir volumes\etcd
if not exist volumes\minio mkdir volumes\minio
if not exist volumes\milvus mkdir volumes\milvus
if not exist volumes\qdrant mkdir volumes\qdrant
if not exist rag-backend\uploads mkdir rag-backend\uploads

REM 构建并启动服务
echo 正在构建镜像...
docker-compose build

echo.
echo 正在启动服务...
docker-compose up -d

echo.
echo 等待服务启动...
timeout /t 10 /nobreak >nul

REM 检查服务状态
echo.
echo 检查服务状态...
docker-compose ps

echo.
echo ==========================================
echo   部署完成！
echo ==========================================
echo.
echo 服务地址:
echo   - 前端: http://localhost:8021
echo   - 后端: http://localhost:8022
echo   - API文档: http://localhost:8022/swagger-ui.html
echo.
echo 常用命令:
echo   - 查看日志: docker-compose logs -f
echo   - 停止服务: docker-compose down
echo   - 重启服务: docker-compose restart
echo   - 重新构建: docker-compose up -d --build
echo.

pause
