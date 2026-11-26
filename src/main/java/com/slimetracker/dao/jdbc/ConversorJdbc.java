package com.slimetracker.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/** Lecturas de columnas que pueden ser NULL. */
final class ConversorJdbc {

    private ConversorJdbc() {
    }

    static Instant instante(ResultSet filas, String columna) throws SQLException {
        Timestamp valor = filas.getTimestamp(columna);
        return valor == null ? null : valor.toInstant();
    }

    static Long largoONulo(ResultSet filas, String columna) throws SQLException {
        long valor = filas.getLong(columna);
        return filas.wasNull() ? null : valor;
    }
}
