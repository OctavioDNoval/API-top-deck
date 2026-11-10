package org.example.topdeckapi.src.service.IMPL;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/*
* Issues Firebase custom tokens carrying the "admin" claim. Storage rules only
* allow writes when request.auth.token.admin == true, so the public web config
* is no longer enough to upload or delete images.
* */
@Service
@Slf4j
public class FirebaseTokenService {

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    private FirebaseAuth firebaseAuth;

    @PostConstruct
    void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_PATH no configurado: los admins no podran subir imagenes");
            return;
        }
        try (InputStream credentials = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            firebaseAuth = FirebaseAuth.getInstance(app);
        } catch (IOException e) {
            log.error("No se pudieron leer las credenciales de Firebase: {}", e.getMessage());
        }
    }

    public String crearTokenAdmin(String uid) {
        if (firebaseAuth == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase no esta configurado");
        }
        try {
            return firebaseAuth.createCustomToken(uid, Map.of("admin", true));
        } catch (FirebaseAuthException e) {
            log.error("Error creando custom token de Firebase: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo generar el token de Firebase");
        }
    }
}
