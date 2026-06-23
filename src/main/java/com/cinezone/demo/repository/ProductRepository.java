package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    java.util.List<Product> findByCategoria(com.cinezone.demo.model.enums.ProductCategory categoria);
    java.util.List<Product> findByEsInsumo(Boolean esInsumo);
}