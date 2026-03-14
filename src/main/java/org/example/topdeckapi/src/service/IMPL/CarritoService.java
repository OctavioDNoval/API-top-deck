package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.CarritoMapper;
import org.example.topdeckapi.src.DTOs.mappers.DetalleCarritoMapper;
import org.example.topdeckapi.src.DTOs.response.CarritoResponse;
import org.example.topdeckapi.src.DTOs.response.DetalleCarritoResponse;
import org.example.topdeckapi.src.Exception.CarritoNotFoundException;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IDetalleCarritoRepository;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.DetalleCarrito;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.ICarritoService;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class CarritoService implements ICarritoService {
    private final ICarritoRepository carritoRepository;
    private final IDetalleCarritoRepository detalleCarritoRepository;
    private final IUsuarioRepo usuarioRepo;
    private final IProductoRepo productoRepo;
    private final CarritoMapper carritoMapper;
    private final DetalleCarritoMapper detalleCarritoMapper;

    //METODOS PARA EL CARRITO DE USUARIO REGISTRADO
    public CarritoResponse obtenerCarritoPorUsuario(Long idUsuario){
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario no encontrado"));

        Carrito c = carritoRepository.findByUsuario(u)
                .orElseGet(()->{
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(u);
                    return carritoRepository.save(nuevoCarrito);
                });

        return carritoMapper.toResponse(c);
    }

    public DetalleCarritoResponse actualizarCantidad (Long idDetalle, Integer nuevaCantidad){
        DetalleCarrito dc = detalleCarritoRepository.findById(idDetalle).orElse(null);
        if(dc != null ){
            dc.setCantidad(nuevaCantidad);
            detalleCarritoRepository.save(dc);
        }
        return detalleCarritoMapper.toResponse(dc);
    }

    public List<DetalleCarritoResponse> obtenerDetalleCarrito(Long idCarrito){
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detalle = detalleCarritoRepository.findByCarrito(carrito);

        return detalle.stream()
                .map(detalleCarritoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public DetalleCarritoResponse agregarAlCarrito (Long idProducto, Long idCarrito, Integer cantidad){
        Producto p = productoRepo.findById(idProducto)
                .orElseThrow(()-> new RuntimeException("Producto no encontrado"));

        Carrito c = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        DetalleCarrito detalle = new DetalleCarrito();
        detalle.setCarrito(c);
        detalle.setCantidad(cantidad);
        detalle.setProducto(p);

        DetalleCarrito savedDetalle = detalleCarritoRepository.save(detalle);
        return detalleCarritoMapper.toResponse(savedDetalle);
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


    //METODO PARA EL CARRITO EFIMERO USUARIOS TIPO GUESS

    public CarritoResponse obtenerCarritoEfimero(String sessionId){
        Carrito carritoEfimero = carritoRepository.findBySessionId(sessionId)
                .orElseGet(()->{
                    Carrito carrito = new Carrito();
                    carrito.setSessionId(sessionId);
                    carrito.setFechaCreacion(LocalDateTime.now());
                    return carritoRepository.save(carrito);
                });

        return carritoMapper.toResponse(carritoEfimero);
    }
}
