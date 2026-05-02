package com.rag.service;

import com.rag.dto.ChatMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final PgVectorStoreService vectorStoreService;
    private final SpringAIService aiService;

    @Value("${ai.chat.system-prompt:你是一个专业的知识库助手。}")
    private String systemPrompt;

    public RAGService(PgVectorStoreService vectorStoreService, SpringAIService aiService) {
        this.vectorStoreService = vectorStoreService;
        this.aiService = aiService;
    }

    public ChatMessageDTO answer(String question, Long knowledgeBaseId) {
        log.info("Processing RAG question - question: {}, KB: {}", question, knowledgeBaseId);

        try {
            List<String> relevantSegments = vectorStoreService.search(question);
            String context = buildContext(relevantSegments);
            List<ChatMessageDTO.SourceDTO> sources = buildSources(relevantSegments);

            String answer;
            if (context.isEmpty()) {
                answer = generateWithoutContext(question);
            } else {
                answer = aiService.chat(question, context);
            }

            ChatMessageDTO response = new ChatMessageDTO();
            response.setId(System.currentTimeMillis());
            response.setRole("assistant");
            response.setContent(answer);
            response.setSources(sources);
            response.setTimestamp(java.time.LocalDateTime.now());

            log.info("RAG answer generated - length: {}, sources: {}", answer.length(), sources.size());
            return response;
        } catch (Exception e) {
            log.error("Error in RAG processing", e);
            return createErrorResponse("处理失败：" + e.getMessage());
        }
    }

    public Flux<String> answerStream(String question, Long knowledgeBaseId) {
        log.info("Processing RAG stream question - question: {}, KB: {}", question, knowledgeBaseId);

        try {
            List<String> relevantSegments = vectorStoreService.search(question);
            String context = buildContext(relevantSegments);

            if (context.isEmpty()) {
                return generateWithoutContextStream(question);
            } else {
                return aiService.chatStream(question, context);
            }
        } catch (Exception e) {
            log.error("Error in RAG stream processing", e);
            return Flux.just("抱歉，处理您的请求时遇到问题：" + e.getMessage());
        }
    }

    public Flux<String> answerStream(String question) {
        return answerStream(question, null);
    }

    public ChatMessageDTO answer(String question) {
        return answer(question, null);
    }

    private String buildContext(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }

        return segments.stream()
                .collect(Collectors.joining("\n\n"));
    }

    private List<ChatMessageDTO.SourceDTO> buildSources(List<String> segments) {
        if (segments == null) {
            return List.of();
        }

        return segments.stream()
                .limit(5)
                .map(segment -> {
                    ChatMessageDTO.SourceDTO source = new ChatMessageDTO.SourceDTO();
                    source.setTitle("相关文档片段");
                    source.setPage("N/A");
                    source.setScore(1.0);
                    return source;
                })
                .collect(Collectors.toList());
    }

    private String generateWithoutContext(String question) {
        log.warn("No relevant context found, generating response without context");

        String prompt = String.format(
                "你是一个专业的知识库助手。用户询问：%s\n\n" +
                "请礼貌地告知用户，目前知识库中没有找到相关信息，" +
                "建议用户：\n" +
                "1. 检查问题是否与已上传的文档相关\n" +
                "2. 尝试使用更具体的关键词\n" +
                "3. 上传更多相关文档到知识库",
                question
        );

        return aiService.generate(prompt);
    }

    private Flux<String> generateWithoutContextStream(String question) {
        log.warn("No relevant context found, streaming response without context");

        String prompt = String.format(
                "你是一个专业的知识库助手。用户询问：%s\n\n" +
                "请礼貌地告知用户，目前知识库中没有找到相关信息，" +
                "建议用户：\n" +
                "1. 检查问题是否与已上传的文档相关\n" +
                "2. 尝试使用更具体的关键词\n" +
                "3. 上传更多相关文档到知识库",
                question
        );

        return aiService.generateStream(prompt);
    }

    private ChatMessageDTO createErrorResponse(String errorMessage) {
        ChatMessageDTO response = new ChatMessageDTO();
        response.setId(System.currentTimeMillis());
        response.setRole("assistant");
        response.setContent("抱歉，处理您的请求时遇到问题：" + errorMessage);
        response.setSources(List.of());
        response.setTimestamp(java.time.LocalDateTime.now());
        return response;
    }

    public void addToKnowledgeBase(Long knowledgeBaseId, String documentId, List<Document> contentChunks) {
        log.info("Adding content to knowledge base - KB: {}, doc: {}, chunks: {}",
                knowledgeBaseId, documentId, contentChunks.size());

        vectorStoreService.addDocument(documentId, contentChunks);
    }

    public void removeFromKnowledgeBase(String documentId) {
        log.info("Removing document from knowledge base - doc: {}", documentId);
        vectorStoreService.deleteDocument(documentId);
    }
}
