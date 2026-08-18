package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.CarritoMapper;
import org.example.topdeckapi.src.DTOs.mappers.DetalleCarritoMapper;
import org.example.topdeckapi.src.DTOs.request.DetalleCarritoRequest;
import org.example.topdeckapi.src.DTOs.response.CarritoResponse;
import org.example.topdeckapi.src.DTOs.response.DetalleCarritoResponse;
import org.example.topdeckapi.src.Exception.CarritoNotFoundException;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.ProductNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;
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
    private final UsuarioService usuarioService;

    //METODOS PARA EL CARRITO DE USUARIO REGISTRADO
    public CarritoResponse obtenerCarritoPorUsuario(Long idUsuario){
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(()-> new UsuarioNotFoundException("No se encontro Carrito relacionado al usuario. Contacte con soporte"));

        Carrito c = carritoRepository.findByUsuario(u)
                .orElseGet(()->{
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(u);
                    return carritoRepository.save(nuevoCarrito);
                });

        return carritoMapper.toResponse(c);
    }

    public DetalleCarritoResponse actualizarCantidad(String uuidDetalle, Integer nuevaCantidad){
        if(nuevaCantidad == null || nuevaCantidad <= 0){
            throw new BussinesException("La cantidad debe ser mayor a 0");
        }
        DetalleCarrito dc = detalleCarritoRepository.findByUuid(uuidDetalle)
                .orElseThrow(()-> new CarritoNotFoundException("Detalle de carrito no encontrado"));

        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null || !dc.getCarrito().getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
            throw new AccessDeniedException("No tienes acceso a este detalle de carrito");
        }

        Producto producto = dc.getProducto();
        int stock = producto.getStock() != null ? producto.getStock() : 0;
        if (nuevaCantidad > stock) {
            throw new BussinesException("Stock insuficiente para el producto " + producto.getNombre());
        }

        dc.setCantidad(nuevaCantidad);
        detalleCarritoRepository.save(dc);
        return detalleCarritoMapper.toResponse(dc);
    }

    public List<DetalleCarritoResponse> obtenerDetalleCarrito(String uuidCarrito){
        Carrito carrito = carritoRepository.findByUuid(uuidCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null || !carrito.getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
            throw new AccessDeniedException("No tienes acceso a este carrito");
        }

        List<DetalleCarrito> detalle = detalleCarritoRepository.findByCarrito(carrito);

        return detalle.stream()
                .map(detalleCarritoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public DetalleCarritoResponse agregarAlCarrito(DetalleCarritoRequest detalleCarritoRequest){
        Producto p = productoRepo.findByUuid(detalleCarritoRequest.getIdProducto())
                .orElseThrow(()-> new ProductNotFoundException("Producto no encontrado"));

        if (Boolean.FALSE.equals(p.getActivo())) {
            throw new BussinesException("El producto no está disponible");
        }

        int stock = p.getStock() != null ? p.getStock() : 0;
        int cantidadRequest = detalleCarritoRequest.getCantidad() != null ? detalleCarritoRequest.getCantidad() : 0;
        if (cantidadRequest <= 0) {
            throw new BussinesException("La cantidad debe ser mayor a 0");
        }
        if (cantidadRequest > stock) {
            throw new BussinesException("Stock insuficiente para el producto " + p.getNombre());
        }

        Carrito c;
        if(detalleCarritoRequest.getIdCarrito() != null && !detalleCarritoRequest.getIdCarrito().isEmpty()){
            c = carritoRepository.findByUuid(detalleCarritoRequest.getIdCarrito()).orElse(null);
            if (c == null) {
                c = crearCarritoParaUsuarioAutenticado();
            } else {
                Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
                if (usuarioAuth == null || !c.getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
                    throw new AccessDeniedException("No tienes acceso a este carrito");
                }
            }
        } else {
            c = crearCarritoParaUsuarioAutenticado();
        }

        DetalleCarrito detalleExistente = detalleCarritoRepository.findByProductoAndCarrito(p,c)
                .orElse(null);

        if(detalleExistente != null){
            int nuevaCantidad = detalleCarritoRequest.getCantidad() + detalleExistente.getCantidad();
            if (nuevaCantidad > stock) {
                throw new BussinesException("Stock insuficiente para el producto " + p.getNombre());
            }
            detalleExistente.setCantidad(nuevaCantidad);
            DetalleCarrito detalleGuardado = detalleCarritoRepository.save(detalleExistente);
            return detalleCarritoMapper.toResponse(detalleGuardado);
        }

        DetalleCarrito detalle = new DetalleCarrito();
        detalle.setCarrito(c);
        detalle.setProducto(p);
        detalle.setCantidad(detalleCarritoRequest.getCantidad());

        DetalleCarrito savedDetalle = detalleCarritoRepository.save(detalle);
        return detalleCarritoMapper.toResponse(savedDetalle);
    }

    private Carrito crearCarritoParaUsuarioAutenticado(){
        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return carritoRepository.findByUsuario(usuarioAuth)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(usuarioAuth);
                    nuevoCarrito.setFechaCreacion(LocalDateTime.now());
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    public boolean deleteProducto(String uuidDetalle){
        DetalleCarrito dc = detalleCarritoRepository.findByUuid(uuidDetalle)
                .orElseThrow(()-> new CarritoNotFoundException("Detalle de carrito no encontrado"));

        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null || !dc.getCarrito().getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
            throw new AccessDeniedException("No tienes acceso a este detalle de carrito");
        }

        detalleCarritoRepository.delete(dc);
        return true;
    }

    public void borrarCarrito(String uuidCarrito){
        Carrito carrito = carritoRepository.findByUuid(uuidCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null || !carrito.getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
            throw new AccessDeniedException("No tienes acceso a este carrito");
        }

        List<DetalleCarrito> detalles = detalleCarritoRepository.findByCarrito(carrito);
        detalleCarritoRepository.deleteAll(detalles);
    }

    public void borrarCarrito(Long idCarrito){
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detalles = detalleCarritoRepository.findByCarrito(carrito);
        detalleCarritoRepository.deleteAll(detalles);
    }


    // LEGACY: Métodos efímeros — el carrito efímero ahora se maneja 100% en localStorage del frontend
    //
    // public CarritoResponse obtenerCarritoEfimero(String sessionId){
    //     Carrito carritoEfimero = carritoRepository.findBySessionId(sessionId)
    //             .orElseGet(()->{
    //                 Carrito carrito = new Carrito();
    //                 carrito.setSessionId(sessionId);
    //                 carrito.setFechaCreacion(LocalDateTime.now());
    //                 return carritoRepository.save(carrito);
    //             });
    //     return carritoMapper.toResponse(carritoEfimero);
    // }
    //
    // public DetalleCarritoResponse agregarDetalleCarritoEfimero(DetalleCarritoRequest detalleCarritoRequest){
    //     Carrito c = carritoRepository.findByUuid(detalleCarritoRequest.getIdCarrito())
    //             .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));
    //     Producto p = productoRepo.findByUuid(detalleCarritoRequest.getIdProducto())
    //             .orElseThrow(()-> new ProductNotFoundException("Producto no encontrado"));
    //     DetalleCarrito detalleExistente = detalleCarritoRepository.findByProductoAndCarrito(p,c).orElse(null);
    //     if(detalleExistente != null){
    //         detalleExistente.setCantidad(detalleCarritoRequest.getCantidad() + detalleExistente.getCantidad());
    //         DetalleCarrito detalleGuardado = detalleCarritoRepository.save(detalleExistente);
    //         return detalleCarritoMapper.toResponse(detalleGuardado);
    //     }
    //     DetalleCarrito dc = detalleCarritoMapper.toEntity(detalleCarritoRequest);
    //     dc.setCarrito(c);
    //     dc.setProducto(p);
    //     DetalleCarrito savedDetalleCarrito = detalleCarritoRepository.save(dc);
    //     return detalleCarritoMapper.toResponse(savedDetalleCarrito);
    // }
    //
    // public Boolean eliminarDeCarritoEfimero(String uuidDetalleCarrito){
    //     DetalleCarrito dc = detalleCarritoRepository.findByUuid(uuidDetalleCarrito)
    //             .orElseThrow(()-> new CarritoNotFoundException("Detalle de carrito no encontrado"));
    //     detalleCarritoRepository.delete(dc);
    //     return true;
    // }
    //
    // public CarritoResponse mergeCarritoEfimeroToUser(String sessionId, Long idUsuario){
    //     Usuario usuario = usuarioRepo.findById(idUsuario)
    //             .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado"));
    //     Carrito userCarrito = carritoRepository.findByUsuario(usuario)
    //             .orElseGet(() -> {
    //                 Carrito nuevo = new Carrito();
    //                 nuevo.setUsuario(usuario);
    //                 nuevo.setFechaCreacion(LocalDateTime.now());
    //                 return carritoRepository.save(nuevo);
    //             });
    //     Carrito efimeroCarrito = carritoRepository.findBySessionId(sessionId).orElse(null);
    //     if (efimeroCarrito != null) {
    //         List<DetalleCarrito> efimeroDetalles = detalleCarritoRepository.findByCarrito(efimeroCarrito);
    //         for (DetalleCarrito det : efimeroDetalles) {
    //             DetalleCarrito existing = detalleCarritoRepository.findByProductoAndCarrito(det.getProducto(), userCarrito).orElse(null);
    //             if (existing != null) {
    //                 existing.setCantidad(existing.getCantidad() + det.getCantidad());
    //                 detalleCarritoRepository.save(existing);
    //             } else {
    //                 DetalleCarrito nuevoDetalle = new DetalleCarrito();
    //                 nuevoDetalle.setCarrito(userCarrito);
    //                 nuevoDetalle.setProducto(det.getProducto());
    //                 nuevoDetalle.setCantidad(det.getCantidad());
    //                 detalleCarritoRepository.save(nuevoDetalle);
    //             }
    //         }
    //         detalleCarritoRepository.deleteAll(efimeroDetalles);
    //     }
    //     return carritoMapper.toResponse(userCarrito);
    // }
}
