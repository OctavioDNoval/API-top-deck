package org.example.topdeckapi.src.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.topdeckapi.src.DTOs.request.PedidoEfimeroRequest;
import org.example.topdeckapi.src.DTOs.request.PedidoRequest;
import org.example.topdeckapi.src.DTOs.response.DetallePedidoResponse;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.PedidoResponse;
import org.example.topdeckapi.src.service.IMPL.DetallePedidoService;
import org.example.topdeckapi.src.service.IMPL.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pedidos")
@Slf4j
public class PedidoController {
    private final PedidoService pedidoService;
    private final DetallePedidoService detallePedidoService;

    @GetMapping("/admin/obtenerPaginados")
    public ResponseEntity<PaginacionResponse<PedidoResponse>> obtenerPedidosPaginados(
            @RequestParam(defaultValue = "1") Integer pagina,
            @RequestParam(defaultValue = "15") Integer tamanio,
            @RequestParam(defaultValue = "idPedido") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String filter
    ){
        PaginacionResponse<PedidoResponse> paginacionResponse;
        if(filter == null || filter.trim().isEmpty()){
            paginacionResponse = pedidoService.obtenerPaginados(pagina, tamanio, sortBy, direction);
        }else{
            paginacionResponse = pedidoService.obtenerPaginadosConFiltro(pagina, tamanio, sortBy, direction, filter);
        }
        return ResponseEntity.ok(paginacionResponse);
    }

    @GetMapping("/admin/{idPedido}/getDetalles")
    public ResponseEntity<List<DetallePedidoResponse>> getDetallesPedido(@PathVariable Long idPedido){
        return ResponseEntity.ok(detallePedidoService.obtenerDetallesByIdPedido(idPedido));
    }

    @PostMapping("/public/newPedido")
    public ResponseEntity<PedidoResponse> newPedido(@RequestBody PedidoRequest newPedido){
        log.info("PedidoRequest completo: {}", newPedido);

            if (newPedido.getIdUsuario() != null) {
                log.info("Usuario id: {}", newPedido.getIdUsuario());
            } else {
                log.info("Usuario es NULL!");
            }

        return ResponseEntity.ok(pedidoService.guardar(newPedido));
    }

    @PostMapping("/public/pedidoEfimero")
    public ResponseEntity<PedidoResponse> pedidoEfimero(@RequestBody PedidoEfimeroRequest newPedido){
        return ResponseEntity.ok(pedidoService.guardarPedidoEfimero(newPedido));
    }

    @PatchMapping("/admin/{idPedido}/newState")
    public ResponseEntity<PedidoResponse> actualizarEstadoPedido(@PathVariable Long idPedido, @RequestBody String newEstado) {
        PedidoResponse pedidoResponse = pedidoService.actualizarEstado(idPedido, newEstado);
        return ResponseEntity.ok(pedidoResponse);
    }

    @DeleteMapping("/admin/delete/{idPedido}")
    public ResponseEntity<Void> deletePedido(@PathVariable Long idPedido){
        boolean isDeleted = pedidoService.delete(idPedido);
        return isDeleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
