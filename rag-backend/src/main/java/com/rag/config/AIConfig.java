package com.rag.config;

import com.rag.ai.embedding.VolcengineEmbeddingModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.model:doubao-pro-32k}")
    private String chatModel;
    @Value("${spring.ai.openai.chat.completionsPath:/chat/completions}")
    private String completionsPath;

    @Value("${spring.ai.openai.chat.maxTokens:2000}")
    private Integer maxTokens;

    @Value("${spring.ai.openai.embedding.model:doubao-embedding-vision-250615}")
    private String embeddingModel;

    @Value("${spring.ai.openai.embedding.dimensions:1024}")
    private Integer embeddingDimensions;

    @Bean
    @Primary
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @Primary
    public OpenAiChatModel chatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(chatModel)
                                .maxTokens(maxTokens)
                                .build()
                )
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)

                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    @Primary
    public VolcengineEmbeddingModel embeddingModel() {
        return new VolcengineEmbeddingModel();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(
            500,
            50,
            5,
            1000,
            true
        );
    }
}
