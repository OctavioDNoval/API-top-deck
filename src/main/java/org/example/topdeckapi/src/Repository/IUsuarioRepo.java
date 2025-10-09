package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUsuarioRepo extends JpaRepository<Usuario,Long> {
}
