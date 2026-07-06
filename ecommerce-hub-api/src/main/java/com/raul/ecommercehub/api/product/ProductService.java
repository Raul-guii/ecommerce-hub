package com.raul.ecommercehub.api.product;

import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(UUID tenantId, ProductRequest request) {
        Product product = new Product(
                UUID.randomUUID(),
                tenantId,
                request.sku(),
                request.name(),
                request.stockQuantity(),
                request.price(),
                LocalDateTime.now());
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.updateDetails(request.sku(), request.name(), request.stockQuantity(), request.price());
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(), product.getSku(), product.getName(),
                product.getStockQuantity(), product.getPrice(), product.getUpdatedAt());
    }
}