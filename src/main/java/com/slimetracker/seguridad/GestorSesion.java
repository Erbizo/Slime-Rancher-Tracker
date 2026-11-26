package com.slimetracker.seguridad;

import com.slimetracker.excepcion.NoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/** Inicio, consulta y cierre de la sesión HTTP. */
public final class GestorSesion {

    static final String ATRIBUTO = "usuario";
    static final String SIN_SESION = "Tienes que entrar para continuar.";

    private GestorSesion() {
    }

    /** Si ya había sesión se le cambia el id (evita la fijación de sesión). */
    public static void iniciar(HttpServletRequest peticion, long usuarioId, String nombreUsuario) {
        if (peticion.getSession(false) != null) {
            peticion.changeSessionId();
        }
        peticion.getSession(true).setAttribute(ATRIBUTO, new UsuarioSesion(usuarioId, nombreUsuario));
    }

    public static void cerrar(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }

    public static Optional<UsuarioSesion> obtener(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion == null) {
            return Optional.empty();
        }
        return sesion.getAttribute(ATRIBUTO) instanceof UsuarioSesion usuario ? Optional.of(usuario) : Optional.empty();
    }

    /** Id del usuario en sesión; 401 si no hay. */
    public static long usuarioId(HttpServletRequest peticion) {
        return obtener(peticion).map(UsuarioSesion::id).orElseThrow(() -> new NoAutenticadoException(SIN_SESION));
    }
}
