package com.rag.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("全模态RAG知识库系统 API")
                        .version("1.0.0")
                        .description("全模态RAG（检索增强生成）知识库系统的REST API文档，包含知识库管理、文档管理、智能问答等功能")
                        .contact(new Contact()
                                .name("RAG团队")
                                .email("support@rag-system.com"))
                        .termsOfService("https://www.rag-system.com/terms"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("开发环境服务器")
                ));
    }
}
