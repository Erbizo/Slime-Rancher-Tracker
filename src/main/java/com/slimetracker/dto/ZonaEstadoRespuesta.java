package com.slimetracker.dto;

import java.time.Instant;

/** Zona vista desde una partida: si está desbloqueada, si se puede desbloquear y sus slimes. */
public record ZonaEstadoRespuesta(long id, String nombre, String descripcion, int orden, boolean esInicial,
                                  Long requisitoId, String requisitoNombre,
                                  boolean desbloqueada, Instant desbloqueadaEn, boolean puedeDesbloquear,
                                  int slimesTotal, int slimesDescubiertos) {
}
