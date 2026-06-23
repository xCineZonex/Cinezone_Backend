package com.cinezone.demo.service.impl;

import com.cinezone.demo.exception.BusinessRuleException;
import com.cinezone.demo.service.RedisStockService;
import com.cinezone.demo.repository.SystemAlertRepository;
import com.cinezone.demo.repository.ProductRepository;
import com.cinezone.demo.model.entity.SystemAlert;
import com.cinezone.demo.model.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import com.cinezone.demo.repository.ProductStockRepository;
import com.cinezone.demo.model.entity.ProductStock;

@Service
@RequiredArgsConstructor
public class RedisStockServiceImpl implements RedisStockService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SystemAlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    @Override
    public void decrementStock(Long productId, Long sedeId, int cantidad) {
        String key = "stock:product:" + productId + ":sede:" + sedeId;
        
        // Inicializar si no existe
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            ProductStock stock = productStockRepository.findByProductIdAndCinemaId(productId, sedeId).orElse(null);
            if (stock != null && stock.getStock() != null) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(stock.getStock()));
            } else {
                throw new BusinessRuleException("Stock no inicializado en base de datos para el producto ID: " + productId);
            }
        }

        // Decrementar atómicamente
        Long newStock = stringRedisTemplate.opsForValue().decrement(key, cantidad);
        
        if (newStock == null || newStock < 0) {
            // Revertir si quedó en negativo
            if (newStock != null) {
                stringRedisTemplate.opsForValue().increment(key, cantidad);
            }
            throw new BusinessRuleException("Stock insuficiente en tiempo real para el producto ID: " + productId);
        }

        // Si el stock nuevo es <= 20 y el anterior era > 20, enviar alerta
        if (newStock <= 20 && (newStock + cantidad) > 20) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                SystemAlert alert = SystemAlert.builder()
                        .sedeId(sedeId)
                        .emisorEmail("SYSTEM")
                        .receptorRol("JEFE_SALA")
                        .tipoAlerta("LOW_STOCK_VERIFICATION")
                        .mensaje("Alerta Automática: El producto '" + product.getNombre() + "' alcanzó " + newStock + " unidades. Por favor, corrobora el inventario físico y envía una solicitud de restock al Administrador de Sede.")
                        .leido(false)
                        .fechaCreacion(LocalDateTime.now())
                        .build();
                alertRepository.save(alert);
            }
        }
    }

    @Override
    public void syncStock(Long productId, Long sedeId, int newStock) {
        String key = "stock:product:" + productId + ":sede:" + sedeId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(newStock));
    }
}
