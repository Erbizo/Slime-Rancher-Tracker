package com.slimetracker.recurso;

import com.slimetracker.excepcion.ValidacionException;

/**
 * Validación de entrada de la capa de control: ids numéricos y positivos, booleanos
 * y cuerpo presente. Se comprueba antes de llamar al servicio (400).
 */
final class Parametros {

    static final String ID_NO_VALIDO = "Tiene que ser un número entero positivo.";
    static final String BOOLEANO_NO_VALIDO = "Tiene que ser true o false.";
    static final String FALTA_CUERPO = "Falta el cuerpo de la petición en JSON.";

    private Parametros() {
    }

    static long id(String valor, String nombre) {
        try {
            long id = Long.parseLong(valor);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException e) {
            // cae al error de abajo
        }
        throw ValidacionException.deCampo(nombre, ID_NO_VALIDO);
    }

    static Long idOpcional(String valor, String nombre) {
        return valor == null || valor.isBlank() ? null : id(valor, nombre);
    }

    static Boolean booleanoOpcional(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return switch (valor) {
            case "true" -> true;
            case "false" -> false;
            default -> throw ValidacionException.deCampo(nombre, BOOLEANO_NO_VALIDO);
        };
    }

    static <T> T cuerpo(T cuerpo) {
        if (cuerpo == null) {
            throw ValidacionException.deCampo("cuerpo", FALTA_CUERPO);
        }
        return cuerpo;
    }
}
