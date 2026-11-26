package com.slimetracker.excepcion;

/** La operación incumple una regla de negocio o choca con el estado actual (409). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
