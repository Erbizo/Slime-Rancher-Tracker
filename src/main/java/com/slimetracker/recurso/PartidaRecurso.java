package com.slimetracker.recurso;

import com.slimetracker.dto.PartidaPeticion;
import com.slimetracker.dto.PartidaRespuesta;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("partidas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartidaRecurso extends RecursoBase {

    @GET
    public Response listar() {
        return Response.ok(app().partidaServicio().listar(usuarioId())).build();
    }

    @POST
    public Response crear(PartidaPeticion cuerpo) {
        PartidaRespuesta partida = app().partidaServicio().crear(usuarioId(), Parametros.cuerpo(cuerpo));
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(partida.id())).build())
                .entity(partida).build();
    }

    @GET
    @Path("{id}")
    public Response obtener(@PathParam("id") String id) {
        return Response.ok(app().partidaServicio().obtener(usuarioId(), Parametros.id(id, "id"))).build();
    }

    @PUT
    @Path("{id}")
    public Response renombrar(@PathParam("id") String id, PartidaPeticion cuerpo) {
        long partidaId = Parametros.id(id, "id");
        return Response.ok(app().partidaServicio().renombrar(usuarioId(), partidaId, Parametros.cuerpo(cuerpo)))
                .build();
    }

    @DELETE
    @Path("{id}")
    public Response borrar(@PathParam("id") String id) {
        app().partidaServicio().borrar(usuarioId(), Parametros.id(id, "id"));
        return Response.noContent().build();
    }

    @GET
    @Path("{id}/progreso")
    public Response progreso(@PathParam("id") String id) {
        return Response.ok(app().progresoServicio().obtener(usuarioId(), Parametros.id(id, "id"))).build();
    }
}
