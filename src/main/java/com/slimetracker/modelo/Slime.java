package com.slimetracker.modelo;

/** Fila de la tabla slime. */
public record Slime(Long id, String nombre, Dieta dieta, String comidaFavorita, Rareza rareza,
                    boolean peligroso, String descripcion, String color, Long zonaId) {
}
