package com.slimetracker.recurso;

import com.slimetracker.dto.DescubrirVariosPeticion;
import com.slimetracker.dto.NotaPeticion;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Slimepedia de una partida. */
@Path("partidas/{id}/slimes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartidaSlimeRecurso extends RecursoBase {

    @GET
    public Response listar(@PathParam("id") String id, @QueryParam("zonaId") String zonaId,
                           @QueryParam("descubierto") String descubierto) {
        long partidaId = Parametros.id(id, "id");
        Long zona = Parametros.idOpcional(zonaId, "zonaId");
        Boolean estado = Parametros.booleanoOpcional(descubierto, "descubierto");
        return Response.ok(app().slimeServicio().listarDePartida(usuarioId(), partidaId, zona, estado)).build();
    }

    /** Descubrir varios a la vez: todo o nada. */
    @POST
    public Response descubrirVarios(@PathParam("id") String id, DescubrirVariosPeticion cuerpo) {
        long partidaId = Parametros.id(id, "id");
        return Response.created(uriInfo.getAbsolutePath())
                .entity(app().slimeServicio().descubrirVarios(usuarioId(), partidaId, Parametros.cuerpo(cuerpo)))
                .build();
    }

    @POST
    @Path("{slimeId}")
    public Response descubrir(@PathParam("id") String id, @PathParam("slimeId") String slimeId) {
        long partidaId = Parametros.id(id, "id");
        long slime = Parametros.id(slimeId, "slimeId");
        return Response.created(uriInfo.getAbsolutePath())
                .entity(app().slimeServicio().descubrir(usuarioId(), partidaId, slime)).build();
    }

    @PUT
    @Path("{slimeId}/nota")
    public Response actualizarNota(@PathParam("id") String id, @PathParam("slimeId") String slimeId,
                                   NotaPeticion cuerpo) {
        long partidaId = Parametros.id(id, "id");
        long slime = Parametros.id(slimeId, "slimeId");
        return Response.ok(app().slimeServicio()
                .actualizarNota(usuarioId(), partidaId, slime, Parametros.cuerpo(cuerpo))).build();
    }

    @DELETE
    @Path("{slimeId}")
    public Response olvidar(@PathParam("id") String id, @PathParam("slimeId") String slimeId) {
        long partidaId = Parametros.id(id, "id");
        long slime = Parametros.id(slimeId, "slimeId");
        app().slimeServicio().olvidar(usuarioId(), partidaId, slime);
        return Response.noContent().build();
    }
}
