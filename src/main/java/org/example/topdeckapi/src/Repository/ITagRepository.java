package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ITagRepository extends JpaRepository<Tag,Long> {
    boolean existsByNombre(String nombre);

    Optional<Tag> findByNombre(String nombre);
}
