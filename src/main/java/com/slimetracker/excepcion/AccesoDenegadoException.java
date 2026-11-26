package com.slimetracker.excepcion;

/** El usuario intenta tocar algo que no es suyo (403). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
