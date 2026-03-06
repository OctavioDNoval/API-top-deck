package org.example.topdeckapi.src.DTOs.mappers;

import org.example.topdeckapi.src.DTOs.request.ProductoRequest;
import org.example.topdeckapi.src.DTOs.response.ProductoResponse;
import org.example.topdeckapi.src.model.Categoria;
import org.example.topdeckapi.src.model.Producto;
import org.example.topdeckapi.src.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {
        CategoriaMapper.class,
        TagMapper.class,
})
public interface ProductoMapper {

    @Mapping(source = "nombre", target = "nombreProducto")
    ProductoResponse toResponse(Producto producto);

    @Mapping(target="categoria", source = "idCategoria", qualifiedByName = "idToCategoria")
    @Mapping(target = "tag", source = "idTag", qualifiedByName = "idToTag")
    Producto toEntity(ProductoRequest productoRequest);

    @Named("idToTag")
    default Tag idToTag(Long id) {
        if (id == null) return null;

        Tag tag = new Tag();
        tag.setIdTag(id);
        return tag;
    }

    @Named("idToCategoria")
    default Categoria idToCategoria(Long id){
        if(id==null) return null;

        Categoria categoria = new Categoria();
        categoria.setIdCategoria(id);
        return categoria;
    }
}
