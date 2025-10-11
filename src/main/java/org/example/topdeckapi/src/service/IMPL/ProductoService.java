package org.example.topdeckapi.src.service.IMPL;

import lombok.RequiredArgsConstructor;
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

    public List<Producto> findAll() {
        return productoRepo.findAll();
    }

    public Producto guardar(Producto producto) {
        return productoRepo.save(producto);
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
