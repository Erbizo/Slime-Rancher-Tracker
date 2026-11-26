package com.slimetracker.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datos de conexión a la base de datos, leídos de variables de entorno.
 * No hay valores por defecto: si falta alguna variable, la aplicación no arranca.
 */
public record ConfigBD(String host, String puerto, String nombre, String usuario, String contrasena) {

    private static final List<String> VARIABLES = List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD");

    public static ConfigBD desdeEntorno() {
        return desde(System.getenv());
    }

    static ConfigBD desde(Map<String, String> entorno) {
        List<String> faltan = new ArrayList<>();
        for (String variable : VARIABLES) {
            String valor = entorno.get(variable);
            if (valor == null || valor.isBlank()) {
                faltan.add(variable);
            }
        }
        if (!faltan.isEmpty()) {
            throw new IllegalStateException(
                    "Faltan variables de entorno de la base de datos: " + String.join(", ", faltan)
                            + ". Defínelas en el archivo .env (mira .env.example).");
        }
        return new ConfigBD(entorno.get("DB_HOST"), entorno.get("DB_PORT"), entorno.get("DB_NAME"),
                entorno.get("DB_USER"), entorno.get("DB_PASSWORD"));
    }

    public String urlJdbc() {
        return "jdbc:mariadb://" + host + ":" + puerto + "/" + nombre;
    }

    /** Evita que la contraseña acabe en un log si alguien imprime la configuración. */
    @Override
    public String toString() {
        return "ConfigBD[url=" + urlJdbc() + ", usuario=" + usuario + "]";
    }
}
