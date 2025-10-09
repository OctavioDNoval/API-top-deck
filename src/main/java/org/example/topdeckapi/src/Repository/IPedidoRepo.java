package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPedidoRepo extends JpaRepository<Pedido,Long> {
}
