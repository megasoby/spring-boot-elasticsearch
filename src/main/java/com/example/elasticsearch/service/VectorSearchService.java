package com.example.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.elasticsearch.entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {
    
    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;
    
    private static final String INDEX_NAME = "products_korean";
    private static final String VECTOR_FIELD = "name_vector";

    public VectorSearchService(ElasticsearchClient elasticsearchClient, EmbeddingService embeddingService) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = embeddingService;
    }

    /**
     * 벡터 기반 검색
     * @param queryText 검색어
     * @param topK 반환할 결과 수
     * @return 유사한 상품 리스트
     */
    public List<Product> vectorSearch(String queryText, int topK) {
        try {
            // 1. 검색어를 벡터로 변환
            List<Float> queryVector = embeddingService.getVector(queryText);

            // 2. Elasticsearch kNN 검색
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .knn(k -> k
                            .field(VECTOR_FIELD)
                            .queryVector(queryVector)
                            .k(topK)
                            .numCandidates(100)
                    )
                    .source(src -> src.filter(f -> f
                            .includes("id", "name", "description", "price", "category", "stock")
                    ))
            );

            // 3. 검색 실행
            SearchResponse<Product> response = elasticsearchClient.search(searchRequest, Product.class);

            // 4. 결과 반환
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("벡터 검색 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 벡터 기반 검색 (기본 5개 결과)
     * @param queryText 검색어
     * @return 유사한 상품 리스트
     */
    public List<Product> vectorSearch(String queryText) {
        return vectorSearch(queryText, 5);
    }
}

