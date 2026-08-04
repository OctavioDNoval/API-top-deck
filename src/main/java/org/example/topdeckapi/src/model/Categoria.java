package org.example.topdeckapi.src.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name= "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private String uuid;

    @PrePersist
    protected void onCreate() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Column(name = "nombre")
    private String nombre;
}
