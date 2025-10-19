package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateDetallePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.DetallePedidoDTO;
import org.example.topdeckapi.src.DTOs.DTO.PedidoDTO;
import org.example.topdeckapi.src.DTOs.UpdateDTO.UpdateDetallePedidoDTO;
import org.example.topdeckapi.src.Exception.PedidoNotFoundException;
import org.example.topdeckapi.src.Exception.ProductNotFoundException;
import org.example.topdeckapi.src.Repository.IDetallePedidoRepo;
import org.example.topdeckapi.src.model.DetallePedido;
import org.example.topdeckapi.src.model.Pedido;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.service.Interface.IDetallePedidoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetallePedidoService implements IDetallePedidoService{
    private final IDetallePedidoRepo detallePedidoRepo;
    private final ProductoService productoService;
    private final PedidoService pedidoService;


    protected DetallePedidoDTO convertEntityToDTO (DetallePedido dp){
        Long idProducto = dp.getProducto() != null ? dp.getProducto().getProductoId() : null;
        Long idPedido = dp.getPedido() != null ? dp.getPedido().getIdPedido() : null;

        return new DetallePedidoDTO(
                dp.getIdDetallePedido(),
                dp.getCantidad(),
                dp.getPrecioUnitario(),
                idProducto,
                idPedido
        );
    }

    protected DetallePedido convertDTOToEntity(DetallePedidoDTO dto, Pedido p){
        if(dto == null) return null;

        Optional<Producto> prod = productoService.buscarPorId(dto.getId_producto());
        Producto producto = prod.orElseThrow(()-> new ProductNotFoundException("Producto no encontrado"));

        DetallePedido dp = new DetallePedido();
        dp.setIdDetallePedido(dto.getId_detalle_pedido());
        dp.setCantidad(dto.getCantidad());
        dp.setPrecioUnitario(dto.getPrecio_unitario());
        dp.setProducto(producto);
        dp.setPedido(p);
        return dp;
    }

    protected DetallePedido convertDTOToEntity(CreateDetallePedidoDTO dto, Pedido p){
        if (dto == null) return null;

        Producto producto = productoService.buscarPorId(dto.getId_Producto())
                .orElseThrow(()-> new ProductNotFoundException("Producto "+ dto.getId_Producto() +"no encontrado"));

        DetallePedido dp = new DetallePedido();
        dp.setCantidad(dto.getCantidad());
        dp.setPrecioUnitario(dto.getPrecio_unitario());
        dp.setProducto(producto);
        dp.setPedido(p);

        return dp;
    }

    public List<DetallePedidoDTO> getAll(){
        return detallePedidoRepo.findAll().stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    public Optional<DetallePedidoDTO> getById(Long id){
        return detallePedidoRepo.findById(id)
                .map(this::convertEntityToDTO);
    }

    public DetallePedido guardar(CreateDetallePedidoDTO dto){
        Pedido p = pedidoService.getEntityById(dto.getId_pedido())
                .orElseThrow(()-> new PedidoNotFoundException("Pedido con id:"+ dto.getId_pedido()+" no encontrado"));

        DetallePedido entidad = convertDTOToEntity(dto,p);
        return detallePedidoRepo.save(entidad);
    }

    public Optional<DetallePedidoDTO> actualizarDetallePedido(UpdateDetallePedidoDTO dto, Long id){
        return detallePedidoRepo.findById(id)
                .map(dp->{
                    if(dto.getCantidad()!= null){
                        dp.setCantidad(dto.getCantidad());
                    }

                    DetallePedido dpActualizado = detallePedidoRepo.save(dp);
                    return convertEntityToDTO(dpActualizado);
                });
    }

    public boolean delete(Long id){
        if (detallePedidoRepo.existsById(id)) {
            detallePedidoRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }
}
