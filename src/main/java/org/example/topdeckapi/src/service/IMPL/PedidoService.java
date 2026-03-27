package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import org.example.topdeckapi.src.DTOs.mappers.DetallePedidoMapper;
import org.example.topdeckapi.src.DTOs.mappers.PedidoMapper;
import org.example.topdeckapi.src.DTOs.request.DetallePedidoRequest;
import org.example.topdeckapi.src.DTOs.request.PedidoEfimeroRequest;
import org.example.topdeckapi.src.DTOs.request.PedidoRequest;

import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.PedidoResponse;

import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;
import org.example.topdeckapi.src.Exception.PedidoNotFoundException;
import org.example.topdeckapi.src.Repository.*;
import org.example.topdeckapi.src.model.*;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
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
    private final ICarritoRepository carritoRepository;
    private final CarritoService carritoService;

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

    public PedidoResponse getById(Long id){
        Pedido p = pedidoRepo.findById(id)
                .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));
        return pedidoMapper.toResponse(p);
    }

    public PedidoResponse guardar(PedidoRequest newPedido){
        Pedido pedido = new Pedido();
        Usuario usuarioAsociado = usuarioRepo.findById(newPedido.getIdUsuario())
                .orElseThrow(()-> new RuntimeException("Usuario asociado al pedido no encontrado"));

        Direccion direccionAsociada = direccionRepo.findById(newPedido.getIdDireccion())
                .orElseThrow(()-> new RuntimeException("Direccion asociada al pedido no encontrada"));

        pedido.setIpUsuario(newPedido.getIpUsuario());
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(ESTADO_PEDIDO.PENDIENTE);
        pedido.setUsuario(usuarioAsociado);
        pedido.setDireccion(direccionAsociada);
        Pedido pedidoGuardado = pedidoRepo.save(pedido);

        List<DetallePedido> detalles = newPedido.getDetalles().stream()
                .map(dp->{
                    Producto producto = productoRepo.findById(dp.getIdProducto())
                            .orElseThrow(()-> new RuntimeException("Producto no encontrado"));

                    return DetallePedido.builder()
                            .pedido(pedidoGuardado)
                            .producto(producto)
                            .cantidad(dp.getCantidad())
                            .precioUnitario(dp.getPrecioUnitario())
                            .subTotal(dp.getPrecioUnitario()*dp.getCantidad())
                            .build();
                })
                .collect(Collectors.toList());

        double total = detalles.stream()
                .mapToDouble(DetallePedido::getSubTotal)
                .sum();

        List<DetallePedido> detallePedidosGuardado = detallePedidoRepo.saveAll(detalles);

        pedidoGuardado.setDetalles(detallePedidosGuardado);
        pedidoGuardado.setTotal(total);
        return pedidoMapper.toResponse(pedidoRepo.save(pedidoGuardado));
    }

    public PedidoResponse actualizarEstado(Long idPedido,String nuevoEstado){
        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));

        try{
            ESTADO_PEDIDO estado = ESTADO_PEDIDO.valueOf(nuevoEstado.toUpperCase());
            if(estado == ESTADO_PEDIDO.CONFIRMADO){
                List<DetallePedido> detalles = pedido.getDetalles();
                for(DetallePedido detalle: detalles){
                    Producto p =  detalle.getProducto();
                    p.setStock(p.getStock()-detalle.getCantidad());
                    productoRepo.save(p);
                }
            }
            if(pedido.getEstado() == ESTADO_PEDIDO.CONFIRMADO && estado == ESTADO_PEDIDO.RECHAZADO){
                List<DetallePedido> detalles = pedido.getDetalles();
                for(DetallePedido detalle: detalles){
                    Producto p =  detalle.getProducto();
                    p.setStock(p.getStock()+detalle.getCantidad());
                    productoRepo.save(p);
                }
            }

            pedido.setEstado(estado);
        }catch (Exception e){
            throw new IllegalArgumentException("No es un valor permitido (" + nuevoEstado + ")\n" +
                                                "los valores permitidos son PENDIENTE, CONFIRMADO, RECHAZADO"
            );
        }
        Pedido pedidoActualizado = pedidoRepo.save(pedido);
        return pedidoMapper.toResponse(pedidoActualizado);
    }

    public PedidoResponse guardarPedidoEfimero (PedidoEfimeroRequest request, String sessionId){
        if(request.getDetalles().isEmpty()) throw new RuntimeException("El carrito esta vacio");

        Usuario usuario = usuarioService.crearUsuarioEfimero(request.getUsuario());
        Direccion direccion = direccionService.guardarDireccionParaGuest(request.getDireccion(),usuario);
        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .direccion(direccion)
                .fechaPedido(LocalDateTime.now())
                .estado(ESTADO_PEDIDO.PENDIENTE)
                .ipUsuario(request.getIpUsuario())
                .build();

        pedido = pedidoRepo.save(pedido);

        List<DetallePedido> detalles = new ArrayList<>();
        for(DetallePedidoRequest detallePedidoRequest : request.getDetalles()){
            DetallePedido dp = detallePedidoMapper.toEntity(detallePedidoRequest);
            Producto producto = productoRepo.findById(detallePedidoRequest.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detallePedidoRequest.getIdProducto()));
            dp.setProducto(producto);
            dp.setPedido(pedido);
            if(dp.getProducto().getDescuento() > 0){
                dp.setSubTotal(dp.getPrecioUnitario() * dp.getCantidad() * (1-dp.getProducto().getDescuento() / 100));
            }else{
                dp.setSubTotal(dp.getPrecioUnitario() * dp.getCantidad());
            }
            detalles.add(dp);
        }
        List<DetallePedido> detallesGuardados = detallePedidoRepo.saveAll(detalles);
        pedido.setDetalles(detallesGuardados);
        double total = detallesGuardados.stream()
                .mapToDouble(DetallePedido::getSubTotal)
                .sum();
        pedido.setTotal(total);

        Pedido pedidoTerminado = pedidoRepo.save(pedido);
        Hibernate.initialize(pedidoTerminado.getDetalles());
        pedidoTerminado.getDetalles().forEach(detalle ->
                Hibernate.initialize(detalle.getProducto())
        );

        Carrito c = carritoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + sessionId));
        carritoService.borrarCarrito(c.getIdCarrito());
        return pedidoMapper.toResponse(pedidoTerminado);
    }

    public boolean delete(Long id){
        if (pedidoRepo.existsById(id)) {
            pedidoRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
