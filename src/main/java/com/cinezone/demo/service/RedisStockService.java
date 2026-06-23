package com.cinezone.demo.service;

public interface RedisStockService {
    void decrementStock(Long productId, Long sedeId, int cantidad);
    void syncStock(Long productId, Long sedeId, int newStock);
}
