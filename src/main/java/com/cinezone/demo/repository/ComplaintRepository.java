package com.cinezone.demo.repository;

import com.cinezone.demo.model.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    long countByEstado(String estado);

    @org.springframework.data.jpa.repository.Query("SELECT c.tipoReclamo, COUNT(c) FROM Complaint c " +
           "WHERE c.fechaReclamo >= :start AND c.fechaReclamo <= :end " +
           "GROUP BY c.tipoReclamo")
    java.util.List<Object[]> countByTipoReclamo(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                                                @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT c.sedeId, COUNT(c) FROM Complaint c GROUP BY c.sedeId")
    java.util.List<Object[]> countComplaintsGroupedBySedeId();

    long countByFechaReclamoBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    long countByFechaReclamoBetweenAndEstado(java.time.LocalDateTime start, java.time.LocalDateTime end, String estado);

    java.util.List<Complaint> findAllBySedeId(Long sedeId);
}
