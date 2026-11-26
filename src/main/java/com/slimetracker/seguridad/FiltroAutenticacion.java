package com.slimetracker.seguridad;

import com.slimetracker.excepcion.NoAutenticadoException;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;

/** Exige sesión en toda la API salvo registro, login y salud (401 si no la hay). */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class FiltroAutenticacion implements ContainerRequestFilter {

    static final Set<String> RUTAS_PUBLICAS = Set.of("auth/registro", "auth/login", "salud");

    @Context
    private HttpServletRequest peticion;

    public FiltroAutenticacion() {
    }

    /** Para los tests: Jersey inyecta la petición en el campo. */
    FiltroAutenticacion(HttpServletRequest peticion) {
        this.peticion = peticion;
    }

    @Override
    public void filter(ContainerRequestContext contexto) {
        String ruta = contexto.getUriInfo().getPath();
        if (ruta.startsWith("/")) {
            ruta = ruta.substring(1);
        }
        if (RUTAS_PUBLICAS.contains(ruta)) {
            return;
        }
        if (GestorSesion.obtener(peticion).isEmpty()) {
            throw new NoAutenticadoException(GestorSesion.SIN_SESION);
        }
    }
}
