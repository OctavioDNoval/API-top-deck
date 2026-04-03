package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ITagRepository extends JpaRepository<Tag,Long> {
    boolean existsByNombre(String nombre);

    @Query("SELECT t FROM Tag t where lower(:nombre) = lower( t.nombre) ")
    Optional<Tag> findByNombreNormalizado(@Param("nombre")String nombre);
}
