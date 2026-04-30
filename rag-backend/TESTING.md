# 测试指南

本文档说明如何运行单元测试和访问API文档。

## 单元测试

### 运行所有测试

```bash
cd rag-backend
mvn test
```

### 运行特定测试类

```bash
# 测试知识库服务
mvn test -Dtest=KnowledgeBaseServiceTest

# 测试文档服务
mvn test -Dtest=DocumentServiceTest

# 测试聊天服务
mvn test -Dtest=ChatServiceTest

# 测试文件验证器
mvn test -Dtest=FileValidatorTest
```

### 测试覆盖率报告

生成测试覆盖率报告：

```bash
mvn test jacoco:report
```

报告位置：`target/site/jacoco/index.html`

### 测试类列表

1. **KnowledgeBaseServiceTest** - 知识库服务测试
   - 测试知识库的CRUD操作
   - 测试搜索功能
   - 测试异常处理

2. **DocumentServiceTest** - 文档服务测试
   - 测试文档上传、查询、删除
   - 测试向量统计
   - 测试状态更新

3. **ChatServiceTest** - 聊天服务测试
   - 测试问答功能
   - 测试关键词匹配
   - 测试会话管理
   - 测试多会话隔离

4. **FileValidatorTest** - 文件验证器测试
   - 测试文件类型验证
   - 测试文件大小限制
   - 测试文件名安全
   - 测试边界条件

## API文档

### Swagger UI

访问地址：http://localhost:8080/swagger-ui.html

### OpenAPI JSON

访问地址：http://localhost:8080/v3/api-docs

### API文档功能

- ✨ 查看所有API接口列表
- ✨ 查看接口详细信息
- ✨ 测试API接口
- ✨ 查看请求/响应示例
- ✨ 查看参数说明
- ✨ 查看错误码说明

### 主要API模块

#### 1. 知识库管理 `/api/knowledge-bases`
- `GET /api/knowledge-bases` - 获取所有知识库
- `POST /api/knowledge-bases` - 创建知识库
- `PUT /api/knowledge-bases/{id}` - 更新知识库
- `DELETE /api/knowledge-bases/{id}` - 删除知识库
- `GET /api/knowledge-bases/search` - 搜索知识库

#### 2. 文档管理 `/api/documents`
- `GET /api/documents` - 获取所有文档
- `GET /api/documents/recent` - 获取最近文档
- `POST /api/documents/upload` - 上传文档
- `DELETE /api/documents/{id}` - 删除文档

#### 3. 智能问答 `/api/chat`
- `POST /api/chat` - 发送问题
- `GET /api/chat/history/{sessionId}` - 获取聊天历史
- `DELETE /api/chat/session/{sessionId}` - 清空会话

#### 4. 系统配置 `/api/config`
- `GET /api/config` - 获取所有配置
- `PUT /api/config` - 更新配置
- `PUT /api/config/{key}` - 更新单个配置

#### 5. 工作台 `/api/dashboard`
- `GET /api/dashboard/stats` - 获取统计数据

## 常用测试场景

### 1. 创建知识库

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -d '{"name":"测试知识库","description":"测试描述","owner":"测试团队"}'
```

### 2. 上传文档

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test.pdf" \
  -F "knowledgeBaseId=1"
```

### 3. 智能问答

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"系统有哪些功能？","knowledgeBaseId":1}'
```

### 4. 获取统计数据

```bash
curl http://localhost:8080/api/dashboard/stats
```

## 性能测试

建议使用以下工具进行性能测试：

- **JMeter** - Apache JMeter
- **Postman** - Postman Collections
- **k6** - Grafana k6

## 集成测试

添加集成测试需要使用@SpringBootTest注解：

```java
@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {
    @Test
    void testKnowledgeBaseAPI() {
        // 集成测试代码
    }
}
```

## 测试数据

测试数据位于：`src/test/resources/data.sql`

## 持续集成

建议配置CI/CD自动运行测试：

- GitHub Actions
- Jenkins
- GitLab CI
