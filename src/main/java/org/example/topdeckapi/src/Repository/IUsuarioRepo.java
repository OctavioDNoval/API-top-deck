package org.example.topdeckapi.src.Repository;

import org.example.topdeckapi.src.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUsuarioRepo extends JpaRepository<Usuario,Long> {
}
