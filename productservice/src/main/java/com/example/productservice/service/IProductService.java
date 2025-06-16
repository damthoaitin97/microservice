package com.example.productservice.service;


import com.example.productservice.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductService {
    Mono<Product> createProduct(Product product);
    Flux<Product> getAllProducts();
}