package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.util.Montos;
import java.util.LinkedHashMap;
import java.math.BigDecimal;
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

import org.example.topdeckapi.src.Enumerados.TIPO_ENTREGA;
import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;
import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.PedidoNotFoundException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.*;
import org.example.topdeckapi.src.model.*;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.example.topdeckapi.src.util.FechaUtils;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

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
    private final AuditService auditService;
    private final EmailService emailService;

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

        Direccion direccionAsociada = null;
        if (newPedido.getTipoEntrega() == TIPO_ENTREGA.ENVIO) {
            if (newPedido.getIdDireccion() == null || newPedido.getIdDireccion().isBlank()) {
                throw new BussinesException("Elegí una dirección de envío");
            }
            direccionAsociada = direccionRepo.findByUuid(newPedido.getIdDireccion())
                    .orElseThrow(()-> new ResourceNotFoundException("Direccion asociada al pedido no encontrada"));

            if (!direccionAsociada.getUsuario().getIdUsuario().equals(usuarioAuth.getIdUsuario())) {
                throw new AccessDeniedException("La dirección no pertenece al usuario autenticado");
            }
        }

        pedido.setIpUsuario(newPedido.getIpUsuario());
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(ESTADO_PEDIDO.PENDIENTE);
        pedido.setUsuario(usuarioAuth);
        pedido.setDireccion(direccionAsociada);
        pedido.setTipoEntrega(newPedido.getTipoEntrega());
        pedido.setTelefonoContacto(newPedido.getTelefonoContacto().trim());
        guardarTelefonoSiFalta(usuarioAuth, newPedido.getTelefonoContacto());

        List<DetallePedido> detalles = construirDetalles(newPedido.getDetalles());
        pedido.setTotal(calcularTotal(detalles));
        Pedido pedidoGuardado = pedidoRepo.save(pedido);

        detalles.forEach(detalle -> detalle.setPedido(pedidoGuardado));
        List<DetallePedido> detallePedidosGuardado = detallePedidoRepo.saveAll(detalles);

        pedidoGuardado.setDetalles(detallePedidosGuardado);

        auditService.registrar("INSERT", "pedido");
        Pedido pedidoFinal = pedidoRepo.save(pedidoGuardado);
        notificarNuevoPedidoAdmins(pedidoFinal);
        return pedidoMapper.toResponse(pedidoFinal);
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

        boolean mismoEstado = estado == estadoAnterior;
        boolean transicionValida = mismoEstado
                || (estadoAnterior == ESTADO_PEDIDO.PENDIENTE
                    && (estado == ESTADO_PEDIDO.CONFIRMADO || estado == ESTADO_PEDIDO.RECHAZADO))
                || (estadoAnterior == ESTADO_PEDIDO.CONFIRMADO && estado == ESTADO_PEDIDO.RECHAZADO);

        if (!transicionValida) {
            throw new BussinesException(
                    "Transición de estado no permitida: " + estadoAnterior + " -> " + estado
            );
        }

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
            reponerStock(detalles);
        }

        pedido.setEstado(estado);
        Pedido pedidoActualizado = pedidoRepo.save(pedido);
        auditService.registrar("UPDATE", "pedido");

        if (estado == ESTADO_PEDIDO.CONFIRMADO && estadoAnterior != ESTADO_PEDIDO.CONFIRMADO) {
            enviarConfirmacionPedido(pedidoActualizado);
        }

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
                FechaUtils.parseFechaTerminos(request.getUsuario().getFechaAceptacionTerminos())
            );
            usuarioRepo.save(usuario);
        }

        Direccion direccion = null;
        if (request.getTipoEntrega() == TIPO_ENTREGA.ENVIO) {
            if (request.getDireccion() == null) {
                throw new BussinesException("Completá la dirección de envío");
            }
            direccion = direccionService.guardarDireccionParaGuest(request.getDireccion(), usuario);
        }
        guardarTelefonoSiFalta(usuario, request.getTelefonoContacto());

        List<DetallePedido> detalles = construirDetalles(request.getDetalles());
        BigDecimal total = calcularTotal(detalles);

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .direccion(direccion)
                .fechaPedido(LocalDateTime.now())
                .estado(ESTADO_PEDIDO.PENDIENTE)
                .ipUsuario(request.getIpUsuario())
                .tipoEntrega(request.getTipoEntrega())
                .telefonoContacto(request.getTelefonoContacto().trim())
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

        auditService.registrar("INSERT", "pedido");
        notificarNuevoPedidoAdmins(pedidoTerminado);
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
        // A confirmed order already took its units out of stock: give them back.
        if (pedido.getEstado() == ESTADO_PEDIDO.CONFIRMADO) {
            reponerStock(pedido.getDetalles());
        }
        auditService.registrar("DELETE", "pedido");
        pedidoRepo.delete(pedido);
        return true;
    }

    private void reponerStock(List<DetallePedido> detalles) {
        if (detalles == null) return;
        for (DetallePedido detalle : detalles) {
            Long productoId = detalle.getProducto().getIdProducto();
            Producto p = productoRepo.findByIdForUpdate(productoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
            p.setStock((p.getStock() != null ? p.getStock() : 0) + cantidad);
            productoRepo.save(p);
        }
    }

    /*
    * Builds the order lines from the request. Repeated products are merged
    * first, so stock is checked against the total quantity requested, not
    * line by line. Prices always come from the database.
    * */
    private List<DetallePedido> construirDetalles(List<DetallePedidoRequest> requests) {
        Map<String, Integer> cantidadesPorProducto = new LinkedHashMap<>();
        for (DetallePedidoRequest dp : requests) {
            if (dp == null || dp.getIdProducto() == null) {
                throw new BussinesException("Un detalle del pedido no tiene producto");
            }
            int cantidad = dp.getCantidad() != null ? dp.getCantidad() : 0;
            if (cantidad <= 0) {
                throw new BussinesException("La cantidad de un detalle del pedido no es valida");
            }
            cantidadesPorProducto.merge(dp.getIdProducto(), cantidad, Integer::sum);
        }

        List<DetallePedido> detalles = new ArrayList<>();
        cantidadesPorProducto.forEach((idProducto, cantidad) -> {
            Producto producto = productoRepo.findByUuid(idProducto)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new BussinesException("El producto no está disponible");
            }
            int stock = producto.getStock() != null ? producto.getStock() : 0;
            if (stock < cantidad) {
                throw new BussinesException("Stock insuficiente para el producto " + producto.getNombre());
            }
            detalles.add(DetallePedido.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecio())
                    .subTotal(Montos.subtotal(producto.getPrecio(), producto.getDescuento(), cantidad))
                    .build());
        });
        return detalles;
    }

    /** Keeps the customer's phone on their profile the first time they give one. */
    private void guardarTelefonoSiFalta(Usuario usuario, String telefono) {
        if (usuario != null && (usuario.getTelefono() == null || usuario.getTelefono().isBlank())) {
            usuario.setTelefono(telefono.trim());
            usuarioRepo.save(usuario);
        }
    }

    private BigDecimal calcularTotal(List<DetallePedido> detalles) {
        return detalles.stream()
                .map(DetallePedido::getSubTotal)
                .reduce(Montos.redondear(BigDecimal.ZERO), BigDecimal::add);
    }

    private void notificarNuevoPedidoAdmins(Pedido pedido) {
        try {
            List<Usuario> admins = usuarioRepo.findByRol(ROL.ADMIN);
            emailService.enviarNuevoPedidoAdmin(admins, pedido);
        } catch (Exception e) {
            log.error("Error al notificar nuevo pedido a admins: {}", e.getMessage());
        }
    }

    private void enviarConfirmacionPedido(Pedido pedido) {
        try {
            emailService.enviarPedidoConfirmado(pedido.getUsuario(), pedido);
        } catch (Exception e) {
            log.error("Error al enviar confirmacion de pedido: {}", e.getMessage());
        }
    }
}
