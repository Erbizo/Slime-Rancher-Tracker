package com.slimetracker.mapeador;

import com.slimetracker.dto.LogroEstadoRespuesta;
import com.slimetracker.dto.LogroRespuesta;
import com.slimetracker.modelo.Logro;
import com.slimetracker.modelo.LogroConEstado;

public final class LogroMapeador {

    private LogroMapeador() {
    }

    public static LogroRespuesta aRespuesta(Logro logro) {
        return new LogroRespuesta(logro.id(), logro.nombre(), logro.descripcion(), logro.categoria().name());
    }

    public static LogroEstadoRespuesta aEstado(LogroConEstado estado) {
        Logro logro = estado.logro();
        return new LogroEstadoRespuesta(logro.id(), logro.nombre(), logro.descripcion(), logro.categoria().name(),
                estado.conseguido(), estado.conseguidoEn(), estado.nota());
    }
}
