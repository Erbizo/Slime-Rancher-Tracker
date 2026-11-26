package com.slimetracker.excepcion;

/** Error de base de datos traducido desde una SQLException. Su mensaje nunca se envía al cliente. */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
