package com.slimetracker.excepcion;

import com.fasterxml.jackson.core.JsonParseException;
import com.slimetracker.dto.ErrorRespuesta;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MapeadorErrores: formato de error uniforme y códigos HTTP")
class MapeadorErroresTest {

    private final MapeadorErrores mapeador = new MapeadorErrores();

    static Stream<Arguments> excepciones() {
        return Stream.of(
                Arguments.of(new NoAutenticadoException("Tienes que entrar."), 401, "Unauthorized", "Tienes que entrar."),
                Arguments.of(new AccesoDenegadoException("Esa partida no es tuya."), 403, "Forbidden", "Esa partida no es tuya."),
                Arguments.of(new NoEncontradoException("La zona no existe."), 404, "Not Found", "La zona no existe."),
                Arguments.of(new ReglaNegocioException("Primero tienes que desbloquear Dry Reef."), 409, "Conflict",
                        "Primero tienes que desbloquear Dry Reef."),
                Arguments.of(new EntidadDuplicadaException("dup", null), 409, "Conflict", MapeadorErrores.DUPLICADO),
                Arguments.of(new IntegridadReferencialException("fk", null), 409, "Conflict", MapeadorErrores.INTEGRIDAD),
                Arguments.of(new JsonParseException(null, "roto"), 400, "Bad Request", MapeadorErrores.JSON_NO_VALIDO),
                Arguments.of(new NotFoundException(), 404, "Not Found", MapeadorErrores.RUTA_NO_EXISTE),
                Arguments.of(new NotAllowedException("GET"), 405, "Method Not Allowed", MapeadorErrores.METODO_NO_PERMITIDO));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("excepciones")
    @DisplayName("cada excepción propia se convierte en su código con el mensaje para el usuario")
    void codigos(Throwable excepcion, int status, String error, String mensaje) {
        Response respuesta = mapeador.toResponse(excepcion);
        ErrorRespuesta cuerpo = (ErrorRespuesta) respuesta.getEntity();

        assertThat(respuesta.getStatus()).isEqualTo(status);
        assertThat(cuerpo.status()).isEqualTo(status);
        assertThat(cuerpo.error()).isEqualTo(error);
        assertThat(cuerpo.mensaje()).isEqualTo(mensaje);
        assertThat(cuerpo.timestamp()).isNotNull();
        assertThat(cuerpo.campos()).isNull();
    }

    @Test
    @DisplayName("ValidacionException → 400 con todos los errores por campo")
    void validacion() {
        Response respuesta = mapeador.toResponse(new ValidacionException(Map.of("nombre", "Obligatorio", "nota", "Larga")));
        ErrorRespuesta cuerpo = (ErrorRespuesta) respuesta.getEntity();

        assertThat(respuesta.getStatus()).isEqualTo(400);
        assertThat(cuerpo.campos()).containsOnlyKeys("nombre", "nota");
    }

    @Test
    @DisplayName("un error inesperado → 500 genérico, sin detalles internos")
    void inesperado() {
        Response respuesta = mapeador.toResponse(new NullPointerException("campo secreto de la clase X"));
        ErrorRespuesta cuerpo = (ErrorRespuesta) respuesta.getEntity();

        assertThat(respuesta.getStatus()).isEqualTo(500);
        assertThat(cuerpo.mensaje()).isEqualTo(MapeadorErrores.INESPERADO).doesNotContain("secreto");
    }

    @Test
    @DisplayName("un fallo de base de datos → 500 con un mensaje claro y sin SQL")
    void baseDeDatos() {
        AccesoDatosException fallo = new AccesoDatosException("Error", new SQLException("SELECT * FROM usuario"));
        ErrorRespuesta cuerpo = (ErrorRespuesta) mapeador.toResponse(fallo).getEntity();

        assertThat(cuerpo.status()).isEqualTo(500);
        assertThat(cuerpo.mensaje()).isEqualTo(MapeadorErrores.SIN_DATOS).doesNotContain("SELECT");
    }

    @Test
    @DisplayName("415 y otros 4xx de JAX-RS conservan su código, con el mensaje en español")
    void otrosCuatrocientos() {
        ErrorRespuesta sinJson = (ErrorRespuesta) mapeador.toResponse(new NotSupportedException()).getEntity();
        ErrorRespuesta noAceptable = (ErrorRespuesta) mapeador.toResponse(new NotAcceptableException()).getEntity();

        assertThat(sinJson.status()).isEqualTo(415);
        assertThat(sinJson.mensaje()).isEqualTo(MapeadorErrores.SOLO_JSON);
        assertThat(noAceptable.status()).isEqualTo(406);
        assertThat(noAceptable.mensaje()).isEqualTo(MapeadorErrores.PETICION_NO_VALIDA).doesNotContain("HTTP");
    }
}
