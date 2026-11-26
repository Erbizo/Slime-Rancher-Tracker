package com.slimetracker.excepcion;

import java.sql.SQLException;
import java.util.Set;

/**
 * Convierte una SQLException en una excepción propia sin comprobar.
 * Mira el código de error de MariaDB y, si no lo reconoce, el SQLState estándar
 * (así también funciona con H2 en los tests, que usa sus propios códigos).
 */
public final class TraductorSqlException {

    private static final int DUPLICADO = 1062;
    private static final Set<Integer> INTEGRIDAD = Set.of(1451, 1452);
    private static final String SQLSTATE_DUPLICADO = "23505";
    private static final Set<String> SQLSTATE_INTEGRIDAD = Set.of("23503", "23506");

    private TraductorSqlException() {
    }

    public static AccesoDatosException traducir(SQLException e) {
        int codigo = e.getErrorCode();
        String estado = e.getSQLState();
        if (codigo == DUPLICADO || SQLSTATE_DUPLICADO.equals(estado)) {
            return new EntidadDuplicadaException("Registro duplicado", e);
        }
        if (INTEGRIDAD.contains(codigo) || SQLSTATE_INTEGRIDAD.contains(estado)) {
            return new IntegridadReferencialException("Violación de integridad referencial", e);
        }
        return new AccesoDatosException("Error de acceso a datos", e);
    }
}
