package com.slimetracker.modelo;

import java.time.Instant;

/** Fila de partida_zona: una zona desbloqueada en una partida. */
public record PartidaZona(Long partidaId, Long zonaId, Instant desbloqueadaEn) {
}
