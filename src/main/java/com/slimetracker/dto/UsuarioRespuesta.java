package com.slimetracker.dto;

/** Datos públicos del usuario. Nunca lleva el hash de la contraseña. */
public record UsuarioRespuesta(long id, String nombreUsuario) {
}
