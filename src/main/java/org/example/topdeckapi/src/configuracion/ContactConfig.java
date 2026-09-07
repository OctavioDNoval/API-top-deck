package org.example.topdeckapi.src.configuracion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class ContactConfig {

    private String baseUrl;
    private Contacto contacto = new Contacto();
    private Email email = new Email();

    @Data
    public static class Contacto {
        private String telefono;
        private String email;
        private String direccion;
        private String horario;
        private String nombreEmpresa;
    }

    @Data
    public static class Email {
        private String remitente;
        private String nombreRemitente;
    }
}
