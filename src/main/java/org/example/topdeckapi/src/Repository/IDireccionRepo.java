package org.example.topdeckapi.src.Repository;

import jakarta.validation.constraints.NotBlank;
import org.example.topdeckapi.src.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDireccionRepo extends JpaRepository<Direccion,Long> {
    List<Direccion> findByUsuario_IdUsuario(Long usuarioIdUsuario);
    Optional<Direccion> findByDireccionAndAlturaAndPisoAndCiudadAndProvinciaAndPaisAndCodigoPostalAndUsuario_IdUsuario(
            String direccion,
            String altura,
            String piso,
            String ciudad,
            String provincia,
            String pais,
            String codigoPostal,
            Long usuarioId
    );

}
