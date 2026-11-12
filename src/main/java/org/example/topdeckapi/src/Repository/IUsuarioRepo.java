package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.Enumerados.ROL;
import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUsuarioRepo extends JpaRepository<Usuario,Long> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdUsuarioNot(String email, Long idUsuario);

    boolean existsByEmailAndRol(String email, ROL rol);

    List<Usuario> findByRol(ROL rol);

    long countByRol(ROL rol);

    Page<Usuario> findByFechaBajaIsNull(Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.fechaBaja IS NULL AND (" +
            "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Usuario> findBySearch(@Param("search") String search, Pageable pageable);

    Optional<Usuario> findByUuid(String uuid);

    Optional<Usuario> findByTokenVerificacionHash(String tokenVerificacionHash);

    Optional<Usuario> findByTokenResetHash(String tokenResetHash);

    Optional<Usuario> findByTokenEmailHash(String tokenEmailHash);

    boolean existsByTelefono(String telefono);

    @Query(value = "SELECT u.rol, COUNT(*) FROM usuario u WHERE u.fecha_baja IS NULL GROUP BY u.rol", nativeQuery = true)
    List<Object[]> contarPorRol();
}
