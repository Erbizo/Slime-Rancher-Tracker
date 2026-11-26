package com.slimetracker.servicio;

import com.slimetracker.dao.LogroDao;
import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dto.LogroEstadoRespuesta;
import com.slimetracker.dto.LogroRespuesta;
import com.slimetracker.dto.NotaPeticion;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.mapeador.LogroMapeador;
import com.slimetracker.modelo.LogroConEstado;
import com.slimetracker.tx.GestorTransacciones;

import java.sql.Connection;
import java.util.List;

/** R8: marcar, desmarcar y anotar logros. */
public class LogroServicio {

    static final String LOGRO_NO_EXISTE = "El logro no existe.";

    private final GestorTransacciones gestor;
    private final LogroDao logroDao;
    private final PartidaDao partidaDao;

    public LogroServicio(GestorTransacciones gestor, LogroDao logroDao, PartidaDao partidaDao) {
        this.gestor = gestor;
        this.logroDao = logroDao;
        this.partidaDao = partidaDao;
    }

    public List<LogroRespuesta> listarCatalogo() {
        return gestor.soloLectura(conexion -> logroDao.listar(conexion).stream()
                .map(LogroMapeador::aRespuesta)
                .toList());
    }

    public List<LogroEstadoRespuesta> listarDePartida(long usuarioId, long partidaId) {
        return gestor.soloLectura(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            return logroDao.listarConEstado(conexion, partidaId).stream()
                    .map(LogroMapeador::aEstado)
                    .toList();
        });
    }

    /** R8: 409 si ya estaba conseguido. */
    public LogroEstadoRespuesta marcar(long usuarioId, long partidaId, long logroId) {
        return gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            LogroConEstado logro = buscar(conexion, partidaId, logroId);
            if (logro.conseguido()) {
                throw new ReglaNegocioException("Ya habías conseguido «" + logro.logro().nombre() + "».");
            }
            logroDao.marcar(conexion, partidaId, logroId);
            partidaDao.tocar(conexion, partidaId);
            return LogroMapeador.aEstado(buscar(conexion, partidaId, logroId));
        });
    }

    public void desmarcar(long usuarioId, long partidaId, long logroId) {
        gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            LogroConEstado logro = buscar(conexion, partidaId, logroId);
            if (!logro.conseguido()) {
                throw new ReglaNegocioException("No tenías conseguido «" + logro.logro().nombre() + "».");
            }
            logroDao.desmarcar(conexion, partidaId, logroId);
            partidaDao.tocar(conexion, partidaId);
            return null;
        });
    }

    /** R8: nota de máximo 200 caracteres, solo en logros conseguidos. */
    public LogroEstadoRespuesta actualizarNota(long usuarioId, long partidaId, long logroId, NotaPeticion peticion) {
        String nota = ValidacionNota.validar(peticion);
        return gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            LogroConEstado logro = buscar(conexion, partidaId, logroId);
            if (!logro.conseguido()) {
                throw new ReglaNegocioException("Solo se pueden anotar logros conseguidos.");
            }
            logroDao.actualizarNota(conexion, partidaId, logroId, nota);
            partidaDao.tocar(conexion, partidaId);
            return LogroMapeador.aEstado(buscar(conexion, partidaId, logroId));
        });
    }

    private LogroConEstado buscar(Connection conexion, long partidaId, long logroId) {
        return logroDao.buscarConEstado(conexion, partidaId, logroId)
                .orElseThrow(() -> new NoEncontradoException(LOGRO_NO_EXISTE));
    }
}
