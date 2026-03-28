package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Producto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IProductoRepo extends JpaRepository<Producto,Long> {


    @Query("SELECT p FROM Producto p WHERE " +
            "(:search IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:idCategoria IS NULL OR p.categoria.idCategoria = :idCategoria) AND " +
            "(:idTag IS NULL OR p.tag.idTag = :idTag)")
    Page<Producto> findByFiltros(@Param("search") String search,
                                 @Param("idCategoria") Long idCategoria,
                                 @Param("idTag") Long idTag,
                                 Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE " +
            "(:search IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:idCategoria IS NULL OR p.categoria.idCategoria = :idCategoria) AND " +
            "(:idTag IS NULL OR p.tag.idTag = :idTag) AND " +
            "p.activo = true ")
    Page<Producto> findByFiltrosAndActivo(@Param("search") String search,
                                          @Param("idCategoria") Long idCategoria,
                                          @Param("idTag") Long idTag,
                                          Pageable pageable);

    boolean existsByNombre(String nombre);
}
