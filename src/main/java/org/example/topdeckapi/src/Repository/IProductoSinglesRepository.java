package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.ProductoSingles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IProductoSinglesRepository extends JpaRepository<ProductoSingles, Long> {

    Optional<ProductoSingles> findByProducto_Uuid(String productoUuid);

    Optional<ProductoSingles> findByProducto_IdProducto(Long idProducto);

    @Query(value = "SELECT ps.* FROM producto_singles ps " +
            "WHERE ps.atributos->>'rareza' = :rareza", nativeQuery = true)
    List<ProductoSingles> findByRareza(@Param("rareza") String rareza);

    @Query(value = "SELECT ps.* FROM producto_singles ps " +
            "WHERE ps.atributos->>'set' = :set", nativeQuery = true)
    List<ProductoSingles> findBySet(@Param("set") String set);

    @Query(value = "SELECT ps.* FROM producto_singles ps " +
            "WHERE ps.atributos->>'codigo' = :codigo", nativeQuery = true)
    Optional<ProductoSingles> findByCodigo(@Param("codigo") String codigo);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM producto_singles ps " +
            "WHERE ps.atributos->>'id_tcg' = :idTcg)", nativeQuery = true)
    boolean existsByAtributoIdTcg(@Param("idTcg") String idTcg);
}
