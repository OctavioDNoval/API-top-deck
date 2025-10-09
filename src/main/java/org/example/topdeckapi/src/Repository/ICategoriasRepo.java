package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICategoriasRepo extends JpaRepository<Categoria,Long> {
}
