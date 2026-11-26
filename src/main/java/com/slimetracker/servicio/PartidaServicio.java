package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.ProgresoDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.PartidaPeticion;
import com.slimetracker.dto.PartidaRespuesta;
import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.mapeador.PartidaMapeador;
import com.slimetracker.modelo.ConteoProgreso;
import com.slimetracker.modelo.Partida;
import com.slimetracker.tx.GestorTransacciones;
import com.slimetracker.tx.FuncionSql;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/** R3: partidas del usuario. */
public class PartidaServicio {

    static final String ERROR_NOMBRE = "Escribe un nombre de entre 1 y 40 caracteres.";
    static final String NOMBRE_REPETIDO = "Ya tienes una partida con ese nombre.";
    private static final int MAXIMO_NOMBRE = 40;

    private final GestorTransacciones gestor;
    private final PartidaDao partidaDao;
    private final ZonaDao zonaDao;
    private final ProgresoDao progresoDao;

    public PartidaServicio(GestorTransacciones gestor, PartidaDao partidaDao, ZonaDao zonaDao,
                           ProgresoDao progresoDao) {
        this.gestor = gestor;
        this.partidaDao = partidaDao;
        this.zonaDao = zonaDao;
        this.progresoDao = progresoDao;
    }

    public List<PartidaRespuesta> listar(long usuarioId) {
        return gestor.soloLectura(conexion -> {
            List<Partida> partidas = partidaDao.listarPorUsuario(conexion, usuarioId);
            Map<Long, ConteoProgreso> conteos = progresoDao.contarPorUsuario(conexion, usuarioId);
            return partidas.stream()
                    .map(partida -> PartidaMapeador.aRespuesta(partida, porcentaje(conteos.get(partida.id()))))
                    .toList();
        });
    }

    public PartidaRespuesta obtener(long usuarioId, long partidaId) {
        return gestor.soloLectura(conexion -> {
            Partida partida = PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            return aRespuesta(conexion, partida);
        });
    }

    /** R3: nombre válido y único por usuario; se crea con la zona inicial desbloqueada (una transacción). */
    public PartidaRespuesta crear(long usuarioId, PartidaPeticion peticion) {
        String nombre = validarNombre(peticion);
        return conNombreUnico(conexion -> {
            long partidaId = NuevaPartida.crear(partidaDao, zonaDao, conexion, usuarioId, nombre);
            return aRespuesta(conexion, buscar(conexion, partidaId));
        });
    }

    public PartidaRespuesta renombrar(long usuarioId, long partidaId, PartidaPeticion peticion) {
        String nombre = validarNombre(peticion);
        return conNombreUnico(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            partidaDao.renombrar(conexion, partidaId, nombre);
            return aRespuesta(conexion, buscar(conexion, partidaId));
        });
    }

    /** Borra la partida; su progreso se va en cascada (ON DELETE CASCADE). */
    public void borrar(long usuarioId, long partidaId) {
        gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            return partidaDao.borrar(conexion, partidaId);
        });
    }

    private PartidaRespuesta conNombreUnico(FuncionSql<Connection, PartidaRespuesta> trabajo) {
        try {
            return gestor.enTransaccion(trabajo);
        } catch (EntidadDuplicadaException e) {
            throw new ReglaNegocioException(NOMBRE_REPETIDO);
        }
    }

    private Partida buscar(Connection conexion, long partidaId) {
        return partidaDao.buscarPorId(conexion, partidaId)
                .orElseThrow(() -> new IllegalStateException("La partida " + partidaId + " acaba de desaparecer"));
    }

    private PartidaRespuesta aRespuesta(Connection conexion, Partida partida) {
        return PartidaMapeador.aRespuesta(partida, porcentaje(progresoDao.contar(conexion, partida.id())));
    }

    private static int porcentaje(ConteoProgreso conteo) {
        return conteo == null ? 0 : CalculadoraProgreso.porcentajeGlobal(conteo);
    }

    private static String validarNombre(PartidaPeticion peticion) {
        String nombre = Textos.recortar(peticion.nombre());
        if (nombre == null || nombre.isEmpty() || nombre.length() > MAXIMO_NOMBRE) {
            throw ValidacionException.deCampo("nombre", ERROR_NOMBRE);
        }
        return nombre;
    }
}
