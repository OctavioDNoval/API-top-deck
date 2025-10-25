package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.DetalleCarrito;

import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IDetalleCarritoRepository extends JpaRepository<DetalleCarrito,Long> {
    List<DetalleCarrito> findByCarrito(Carrito carrito);
    Optional<DetalleCarrito> findByCarritoAndProducto(Carrito carrito, Producto producto);
}
