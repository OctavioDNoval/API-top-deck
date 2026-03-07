package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import org.example.topdeckapi.src.DTOs.mappers.ProductoMapper;
import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.Security.AuditUtils;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Tag;
import org.example.topdeckapi.src.service.Interface.IProductoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService implements IProductoService {
    private final IProductoRepo productoRepo;
    private final ICategoriasRepo  categoriasRepo;
    private final ITagRepository tagRepository;
    private final AuditUtils  auditUtils;
    private final PaginacionService paginationService;
    private final ProductoMapper productoMapper;

    private Sort buildSort(String sortBy, String direction){
        Map<String,String> mapeoCampos = Map.of(
                "nombre", "nombre",
                "precio", "precio",
                "idProducto", "idProducto"
        );

        String campoReal = mapeoCampos.getOrDefault(sortBy, "idProducto");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, campoReal);
    }

    public PaginacionResponse<ProductoResponse> obtenerPaginados(Integer pagina, Integer tamanio, String sortBy, String direction){
        Sort sort = buildSort(sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Producto> paginaProducto = productoRepo.findAll(pageable);
        return paginationService.crearPaginacionResponse(paginaProducto,pagina,tamanio,productoMapper::toResponse);
    }

    public PaginacionResponse<ProductoResponse> obtenerPaginadosConFiltro(Integer pagina, Integer tamanio, String sortBy, String direction, String filter){
        Sort sort = buildSort(sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);
        Page<Producto> paginaProducto = productoRepo.findBySearch(filter, pageable);
        return  paginationService.crearPaginacionResponse(paginaProducto,pagina,tamanio,productoMapper::toResponse);
    }

    public ProductoResponse guardar(ProductoRequest producto) {
        if(productoRepo.existsByNombre(producto.getNombre())){
            throw new RuntimeException("El producto ya existe");
        }
        Producto nuevoProducto = productoMapper.toEntity(producto);
        Producto productoGuardado = productoRepo.save(nuevoProducto);
        auditUtils.setCurrentUserForAudit();
        return productoMapper.toResponse(productoGuardado);

    }

    public ProductoResponse buscarPorId(Long id) {
        return productoMapper.toResponse(productoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe el producto")));
    }

    public ProductoResponse actualizarProducto(Long id, ProductoRequest newProducto) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe el producto"));

        if(newProducto.getNombre() != null && !newProducto.getNombre().trim().isEmpty()){
            String nuevoNombre = newProducto.getNombre().trim();
            if(!p.getNombre().equals(nuevoNombre)) {
                if(productoRepo.existsByNombre(nuevoNombre)) {
                    throw new RuntimeException("Ya existe un producto con el nombre: " + nuevoNombre);
                }
                p.setNombre(nuevoNombre);
            }
        }
        if(newProducto.getIdCategoria() != null &&
                !p.getCategoria().getIdCategoria().equals(newProducto.getIdCategoria())){
            Categoria categoria = categoriasRepo.findById(newProducto.getIdCategoria())
                    .orElseThrow(() -> new RuntimeException("No existe la categoría"));
            p.setCategoria(categoria);
        }
        if(newProducto.getIdTag() != null &&
                !p.getTag().getIdTag().equals(newProducto.getIdTag())){
            Tag tag = tagRepository.findById(newProducto.getIdTag())
                    .orElseThrow(() -> new RuntimeException("No existe el tag"));
            p.setTag(tag);
        }
        Optional.ofNullable(newProducto.getDescripcion())
                .ifPresent(desc -> p.setDescripcion(desc.trim()));

        Optional.ofNullable(newProducto.getStock())
                .ifPresent(p::setStock);

        Optional.ofNullable(newProducto.getPrecio())
                .ifPresent(p::setPrecio);

        Optional.ofNullable(newProducto.getImgUrl())
                .ifPresent(img -> p.setImgUrl(img.trim()));

        Optional.ofNullable(newProducto.getDescuento())
                .ifPresent(p::setDescuento);

        Producto productoActualizado = productoRepo.save(p);
        return productoMapper.toResponse(productoActualizado);
    }

    public boolean borrarProducto(Long id) {
        if (productoRepo.existsById(id)) {
            auditUtils.setCurrentUserForAudit();
            productoRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }


}
