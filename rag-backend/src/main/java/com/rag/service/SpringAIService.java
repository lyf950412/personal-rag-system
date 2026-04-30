package com.rag.service;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SpringAIService {

    private final org.springframework.ai.chat.client.ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    @Value("${ai.chat.system-prompt:你是一个专业的知识库助手。请根据提供的上下文信息，准确、详细地回答用户的问题。}")
    private String systemPrompt;

    public SpringAIService(org.springframework.ai.chat.client.ChatClient chatClient, EmbeddingModel embeddingModel) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
    }

    public String chat(String question, String context) {
        try {
            String prompt;
            if (context != null && !context.isEmpty()) {
                prompt = String.format(
                        "%s\n\n上下文信息：\n%s\n\n用户问题：%s\n\n请根据上述上下文信息回答用户的问题。如果上下文中没有相关信息，请明确告知用户。",
                        systemPrompt, context, question
                );
            } else {
                prompt = systemPrompt + "\n\n用户问题：" + question;
            }

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process chat request", e);
        }
    }

    public Flux<String> chatStream(String question, String context) {
        try {
            String prompt;
            if (context != null && !context.isEmpty()) {
                prompt = String.format(
                        "%s\n\n上下文信息：\n%s\n\n用户问题：%s\n\n请根据上述上下文信息回答用户的问题。如果上下文中没有相关信息，请明确告知用户。",
                        systemPrompt, context, question
                );
            } else {
                prompt = systemPrompt + "\n\n用户问题：" + question;
            }

            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .filter(Objects::nonNull)
                    .map(content -> content != null ? content : "");
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream chat request", e);
        }
    }

    public String generate(String prompt) {
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate text", e);
        }
    }

    public Flux<String> generateStream(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .filter(Objects::nonNull)
                    .map(content -> content != null ? content : "");
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream generate text", e);
        }
    }

    public float[] embed(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public double similarity(String text1, String text2) {
        try {
            float[] embedding1 = embed(text1);
            float[] embedding2 = embed(text2);

            double dotProduct = 0.0;
            double norm1 = 0.0;
            double norm2 = 0.0;

            for (int i = 0; i < embedding1.length; i++) {
                double v1 = embedding1[i];
                double v2 = embedding2[i];
                dotProduct += v1 * v2;
                norm1 += v1 * v1;
                norm2 += v2 * v2;
            }

            norm1 = Math.sqrt(norm1);
            norm2 = Math.sqrt(norm2);

            return dotProduct / (norm1 * norm2);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate similarity", e);
        }
    }

    public String chatWithHistory(List<String> history, String question) {
        try {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < history.size(); i++) {
                content.append(history.get(i)).append("\n");
            }
            content.append(question);

            String response = chatClient.prompt()
                    .user(content.toString())
                    .call()
                    .content();

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process chat with history", e);
        }
    }
}
