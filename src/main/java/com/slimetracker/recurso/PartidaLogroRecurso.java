package com.slimetracker.recurso;

import com.slimetracker.dto.NotaPeticion;
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

/** Logros de una partida. */
@Path("partidas/{id}/logros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartidaLogroRecurso extends RecursoBase {

    @GET
    public Response listar(@PathParam("id") String id) {
        return Response.ok(app().logroServicio().listarDePartida(usuarioId(), Parametros.id(id, "id"))).build();
    }

    @POST
    @Path("{logroId}")
    public Response marcar(@PathParam("id") String id, @PathParam("logroId") String logroId) {
        long partidaId = Parametros.id(id, "id");
        long logro = Parametros.id(logroId, "logroId");
        return Response.created(uriInfo.getAbsolutePath())
                .entity(app().logroServicio().marcar(usuarioId(), partidaId, logro)).build();
    }

    @PUT
    @Path("{logroId}/nota")
    public Response actualizarNota(@PathParam("id") String id, @PathParam("logroId") String logroId,
                                   NotaPeticion cuerpo) {
        long partidaId = Parametros.id(id, "id");
        long logro = Parametros.id(logroId, "logroId");
        return Response.ok(app().logroServicio()
                .actualizarNota(usuarioId(), partidaId, logro, Parametros.cuerpo(cuerpo))).build();
    }

    @DELETE
    @Path("{logroId}")
    public Response desmarcar(@PathParam("id") String id, @PathParam("logroId") String logroId) {
        long partidaId = Parametros.id(id, "id");
        long logro = Parametros.id(logroId, "logroId");
        app().logroServicio().desmarcar(usuarioId(), partidaId, logro);
        return Response.noContent().build();
    }
}
