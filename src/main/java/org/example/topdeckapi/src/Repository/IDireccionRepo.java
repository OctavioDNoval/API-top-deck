package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDireccionRepo extends JpaRepository<Direccion,Long> {
    List<Direccion> findByUsuario_IdUsuario(Long usuarioIdUsuario);
}
