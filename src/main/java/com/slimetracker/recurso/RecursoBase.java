package com.slimetracker.recurso;

import com.slimetracker.config.ContextoApp;
import com.slimetracker.seguridad.GestorSesion;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

/** Acceso a los servicios (ContextoApp) y al usuario de la sesión. */
abstract class RecursoBase {

    @Context
    private ServletContext servletContext;

    @Context
    protected HttpServletRequest peticion;

    @Context
    protected UriInfo uriInfo;

    protected ContextoApp app() {
        return (ContextoApp) servletContext.getAttribute(ContextoApp.ATRIBUTO);
    }

    protected long usuarioId() {
        return GestorSesion.usuarioId(peticion);
    }
}
