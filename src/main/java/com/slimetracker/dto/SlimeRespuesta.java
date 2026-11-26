package com.slimetracker.dto;

/** Slime del catálogo. color es el hex #RRGGBB con el que se pinta en la web. */
public record SlimeRespuesta(long id, String nombre, String dieta, String comidaFavorita, String rareza,
                             boolean peligroso, String descripcion, String color, long zonaId, String zonaNombre) {
}
