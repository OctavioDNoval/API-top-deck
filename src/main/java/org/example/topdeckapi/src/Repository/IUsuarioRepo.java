package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepo extends JpaRepository<Usuario,Long> {
    Optional<Usuario> findByEmail(String email);
}
