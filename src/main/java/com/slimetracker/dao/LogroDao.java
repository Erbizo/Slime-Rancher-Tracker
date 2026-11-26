package com.slimetracker.dao;

import com.slimetracker.modelo.Logro;
import com.slimetracker.modelo.LogroConEstado;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface LogroDao {

    List<Logro> listar(Connection conexion);

    /** Todos los logros con su estado en la partida (una sola consulta con LEFT JOIN). */
    List<LogroConEstado> listarConEstado(Connection conexion, long partidaId);

    /** Un logro con su estado en la partida; vacío si el logro no existe. */
    Optional<LogroConEstado> buscarConEstado(Connection conexion, long partidaId, long logroId);

    void marcar(Connection conexion, long partidaId, long logroId);

    /** Devuelve las filas afectadas (0 si no estaba conseguido). */
    int desmarcar(Connection conexion, long partidaId, long logroId);

    /** Devuelve las filas afectadas (0 si no estaba conseguido). */
    int actualizarNota(Connection conexion, long partidaId, long logroId, String nota);
}
