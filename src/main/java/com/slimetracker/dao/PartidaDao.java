package com.slimetracker.dao;

import com.slimetracker.modelo.Partida;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface PartidaDao {

    /** Inserta la partida y devuelve su id generado. */
    long insertar(Connection conexion, long usuarioId, String nombre);

    Optional<Partida> buscarPorId(Connection conexion, long id);

    /** Partidas del usuario, la última modificada primero. */
    List<Partida> listarPorUsuario(Connection conexion, long usuarioId);

    /** Devuelve las filas afectadas (0 si no existe). */
    int renombrar(Connection conexion, long id, String nombre);

    /** Devuelve las filas afectadas (0 si no existe). El progreso se borra en cascada. */
    int borrar(Connection conexion, long id);

    /** Pone actualizada_en a la hora actual. Devuelve las filas afectadas. */
    int tocar(Connection conexion, long id);
}
