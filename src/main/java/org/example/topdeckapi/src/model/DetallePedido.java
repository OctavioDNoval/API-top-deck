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
    private Long idDetallePedido;

    @Column(name = "cantidad")
    private Integer cantidad;

    @Column(name = "precio_unitario")
    private Double precioUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_pedido", nullable = false)
    private Pedido pedido;

    /*
    * Las relaciones Many To One, One To Many siempre
    * deben apuntar a una entidad y no a una clase de java
    * como por ejenmplo Long o Integer
    * */
}
