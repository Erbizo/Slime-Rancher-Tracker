package com.slimetracker.recurso;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/** Punto de entrada de Jersey: toda la API cuelga de /api. */
@ApplicationPath("/api")
public class AplicacionRest extends ResourceConfig {

    public AplicacionRest() {
        packages("com.slimetracker.recurso", "com.slimetracker.seguridad", "com.slimetracker.excepcion");
        // Jackson sin sus ExceptionMappers: el JSON mal formado lo atiende MapeadorErrores con el formato común.
        register(JacksonFeature.withoutExceptionMappers());
        // No usamos WADL; sin esto Jersey avisa en cada arranque de que falta JAXB.
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }
}
