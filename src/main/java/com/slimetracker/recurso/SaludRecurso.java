package com.slimetracker.recurso;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("salud")
@Produces(MediaType.APPLICATION_JSON)
public class SaludRecurso extends RecursoBase {

    @GET
    public Response comprobar() {
        return Response.ok(app().saludServicio().comprobar()).build();
    }
}
