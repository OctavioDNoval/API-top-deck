package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.mappers.DetallePedidoMapper;
import org.example.topdeckapi.src.DTOs.request.DetallePedidoRequest;
import org.example.topdeckapi.src.DTOs.response.DetallePedidoResponse;
import org.example.topdeckapi.src.Exception.PedidoNotFoundException;
import org.example.topdeckapi.src.Repository.IDetallePedidoRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.service.Interface.IDetallePedidoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class DetallePedidoService implements IDetallePedidoService{
    private final IDetallePedidoRepo detallePedidoRepo;
    private final IPedidoRepo pedidoRepo;
    private final DetallePedidoMapper detallePedidoMapper;

    public DetallePedidoResponse getById(Long id){
        return detallePedidoRepo.findById(id)
                .map(detallePedidoMapper::toResponse)
                .orElseThrow(()-> new RuntimeException("Detalle pedido No encontrado"));
    }

    public DetallePedidoResponse guardar(DetallePedidoRequest dp){
        Pedido p = pedidoRepo.findById(dp.getIdPedido())
                .orElseThrow(()-> new PedidoNotFoundException("Pedido con id:"+ dp.getIdPedido()+" no encontrado"));
        DetallePedido d = detallePedidoMapper.toEntity(dp);
        d.setPedido(p);
        DetallePedido detallePedidoGuardado = detallePedidoRepo.save(d);
        return detallePedidoMapper.toResponse(detallePedidoGuardado);
    }

    public DetallePedidoResponse actualizarDetallePedido(DetallePedidoRequest dto, Long id){
        DetallePedido dp = detallePedidoRepo.findById(id)
                .orElseThrow(()-> new PedidoNotFoundException("Detalle pedido no encontrado"));

        dp.setCantidad(dto.getCantidad());
        return detallePedidoMapper.toResponse(
                detallePedidoRepo.save(dp)
        );
    }

    public boolean delete(Long id){
        if (detallePedidoRepo.existsById(id)) {
            detallePedidoRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }

    public List<DetallePedidoResponse> obtenerDetallesByIdPedido(Long idPedido){
        Pedido p = pedidoRepo.findById(idPedido)
                        .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));
        List<DetallePedido> detales = detallePedidoRepo.findByPedido(p);
        return detales.stream()
                .map(detallePedidoMapper::toResponse)
                .collect(Collectors.toList());
    }
}
