package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {
    Optional<ProductStock> findByProductIdAndCinemaId(Long productId, Long cinemaId);
    List<ProductStock> findByCinemaId(Long cinemaId);
    void deleteByProductId(Long productId);
}
