package com.slimetracker.dto;

import java.time.Instant;

/** Partida con su porcentaje global de progreso (para la lista de Mis partidas). */
public record PartidaRespuesta(long id, String nombre, Instant creadaEn, Instant actualizadaEn, int porcentaje) {
}
