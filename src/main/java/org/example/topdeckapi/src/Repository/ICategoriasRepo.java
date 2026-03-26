package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICategoriasRepo extends JpaRepository<Categoria,Long> {
    Optional<Categoria> findByNombre(String nombre);
}
