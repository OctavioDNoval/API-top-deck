package org.example.topdeckapi.src.service.IMPL;

import org.example.topdeckapi.src.DTOs.DTO.DetalleCarritoDTO;
import org.example.topdeckapi.src.DTOs.DTO.ProductoDTO;
import org.example.topdeckapi.src.DTOs.DTO.UsuarioDTO;
import org.example.topdeckapi.src.Exception.CarritoNotFoundException;
import org.example.topdeckapi.src.Exception.ProductNotFoundException;
import org.example.topdeckapi.src.Exception.UsuarioNotFoundException;
import org.example.topdeckapi.src.Repository.ICarritoRepository;
import org.example.topdeckapi.src.Repository.IDetalleCarritoRepository;
import org.example.topdeckapi.src.model.Carrito;
import org.example.topdeckapi.src.model.DetalleCarrito;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Usuario;
import org.example.topdeckapi.src.service.Interface.ICarritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;


@Service
public class CarritoService implements ICarritoService {
    private final ICarritoRepository carritoRepository;
    private final IDetalleCarritoRepository detalleCarritoRepository;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;

    @Autowired
    public CarritoService(ICarritoRepository carritoRepository, IDetalleCarritoRepository detalleCarritoRepository, UsuarioService usuarioService, ProductoService productoService) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    protected DetalleCarritoDTO convertToDTO(DetalleCarrito dc) {
        DetalleCarritoDTO dto = new DetalleCarritoDTO();
        dto.setId_carrito(dc.getCarrito().getIdCarrito());
        dto.setId_DetalleCarrito(dc.getId_detalle_carrito());
        dto.setCantidad(dc.getCantidad());

        Producto p = dc.getProducto();
        ProductoDTO pDto = new ProductoDTO();
        pDto.setIdProducto(p.getProductoId());
        pDto.setNombre(p.getNombre());
        pDto.setPrecio(p.getPrecio());
        pDto.setDescripcion(p.getDescripcion());
        pDto.setImg_url(p.getImg_url());

        dto.setProductoDTO(pDto);

        return dto;
    }

    public Carrito obtenerCarritoPorUsuario(Long idUsuario){
        UsuarioDTO usuarioDTO = usuarioService.buscarPorId(idUsuario)
                .orElseThrow(()-> new UsuarioNotFoundException("Usuario no encontrado"));

        Usuario usuario = usuarioService.convertToEntity(usuarioDTO);

        return carritoRepository.findByUsuario(usuario)
                .orElseGet(()->{
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(usuario);
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    public List<DetalleCarritoDTO> obtenerDetalleCarrito(Long idCarrito){
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detalle = detalleCarritoRepository.findByCarrito(carrito);

        return detalle.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DetalleCarritoDTO agregarAlCarrito (Long id_producto, Long id_carrito, int cantidad){
        Producto producto = productoService.buscarPorId(id_producto)
                .orElseThrow(()-> new ProductNotFoundException("Producto no encontrado"));

        Carrito carrito = carritoRepository.findById(id_carrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        DetalleCarrito detalle = new DetalleCarrito();
        detalle.setCarrito(carrito);
        detalle.setCantidad(cantidad);
        detalle.setProducto(producto);

        DetalleCarrito savedDetalle = detalleCarritoRepository.save(detalle);
        return convertToDTO(savedDetalle);
    }

    public boolean deleteProducto (Long detalleCarrito){
        if(detalleCarritoRepository.existsById(detalleCarrito)){
            detalleCarritoRepository.deleteById(detalleCarrito);
            return true;
        }else
            return false;
    }

    public void borrarCarrito (Long idCarrito){
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(()-> new CarritoNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detalles = detalleCarritoRepository.findByCarrito(carrito);
        detalleCarritoRepository.deleteAll(detalles);
    }
}
