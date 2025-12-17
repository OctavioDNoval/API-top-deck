package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDetallePedidoRepo extends JpaRepository<DetallePedido,Long> {
    List<DetallePedido> findByPedido(Pedido pedido);
}
