package com.raul.ecommercehub.api.batch;

public class ProductSkuNotFoundException extends RuntimeException {
    public ProductSkuNotFoundException(String sku) {
        super("Product not found for SKU: " + sku);
    }
}