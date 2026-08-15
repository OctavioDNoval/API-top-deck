package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPedidoRepo extends JpaRepository<Pedido,Long> {
    @Query("SELECT p FROM Pedido p WHERE " +
            "LOWER(p.estado) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.usuario.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.ipUsuario) LIKE LOWER(CONCAT('%', :search, '%')) ")
    Page<Pedido> findBySearch(@Param("search") String search, Pageable pageable);
    Optional<Pedido> findByUuid(String uuid);
    List<Pedido> findByUsuario_IdUsuario(Long idUsuario);

    @Query(value = "SELECT p.estado, COUNT(*) FROM pedido p GROUP BY p.estado", nativeQuery = true)
    List<Object[]> contarPorEstado();

    @Query(value = "SELECT DATE_FORMAT(p.fecha_pedido, '%Y-%m') AS periodo, COALESCE(SUM(p.total), 0) AS total, COUNT(*) AS cantidad " +
            "FROM pedido p WHERE p.estado = 'CONFIRMADO' " +
            "GROUP BY DATE_FORMAT(p.fecha_pedido, '%Y-%m') ORDER BY periodo", nativeQuery = true)
    List<Object[]> ventasPorPeriodo();

    @Query(value = "SELECT DAYOFWEEK(p.fecha_pedido) AS dia, COUNT(*) AS cantidad " +
            "FROM pedido p WHERE p.estado = 'CONFIRMADO' " +
            "GROUP BY DAYOFWEEK(p.fecha_pedido) ORDER BY dia", nativeQuery = true)
    List<Object[]> pedidosPorDiaSemana();

    @Query(value = "SELECT COALESCE(AVG(p.total), 0) FROM pedido p WHERE p.estado = 'CONFIRMADO'", nativeQuery = true)
    Double ticketPromedio();

    @Query(value = "SELECT COALESCE(SUM(p.total), 0) FROM pedido p WHERE p.estado = 'CONFIRMADO'", nativeQuery = true)
    Double revenueTotal();

    @Query(value = "SELECT COUNT(*) FROM pedido p WHERE p.estado = 'PENDIENTE'", nativeQuery = true)
    Long contarPendientes();
}
