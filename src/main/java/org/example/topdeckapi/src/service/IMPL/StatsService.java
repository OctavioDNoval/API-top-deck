package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.response.StatsResponse;
import org.example.topdeckapi.src.Repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final IProductoRepo productoRepo;
    private final IPedidoRepo pedidoRepo;
    private final IUsuarioRepo usuarioRepo;
    private final IDetallePedidoRepo detallePedidoRepo;
    private final IAuditRepository auditRepository;
    private final IEventoRepository eventoRepository;

    private static final String[] DIAS_SEMANA = {"Domingo", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado"};

    public StatsResponse obtenerEstadisticas() {

        // ── Resumen General ──
        long totalProductos = productoRepo.count();
        long totalUsuarios = usuarioRepo.count();
        long totalPedidos = pedidoRepo.count();
        double revenueTotal = pedidoRepo.revenueTotal() != null ? pedidoRepo.revenueTotal() : 0;
        long totalEventos = eventoRepository.count();
        long pedidosPendientes = pedidoRepo.contarPendientes() != null ? pedidoRepo.contarPendientes() : 0;
        long productosSinStock = productoRepo.contarSinStock() != null ? productoRepo.contarSinStock() : 0;
        double ticketPromedio = pedidoRepo.ticketPromedio() != null ? pedidoRepo.ticketPromedio() : 0;

        StatsResponse.ResumenGeneral resumen = new StatsResponse.ResumenGeneral(
                totalProductos, totalUsuarios, totalPedidos,
                revenueTotal, totalEventos, pedidosPendientes,
                productosSinStock, ticketPromedio
        );

        // ── Ventas por Periodo ──
        List<StatsResponse.VentaPorPeriodo> ventasPorPeriodo = pedidoRepo.ventasPorPeriodo().stream()
                .map(arr -> new StatsResponse.VentaPorPeriodo(
                        (String) arr[0],
                        ((Number) arr[1]).doubleValue(),
                        ((Number) arr[2]).intValue()
                ))
                .toList();

        // ── Pedidos por Estado ──
        List<StatsResponse.ConteoEstado> pedidosPorEstado = pedidoRepo.contarPorEstado().stream()
                .map(arr -> new StatsResponse.ConteoEstado(
                        (String) arr[0],
                        ((Number) arr[1]).longValue()
                ))
                .toList();

        // ── Productos por Categoria ──
        List<StatsResponse.ConteoNombre> productosPorCategoria = productoRepo.contarPorCategoria().stream()
                .map(arr -> new StatsResponse.ConteoNombre(
                        (String) arr[0],
                        ((Number) arr[1]).longValue()
                ))
                .toList();

        // ── Productos por Tag ──
        List<StatsResponse.ConteoNombre> productosPorTag = productoRepo.contarPorTag().stream()
                .map(arr -> new StatsResponse.ConteoNombre(
                        (String) arr[0],
                        ((Number) arr[1]).longValue()
                ))
                .toList();

        // ── Top Productos Vendidos ──
        List<StatsResponse.TopProducto> topProductosVendidos = detallePedidoRepo
                .topProductosVendidos(PageRequest.of(0, 10)).stream()
                .map(arr -> new StatsResponse.TopProducto(
                        (String) arr[0],
                        arr[1] != null ? (String) arr[1] : "Sin categoria",
                        arr[2] != null ? (String) arr[2] : "Sin tag",
                        ((Number) arr[3]).intValue(),
                        ((Number) arr[4]).doubleValue()
                ))
                .toList();

        // ── Distribucion Stock ──
        List<Object[]> stockList = productoRepo.distribucionStock();
        Object[] stockRaw = stockList.isEmpty() ? new Object[]{0L, 0L, 0L} : stockList.get(0);
        StatsResponse.DistribucionStock stockDistribucion = new StatsResponse.DistribucionStock(
                stockRaw[0] != null ? ((Number) stockRaw[0]).longValue() : 0,
                stockRaw[1] != null ? ((Number) stockRaw[1]).longValue() : 0,
                stockRaw[2] != null ? ((Number) stockRaw[2]).longValue() : 0
        );

        // ── Usuarios por Rol ──
        List<StatsResponse.ConteoNombre> usuariosPorRol = usuarioRepo.contarPorRol().stream()
                .map(arr -> new StatsResponse.ConteoNombre(
                        (String) arr[0],
                        ((Number) arr[1]).longValue()
                ))
                .toList();

        // ── Usuarios por Mes (no se renderiza en dashboard actual, se deja vacio) ──
        List<StatsResponse.RegistroPorMes> usuariosPorMes = List.of();

        // ── Logs por Accion ──
        List<StatsResponse.ConteoNombre> logsPorAccion = auditRepository.contarPorAccion().stream()
                .map(arr -> new StatsResponse.ConteoNombre(
                        (String) arr[0],
                        ((Number) arr[1]).longValue()
                ))
                .toList();

        // ── Logs por Periodo (no se renderiza en dashboard actual, se deja vacio) ──
        List<StatsResponse.RegistroPorMes> logsPorPeriodo = List.of();

        // ── Eventos por Estado ──
        List<StatsResponse.ConteoNombre> eventosPorEstado = eventoRepository.contarPorEstado().stream()
                .map(arr -> new StatsResponse.ConteoNombre(
                        (String) arr[0],
                        ((Number) arr[1]).longValue()
                ))
                .toList();

        // ── Revenue por Tag ──
        List<StatsResponse.RevenueTag> revenuePorTag = detallePedidoRepo.revenuePorTag().stream()
                .map(arr -> new StatsResponse.RevenueTag(
                        (String) arr[0],
                        ((Number) arr[1]).doubleValue()
                ))
                .toList();

        // ── Pedidos por Dia de la Semana ──
        // DAYOFWEEK: 1=Domingo, 2=Lunes, ..., 7=Sabado
        List<StatsResponse.ConteoNombre> pedidosPorDiaSemana = pedidoRepo.pedidosPorDiaSemana().stream()
                .map(arr -> {
                    int dayIndex = ((Number) arr[0]).intValue();
                    String diaNombre = (dayIndex >= 1 && dayIndex <= 7) ? DIAS_SEMANA[dayIndex - 1] : "Desconocido";
                    return new StatsResponse.ConteoNombre(diaNombre, ((Number) arr[1]).longValue());
                })
                .toList();

        // ── Construir respuesta ──
        return new StatsResponse(
                resumen,
                ventasPorPeriodo,
                pedidosPorEstado,
                productosPorCategoria,
                productosPorTag,
                topProductosVendidos,
                stockDistribucion,
                usuariosPorRol,
                usuariosPorMes,
                logsPorAccion,
                logsPorPeriodo,
                eventosPorEstado,
                revenuePorTag,
                pedidosPorDiaSemana
        );
    }
}
