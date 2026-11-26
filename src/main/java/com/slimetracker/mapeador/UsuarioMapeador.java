package com.slimetracker.mapeador;

import com.slimetracker.dto.UsuarioRespuesta;
import com.slimetracker.modelo.Usuario;

public final class UsuarioMapeador {

    private UsuarioMapeador() {
    }

    /** El hash de la contraseña se queda fuera a propósito. */
    public static UsuarioRespuesta aRespuesta(Usuario usuario) {
        return new UsuarioRespuesta(usuario.id(), usuario.nombreUsuario());
    }
}
