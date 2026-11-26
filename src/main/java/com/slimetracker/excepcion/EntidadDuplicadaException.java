package com.slimetracker.excepcion;

/** Se ha violado una clave única o primaria (MariaDB 1062). */
public class EntidadDuplicadaException extends AccesoDatosException {

    public EntidadDuplicadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
