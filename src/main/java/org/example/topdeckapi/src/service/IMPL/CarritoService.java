package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.Exception.CarritoNotFoundException;
import org.example.topdeckapi.src.Exception.ProductNotFoundException;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IDetalleCarritoRepository;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.DetalleCarrito;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.ICarritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CarritoService implements ICarritoService {
    private final ICarritoRepository carritoRepository;
    private final IDetalleCarritoRepository detalleCarritoRepository;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;

    @Autowired

    public CarritoService(ICarritoRepository carritoRepository, IDetalleCarritoRepository detalleCarritoRepository, UsuarioService usuarioService, ProductoService productoService) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    public Carrito obtenerCarritoPorUsuario(Long idUsuario){
        UsuarioDTO usuarioDTO = usuarioService.buscarPorId(idUsuario)
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario no encontrado"));

        Usuario usuario = usuarioService.convertToEntity(usuarioDTO);

        return carritoRepository.findByUsuario(usuario)
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario no encontrado"));
    }

    public List<DetalleCarrito> obtenerDetalleCarrito(Long idCarrito){
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        return detalleCarritoRepository.findByCarrito(carrito);
    }

    public DetalleCarrito agregarAlCarrito (Long id_producto, Long id_carrito, int cantidad){
        Producto producto = productoService.buscarPorId(id_producto)
                .orElseThrow(()-> new ProductNotFoundException("Producto no encontrado"));

        Carrito carrito = carritoRepository.findById(id_carrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        return detalleCarritoRepository.findByCarritoAndProducto(carrito,producto)
                .map((p) ->{
                    p.setCantidad(p.getCantidad()+cantidad);
                    return detalleCarritoRepository.save(p);
                })
                .orElseGet(()->{
                    return detalleCarritoRepository.save(new DetalleCarrito() {{
                        setCarrito(carrito);
                        setProducto(producto);
                        setCantidad(cantidad);
                    }});
                });
    }

    public boolean deleteProducto (Long detalleCarrito){
        if(detalleCarritoRepository.existsById(detalleCarrito)){
            detalleCarritoRepository.deleteById(detalleCarrito);
            return true;
        }else
            return false;
    }

    public void borrarCarrito (Long idCarrito){
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detalles = detalleCarritoRepository.findByCarrito(carrito);
        detalleCarritoRepository.deleteAll(detalles);
    }
}
