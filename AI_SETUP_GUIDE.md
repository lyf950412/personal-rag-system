# RAG 知识库系统 - AI 技术栈配置指南

## 🎯 已集成的 AI 技术栈

本项目已成功集成**目前市面上最流行**的 Java AI 技术栈：

### 核心技术栈

| 技术 | 说明 | 版本 |
|------|------|------|
| **LangChain4j** | Java 生态最流行的 AI 框架，提供统一的 LLM 和向量存储接口 | 1.0.0-alpha1 |
| **Spring AI** | Spring 官方 AI 框架，与 Spring Boot 深度集成 | 1.0.0 |
| **OpenAI GPT-4o** | 最流行的大语言模型，支持多模态理解 | - |
| **In-Memory Vector Store** | 轻量级向量存储，适合开发和测试环境 | 内置 |

### 架构特点

```
用户问题 → RAGService → VectorStoreService → 向量相似度检索
                      ↓
              AIService → ChatLanguageModel → GPT-4o API
                      ↓
              生成回答 ← 检索到的上下文
```

## 📝 配置步骤

### 1. 配置 OpenAI API Key

在项目根目录创建 `.env` 文件（或设置环境变量）：

```bash
# 必需：OpenAI API Key
OPENAI_API_KEY=sk-your-api-key-here
```

**获取方式**：
- 访问 https://platform.openai.com/api-keys
- 创建新的 API Key
- 建议设置使用额度限制以控制成本

### 2. 修改配置文件

编辑 `rag-backend/src/main/resources/application.yml`：

```yaml
ai:
  llm:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}  # 从环境变量读取
      model: gpt-4o                # 或使用 gpt-4-turbo, gpt-3.5-turbo
      temperature: 0.7              # 控制创造性（0-1，越高越有创造性）
      max-tokens: 2000              # 最大输出 token 数
      base-url: https://api.openai.com/v1

  embedding:
    provider: openai
    model: text-embedding-3-small   # 高效且便宜的 embedding 模型
    dimension: 1536                 # 向量维度
    batch-size: 100                 # 批处理大小
```

### 3. 启动应用

```bash
cd rag-backend
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/rag-backend-1.0.0.jar
```

## 🔧 可选：集成生产级向量数据库

当前配置使用内存向量存储，适合开发和测试。如需生产环境部署：

### Milvus 向量数据库

1. 启动 Milvus：
```bash
docker-compose up milvus-standalone
```

2. 更新配置：
```yaml
ai:
  vector-db:
    type: milvus
    milvus:
      host: localhost
      port: 19530
      collection-name: rag_documents
      dimension: 1536
```

### Qdrant 向量数据库

1. 启动 Qdrant：
```bash
docker-compose up qdrant
```

2. 更新配置：
```yaml
ai:
  vector-db:
    type: qdrant
    qdrant:
      host: localhost
      port: 6333
      collection-name: rag_documents
      dimension: 1536
```

## 🎨 支持的模型

### OpenAI 系列
- GPT-4o（推荐）
- GPT-4-turbo
- GPT-3.5-turbo

### Anthropic Claude（可选）
```yaml
ai:
  llm:
    provider: anthropic
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-5-sonnet-20241022
```

### 国产模型（可选）

**智谱 GLM-4**：
```yaml
ai:
  llm:
    provider: zhipuai
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY}
      model: glm-4
```

**阿里通义千问**：
```yaml
ai:
  llm:
    provider: dashscope
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      model: qwen-turbo
```

## 📊 性能调优

### 检索参数
```yaml
ai:
  rag:
    retrieval:
      top-k: 5                    # 检索最相关的 5 个片段
      similarity-threshold: 0.7   # 相似度阈值
      max-chunk-size: 1000       # 每个文本块的最大字符数
      chunk-overlap: 200          # 文本块重叠字符数
```

### 生成参数
```yaml
ai:
  rag:
    generation:
      max-tokens: 2000
      temperature: 0.7
      presence-penalty: 0.0
      frequency-penalty: 0.0
```

## 🚀 使用示例

### API 调用示例

**上传文档**：
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.pdf" \
  -F "knowledgeBaseId=1"
```

**问答**：
```bash
curl -X POST http://localhost:8080/api/chat/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "user123",
    "question": "文档的主要内容是什么？",
    "knowledgeBaseId": 1
  }'
```

## ⚙️ 提示词模板自定义

系统默认提示词：
```
你是一个专业的知识库助手。请根据提供的上下文信息，准确、详细地回答用户的问题。
如果上下文中没有相关信息，请明确告知用户。
```

可以在配置文件中自定义：
```yaml
ai:
  rag:
    prompt:
      system: |
        [你的自定义系统提示词]
      user: |
        上下文信息：
        {context}

        用户问题：{question}
```

## 🔒 安全建议

1. **API Key 安全**：
   - ✅ 使用环境变量存储 API Key
   - ❌ 不要将 API Key 提交到代码仓库
   - ✅ 设置 API Key 使用额度限制

2. **网络访问**：
   - 生产环境建议配置 API 访问白名单
   - 使用 HTTPS 传输

3. **成本控制**：
   - 监控 API 调用量和成本
   - 使用 `max-tokens` 限制单次响应长度
   - 启用缓存减少重复调用

## 🐛 常见问题

### 1. 编译失败
```bash
# 清理 Maven 缓存
mvn clean
# 强制更新依赖
mvn clean install -U
```

### 2. API 调用失败
- 检查 API Key 是否正确配置
- 确认网络可以访问 OpenAI API
- 查看日志中的详细错误信息

### 3. 向量检索无结果
- 确认文档已上传并处理完成
- 检查向量数据库是否启动
- 调整 `similarity-threshold` 参数

## 📚 技术文档

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [OpenAI API 文档](https://platform.openai.com/docs)
- [Spring AI 官方文档](https://spring.io/projects/spring-ai)

## 🔄 更新日志

- **2026-04-24**: 完成 AI 技术栈集成
  - 集成 LangChain4j 1.0.0-alpha1
  - 支持 OpenAI GPT-4o
  - 实现 RAG 检索增强功能
  - 支持多 LLM 提供商（OpenAI, Claude, 智谱, 通义千问）
