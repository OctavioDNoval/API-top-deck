package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreatePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.PedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdatePedidoDTO;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService implements IPedidoService {
    private final IPedidoRepo pedidoRepo;
    private final UsuarioService usuarioService;
    private final DetallePedidoService detallePedidoService;

    public PedidoService(IPedidoRepo pedidoRepo, UsuarioService usuarioService, @Lazy DetallePedidoService detallePedidoService) {
        this.pedidoRepo = pedidoRepo;
        this.usuarioService = usuarioService;
        this.detallePedidoService = detallePedidoService;
    }

    protected PedidoDTO convertToDTO(Pedido p) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId_pedido(p.getIdPedido());
        dto.setEstado(p.getEstado());
        dto.setTotal(p.getTotal());
        dto.setUsuario(usuarioService.convertToDto(p.getUsuario()));
        dto.setFecha_pedido(p.getFechaPedido());
        dto.setDetalles(p.getDetalles().stream()
                .map(detallePedidoService::convertEntityToDTO)
                .collect(Collectors.toList())
        );

        return dto;
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

    public Optional<Pedido> getEntityById(Long id){
        return pedidoRepo.findById(id);
    }

    public Pedido guardar(CreatePedidoDTO newPedido){
        Pedido entidad = convertCreateDTOToEntity(newPedido);
        return pedidoRepo.save(entidad);
    }

    public Optional<PedidoDTO> actualizarPedido(UpdatePedidoDTO dto, Long id){
        return pedidoRepo.findById(id)
                .map(p->{
                    if(dto.getFecha_pedido()!=null){
                        p.setFechaPedido(dto.getFecha_pedido());
                    }
                    if(dto.getPrecio()!=null){
                        p.setTotal(dto.getPrecio());
                    }
                    if(dto.getEstado()!=null){
                        p.setEstado(dto.getEstado());
                    }
                    Pedido pedidoActualizado = pedidoRepo.save(p);
                    return convertToDTO(pedidoActualizado);
                });
    }

    public boolean delete(Long id){
        if (pedidoRepo.existsById(id)) {
            pedidoRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
