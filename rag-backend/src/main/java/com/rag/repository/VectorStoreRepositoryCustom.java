package com.rag.repository;

import java.util.List;

public interface VectorStoreRepositoryCustom {
    List<String> searchBySimilarity(String queryEmbedding, int topK);
}
