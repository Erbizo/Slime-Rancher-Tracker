package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.SaludDao;
import com.slimetracker.excepcion.TraductorSqlException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SaludDaoJdbc implements SaludDao {

    private static final String SQL_COMPROBAR = "SELECT 1";

    @Override
    public boolean comprobar(Connection conexion) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_COMPROBAR);
             ResultSet filas = sentencia.executeQuery()) {
            return filas.next() && filas.getInt(1) == 1;
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }
}
