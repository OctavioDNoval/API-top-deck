package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPedidoRepo extends JpaRepository<Pedido,Long> {
}
