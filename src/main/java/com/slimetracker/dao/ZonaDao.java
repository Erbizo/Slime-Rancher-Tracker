package com.slimetracker.dao;

import com.slimetracker.modelo.Zona;
import com.slimetracker.modelo.ZonaConEstado;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ZonaDao {

    /** Catálogo completo ordenado por orden. */
    List<Zona> listar(Connection conexion);

    Optional<Zona> buscarPorId(Connection conexion, long id);

    Optional<Zona> buscarInicial(Connection conexion);

    /** Todas las zonas con su estado en la partida y el recuento de slimes (una sola consulta). */
    List<ZonaConEstado> listarConEstado(Connection conexion, long partidaId);

    void desbloquear(Connection conexion, long partidaId, long zonaId);

    /** Devuelve las filas afectadas (0 si no estaba desbloqueada). */
    int bloquear(Connection conexion, long partidaId, long zonaId);

    boolean estaDesbloqueada(Connection conexion, long partidaId, long zonaId);

    /** Zonas desbloqueadas en la partida que tienen a zonaId como requisito. */
    List<Zona> listarDependientesDesbloqueadas(Connection conexion, long partidaId, long zonaId);
}
