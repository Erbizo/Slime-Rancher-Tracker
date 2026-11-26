package com.slimetracker.dto;

import java.time.Instant;

/** Slime visto desde una partida (Slimepedia). */
public record SlimeEstadoRespuesta(long id, String nombre, String dieta, String comidaFavorita, String rareza,
                                   boolean peligroso, String descripcion, String color,
                                   long zonaId, String zonaNombre, boolean zonaDesbloqueada,
                                   boolean descubierto, Instant descubiertoEn, String nota) {
}
