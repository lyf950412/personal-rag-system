# Spring Boot YAML 配置说明

## 📋 配置文件位置

- 开发环境: `src/main/resources/application.yml`
- 测试环境: `src/test/resources/application-test.yml`
- 生产环境: `src/main/resources/application-prod.yml`

## 🎯 主要配置项

### 1. 应用配置

```yaml
spring:
  application:
    name: rag-backend  # 应用名称
```

### 2. 数据源配置

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:rag_db  # H2内存数据库
    username: sa
    password: ""  # 空密码
    driver-class-name: org.h2.Driver
```

#### MySQL配置示例

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 3. JPA/Hibernate配置

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # update/create/none/validate
    show-sql: true  # 显示SQL语句
    properties:
      hibernate:
        format_sql: true  # 格式化SQL
        use_sql_comments: true  # SQL注释
        batch_size: 20  # 批量处理大小
        fetch_size: 100  # fetch大小
```

#### ddl-auto 选项

- `update`: 自动更新表结构（推荐开发环境）
- `create`: 每次启动创建新表（会丢失数据）
- `create-drop`: 启动时创建，关闭时删除
- `validate`: 验证表结构，不修改
- `none`: 不做任何操作

### 4. H2控制台配置

```yaml
spring:
  h2:
    console:
      enabled: true  # 启用H2控制台
      path: /h2-console  # 访问路径
```

访问地址: http://localhost:8080/h2-console

### 5. 文件上传配置

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 500MB  # 单文件大小
      max-request-size: 500MB  # 请求总大小
      file-size-threshold: 10MB  # 阈值
```

### 6. 日志配置

```yaml
logging:
  level:
    root: INFO
    com.rag: DEBUG  # 应用日志级别
    org.springframework: INFO
    org.hibernate: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

#### 日志级别

- TRACE: 跟踪信息
- DEBUG: 调试信息
- INFO: 信息
- WARN: 警告
- ERROR: 错误

### 7. Swagger/API文档配置

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
```

访问地址: http://localhost:8080/swagger-ui.html

## 🔄 多环境配置

### 创建环境配置文件

```bash
# application-dev.yml - 开发环境
# application-prod.yml - 生产环境
# application-test.yml - 测试环境
```

### 激活环境

```bash
# 通过命令行
java -jar app.jar --spring.profiles.active=prod

# 或YAML中指定
spring:
  profiles:
    active: dev
```

## 💡 常用配置技巧

### 1. 引用环境变量

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:default_password}
```

### 2. 使用随机值

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb-${random.uuid}
```

### 3. 配置加密

```yaml
spring:
  datasource:
    password: ENC(xxx)  # Jasypt加密
```

### 4. 导入额外配置

```yaml
spring:
  config:
    import: optional:file:./config/local.yml
```

## 🚀 性能优化配置

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          batch_versioned_uniform=true
        order_insert=true
        order_update=true
  hikari:
    maximum-pool-size: 20
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

## 🔒 安全配置

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEY_STORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
```

## 📊 监控配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
  endpoint:
    health:
      show-details: always
```

## 🌐 CORS配置

在代码中配置

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        // CORS配置
    }
}
```

## 🎯 配置优先级

1. 命令行参数
2. 系统环境变量
3. application-{profile}.yml
4. application.yml
5. application.properties
6. @PropertySource注释
7. 默认值

## 📝 常用命令

```bash
# 查看所有配置
java -jar app.jar --debug

# 列出配置属性
java -jar app.jar --spring.config.additional-location=optional:file:./config.yaml
```

## 💻 实际配置示例

完整配置示例参考 `application.yml` 文件，包含所有推荐的生产环境配置。
