package com.slimetracker.dao;

import com.slimetracker.modelo.Actividad;
import com.slimetracker.modelo.ConteoProgreso;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/** Consultas de solo lectura para el panel de progreso. */
public interface ProgresoDao {

    ConteoProgreso contar(Connection conexion, long partidaId);

    /** Conteo de cada partida del usuario (clave: id de partida), en una sola consulta. */
    Map<Long, ConteoProgreso> contarPorUsuario(Connection conexion, long usuarioId);

    /** Últimos cambios de la partida (zonas, slimes y logros), del más reciente al más antiguo. */
    List<Actividad> ultimasActividades(Connection conexion, long partidaId, int limite);
}
