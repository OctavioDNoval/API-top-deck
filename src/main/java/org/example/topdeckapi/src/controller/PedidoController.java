package org.example.topdeckapi.src.controller;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreatePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.PedidoDTO;
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

    @PostMapping("/public/newPedido")
    public ResponseEntity<Pedido> newPedido(@RequestBody CreatePedidoDTO pedidoDTO){
        return ResponseEntity.ok(pedidoService.guardar(pedidoDTO));
    }
}
