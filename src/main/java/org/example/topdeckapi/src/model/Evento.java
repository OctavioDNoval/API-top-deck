package org.example.topdeckapi.src.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ESTADO_EVENTO;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "nombre_evento")
    private String nombreEvento;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "precio_entrada")
    private Integer precioEntrada;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private ESTADO_EVENTO estado;
}
