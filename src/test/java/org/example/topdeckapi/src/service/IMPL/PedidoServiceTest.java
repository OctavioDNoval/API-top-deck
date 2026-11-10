package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.mappers.PedidoMapper;
import org.example.topdeckapi.src.DTOs.request.DetallePedidoRequest;
import org.example.topdeckapi.src.DTOs.request.PedidoRequest;
import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;
import org.example.topdeckapi.src.Enumerados.TIPO_ENTREGA;
import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Repository.IDetallePedidoRepo;
import org.example.topdeckapi.src.Repository.IDireccionRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.IUsuarioRepo;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock private IPedidoRepo pedidoRepo;
    @Mock private IDetallePedidoRepo detallePedidoRepo;
    @Mock private IUsuarioRepo usuarioRepo;
    @Mock private IDireccionRepo direccionRepo;
    @Mock private IProductoRepo productoRepo;
    @Mock private UsuarioService usuarioService;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;
    @Mock private PedidoMapper pedidoMapper;

    @InjectMocks private PedidoService pedidoService;

    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1L);

        Direccion direccion = new Direccion();
        direccion.setUsuario(usuario);

        producto = new Producto();
        producto.setIdProducto(10L);
        producto.setNombre("Sobre");
        producto.setPrecio(new BigDecimal("1000.00"));
        producto.setDescuento(10);
        producto.setStock(5);
        producto.setActivo(true);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(direccionRepo.findByUuid("dir-1")).thenReturn(Optional.of(direccion));
        when(productoRepo.findByUuid("prod-1")).thenReturn(Optional.of(producto));
        when(productoRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(producto));
        when(pedidoRepo.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detallePedidoRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepo.findByRol(any())).thenReturn(List.of());
    }

    private static DetallePedidoRequest linea(String idProducto, int cantidad) {
        DetallePedidoRequest d = new DetallePedidoRequest();
        d.setIdProducto(idProducto);
        d.setCantidad(cantidad);
        return d;
    }

    private static PedidoRequest pedido(DetallePedidoRequest... lineas) {
        PedidoRequest r = new PedidoRequest();
        r.setIdDireccion("dir-1");
        r.setTipoEntrega(TIPO_ENTREGA.ENVIO);
        r.setTelefonoContacto("11 5555-1234");
        r.setDetalles(List.of(lineas));
        return r;
    }

    @Test
    void repeatedProductsAreCheckedAgainstTheirTotalQuantity() {
        // 3 + 3 = 6 > stock 5, even though each line alone fits.
        assertThatThrownBy(() -> pedidoService.guardar(pedido(linea("prod-1", 3), linea("prod-1", 3))))
                .isInstanceOf(BussinesException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(detallePedidoRepo, never()).saveAll(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void repeatedProductsAreMergedIntoOneLineWithExactTotals() {
        pedidoService.guardar(pedido(linea("prod-1", 2), linea("prod-1", 1)));

        ArgumentCaptor<List<DetallePedido>> captor = ArgumentCaptor.forClass(List.class);
        verify(detallePedidoRepo).saveAll(captor.capture());
        List<DetallePedido> detalles = captor.getValue();

        assertThat(detalles).hasSize(1);
        assertThat(detalles.get(0).getCantidad()).isEqualTo(3);
        // 1000 * 3 * 90% = 2700.00
        assertThat(detalles.get(0).getSubTotal()).isEqualTo(new BigDecimal("2700.00"));
        assertThat(detalles.get(0).getPedido().getTotal()).isEqualTo(new BigDecimal("2700.00"));
    }

    @Test
    void zeroQuantityIsRejected() {
        assertThatThrownBy(() -> pedidoService.guardar(pedido(linea("prod-1", 0))))
                .isInstanceOf(BussinesException.class);
    }

    private Pedido pedidoCapturado() {
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepo, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void pickupOrderNeedsNoAddressAndKeepsThePhone() {
        PedidoRequest r = pedido(linea("prod-1", 1));
        r.setIdDireccion(null);
        r.setTipoEntrega(TIPO_ENTREGA.RETIRO);

        pedidoService.guardar(r);

        Pedido guardado = pedidoCapturado();
        assertThat(guardado.getTipoEntrega()).isEqualTo(TIPO_ENTREGA.RETIRO);
        assertThat(guardado.getDireccion()).isNull();
        assertThat(guardado.getTelefonoContacto()).isEqualTo("11 5555-1234");
        verify(direccionRepo, never()).findByUuid(any());
    }

    @Test
    void shippingOrderWithoutAddressIsRejected() {
        PedidoRequest r = pedido(linea("prod-1", 1));
        r.setIdDireccion(" ");

        assertThatThrownBy(() -> pedidoService.guardar(r))
                .isInstanceOf(BussinesException.class)
                .hasMessageContaining("dirección");
    }

    @Test
    void phoneIsSavedOnTheProfileOnlyWhenMissing() {
        usuario.setTelefono("223 111-2222");
        pedidoService.guardar(pedido(linea("prod-1", 1)));
        assertThat(usuario.getTelefono()).isEqualTo("223 111-2222");

        usuario.setTelefono(null);
        pedidoService.guardar(pedido(linea("prod-1", 1)));
        assertThat(usuario.getTelefono()).isEqualTo("11 5555-1234");
    }

    private Pedido pedidoGuardado(ESTADO_PEDIDO estado, int cantidad) {
        DetallePedido detalle = DetallePedido.builder().producto(producto).cantidad(cantidad).build();
        Pedido p = new Pedido();
        p.setEstado(estado);
        p.setDetalles(new ArrayList<>(List.of(detalle)));
        when(pedidoRepo.findByUuid("ped-1")).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void deletingAConfirmedOrderGivesStockBack() {
        Pedido p = pedidoGuardado(ESTADO_PEDIDO.CONFIRMADO, 2);

        pedidoService.delete("ped-1");

        assertThat(producto.getStock()).isEqualTo(7);
        verify(pedidoRepo).delete(p);
    }

    @Test
    void deletingAPendingOrderDoesNotTouchStock() {
        pedidoGuardado(ESTADO_PEDIDO.PENDIENTE, 2);

        pedidoService.delete("ped-1");

        assertThat(producto.getStock()).isEqualTo(5);
        verify(productoRepo, never()).save(any());
    }
}
