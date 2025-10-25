package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ICarritoRepository extends JpaRepository<Carrito,Long> {
    Optional<Carrito> findByUsuario(Usuario usuario);
}
