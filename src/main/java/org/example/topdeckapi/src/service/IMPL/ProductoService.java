package org.example.topdeckapi.src.service.IMPL;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import org.example.topdeckapi.src.DTOs.mappers.ProductoMapper;
import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.FacetaResponse;
import org.example.topdeckapi.src.DTOs.response.PaginacionResponse;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.DTOs.response.ValorFacetaResponse;

import org.example.topdeckapi.src.Exception.BussinesException;
import org.example.topdeckapi.src.Exception.ResourceNotFoundException;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.IProductoSinglesRepository;
import org.example.topdeckapi.src.Repository.ProductoSinglesQueryRepository;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.ProductoSingles;
import org.example.topdeckapi.src.model.Tag;

import org.example.topdeckapi.src.service.Interface.IProductoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService implements IProductoService {
    private final IProductoRepo productoRepo;
    private final IProductoSinglesRepository productoSinglesRepo;
    private final ProductoSinglesQueryRepository productoSinglesQueryRepo;
    private final ICategoriasRepo  categoriasRepo;
    private final ITagRepository tagRepository;
    private final PaginacionService paginationService;
    private final ProductoMapper productoMapper;
    private final UsuarioService usuarioService;
    private final AuditService auditService;

    private static final List<String> FACET_DENYLIST = List.of(
            "id_tcg", "nombre", "codigo", "set_codigo", "set_slug",
            "imagen_small", "imagen_large", "url_tcgplayer", "_raw",
            "CardText", "Card Text", "Attack 1", "Attack 2", "Attack 3", "Attack 4",
            "Ability", "Effect", "Rules", "FlavorText", "Flavor Text",
            "Description", "Lore", "Text",
            "OracleText", "Oracle Text", "Oracle",
            "Number", "CardNumber", "Card Number", "CollectorNumber", "Collector Number",
            "Power", "Toughness", "Loyalty", "Artist",
            "Keywords", "Watermark", "Layout", "Flavor",
            "ReleaseDate", "Release Date", "FrameVersion", "Frame Version",
            "ManaCost", "Mana Cost", "ManaValue", "Mana Value", "CMC", "Cost");

    private static final int MAX_VALORES_FACETA = 60;

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
            boolean isAdmin,
            String tipoProducto) {

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

        String searchPattern = search != null ? "%" + search.toLowerCase() + "%" : null;

        Page<Producto> paginaProducto;

        if(isAdmin) {
            paginaProducto = productoRepo.findByFiltros(searchPattern, resolvedCategoriaId, resolvedTagId, tipoProducto, pageable);
        } else {
            paginaProducto = productoRepo.findByFiltrosAndActivo(searchPattern, resolvedCategoriaId, resolvedTagId, tipoProducto, pageable);
        }
        return paginationService.crearPaginacionResponse(paginaProducto, pagina, tamanio,
                p -> enrichWithAtributos(productoMapper.toResponse(p), p));
    }

    public PaginacionResponse<ProductoResponse> obtenerSinglesConFiltro(
            Integer pagina,
            Integer tamanio,
            String sortBy,
            String direction,
            String filter,
            String idTag,
            Map<String, List<String>> atributos) {

        String search = (filter == null || filter.trim().isEmpty()) ? null : filter.trim();
        String searchPattern = search != null ? "%" + search.toLowerCase() + "%" : null;

        Long resolvedTagId = null;
        if (idTag != null && !idTag.isEmpty()) {
            resolvedTagId = resolveTag(idTag).getIdTag();
        }

        Map<String, String> mapeoCampos = Map.of(
                "nombre", "nombre",
                "precio", "precio",
                "uuid", "uuid");
        String columna = mapeoCampos.getOrDefault(sortBy, "uuid");
        boolean ascending = "asc".equalsIgnoreCase(direction);

        Page<Producto> paginaProducto = productoSinglesQueryRepo.buscarSingles(
                searchPattern, resolvedTagId, atributos, columna, ascending, pagina - 1, tamanio);

        return paginationService.crearPaginacionResponse(paginaProducto, pagina, tamanio,
                p -> enrichWithAtributos(productoMapper.toResponse(p), p));
    }

    public List<FacetaResponse> obtenerFacetasSingles(String idTag) {
        Long resolvedTagId = null;
        if (idTag != null && !idTag.isEmpty()) {
            resolvedTagId = resolveTag(idTag).getIdTag();
        }

        List<Object[]> filas = productoSinglesQueryRepo.obtenerFacetas(resolvedTagId, FACET_DENYLIST);

        Map<String, List<ValorFacetaResponse>> agrupado = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            String atributo = (String) fila[0];
            String valor = (String) fila[1];
            Long cantidad = ((Number) fila[2]).longValue();
            agrupado.computeIfAbsent(atributo, k -> new ArrayList<>())
                    .add(new ValorFacetaResponse(valor, cantidad));
        }

        List<FacetaResponse> facetas = new ArrayList<>();
        for (Map.Entry<String, List<ValorFacetaResponse>> entry : agrupado.entrySet()) {
            List<ValorFacetaResponse> valores = entry.getValue();
            if (valores.size() > MAX_VALORES_FACETA) {
                valores = valores.subList(0, MAX_VALORES_FACETA);
            }
            facetas.add(new FacetaResponse(entry.getKey(), valores));
        }
        facetas.sort(Comparator
                .comparingInt((FacetaResponse f) -> prioridadFaceta(f.getAtributo()))
                .thenComparing(FacetaResponse::getAtributo));
        return facetas;
    }

    private static final List<String> FACET_PRIORITY = List.of(
            "set", "rarity", "color", "colors", "type", "card type");

    private int prioridadFaceta(String atributo) {
        String key = atributo == null ? "" : atributo.toLowerCase();
        int index = FACET_PRIORITY.indexOf(key);
        return index == -1 ? FACET_PRIORITY.size() : index;
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
        String tipo = producto.getTipoProducto();
        boolean esSingle = "SINGLE".equalsIgnoreCase(tipo);

        if (esSingle) {
            Object idTcg = producto.getAtributos() != null ? producto.getAtributos().get("id_tcg") : null;
            if (idTcg != null) {
                if (productoSinglesRepo.existsByAtributoIdTcg(idTcg.toString())) {
                    throw new BussinesException("La carta ya está cargada");
                }
            } else if (productoRepo.existsByNombre(producto.getNombre())) {
                throw new BussinesException("El producto ya existe");
            }
        } else if (productoRepo.existsByNombre(producto.getNombre())) {
            throw new BussinesException("El producto ya existe");
        }

        Producto nuevoProducto = productoMapper.toEntity(producto);
        Tag tag = resolveTag(producto.getIdTag());

        if (producto.getIdCategoria() != null && !producto.getIdCategoria().isBlank()) {
            nuevoProducto.setCategoria(resolveCategoria(producto.getIdCategoria()));
        }
        nuevoProducto.setTag(tag);
        nuevoProducto.setActivo(true);

        nuevoProducto.setTipoProducto(tipo != null ? tipo : "PRODUCTO");

        Producto productoGuardado = productoRepo.save(nuevoProducto);

        if ("SINGLE".equalsIgnoreCase(nuevoProducto.getTipoProducto())
                && producto.getAtributos() != null && !producto.getAtributos().isEmpty()) {
            ProductoSingles single = new ProductoSingles();
            single.setProducto(productoGuardado);
            single.setAtributos(producto.getAtributos());
            productoSinglesRepo.save(single);
        }

        auditService.registrar("INSERT", "producto");
        return enrichWithAtributos(productoMapper.toResponse(productoGuardado), productoGuardado);
    }

    public List<ProductoResponse> obtenerOfertas() {
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "descuento"));
        return productoRepo.findOfertas(pageable).stream()
                .map(p -> enrichWithAtributos(productoMapper.toResponse(p), p))
                .toList();
    }

    public ProductoResponse buscarPorId(String uuid) {
        Producto p = productoRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));

        if (Boolean.FALSE.equals(p.getActivo())) {
            throw new ResourceNotFoundException("No existe el producto");
        }

        return enrichWithAtributos(productoMapper.toResponse(p), p);
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

        if ("SINGLE".equalsIgnoreCase(p.getTipoProducto())
                && newProducto.getAtributos() != null && !newProducto.getAtributos().isEmpty()) {
            ProductoSingles single = productoSinglesRepo.findByProducto_IdProducto(p.getIdProducto())
                    .orElseGet(() -> {
                        ProductoSingles s = new ProductoSingles();
                        s.setProducto(p);
                        return s;
                    });
            single.setAtributos(newProducto.getAtributos());
            productoSinglesRepo.save(single);
        }

        return enrichWithAtributos(productoMapper.toResponse(productoActualizado), productoActualizado);
    }

    public ProductoResponse cambiarEstadoProducto(String uuidProducto){
        Producto p = productoRepo.findByUuid(uuidProducto)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));

        boolean estadoActual = p.getActivo();
        p.setActivo(!estadoActual);
        auditService.registrar("UPDATE", "producto");
        return enrichWithAtributos(productoMapper.toResponse(productoRepo.save(p)), p);
    }

    public ProductoResponse listarProducto(String uuidProducto){
        Producto p = productoRepo.findByUuid(uuidProducto)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));

        p.setActivo(true);
        auditService.registrar("UPDATE", "producto");
        return enrichWithAtributos(productoMapper.toResponse(productoRepo.save(p)), p);
    }

    public boolean borrarProducto(String uuid) {
        Producto p = productoRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));
        auditService.registrar("DELETE", "producto");
        productoRepo.delete(p);
        return true;
    }

    private ProductoResponse enrichWithAtributos(ProductoResponse response, Producto producto) {
        if ("SINGLE".equalsIgnoreCase(producto.getTipoProducto())) {
            productoSinglesRepo.findByProducto_IdProducto(producto.getIdProducto())
                    .ifPresent(single -> response.setAtributos(single.getAtributos()));
        }
        return response;
    }


}
