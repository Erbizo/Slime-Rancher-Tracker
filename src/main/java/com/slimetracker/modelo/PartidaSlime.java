package com.slimetracker.modelo;

import java.time.Instant;

/** Fila de partida_slime: un slime descubierto en una partida. */
public record PartidaSlime(Long partidaId, Long slimeId, Instant descubiertoEn, String nota) {
}
