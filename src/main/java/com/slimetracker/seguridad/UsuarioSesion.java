package com.slimetracker.seguridad;

import java.io.Serializable;

/** Lo único que se guarda en la sesión: id y nombre del usuario (nunca el hash). */
public record UsuarioSesion(long id, String nombreUsuario) implements Serializable {
}
