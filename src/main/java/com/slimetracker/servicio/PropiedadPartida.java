package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.excepcion.AccesoDenegadoException;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.modelo.Partida;

import java.sql.Connection;

/**
 * Comprobación común a todos los métodos de partida:
 * si no existe → 404; si es de otro usuario → 403.
 */
final class PropiedadPartida {

    static final String NO_EXISTE = "La partida no existe.";
    static final String NO_ES_TUYA = "Esa partida no es tuya.";

    private PropiedadPartida() {
    }

    static Partida comprobar(PartidaDao partidaDao, Connection conexion, long usuarioId, long partidaId) {
        Partida partida = partidaDao.buscarPorId(conexion, partidaId)
                .orElseThrow(() -> new NoEncontradoException(NO_EXISTE));
        if (partida.usuarioId() != usuarioId) {
            throw new AccesoDenegadoException(NO_ES_TUYA);
        }
        return partida;
    }
}
