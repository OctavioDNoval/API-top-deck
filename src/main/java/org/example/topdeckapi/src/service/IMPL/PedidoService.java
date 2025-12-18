package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.CreateDTO.CreateDetallePedidoDTO;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreatePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.*;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdatePedidoDTO;
import org.example.topdeckapi.src.Enumerados.ESTADO_PEDIDO;
import org.example.topdeckapi.src.Exception.PedidoNotFoundException;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.IDetallePedidoRepo;
import org.example.topdeckapi.src.Repository.IPedidoRepo;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Direccion;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.IPedidoService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService implements IPedidoService {
    private final IPedidoRepo pedidoRepo;
    private final IDetallePedidoRepo  detallePedidoRepo;
    private final UsuarioService usuarioService;
    private final DetallePedidoService detallePedidoService;
    private final DireccionService direccionService;

    public PedidoService(IPedidoRepo pedidoRepo, IDetallePedidoRepo detallePedidoRepo, UsuarioService usuarioService, DetallePedidoService detallePedidoService, DireccionService direccionService) {
        this.pedidoRepo = pedidoRepo;
        this.detallePedidoRepo = detallePedidoRepo;
        this.usuarioService = usuarioService;
        this.detallePedidoService = detallePedidoService;
        this.direccionService = direccionService;
    }

    protected PedidoDTO convertToDTO(Pedido p) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId_pedido(p.getIdPedido());
        dto.setEstado(p.getEstado());
        dto.setTotal(p.getTotal());
        dto.setDireccion(direccionService.convertToDTO(p.getDireccion()));
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

        DireccionDTO direccionDTO = direccionService.getById(createDto.getDireccionDTO().getId_direccion())
                .orElseThrow(RuntimeException::new);

        Direccion d = direccionService.convertToEntity(direccionDTO);

        Pedido p = new Pedido();
        p.setUsuario(u);
        p.setFechaPedido(createDto.getFecha_pedido());
        p.setTotal(createDto.getPrecio());
        p.setDireccion(d);
        p.setEstado(ESTADO_PEDIDO.PENDIENTE.name());
        p.setDetalles(new ArrayList<>());
        p.setIp_usuario(createDto.getIp_usuario());
        p.setTerminos_aceptados(createDto.getTerminos_aceptados());
        p.setVersion_terminos_y_condiciones(createDto.getVersion_terminos_y_condiciones());

        return p;
    }

    public List<DetallePedidoDTOCompleto> getByPedidoId(Long idPedido){
        Pedido p = pedidoRepo.findById(idPedido)
                .orElseThrow(()-> new PedidoNotFoundException("Pedido no encontrado"));

        List<DetallePedido> detalles = detallePedidoRepo.findByPedido(p);
        return detalles.stream()
                .map(detallePedidoService::convertEntityToDTOCompleto)
                .collect(Collectors.toList());
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

    public List<DetallePedido> guardarDetalles (List<CreateDetallePedidoDTO> pedidoDTOS){
        List<DetallePedido> detalles = new ArrayList<>();
        pedidoDTOS.forEach(dto->{
            detalles.add(detallePedidoService.guardar(dto));
        });
        return detalles;
    }

    public Optional<Pedido> getEntityById(Long id){
        return pedidoRepo.findById(id);
    }

    public Pedido guardar(CreatePedidoDTO newPedido){
        Pedido entidadPedido = convertCreateDTOToEntity(newPedido);
        entidadPedido.setDetalles(new ArrayList<>());
        Pedido pedidoGuardado = pedidoRepo.save(entidadPedido);
        List<DetallePedido> detalles = new ArrayList<>();

        for(CreateDetallePedidoDTO dto : newPedido.getDetalles()){
            dto.setId_pedido(pedidoGuardado.getIdPedido());
            DetallePedido detallePedido = detallePedidoService.convertDTOToEntity(dto,entidadPedido);
            DetallePedido detalleGuardado = detallePedidoService.guardar(dto);
            detalles.add(detalleGuardado);
        }
        return entidadPedido;
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

    public Optional<PedidoDTO> actualizarEstado(Long idPedido,String nuevoEstado){
        return pedidoRepo.findById(idPedido)
                .map(p->{
                    p.setEstado(nuevoEstado);
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
