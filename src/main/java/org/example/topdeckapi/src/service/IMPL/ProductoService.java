package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
import org.example.topdeckapi.src.DTOs.CreateDTO.CreateProductDTO;
import org.example.topdeckapi.src.Repository.ICategoriasRepo;
import org.example.topdeckapi.src.Repository.IProductoRepo;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.service.Interface.IProductoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService implements IProductoService {
    private final IProductoRepo productoRepo;
    private final ICategoriasRepo  categoriasRepo;


    public List<Producto> findAll() {
        return productoRepo.findAll();
    }

    public Producto guardar(CreateProductDTO producto) {
        Producto newProducto = new Producto();
        newProducto.setNombre(producto.getNombre());
        newProducto.setDescripcion(producto.getDescripcion());
        newProducto.setPrecio(producto.getPrecio());
        newProducto.setCategoria(categoriasRepo.findById(producto.getId_categoria()).orElse(null));
        newProducto.setImg_url(producto.getImg_url());
        newProducto.setStock(producto.getStock());


        return productoRepo.save(newProducto);
    }

    public Optional<Producto> buscarPorId(long id) {
        return productoRepo.findById(id);
    }

    public Optional<Producto> actualizarProducto(long id, Producto newProducto) {
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

                    return productoRepo.save(p);
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
