package com.example.elasticsearch.service;

import com.example.elasticsearch.entity.Product;
import com.example.elasticsearch.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    
    private final ProductRepository productRepository;
    private final EmbeddingService embeddingService;

    @Autowired
    public ProductService(ProductRepository productRepository, EmbeddingService embeddingService) {
        this.productRepository = productRepository;
        this.embeddingService = embeddingService;
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            product.setId(UUID.randomUUID().toString());
        }
        
        // 자동 벡터 생성
        if (product.getName() != null && !product.getName().isEmpty()) {
            try {
                log.info("상품 '{}' 벡터 생성 중...", product.getName());
                List<Float> vector = embeddingService.getVector(product.getName());
                product.setNameVector(vector);
                log.info("벡터 생성 완료 ({}차원)", vector.size());
            } catch (Exception e) {
                log.error("벡터 생성 실패 (상품: {}): {}", product.getName(), e.getMessage());
                // 벡터 생성 실패 시에도 상품은 저장됨 (벡터 없이)
            }
        }
        
        return productRepository.save(product);
    }

    public Optional<Product> getProduct(String id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        productRepository.findAll().forEach(products::add);
        return products;
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    public List<Product> searchByName(String name) {
        return productRepository.findByNameContaining(name);
    }

    public List<Product> searchByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> searchByPrice(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }
}

