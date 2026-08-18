package org.example.topdeckapi.src.util;

import org.example.topdeckapi.src.Exception.BussinesException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class FechaUtils {

    private FechaUtils() {
    }

    public static LocalDateTime parseFechaTerminos(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String s = valor.trim();

        try {
            return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        throw new BussinesException("Formato de fecha inválido");
    }
}