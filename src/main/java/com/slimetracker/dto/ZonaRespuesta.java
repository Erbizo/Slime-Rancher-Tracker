package com.slimetracker.dto;

/** Zona del catálogo. */
public record ZonaRespuesta(long id, String nombre, String descripcion, int orden, boolean esInicial, Long requisitoId) {
}
