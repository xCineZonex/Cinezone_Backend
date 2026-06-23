package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.SystemAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemAlertRepository extends JpaRepository<SystemAlert, UUID> {
    List<SystemAlert> findBySedeIdAndReceptorRolAndLeidoFalseOrderByFechaCreacionDesc(Long sedeId, String receptorRol);
    List<SystemAlert> findBySedeIdAndTipoAlertaOrderByFechaCreacionDesc(Long sedeId, String tipoAlerta);
}
