package com.slimetracker.tx;

import com.slimetracker.excepcion.TraductorSqlException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abre una conexión del pool y ejecuta un trabajo con ella.
 * enTransaccion: commit si todo va bien y rollback ante cualquier excepción.
 */
public class GestorTransacciones {

    private final DataSource dataSource;

    public GestorTransacciones(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T enTransaccion(FuncionSql<Connection, T> trabajo) {
        try (Connection conexion = dataSource.getConnection()) {
            boolean autoCommitPrevio = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            try {
                T resultado = trabajo.aplicar(conexion);
                conexion.commit();
                return resultado;
            } catch (Throwable e) {
                deshacer(conexion, e);
                throw e;
            } finally {
                conexion.setAutoCommit(autoCommitPrevio);
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    /** Para consultas: sin transacción explícita (autocommit). */
    public <T> T soloLectura(FuncionSql<Connection, T> trabajo) {
        try (Connection conexion = dataSource.getConnection()) {
            return trabajo.aplicar(conexion);
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private static void deshacer(Connection conexion, Throwable causa) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            causa.addSuppressed(e);
        }
    }
}
