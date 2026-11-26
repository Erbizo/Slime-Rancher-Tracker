package com.slimetracker.recurso;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Catálogo de solo lectura: zonas, slimes y logros. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogoRecurso extends RecursoBase {

    @GET
    @Path("zonas")
    public Response zonas() {
        return Response.ok(app().zonaServicio().listarCatalogo()).build();
    }

    @GET
    @Path("slimes")
    public Response slimes(@QueryParam("zonaId") String zonaId, @QueryParam("dieta") String dieta,
                           @QueryParam("rareza") String rareza) {
        Long filtroZona = Parametros.idOpcional(zonaId, "zonaId");
        return Response.ok(app().slimeServicio().listarCatalogo(filtroZona, dieta, rareza)).build();
    }

    @GET
    @Path("logros")
    public Response logros() {
        return Response.ok(app().logroServicio().listarCatalogo()).build();
    }
}
