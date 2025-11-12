package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateProductDTO;
import org.example.topdeckapi.src.DTOs.DTO.ProductoDTO;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.Repository.ITagRepository;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Tag;
import org.example.topdeckapi.src.service.Interface.IProductoService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService implements IProductoService {
    private final IProductoRepo productoRepo;
    private final ICategoriasRepo  categoriasRepo;
    private final ITagRepository tagRepository;
    private final TagService tagService;

    protected ProductoDTO convertToDTO(Producto p) {
        ProductoDTO productoDTO = new ProductoDTO();
        productoDTO.setIdProducto(p.getProductoId());
        productoDTO.setNombre(p.getNombre());
        productoDTO.setDescripcion(p.getDescripcion());
        productoDTO.setStock(p.getStock());
        productoDTO.setPrecio(p.getPrecio());
        productoDTO.setImg_url(p.getImg_url());
        productoDTO.setCategoriaId(p.getCategoria().getIdCategoria());
        productoDTO.setCategoriaNombre(p.getCategoria().getNombre());
        productoDTO.setTagId(p.getTag().getIdTag());
        productoDTO.setTagNombre(p.getTag().getNombre());

        return productoDTO;
    }

    protected Producto convertToEntity(ProductoDTO p) {
        Producto producto = new Producto();
        producto.setProductoId(p.getIdProducto());
        producto.setNombre(p.getNombre());
        producto.setDescripcion(p.getDescripcion());
        producto.setPrecio(p.getPrecio());
        producto.setImg_url(p.getImg_url());
        producto.setStock(p.getStock());

        Categoria c = new Categoria();
        c.setNombre(p.getCategoriaNombre());
        c.setIdCategoria(p.getCategoriaId());
        producto.setCategoria(c);

        Tag t = new Tag();
        t.setNombre(p.getTagNombre());
        t.setIdTag(p.getTagId());

        producto.setTag(t);

        return producto;
    }

    public List<ProductoDTO> findAll() {
        return productoRepo.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Producto guardar(CreateProductDTO producto) {
        Producto newProducto = new Producto();
        newProducto.setNombre(producto.getNombre());
        newProducto.setDescripcion(producto.getDescripcion());
        newProducto.setPrecio(producto.getPrecio());
        newProducto.setCategoria(categoriasRepo.findById(producto.getId_categoria()).orElse(null));
        newProducto.setImg_url(producto.getImg_url());
        newProducto.setStock(producto.getStock());
        newProducto.setTag(tagRepository.findById(producto.getId_tag()).orElse(null));

        return productoRepo.save(newProducto);
    }

    public Optional<ProductoDTO> buscarPorId(long id) {
        return productoRepo.findById(id)
                .map(this::convertToDTO);
    }

    public Optional<ProductoDTO> actualizarProducto(long id, Producto newProducto) {
        return productoRepo.findById(id)
                .map(p-> {
                    if((newProducto.getNombre()!=null) && (!newProducto.getNombre().isEmpty())) {
                        p.setNombre(newProducto.getNombre());
                    }
                    if((newProducto.getDescripcion()!=null) && (!newProducto.getDescripcion().isEmpty())) {
                        p.setDescripcion(newProducto.getDescripcion());
                    }
                    if(newProducto.getPrecio()!=null){
                        p.setPrecio(newProducto.getPrecio());
                    }
                    if (newProducto.getCategoria()!=null){
                        p.setCategoria(newProducto.getCategoria());
                    }
                    if(newProducto.getStock()!=null){
                        p.setStock(newProducto.getStock());
                    }
                    if(newProducto.getTag()!=null){
                        p.setTag(newProducto.getTag());
                    }
                    if(newProducto.getImg_url()!=null){
                        p.setImg_url(newProducto.getImg_url());
                    }

                    Producto productoGuardado = productoRepo.save(p);

                    return convertToDTO(productoGuardado);
                });
    }

    public boolean borrarProducto(long id) {
        if (productoRepo.existsById(id)) {
            productoRepo.deleteById(id);
            return true;
        }else{
            return false;
        }
    }


}
