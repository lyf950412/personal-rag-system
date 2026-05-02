package com.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentReaderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentReaderService.class);

    @Autowired
    private TokenTextSplitter tokenTextSplitter;

    @Autowired(required = false)
    private VectorStore vectorStore;

    public List<String> parseAndSplitDocument(MultipartFile file) throws IOException {
        log.info("开始解析文档(MultipartFile): {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        Resource resource = file.getResource();
        List<Document> documents = parseWithTika(resource);
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        
        log.info("文档解析完成(MultipartFile): 原始块数={}, 分词后块数={}", 
                documents.size(), splitDocuments.size());

        return splitDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    public List<String> parseAndSplitDocument(File file) throws IOException {
        log.info("开始解析文档(File): {}, 大小: {} bytes", file.getName(), file.length());

        Resource resource = new FileSystemResource(file);
        List<Document> documents = parseWithTika(resource);
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        
        log.info("文档解析完成(File): 原始块数={}, 分词后块数={}", 
                documents.size(), splitDocuments.size());

        return splitDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    public List<Document> parseAndSplitDocumentWithMetadata(File file) throws IOException {
        log.info("开始解析文档并保留metadata(File): {}, 大小: {} bytes", file.getName(), file.length());

        Resource resource = new FileSystemResource(file);
        List<Document> documents = parseWithTika(resource);
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        
        log.info("文档解析完成(File): 原始块数={}, 分词后块数={}", 
                documents.size(), splitDocuments.size());

        return splitDocuments;
    }

    public List<Document> parseWithTika(Resource resource) {
        try {
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.read();
        } catch (Exception e) {
            log.error("使用Tika解析文件失败", e);
            throw new RuntimeException("使用Tika解析文件失败: " + e.getMessage(), e);
        }
    }

    public List<Document> parseAndGetDocuments(MultipartFile file) throws IOException {
        Resource resource = file.getResource();
        List<Document> documents = parseWithTika(resource);
        return tokenTextSplitter.apply(documents);
    }

    public List<Document> parseAndGetDocuments(File file) throws IOException {
        Resource resource = new FileSystemResource(file);
        List<Document> documents = parseWithTika(resource);
        return tokenTextSplitter.apply(documents);
    }

    public void addToVectorStore(MultipartFile file) throws IOException {
        if (vectorStore == null) {
            log.warn("VectorStore未配置，跳过向量化存储");
            return;
        }

        List<Document> documents = parseAndGetDocuments(file);
        vectorStore.write(documents);
        log.info("文档已添加到向量存储: {}", file.getOriginalFilename());
    }

    public void addToVectorStore(File file) throws IOException {
        if (vectorStore == null) {
            log.warn("VectorStore未配置，跳过向量化存储");
            return;
        }

        List<Document> documents = parseAndGetDocuments(file);
        vectorStore.write(documents);
        log.info("文档已添加到向量存储: {}", file.getName());
    }

    public String getSupportedFormats() {
        return "支持的文档格式: TXT, PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, HTML, XML 等";
    }
}
