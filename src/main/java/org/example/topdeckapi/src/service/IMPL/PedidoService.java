package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreatePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.PedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService implements IPedidoService {
    private final IPedidoRepo pedidoRepo;
    private final UsuarioService usuarioService;

    protected PedidoDTO convertToDTO(Pedido p) {
        return new PedidoDTO(
                p.getIdPedido(),
                usuarioService.convertToDto(p.getUsuario()),
                p.getFechaPedido(),
                p.getTotal(),
                p.getDetalles()
        );
    }

    protected Pedido convertCreateDTOToEntity(CreatePedidoDTO p) {
        Usuario u = usuarioService.buscarPorId(p.getUsuarioDTO().getId_usuario());

        return new Pedido(
                u,
                p.getFecha_pedido(),
                p.getPrecio(),
                p.getDetalles()
        );
    }

    public List<PedidoDTO> getAll(){
        return pedidoRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<PedidoDTO> getById(Long id){
        return pedidoRepo.findById(id)
                .map(this::convertToDTO);
    }

    public Pedido guardar(CreatePedidoDTO newPedido){
        Pedido entidad = convertCreateDTOToEntity(newPedido);
        return pedidoRepo.save(entidad);
    }

}
