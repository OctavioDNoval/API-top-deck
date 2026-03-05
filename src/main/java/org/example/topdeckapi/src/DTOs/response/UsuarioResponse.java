package org.example.topdeckapi.src.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ROL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsuarioResponse {
    private Long idUsuario;
    private String nombre;
    private String email;
    private String telefono;
    private ROL rol;
    private String versionTerminosYCondicionesAceptados;
    private Boolean terminosAceptados;
}
