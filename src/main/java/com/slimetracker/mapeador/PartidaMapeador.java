package com.slimetracker.mapeador;

import com.slimetracker.dto.PartidaRespuesta;
import com.slimetracker.modelo.Partida;

public final class PartidaMapeador {

    private PartidaMapeador() {
    }

    public static PartidaRespuesta aRespuesta(Partida partida, int porcentaje) {
        return new PartidaRespuesta(partida.id(), partida.nombre(), partida.creadaEn(), partida.actualizadaEn(),
                porcentaje);
    }
}
