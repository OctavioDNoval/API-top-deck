package org.example.topdeckapi.src.DTOs.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.topdeckapi.src.Enumerados.ROL;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsuarioRequest {
    private String nombre;
    private String email;
    private String password;;
    private String telefono;
    private String ipUsuario;
    private String versionTerminosYCondicionesAceptadas = "0.0";
    private Boolean terminosAceptados;
}
