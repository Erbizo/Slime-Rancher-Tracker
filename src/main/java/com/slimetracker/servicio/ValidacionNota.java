package com.slimetracker.servicio;

import com.slimetracker.dto.NotaPeticion;
import com.slimetracker.excepcion.ValidacionException;

/** Notas de slimes y logros (R8): máximo 200 caracteres; vacía = sin nota. */
final class ValidacionNota {

    static final int MAXIMO = 200;
    static final String ERROR = "La nota no puede tener más de 200 caracteres.";

    private ValidacionNota() {
    }

    /** Devuelve la nota recortada, o null si viene vacía. */
    static String validar(NotaPeticion peticion) {
        String nota = Textos.recortar(peticion.nota());
        if (nota == null || nota.isEmpty()) {
            return null;
        }
        if (nota.length() > MAXIMO) {
            throw ValidacionException.deCampo("nota", ERROR);
        }
        return nota;
    }
}
