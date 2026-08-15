package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDetallePedidoRepo extends JpaRepository<DetallePedido,Long> {
    List<DetallePedido> findByPedido(Pedido pedido);
    Optional<DetallePedido> findByUuid(String uuid);

    @Query(value = "SELECT p.nombre AS nombreProducto, c.nombre AS categoria, t.nombre AS tag, " +
            "SUM(dp.cantidad) AS cantidadVendida, SUM(dp.subtotal) AS revenue " +
            "FROM detallepedido dp " +
            "INNER JOIN producto p ON dp.id_producto = p.id_producto " +
            "INNER JOIN pedido pe ON dp.id_pedido = pe.id_pedido " +
            "LEFT JOIN categoria c ON p.id_categoria = c.id_categoria " +
            "LEFT JOIN tag t ON p.id_tag = t.id_tag " +
            "WHERE pe.estado = 'CONFIRMADO' " +
            "GROUP BY p.nombre, c.nombre, t.nombre " +
            "ORDER BY cantidadVendida DESC " +
            "LIMIT :#{#pageable.pageSize}", nativeQuery = true)
    List<Object[]> topProductosVendidos(@Param("pageable") org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT t.nombre AS tag, SUM(dp.subtotal) AS revenue " +
            "FROM detallepedido dp " +
            "INNER JOIN producto p ON dp.id_producto = p.id_producto " +
            "INNER JOIN pedido pe ON dp.id_pedido = pe.id_pedido " +
            "INNER JOIN tag t ON p.id_tag = t.id_tag " +
            "WHERE pe.estado = 'CONFIRMADO' " +
            "GROUP BY t.nombre", nativeQuery = true)
    List<Object[]> revenuePorTag();
}
