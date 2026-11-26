package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.excepcion.AccesoDatosException;
import com.slimetracker.excepcion.TraductorSqlException;
import com.slimetracker.modelo.Partida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartidaDaoJdbc implements PartidaDao {

    private static final String COLUMNAS = "SELECT id, usuario_id, nombre, creada_en, actualizada_en FROM partida";
    private static final String SQL_INSERTAR = "INSERT INTO partida (usuario_id, nombre) VALUES (?, ?)";
    private static final String SQL_POR_ID = COLUMNAS + " WHERE id = ?";
    private static final String SQL_POR_USUARIO = COLUMNAS + " WHERE usuario_id = ? ORDER BY actualizada_en DESC, id DESC";
    private static final String SQL_RENOMBRAR = "UPDATE partida SET nombre = ? WHERE id = ?";
    private static final String SQL_BORRAR = "DELETE FROM partida WHERE id = ?";
    private static final String SQL_TOCAR = "UPDATE partida SET actualizada_en = CURRENT_TIMESTAMP WHERE id = ?";

    @Override
    public long insertar(Connection conexion, long usuarioId, String nombre) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setLong(1, usuarioId);
            sentencia.setString(2, nombre);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new AccesoDatosException("La base de datos no ha devuelto el id de la partida", null);
                }
                return claves.getLong(1);
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<Partida> buscarPorId(Connection conexion, long id) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_POR_ID)) {
            sentencia.setLong(1, id);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public List<Partida> listarPorUsuario(Connection conexion, long usuarioId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_POR_USUARIO)) {
            sentencia.setLong(1, usuarioId);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<Partida> partidas = new ArrayList<>();
                while (filas.next()) {
                    partidas.add(mapear(filas));
                }
                return partidas;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public int renombrar(Connection conexion, long id, String nombre) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_RENOMBRAR)) {
            sentencia.setString(1, nombre);
            sentencia.setLong(2, id);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public int borrar(Connection conexion, long id) {
        return ejecutarPorId(conexion, SQL_BORRAR, id);
    }

    @Override
    public int tocar(Connection conexion, long id) {
        return ejecutarPorId(conexion, SQL_TOCAR, id);
    }

    private int ejecutarPorId(Connection conexion, String sql, long id) {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, id);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private Partida mapear(ResultSet filas) throws SQLException {
        return new Partida(filas.getLong("id"), filas.getLong("usuario_id"), filas.getString("nombre"),
                ConversorJdbc.instante(filas, "creada_en"), ConversorJdbc.instante(filas, "actualizada_en"));
    }
}
