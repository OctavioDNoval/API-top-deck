package org.example.topdeckapi.src.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateDetallePedidoDTO;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreatePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.DetallePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.DetallePedidoDTOCompleto;
import org.example.topdeckapi.src.DTOs.DTO.PedidoDTO;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.service.IMPL.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    @GetMapping("/admin/getAll")
    public ResponseEntity<List<PedidoDTO>> listarPedidos(){
        List<PedidoDTO> pedidoDTOS = pedidoService.getAll();
        return ResponseEntity.ok(pedidoDTOS);
    }

    @GetMapping("/admin/{idPedido}/getDetalles")
    public ResponseEntity<List<DetallePedidoDTOCompleto>> getDetallesPedido(@PathVariable Long idPedido){
        return ResponseEntity.ok(pedidoService.getByPedidoId(idPedido));
    }

    @PostMapping("/public/newPedido")
    public ResponseEntity<Pedido> newPedido(@RequestBody CreatePedidoDTO pedidoDTO){
            System.out.println("pedidoDTO completo: " + pedidoDTO);

            if (pedidoDTO.getUsuarioDTO() != null) {
                System.out.println("UsuarioDTO: " + pedidoDTO.getUsuarioDTO());
                System.out.println("ID Usuario: " + pedidoDTO.getUsuarioDTO().getId_usuario());
            } else {
                System.out.println("UsuarioDTO es NULL!");
            }




        return ResponseEntity.ok(pedidoService.guardar(pedidoDTO));
    }

    @PostMapping("public/detalles")
    public ResponseEntity<List<DetallePedido>> newDetallePedido(@RequestBody List<CreateDetallePedidoDTO> detallePedido){
        List<DetallePedido> lista = pedidoService.guardarDetalles(detallePedido);
        return ResponseEntity.ok(lista);
    }
}
