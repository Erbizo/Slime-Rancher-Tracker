package com.slimetracker.excepcion;

/** Se ha violado una clave ajena (MariaDB 1451 al borrar/actualizar el padre, 1452 al insertar el hijo). */
public class IntegridadReferencialException extends AccesoDatosException {

    public IntegridadReferencialException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
