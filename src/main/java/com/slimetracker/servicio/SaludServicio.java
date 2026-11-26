package com.slimetracker.servicio;

import com.slimetracker.dao.SaludDao;
import com.slimetracker.dto.SaludRespuesta;
import com.slimetracker.tx.GestorTransacciones;

/** Comprueba que la aplicación llega a la base de datos. */
public class SaludServicio {

    private final GestorTransacciones gestor;
    private final SaludDao saludDao;

    public SaludServicio(GestorTransacciones gestor, SaludDao saludDao) {
        this.gestor = gestor;
        this.saludDao = saludDao;
    }

    public SaludRespuesta comprobar() {
        boolean responde = gestor.soloLectura(saludDao::comprobar);
        if (!responde) {
            throw new IllegalStateException("La base de datos no ha respondido a SELECT 1");
        }
        return new SaludRespuesta("OK");
    }
}
