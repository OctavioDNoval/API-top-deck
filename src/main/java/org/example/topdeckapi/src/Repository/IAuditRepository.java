package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Audit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IAuditRepository extends JpaRepository<Audit,Long> {

    @Query(value = "SELECT a.accion, COUNT(*) FROM auditoria a GROUP BY a.accion", nativeQuery = true)
    List<Object[]> contarPorAccion();

    @Query(value = "SELECT TO_CHAR(a.fecha_audit, 'YYYY-MM') AS mes, COUNT(*) FROM auditoria a " +
            "GROUP BY TO_CHAR(a.fecha_audit, 'YYYY-MM') ORDER BY mes", nativeQuery = true)
    List<Object[]> logsPorPeriodo();

    Page<Audit> findByAccionIgnoreCase(String accion, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Audit a WHERE a.fechaAudit < :fecha")
    int eliminarAntiguos(@Param("fecha") LocalDateTime fecha);
}
