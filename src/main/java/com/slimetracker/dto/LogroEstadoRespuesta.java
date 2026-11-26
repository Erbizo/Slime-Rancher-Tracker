package com.slimetracker.dto;

import java.time.Instant;

/** Logro visto desde una partida. */
public record LogroEstadoRespuesta(long id, String nombre, String descripcion, String categoria,
                                   boolean conseguido, Instant conseguidoEn, String nota) {
}
