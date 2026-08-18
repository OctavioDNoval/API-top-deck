package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Producto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("SELECT p FROM Producto p WHERE p.descuento > 0 AND p.activo = true")
    List<Producto> findOfertas(Pageable pageable);

    boolean existsByNombre(String nombre);
    Optional<Producto> findByUuid(String uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.idProducto = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT c.nombre, COUNT(*) FROM producto p " +
            "INNER JOIN categoria c ON p.id_categoria = c.id_categoria " +
            "GROUP BY c.nombre", nativeQuery = true)
    List<Object[]> contarPorCategoria();

    @Query(value = "SELECT t.nombre, COUNT(*) FROM producto p " +
            "INNER JOIN tag t ON p.id_tag = t.id_tag " +
            "GROUP BY t.nombre", nativeQuery = true)
    List<Object[]> contarPorTag();

    @Query(value = "SELECT " +
            "SUM(CASE WHEN p.stock = 0 THEN 1 ELSE 0 END) AS sinStock, " +
            "SUM(CASE WHEN p.stock > 0 AND p.stock < 10 THEN 1 ELSE 0 END) AS bajo, " +
            "SUM(CASE WHEN p.stock >= 10 THEN 1 ELSE 0 END) AS ok " +
            "FROM producto p WHERE p.activo = true", nativeQuery = true)
    List<Object[]> distribucionStock();

    @Query(value = "SELECT COUNT(*) FROM producto p WHERE p.stock = 0 AND p.activo = true", nativeQuery = true)
    Long contarSinStock();
}
