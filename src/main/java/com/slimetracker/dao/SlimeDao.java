package com.slimetracker.dao;

import com.slimetracker.modelo.Dieta;
import com.slimetracker.modelo.Rareza;
import com.slimetracker.modelo.Slime;
import com.slimetracker.modelo.SlimeConEstado;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface SlimeDao {

    /** Catálogo; cada filtro es opcional (null = sin filtrar). */
    List<Slime> listar(Connection conexion, Long zonaId, Dieta dieta, Rareza rareza);

    Optional<Slime> buscarPorId(Connection conexion, long id);

    /** Todos los slimes con su estado en la partida (una sola consulta con LEFT JOIN). */
    List<SlimeConEstado> listarConEstado(Connection conexion, long partidaId);

    /** Un slime con su estado en la partida; vacío si el slime no existe. */
    Optional<SlimeConEstado> buscarConEstado(Connection conexion, long partidaId, long slimeId);

    void descubrir(Connection conexion, long partidaId, long slimeId);

    /** Devuelve las filas afectadas (0 si no estaba descubierto). */
    int olvidar(Connection conexion, long partidaId, long slimeId);

    /** Devuelve las filas afectadas (0 si no estaba descubierto). */
    int actualizarNota(Connection conexion, long partidaId, long slimeId, String nota);

    boolean estaDescubierto(Connection conexion, long partidaId, long slimeId);

    /** Nombres de los slimes de la zona descubiertos en la partida. */
    List<String> listarDescubiertosEnZona(Connection conexion, long partidaId, long zonaId);
}
