package com.slimetracker.modelo;

/** Fila de la tabla zona. zonaRequisitoId es null en la zona inicial. */
public record Zona(Long id, String nombre, String descripcion, int orden, boolean esInicial, Long zonaRequisitoId) {
}
