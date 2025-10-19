package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreatePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.PedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService implements IPedidoService {
    private final IPedidoRepo pedidoRepo;
    private final UsuarioService usuarioService;
    private final DetallePedidoService detallePedidoService;

    protected PedidoDTO convertToDTO(Pedido p) {
        return new PedidoDTO(
                p.getIdPedido(),
                usuarioService.convertToDto(p.getUsuario()),
                p.getFechaPedido(),
                p.getTotal(),
                p.getDetalles().stream()
                        .map(detallePedidoService::convertEntityToDTO)
                        .collect(Collectors.toList())
        );
    }

    protected Pedido convertCreateDTOToEntity(CreatePedidoDTO createDto) {
        UsuarioDTO usuarioDTO = usuarioService.buscarPorId(createDto.getUsuarioDTO().getId_usuario())
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario con el id "+ createDto.getUsuarioDTO().getId_usuario() +" no encontrado"));

        Usuario u = usuarioService.convertToEntity(usuarioDTO);

        Pedido p = new Pedido();
        p.setUsuario(u);
        p.setFechaPedido(createDto.getFecha_pedido());
        p.setTotal(createDto.getPrecio());
        p.setDetalles(new ArrayList<>());

        List<DetallePedido> detalles = createDto.getDetalles().stream()
                .map(dto-> detallePedidoService.convertDTOToEntity(dto,p))
                .collect(Collectors.toList());

        p.setDetalles(detalles);

        return p;
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
