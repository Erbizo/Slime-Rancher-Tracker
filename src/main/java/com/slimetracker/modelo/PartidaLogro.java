package com.slimetracker.modelo;

import java.time.Instant;

/** Fila de partida_logro: un logro conseguido en una partida. */
public record PartidaLogro(Long partidaId, Long logroId, Instant conseguidoEn, String nota) {
}
