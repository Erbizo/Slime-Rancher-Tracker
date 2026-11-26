package com.slimetracker.modelo;

import java.time.Instant;

/** Fila de la tabla usuario. */
public record Usuario(Long id, String nombreUsuario, String hashContrasena, Instant creadoEn) {
}
