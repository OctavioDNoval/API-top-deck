package org.example.topdeckapi.src.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "detallecarrito")
public class DetalleCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_carrito")
    private Long idDetalleCarrito;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private String uuid;

    @PrePersist
    protected void onCreate() {
        this.uuid = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrito", nullable = false)
    @JsonIgnore
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto",nullable = false)
    private Producto producto;

    private Integer cantidad;
}
