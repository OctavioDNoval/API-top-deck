package org.example.topdeckapi.src.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "detalle_pedido")
public class DetallePedido {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name= "id_detalle")
    private int idDetallePedido;

    @Column(name = "cantidad")
    private int cantidad;

    @Column(name = "precio_unitario")
    private double precioUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_pedido", nullable = false)
    private Pedido pedido;
}
