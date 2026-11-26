package com.slimetracker.excepcion;

/** No hay sesión o las credenciales no son correctas (401). */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
