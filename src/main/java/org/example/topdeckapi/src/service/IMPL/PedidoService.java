package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import lombok.extern.slf4j.Slf4j;
import org.example.topdeckapi.src.DTOs.mappers.DetallePedidoMapper;
import org.example.topdeckapi.src.DTOs.mappers.PedidoMapper;
import org.example.topdeckapi.src.DTOs.request.DetallePedidoRequest;
import org.example.topdeckapi.src.DTOs.request.PedidoEfimeroRequest;
import org.example.topdeckapi.src.DTOs.request.PedidoRequest;

import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.PedidoResponse;

import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.PedidoNotFoundException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.*;
import org.example.topdeckapi.src.model.*;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PedidoService implements IPedidoService {
    private final PaginacionService paginacionService;
    private final IPedidoRepo pedidoRepo;
    private final IDetallePedidoRepo  detallePedidoRepo;
    private final PedidoMapper pedidoMapper;
    private final IUsuarioRepo usuarioRepo;
    private final IDireccionRepo direccionRepo;
    private final IProductoRepo productoRepo;
    private final UsuarioService usuarioService;
    private final DireccionService direccionService;
    private final DetallePedidoMapper detallePedidoMapper;

    private Sort buildSort (String sortBy, String direction){
        Map<String,String> mapeoCampos = Map.of(
                "idPedido", "idPedido",
                "estado", "estado",
                "total","total",
                "fechaPedido", "fechaPedido"
        );

        String campoReal = mapeoCampos.getOrDefault(sortBy,"fechaPedido");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, campoReal);
    }

    public PaginacionResponse<PedidoResponse> obtenerPaginados(Integer pagina, Integer tamanio, String sortBy, String direction){
        Sort sort = buildSort(sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Pedido> paginaPedido = pedidoRepo.findAll(pageable);
        return paginacionService.crearPaginacionResponse(paginaPedido,pagina,tamanio,pedidoMapper::toResponse);
    }

    public PaginacionResponse<PedidoResponse> obtenerPaginadosConFiltro(Integer pagina, Integer tamanio, String sortBy, String direction, String filtro){
        Sort sort = buildSort(sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Pedido> paginaPedido = pedidoRepo.findBySearch(filtro,pageable);
        return paginacionService.crearPaginacionResponse(paginaPedido,pagina,tamanio,pedidoMapper::toResponse);
    }

    public PedidoResponse getById(String uuid){
        Pedido p = pedidoRepo.findByUuid(uuid)
                .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));
        return pedidoMapper.toResponse(p);
    }

    public PedidoResponse guardar(PedidoRequest newPedido){
        if(newPedido.getDetalles() == null || newPedido.getDetalles().isEmpty()){
            throw new BussinesException("El pedido no tiene detalles");
        }

        Usuario usuarioAuth = usuarioService.obtenerUsuarioAutenticado();
        if (usuarioAuth == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        Pedido pedido = new Pedido();

        Direccion direccionAsociada = direccionRepo.findByUuid(newPedido.getIdDireccion())
                .orElseThrow(()-> new ResourceNotFoundException("Direccion asociada al pedido no encontrada"));

        pedido.setIpUsuario(newPedido.getIpUsuario());
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(ESTADO_PEDIDO.PENDIENTE);
        pedido.setUsuario(usuarioAuth);
        pedido.setDireccion(direccionAsociada);

        List<DetallePedido> detalles = newPedido.getDetalles().stream()
                .map(dp->{
                    Producto producto = productoRepo.findByUuid(dp.getIdProducto())
                            .orElseThrow(()-> new ResourceNotFoundException("Producto no encontrado"));

                    int cantidad = dp.getCantidad() != null ? dp.getCantidad() : 0;
                    if(cantidad <= 0){
                        throw new BussinesException("La cantidad de un detalle del pedido no es valida");
                    }
                    int stock = producto.getStock() != null ? producto.getStock() : 0;
                    if(stock < cantidad){
                        throw new BussinesException("Stock insuficiente para el producto " + producto.getNombre());
                    }

                    return DetallePedido.builder()
                            .producto(producto)
                            .cantidad(cantidad)
                            .precioUnitario(producto.getPrecio())
                            .subTotal(calcularSubTotal(producto, cantidad))
                            .build();
                })
                .collect(Collectors.toList());

        double total = detalles.stream()
                .mapToDouble(DetallePedido::getSubTotal)
                .sum();

        pedido.setTotal(total);
        Pedido pedidoGuardado = pedidoRepo.save(pedido);

        detalles.forEach(detalle -> detalle.setPedido(pedidoGuardado));
        List<DetallePedido> detallePedidosGuardado = detallePedidoRepo.saveAll(detalles);

        pedidoGuardado.setDetalles(detallePedidosGuardado);

        return pedidoMapper.toResponse(pedidoRepo.save(pedidoGuardado));
    }

    public PedidoResponse actualizarEstado(String uuidPedido, String nuevoEstado){
        Pedido pedido = pedidoRepo.findByUuid(uuidPedido)
                .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));

        ESTADO_PEDIDO estado;
        try{
            estado = ESTADO_PEDIDO.valueOf(nuevoEstado.toUpperCase());
        }catch (Exception e){
            throw new BussinesException("No es un valor permitido (" + nuevoEstado + ")\n" +
                                                "los valores permitidos son PENDIENTE, CONFIRMADO, RECHAZADO"
            );
        }

        ESTADO_PEDIDO estadoAnterior = pedido.getEstado();
        List<DetallePedido> detalles = pedido.getDetalles();

        if(estado == ESTADO_PEDIDO.CONFIRMADO && estadoAnterior != ESTADO_PEDIDO.CONFIRMADO){
            for(DetallePedido detalle: detalles){
                Long productoId = detalle.getProducto().getIdProducto();
                Producto p = productoRepo.findByIdForUpdate(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
                int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
                if(cantidad <= 0){
                    throw new BussinesException("La cantidad de un detalle del pedido no es valida");
                }
                int stock = p.getStock() != null ? p.getStock() : 0;
                if(stock < cantidad){
                    throw new BussinesException("Stock insuficiente para el producto " + p.getNombre());
                }
                p.setStock(stock - cantidad);
                productoRepo.save(p);
            }
        }else if(estadoAnterior == ESTADO_PEDIDO.CONFIRMADO && estado == ESTADO_PEDIDO.RECHAZADO){
            for(DetallePedido detalle: detalles){
                Long productoId = detalle.getProducto().getIdProducto();
                Producto p = productoRepo.findByIdForUpdate(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
                int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
                p.setStock((p.getStock() != null ? p.getStock() : 0) + cantidad);
                productoRepo.save(p);
            }
        }

        pedido.setEstado(estado);
        Pedido pedidoActualizado = pedidoRepo.save(pedido);
        return pedidoMapper.toResponse(pedidoActualizado);
    }

    public PedidoResponse guardarPedidoEfimero (PedidoEfimeroRequest request){
        if(request.getDetalles().isEmpty()) throw new BussinesException("El carrito esta vacio");
        if (!Boolean.TRUE.equals(request.getUsuario().getTerminosAceptados())) {
            throw new BussinesException("Debes aceptar los términos y condiciones");
        }

        Usuario usuario = usuarioService.crearUsuarioEfimero(request.getUsuario());

        if (request.getUsuario().getFechaAceptacionTerminos() != null) {
            usuario.setFechaAceptacionTerminos(
                Instant.parse(request.getUsuario().getFechaAceptacionTerminos()).atZone(ZoneId.systemDefault()).toLocalDateTime()
            );
            usuarioRepo.save(usuario);
        }

        Direccion direccion = direccionService.guardarDireccionParaGuest(request.getDireccion(),usuario);

        List<DetallePedido> detalles = new ArrayList<>();
        for(DetallePedidoRequest detallePedidoRequest : request.getDetalles()){
            DetallePedido dp = detallePedidoMapper.toEntity(detallePedidoRequest);
            Producto producto = productoRepo.findByUuid(detallePedidoRequest.getIdProducto())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detallePedidoRequest.getIdProducto()));
            log.info("Producto {}", producto);

            int cantidad = dp.getCantidad() != null ? dp.getCantidad() : 0;
            if(cantidad <= 0){
                throw new BussinesException("La cantidad de un detalle del pedido no es valida");
            }
            int stock = producto.getStock() != null ? producto.getStock() : 0;
            if(stock < cantidad){
                throw new BussinesException("Stock insuficiente para el producto " + producto.getNombre());
            }

            dp.setProducto(producto);
            dp.setCantidad(cantidad);
            dp.setPrecioUnitario(producto.getPrecio());
            dp.setSubTotal(calcularSubTotal(producto, cantidad));
            detalles.add(dp);
        }

        double total = detalles.stream()
                .mapToDouble(DetallePedido::getSubTotal)
                .sum();

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .direccion(direccion)
                .fechaPedido(LocalDateTime.now())
                .estado(ESTADO_PEDIDO.PENDIENTE)
                .ipUsuario(request.getIpUsuario())
                .total(total)
                .build();

        Pedido pedidoGuardado = pedidoRepo.save(pedido);

        detalles.forEach(detalle -> detalle.setPedido(pedidoGuardado));
        List<DetallePedido> detallesGuardados = detallePedidoRepo.saveAll(detalles);
        pedidoGuardado.setDetalles(detallesGuardados);

        Pedido pedidoTerminado = pedidoRepo.save(pedidoGuardado);
        Hibernate.initialize(pedidoTerminado.getDetalles());
        pedidoTerminado.getDetalles().forEach(detalle ->
                Hibernate.initialize(detalle.getProducto())
        );

        return pedidoMapper.toResponse(pedidoTerminado);
    }

    @Transactional
    public List<PedidoResponse> obtenerPedidosPorUsuario() {
        Usuario u = usuarioService.obtenerUsuarioAutenticado();
        if (u == null) {
            throw new ResourceNotFoundException("Usuario no autenticado");
        }
        return pedidoRepo.findByUsuario_IdUsuario(u.getIdUsuario())
                .stream()
                .map(pedidoMapper::toResponse)
                .collect(Collectors.toList());
    }

    public boolean delete(String uuid){
        Pedido pedido = pedidoRepo.findByUuid(uuid)
                .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));
        pedidoRepo.delete(pedido);
        return true;
    }

    private Double calcularSubTotal(Producto producto, Integer cantidad) {
        double precio = producto.getPrecio() != null ? producto.getPrecio() : 0.0;
        int descuento = producto.getDescuento() != null ? producto.getDescuento() : 0;
        int cant = cantidad != null ? cantidad : 0;
        return precio * cant * (1 - descuento / 100.0);
    }
}
