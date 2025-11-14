package org.example.topdeckapi.src.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auditoria")
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idAuditoria;

    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    @Column(name = "fecha_audit")
    private Date fechaAudit;

    @Column(name = "accion")
    private String accion;

    @Column(name = "tabla")
    private String tabla;

    @Column(name = "nombre_entidad")
    private String nombreEntidad;
}
