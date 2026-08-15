package org.example.topdeckapi.src.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ROL;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private String uuid;

    @PrePersist
    protected void onCreate() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "El email debe tener formato valido")
    private String email;

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    private String password;

    @Column(name = "telefono")
    private String telefono;

    @Enumerated(EnumType.STRING)
    private ROL rol;

    @Column(name = "ip_usuario")
    private String ipUsuario;

    @Column(name = "version_terminos_y_condiciones_aceptadas")
    private String versionTerminosYCondicionesAceptados;

    @Column(name = "terminos_aceptados")
    private Boolean terminosAceptados;

    @Column(name = "fecha_aceptacion_terminos")
    private LocalDateTime fechaAceptacionTerminos;

}
