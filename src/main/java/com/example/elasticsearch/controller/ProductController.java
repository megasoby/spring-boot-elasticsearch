package com.example.elasticsearch.controller;

import com.example.elasticsearch.entity.Product;
import com.example.elasticsearch.service.ProductService;
import com.example.elasticsearch.service.VectorSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final VectorSearchService vectorSearchService;

    @Autowired
    public ProductController(ProductService productService, VectorSearchService vectorSearchService) {
        this.productService = productService;
        this.vectorSearchService = vectorSearchService;
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.saveProduct(product);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        Optional<Product> product = productService.getProduct(id);
        return product.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        Optional<Product> existing = productService.getProduct(id);
        if (existing.isPresent()) {
            product.setId(id);
            Product updated = productService.saveProduct(product);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<Product>> searchByName(@RequestParam String name) {
        List<Product> products = productService.searchByName(name);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/search/category")
    public ResponseEntity<List<Product>> searchByCategory(@RequestParam String category) {
        List<Product> products = productService.searchByCategory(category);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/search/price")
    public ResponseEntity<List<Product>> searchByPrice(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice) {
        List<Product> products = productService.searchByPrice(minPrice, maxPrice);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/search/vector")
    public ResponseEntity<List<Product>> vectorSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        try {
            List<Product> products = vectorSearchService.vectorSearch(query, topK);
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

