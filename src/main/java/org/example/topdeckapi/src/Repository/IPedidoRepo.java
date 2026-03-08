package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IPedidoRepo extends JpaRepository<Pedido,Long> {
    @Query("SELECT p FROM Pedido p WHERE " +
            "LOWER(p.estado) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.usuario.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.ipUsuario) LIKE LOWER(CONCAT('%', :search, '%')) ")
    Page<Pedido> findBySearch(@Param("search") String search, Pageable pageable);
}
