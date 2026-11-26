package com.slimetracker.recurso;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Zonas de una partida: desbloquear (POST) y bloquear (DELETE). */
@Path("partidas/{id}/zonas")
@Produces(MediaType.APPLICATION_JSON)
public class PartidaZonaRecurso extends RecursoBase {

    @GET
    public Response listar(@PathParam("id") String id) {
        return Response.ok(app().zonaServicio().listarDePartida(usuarioId(), Parametros.id(id, "id"))).build();
    }

    @POST
    @Path("{zonaId}")
    public Response desbloquear(@PathParam("id") String id, @PathParam("zonaId") String zonaId) {
        long partidaId = Parametros.id(id, "id");
        long zona = Parametros.id(zonaId, "zonaId");
        return Response.created(uriInfo.getAbsolutePath())
                .entity(app().zonaServicio().desbloquear(usuarioId(), partidaId, zona)).build();
    }

    @DELETE
    @Path("{zonaId}")
    public Response bloquear(@PathParam("id") String id, @PathParam("zonaId") String zonaId) {
        long partidaId = Parametros.id(id, "id");
        long zona = Parametros.id(zonaId, "zonaId");
        app().zonaServicio().bloquear(usuarioId(), partidaId, zona);
        return Response.noContent().build();
    }
}
