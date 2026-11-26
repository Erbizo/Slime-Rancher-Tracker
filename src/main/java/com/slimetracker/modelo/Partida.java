package com.slimetracker.modelo;

import java.time.Instant;

/** Fila de la tabla partida. */
public record Partida(Long id, Long usuarioId, String nombre, Instant creadaEn, Instant actualizadaEn) {
}
