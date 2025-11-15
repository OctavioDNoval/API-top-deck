package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEventoRepository extends JpaRepository<Evento,Long> {
}
