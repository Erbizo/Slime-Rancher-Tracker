package com.slimetracker.mapeador;

import com.slimetracker.dto.ProgresoRespuesta.ActividadRespuesta;
import com.slimetracker.modelo.Actividad;

public final class ProgresoMapeador {

    private ProgresoMapeador() {
    }

    public static ActividadRespuesta aRespuesta(Actividad actividad) {
        return new ActividadRespuesta(actividad.tipo().name(), actividad.nombre(), actividad.fecha());
    }
}
