package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IEventoRepository extends JpaRepository<Evento,Long> {
    Optional<Evento> findByUuid(String uuid);
}
