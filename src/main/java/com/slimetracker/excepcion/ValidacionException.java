package com.slimetracker.excepcion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Datos de entrada no válidos (400). Lleva todos los errores a la vez: campo → mensaje. */
public class ValidacionException extends RuntimeException {

    private final Map<String, String> campos;

    public ValidacionException(Map<String, String> campos) {
        super("Hay datos no válidos.");
        this.campos = Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    public static ValidacionException deCampo(String campo, String mensaje) {
        return new ValidacionException(Map.of(campo, mensaje));
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
