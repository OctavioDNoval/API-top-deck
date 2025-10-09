package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDireccionRepo extends JpaRepository<Direccion,Long> {
}
