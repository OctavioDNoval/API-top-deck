package org.example.topdeckapi.src.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalTime;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEvento")
    private Long idEvento;

    @Column(name = "nombreEvento")
    private String nombreEvento;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "fecha")
    private Date fecha;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "precioEntrada")
    private Integer precioEntrada;

    @Column(name = "estado")
    private ESTADO_EVENTO estado;
}
