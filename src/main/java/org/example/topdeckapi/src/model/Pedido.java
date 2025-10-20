package org.example.topdeckapi.src.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pedido")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long idPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_pedido", nullable = false)
    private Date fechaPedido;


    /*
    * Aca las propiedades de column son
    * - Precision: El digito sera de hasta 10 digitos
    * - Scale: tendra hasta dos decimales
    * */
    @Column(name = "total", nullable = false)
    private Double total;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido(Usuario usuario, Date fechaPedido, Double total, List<DetallePedido> detalles) {
        this.usuario = usuario;
        this.fechaPedido = fechaPedido;
        this.total = total;
        this.detalles = detalles;
    }
}
