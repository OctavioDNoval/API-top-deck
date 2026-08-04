package org.example.topdeckapi.src.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name= "tag")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTag;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private String uuid;

    @PrePersist
    protected void onCreate() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "nombre_normalizado", unique = true)
    private String nombreNormalizado;

    @Column(name="img_url")
    private String imgUrl;
}
