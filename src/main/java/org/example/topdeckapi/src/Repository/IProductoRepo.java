package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IProductoRepo extends JpaRepository<Producto,Long> {
}
