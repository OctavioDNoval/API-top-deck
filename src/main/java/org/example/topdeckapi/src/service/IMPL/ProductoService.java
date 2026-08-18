package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import org.example.topdeckapi.src.DTOs.mappers.ProductoMapper;
import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;

import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Tag;

import org.example.topdeckapi.src.service.Interface.IProductoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService implements IProductoService {
    private final IProductoRepo productoRepo;
    private final ICategoriasRepo  categoriasRepo;
    private final ITagRepository tagRepository;
    private final PaginacionService paginationService;
    private final ProductoMapper productoMapper;
    private final UsuarioService usuarioService;
    private final AuditService auditService;

    private Sort buildSort(String sortBy, String direction){
        Map<String,String> mapeoCampos = Map.of(
                "nombre", "nombre",
                "precio", "precio",
                "uuid", "uuid"
        );

        String campoReal = mapeoCampos.getOrDefault(sortBy, "uuid");
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, campoReal);
    }

    public PaginacionResponse<ProductoResponse> obtenerPaginadosConFiltro(
            Integer pagina,
            Integer tamanio,
            String sortBy,
            String direction,
            String filter,
            String idCategoria,
            String idTag,
            boolean isAdmin) {

        Sort sort = buildSort(sortBy, direction);
        Pageable pageable = PageRequest.of(pagina - 1, tamanio, sort);

        String search = (filter == null || filter.trim().isEmpty()) ? null : filter.trim();

        Long resolvedTagId = null;
        if (idTag != null && !idTag.isEmpty()) {
            Tag tag = resolveTag(idTag);
            resolvedTagId = tag.getIdTag();
        }

        Long resolvedCategoriaId = null;
        if (idCategoria != null && !idCategoria.isEmpty()) {
            Categoria categoria = resolveCategoria(idCategoria);
            resolvedCategoriaId = categoria.getIdCategoria();
        }

        Page<Producto> paginaProducto;

        if(isAdmin) {
            paginaProducto = productoRepo.findByFiltros(search, resolvedCategoriaId, resolvedTagId, pageable);
        } else {
            paginaProducto = productoRepo.findByFiltrosAndActivo(search, resolvedCategoriaId, resolvedTagId, pageable);
        }
        return paginationService.crearPaginacionResponse(paginaProducto, pagina, tamanio, productoMapper::toResponse);
    }

    private boolean isUuid(String value) {
        return value != null && value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    private Tag resolveTag(String idTag) {
        if (isUuid(idTag)) {
            return tagRepository.findByUuid(idTag)
                    .orElseThrow(() -> new ResourceNotFoundException("El tag no existe"));
        }
        try {
            return tagRepository.findById(Long.parseLong(idTag))
                    .orElseThrow(() -> new ResourceNotFoundException("El tag no existe"));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("El tag no existe");
        }
    }

    private Categoria resolveCategoria(String idCategoria) {
        if (isUuid(idCategoria)) {
            return categoriasRepo.findByUuid(idCategoria)
                    .orElseThrow(() -> new ResourceNotFoundException("La categoría no existe"));
        }
        try {
            return categoriasRepo.findById(Long.parseLong(idCategoria))
                    .orElseThrow(() -> new ResourceNotFoundException("La categoría no existe"));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("La categoría no existe");
        }
    }

    public ProductoResponse guardar(ProductoRequest producto) {
        if(productoRepo.existsByNombre(producto.getNombre())){
            throw new BussinesException("El producto ya existe");
        }
        Producto nuevoProducto = productoMapper.toEntity(producto);
        Tag tag = resolveTag(producto.getIdTag());
        Categoria categoria = resolveCategoria(producto.getIdCategoria());

        nuevoProducto.setCategoria(categoria);
        nuevoProducto.setTag(tag);
        nuevoProducto.setActivo(true);
        Producto productoGuardado = productoRepo.save(nuevoProducto);
        auditService.registrar("INSERT", "producto");
        return productoMapper.toResponse(productoGuardado);
    }

    public List<ProductoResponse> obtenerOfertas() {
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "descuento"));
        return productoRepo.findOfertas(pageable).stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    public ProductoResponse buscarPorId(String uuid) {
        Producto p = productoRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));

        if (Boolean.FALSE.equals(p.getActivo())) {
            throw new ResourceNotFoundException("No existe el producto");
        }

        return productoMapper.toResponse(p);
    }

    public ProductoResponse actualizarProducto(String uuid, ProductoRequest newProducto) {
        Producto p = productoRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));

        if(newProducto.getNombre() != null && !newProducto.getNombre().trim().isEmpty()){
            String nuevoNombre = newProducto.getNombre().trim();
            if(!p.getNombre().equals(nuevoNombre)) {
                if(productoRepo.existsByNombre(nuevoNombre)) {
                    throw new BussinesException("Ya existe un producto con el nombre: " + nuevoNombre);
                }
                p.setNombre(nuevoNombre);
            }
        }
        if(newProducto.getIdCategoria() != null &&
                (p.getCategoria() == null || !p.getCategoria().getUuid().equals(newProducto.getIdCategoria()))){
            Categoria categoria = resolveCategoria(newProducto.getIdCategoria());
            p.setCategoria(categoria);
        }
        if(newProducto.getIdTag() != null &&
                (p.getTag() == null || !p.getTag().getUuid().equals(newProducto.getIdTag()))){
            Tag tag = resolveTag(newProducto.getIdTag());
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

        auditService.registrar("UPDATE", "producto");
        Producto productoActualizado = productoRepo.save(p);
        return productoMapper.toResponse(productoActualizado);
    }

    public ProductoResponse cambiarEstadoProducto(String uuidProducto){
        Producto p = productoRepo.findByUuid(uuidProducto)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));

        boolean estadoActual = p.getActivo();
        p.setActivo(!estadoActual);
        auditService.registrar("UPDATE", "producto");
        return productoMapper.toResponse(productoRepo.save(p));
    }

    public boolean borrarProducto(String uuid) {
        Producto p = productoRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));
        auditService.registrar("DELETE", "producto");
        productoRepo.delete(p);
        return true;
    }


}
