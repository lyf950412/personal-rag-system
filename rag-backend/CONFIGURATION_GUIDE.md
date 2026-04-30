# 配置文件使用指南

## 配置文件位置

- 开发配置: `src/main/resources/application.yml`
- 生产配置: `src/main/resources/application-prod.yml`
- 测试配置: `src/test/resources/application-test.yml`

## 配置文件优先级

配置文件加载顺序（后面覆盖前面）:
1. `application.yml`
2. `application-{profile}.yml`
3. 命令行参数
4. 环境变量

## 主要配置说明

### 1. 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:rag_db
    username: sa
    password: 
    driver-class-name: org.h2.Driver
```

### 2. 文件上传配置

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 500MB
      max-request-size: 500MB
      file-size-threshold: 10MB
```

### 3. 日志配置

```yaml
logging:
  level:
    root: INFO
    com.rag: DEBUG
    org.springframework: INFO
    org.hibernate: INFO
```

### 4. Swagger配置

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

## 常用命令

### 查看所有配置
```bash
mvn spring-boot:run --debug
```

### 激活不同环境
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 运行测试
```bash
mvn test -Dspring.profiles.active=test
```

## 最佳实践

1. 敏感信息使用环境变量
2. 不同环境使用不同配置
3. 定期备份配置
4. 使用版本控制管理配置
5. 文档化配置项
