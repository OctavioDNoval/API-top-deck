package org.example.topdeckapi.src.DTOs.response;

import java.util.List;

public record StatsResponse(
        ResumenGeneral resumen,
        List<VentaPorPeriodo> ventasPorPeriodo,
        List<ConteoEstado> pedidosPorEstado,
        List<ConteoNombre> productosPorCategoria,
        List<ConteoNombre> productosPorTag,
        List<TopProducto> topProductosVendidos,
        DistribucionStock stockDistribucion,
        List<ConteoNombre> usuariosPorRol,
        List<RegistroPorMes> usuariosPorMes,
        List<ConteoNombre> logsPorAccion,
        List<RegistroPorMes> logsPorPeriodo,
        List<ConteoNombre> eventosPorEstado,
        List<RevenueTag> revenuePorTag,
        List<ConteoNombre> pedidosPorDiaSemana
) {


    public record ResumenGeneral(
            long totalProductos,
            long totalUsuarios,
            long totalPedidos,
            double revenueTotal,
            long totalEventos,
            long pedidosPendientes,
            long productosSinStock,
            double ticketPromedio
    ) {}

    public record VentaPorPeriodo(
            String periodo,
            double total,
            int cantidadPedidos
    ) {}

    public record ConteoEstado(
            String estado,
            long cantidad
    ) {}

    public record ConteoNombre(
            String nombre,
            long cantidad
    ) {}

    public record TopProducto(
            String nombre,
            String categoria,
            String tag,
            int cantidadVendida,
            double revenue
    ) {}

    public record DistribucionStock(
            long sinStock,
            long bajo,
            long ok
    ) {}

    public record RegistroPorMes(
            String mes,
            long cantidad
    ) {}

    public record RevenueTag(
            String tag,
            double revenue
    ) {}
}
