package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDetallePedidoRepo extends JpaRepository<DetallePedido,Long> {
}
