# RAG知识库系统

一个功能完整的企业级RAG（检索增强生成）知识库系统，支持多格式文档处理、智能向量化存储和基于AI的智能问答。

## 项目特性

### 核心功能

✅ **用户认证系统**
- JWT Token认证
- 用户注册/登录/退出
- 路由保护与权限控制

✅ **知识库管理**
- 创建、编辑、删除知识库
- 知识库搜索和筛选
- 文档数量和向量统计

✅ **文档管理**
- 支持多种文件格式（PDF、Word、Excel、PPT、TXT等）
- 拖拽上传
- 自动解析和分块处理
- 异步文档处理
- 上传进度追踪

✅ **对象存储集成**
- 火山引擎 TOS 对象存储
- 前端直传对象存储（STS临时凭证）
- 后端自动解析文件并向量化
- 私有桶预签名URL支持

✅ **智能问答**
- 基于RAG技术的智能问答
- 支持选择特定知识库
- 答案来源追溯
- 对话历史管理
- 流式输出（SSE）
- 复制、评价、重新生成功能

✅ **向量化存储**
- PostgreSQL + pgvector向量数据库
- 高效的向量检索
- 支持大规模数据存储
- 持久化向量存储

✅ **系统设置**
- LLM模型配置
- Embedding模型配置（支持火山引擎Doubao系列）
- 向量检索参数设置
- 系统功能开关

✅ **工作台**
- 数据统计概览
- 最近上传文档列表
- 知识库统计

## 技术栈

### 后端
- **Spring Boot 3.2.0** - 核心框架
- **Spring AI 1.0.0-M6** - AI集成框架
- **Spring Security** - 安全认证
- **JWT** - Token认证
- **PostgreSQL + pgvector** - 向量数据库
- **MyBatis Plus** - ORM框架
- **Apache Tika** - 文档解析
- **Spring AI DocumentReader** - 文档读取和分块
- **Hutool** - Java工具库
- **Lombok** - 代码简化

### 前端
- **React 18** - UI框架
- **Vite 5** - 构建工具
- **Ant Design 5** - UI组件库
- **React Router 6** - 路由管理
- **Axios** - HTTP客户端
- **CSS Modules** - 样式管理

## 项目结构

```
rag/
├── rag-backend/                    # Spring Boot 后端项目
│   ├── src/main/java/com/rag/
│   │   ├── controller/            # REST API控制器
│   │   │   ├── AuthController.java
│   │   │   ├── ChatController.java
│   │   │   ├── DocumentController.java
│   │   │   ├── KnowledgeBaseController.java
│   │   │   └── DashboardController.java
│   │   ├── service/               # 业务逻辑层
│   │   │   ├── AuthService.java
│   │   │   ├── PersistentChatService.java
│   │   │   ├── DocumentService.java
│   │   │   ├── TikaFileParserService.java
│   │   │   ├── RAGService.java
│   │   │   └── storage/           # 对象存储服务
│   │   │       ├── ObjectStorageService.java
│   │   │       └── VolcengineTosStorageService.java
│   │   ├── entity/                # 数据实体
│   │   ├── repository/            # 数据访问层
│   │   ├── dto/                   # 数据传输对象
│   │   │   ├── StsCredentialRequest.java
│   │   │   └── ConfirmUploadRequest.java
│   │   ├── config/                # 配置类
│   │   │   └── TosConfig.java     # TOS对象存储配置
│   │   ├── security/              # 安全相关
│   │   └── constant/              # 常量定义
│   └── pom.xml
│
└── rag-frontend/                  # React + Vite 前端项目
    ├── src/
    │   ├── pages/                 # 页面组件
    │   │   ├── Login.jsx          # 登录注册页
    │   │   ├── Dashboard.jsx      # 工作台
    │   │   ├── Chat.jsx           # 智能问答
    │   │   ├── KnowledgeBase.jsx  # 知识库管理
    │   │   ├── FileUpload.jsx     # 文件上传
    │   │   └── Settings.jsx       # 系统设置
    │   ├── services/              # API服务
    │   │   ├── auth.js            # 认证服务
    │   │   ├── chat.js            # 聊天服务
    │   │   ├── document.js        # 文档服务
    │   │   └── knowledgeBase.js   # 知识库服务
    │   └── App.jsx                # 主应用组件
    └── package.json
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 16+
- Maven 3.8+
- PostgreSQL 14+（含pgvector扩展）

### 数据库准备

```sql
-- 创建数据库
CREATE DATABASE rag_db;

-- 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;
```

### 启动后端服务

```bash
# 进入后端目录
cd rag-backend

# 配置环境变量
export OPENAI_API_KEY=your-api-key
export OPENAI_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export AI_MODEL=your-chat-model
export EMBEDDING_MODEL=doubao-embedding-vision-250615

# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 启动前端服务

```bash
# 进入前端目录
cd rag-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 http://localhost:5173 启动

### 访问系统

打开浏览器访问：http://localhost:5173

1. 首次使用请先注册账号
2. 登录后即可使用所有功能

## 配置说明

### 后端配置

在 `rag-backend/src/main/resources/application-dev.yml` 中配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_db
    username: postgres
    password: your-password
    driver-class-name: org.postgresql.Driver
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL}
      chat:
        model: ${AI_MODEL}
        maxTokens: 2000
      embedding:
        model: ${EMBEDDING_MODEL}

jwt:
  secret: your-jwt-secret-key
  expiration: 86400000

# 火山引擎TOS对象存储配置
tos:
  endpoint: tos-cn-beijing.volces.com
  access-key: your-tos-access-key
  secret-key: your-tos-secret-key
  bucket-name: your-bucket-name
  region: cn-beijing
  # STS临时凭证配置
  sts:
    role-arn: trn:iam::your-account-id:role/tos_role
    duration-seconds: 3600
```

### 对象存储上传流程

1. 前端请求后端获取 STS 临时凭证（包含文件信息）
2. 后端生成 objectKey 并返回 STS 临时凭证
3. 前端使用 STS 凭证直传文件到 TOS 对象存储
4. 上传成功后前端调用 confirm-upload 接口
5. 后端保存文档记录并异步解析、向量化存储

### 火山引擎配置

系统支持火山引擎的向量化API：

```yaml
spring:
  ai:
    openai:
      base-url: https://ark.cn-beijing.volces.com/api/v3
      embedding:
        model: doubao-embedding-vision-250615
```

支持的模型：
- `doubao-embedding-vision-250615` - 基础版本
- `doubao-embedding-vision-251215` - 支持稀疏向量

## API接口

### 认证接口
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 知识库管理
- `GET /api/knowledge-bases` - 获取所有知识库
- `POST /api/knowledge-bases` - 创建知识库
- `PUT /api/knowledge-bases/{id}` - 更新知识库
- `DELETE /api/knowledge-bases/{id}` - 删除知识库

### 文档管理
- `GET /api/documents/recent` - 获取最近文档
- `GET /api/documents/knowledge-base/{kbId}` - 按知识库获取文档
- `POST /api/documents/sts-credential` - 获取STS临时凭证
- `POST /api/documents/confirm-upload` - 确认上传完成
- `DELETE /api/documents/{id}` - 删除文档

### 智能问答
- `POST /api/chat` - 发送问题获取回答
- `GET /api/chat/history/{sessionId}` - 获取对话历史
- `DELETE /api/chat/session/{sessionId}` - 清空会话

### 系统配置
- `GET /api/config` - 获取所有配置
- `PUT /api/config` - 更新配置

### 工作台
- `GET /api/dashboard/stats` - 获取统计数据

## 认证机制

### JWT Token认证

1. 用户登录获取Token
2. Token存储在localStorage中
3. 所有API请求自动携带Token
4. Token过期自动跳转登录

### 安全特性

- JWT Token认证
- 密码加密存储（BCrypt）
- 请求拦截器自动添加Token
- 401响应自动登出
- 路由保护

## 向量化存储

### PostgreSQL + pgvector

系统使用PostgreSQL的pgvector扩展进行向量存储：

```sql
-- 向量表结构
CREATE TABLE vector_record (
    id BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT,
    document_id BIGINT,
    content TEXT,
    embedding vector(1536),
    metadata JSONB,
    created_at TIMESTAMP
);

-- 创建向量索引
CREATE INDEX ON vector_record USING ivfflat (embedding vector_cosine_ops);
```

### 优势

- 持久化存储
- 支持大规模数据
- 高效的向量检索
- 统一数据管理

## 文档处理流程

```
1. 前端选择文件
   ↓
2. 前端请求后端获取 STS 临时凭证（携带文件信息）
   ↓
3. 后端生成 objectKey 并返回 STS 凭证
   ↓
4. 前端使用 STS 凭证直传文件到 TOS 对象存储
   ↓
5. 上传成功后前端调用 confirm-upload 接口
   ↓
6. 后端保存文档记录
   ↓
7. TikaFileParserService异步解析文档
   ↓
8. DocumentReaderService分块处理
   ↓
9. RAGService向量化存储到 pgvector
   ↓
10. 更新文档状态为已完成
```

## 开发指南

### 添加新的API接口

1. 在 `entity` 包中创建实体类
2. 在 `repository` 包中创建Repository接口
3. 在 `service` 包中创建Service类
4. 在 `controller` 包中创建Controller类
5. 在前端 `services` 目录中创建API调用函数

### 添加新的页面

1. 在 `pages` 目录中创建页面组件
2. 在 `App.jsx` 中添加路由
3. 在 `services` 目录中创建API服务

## 项目进度

- [x] Phase 1: 核心MVP
  - [x] 文本类知识库构建
  - [x] 文本检索与问答
  - [x] 多知识库管理
  - [x] 基础前端界面

- [x] Phase 2: 能力扩展
  - [x] 多格式文档解析
  - [x] 检索结果来源标注
  - [x] 用户认证系统

- [x] Phase 3: 高级特性
  - [x] RAG问答功能
  - [x] 配置管理系统
  - [x] PostgreSQL向量存储
  - [x] 火山引擎向量化API

- [ ] Phase 4: 企业级特性
  - [ ] 用户权限管理
  - [ ] 操作审计日志
  - [ ] API开放平台
  - [ ] 高可用集群部署

## 注意事项

1. 生产环境请修改数据库配置
2. 请妥善保管JWT密钥
3. 向量数据库已集成pgvector，支持持久化存储
4. LLM和Embedding模型需要配置真实的API Key
5. 文件上传大小限制默认为500MB

## License

MIT License
