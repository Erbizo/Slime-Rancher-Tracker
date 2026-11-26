package com.slimetracker.recurso;

import com.slimetracker.dto.LoginPeticion;
import com.slimetracker.dto.RegistroPeticion;
import com.slimetracker.dto.UsuarioRespuesta;
import com.slimetracker.seguridad.GestorSesion;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AutenticacionRecurso extends RecursoBase {

    /** Crea la cuenta y deja la sesión iniciada. */
    @POST
    @Path("registro")
    public Response registrar(RegistroPeticion cuerpo) {
        UsuarioRespuesta usuario = app().autenticacionServicio().registrar(Parametros.cuerpo(cuerpo));
        GestorSesion.iniciar(peticion, usuario.id(), usuario.nombreUsuario());
        return Response.created(uriInfo.getBaseUriBuilder().path("auth/yo").build()).entity(usuario).build();
    }

    @POST
    @Path("login")
    public Response entrar(LoginPeticion cuerpo) {
        UsuarioRespuesta usuario = app().autenticacionServicio().entrar(Parametros.cuerpo(cuerpo));
        GestorSesion.iniciar(peticion, usuario.id(), usuario.nombreUsuario());
        return Response.ok(usuario).build();
    }

    @POST
    @Path("logout")
    public Response salir() {
        GestorSesion.cerrar(peticion);
        return Response.noContent().build();
    }

    @GET
    @Path("yo")
    public Response yo() {
        return Response.ok(app().autenticacionServicio().obtener(usuarioId())).build();
    }
}
