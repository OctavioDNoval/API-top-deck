package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IEventoRepository extends JpaRepository<Evento,Long> {
    Optional<Evento> findByUuid(String uuid);

    @Query(value = "SELECT e.estado, COUNT(*) FROM evento e GROUP BY e.estado", nativeQuery = true)
    List<Object[]> contarPorEstado();
}
