package com.slimetracker.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crea el ContextoApp al desplegar y cierra el pool al parar.
 * Si falta configuración, la excepción hace que Tomcat no despliegue la aplicación.
 */
@WebListener
public class ArranqueAplicacion implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ArranqueAplicacion.class);

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        try {
            ContextoApp contexto = new ContextoApp(ConfigBD.desdeEntorno());
            evento.getServletContext().setAttribute(ContextoApp.ATRIBUTO, contexto);
        } catch (RuntimeException e) {
            // Tomcat solo deja el detalle en logs/localhost.*.log; así sale también en `docker compose logs app`.
            LOG.error("No se puede arrancar la aplicación: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        Object contexto = evento.getServletContext().getAttribute(ContextoApp.ATRIBUTO);
        if (contexto instanceof ContextoApp app) {
            app.cerrar();
        }
    }
}
