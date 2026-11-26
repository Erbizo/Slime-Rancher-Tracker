package com.slimetracker.excepcion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.slimetracker.dto.ErrorRespuesta;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Convierte cualquier excepción en el JSON de error uniforme:
 * {"status":400,"error":"Bad Request","mensaje":"...","campos":{...},"timestamp":"..."}.
 * Los 500 se registran con su traza en el log, pero al cliente solo le llega un mensaje genérico.
 */
@Provider
public class MapeadorErrores implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(MapeadorErrores.class);

    static final String JSON_NO_VALIDO = "El cuerpo de la petición no es un JSON válido o tiene tipos incorrectos.";
    static final String RUTA_NO_EXISTE = "La ruta no existe.";
    static final String METODO_NO_PERMITIDO = "Ese método no está permitido en esta ruta.";
    static final String SOLO_JSON = "El cuerpo de la petición tiene que ir en JSON (Content-Type: application/json).";
    static final String PETICION_NO_VALIDA = "La petición no se puede atender tal como viene.";
    static final String DUPLICADO = "Ya existe un registro con esos datos.";
    static final String INTEGRIDAD = "No se puede hacer: hay datos relacionados.";
    static final String SIN_DATOS = "No se ha podido acceder a los datos. Inténtalo de nuevo en un momento.";
    static final String INESPERADO = "Ha ocurrido un error inesperado.";

    @Override
    public Response toResponse(Throwable excepcion) {
        return switch (excepcion) {
            case ValidacionException e -> respuesta(Status.BAD_REQUEST, e.getMessage(), e.getCampos());
            case JsonProcessingException e -> respuesta(Status.BAD_REQUEST, JSON_NO_VALIDO, null);
            case NoAutenticadoException e -> respuesta(Status.UNAUTHORIZED, e.getMessage(), null);
            case AccesoDenegadoException e -> respuesta(Status.FORBIDDEN, e.getMessage(), null);
            case NoEncontradoException e -> respuesta(Status.NOT_FOUND, e.getMessage(), null);
            case ReglaNegocioException e -> respuesta(Status.CONFLICT, e.getMessage(), null);
            case EntidadDuplicadaException e -> respuesta(Status.CONFLICT, DUPLICADO, null);
            case IntegridadReferencialException e -> respuesta(Status.CONFLICT, INTEGRIDAD, null);
            case AccesoDatosException e -> inesperado(e, SIN_DATOS);
            case NotFoundException e -> respuesta(Status.NOT_FOUND, RUTA_NO_EXISTE, null);
            case NotAllowedException e -> respuesta(Status.METHOD_NOT_ALLOWED, METODO_NO_PERMITIDO, null);
            case NotSupportedException e -> respuesta(Status.UNSUPPORTED_MEDIA_TYPE, SOLO_JSON, null);
            // El mensaje de Jersey viene en inglés ("HTTP 406 Not Acceptable"): al usuario le llega uno propio.
            case WebApplicationException e when e.getResponse().getStatus() < 500 ->
                    respuesta(Status.fromStatusCode(e.getResponse().getStatus()), PETICION_NO_VALIDA, null);
            default -> inesperado(excepcion, INESPERADO);
        };
    }

    private static Response inesperado(Throwable excepcion, String mensaje) {
        LOG.error("Error inesperado atendiendo la petición", excepcion);
        return respuesta(Status.INTERNAL_SERVER_ERROR, mensaje, null);
    }

    static Response respuesta(Status estado, String mensaje, Map<String, String> campos) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(estado.getStatusCode(), estado.getReasonPhrase(), mensaje,
                campos, Instant.now());
        return Response.status(estado).type(MediaType.APPLICATION_JSON_TYPE).entity(cuerpo).build();
    }
}
