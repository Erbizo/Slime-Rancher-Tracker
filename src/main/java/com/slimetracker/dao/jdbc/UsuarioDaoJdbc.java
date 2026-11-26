package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.UsuarioDao;
import com.slimetracker.excepcion.AccesoDatosException;
import com.slimetracker.excepcion.TraductorSqlException;
import com.slimetracker.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UsuarioDaoJdbc implements UsuarioDao {

    private static final String COLUMNAS = "SELECT id, nombre_usuario, hash_contrasena, creado_en FROM usuario";
    private static final String SQL_INSERTAR = "INSERT INTO usuario (nombre_usuario, hash_contrasena) VALUES (?, ?)";
    private static final String SQL_POR_ID = COLUMNAS + " WHERE id = ?";
    private static final String SQL_POR_NOMBRE = COLUMNAS + " WHERE nombre_usuario = ?";

    @Override
    public long insertar(Connection conexion, String nombreUsuario, String hashContrasena) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setString(1, nombreUsuario);
            sentencia.setString(2, hashContrasena);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new AccesoDatosException("La base de datos no ha devuelto el id del usuario", null);
                }
                return claves.getLong(1);
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(Connection conexion, long id) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_POR_ID)) {
            sentencia.setLong(1, id);
            return buscarUno(sentencia);
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorNombre(Connection conexion, String nombreUsuario) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_POR_NOMBRE)) {
            sentencia.setString(1, nombreUsuario);
            return buscarUno(sentencia);
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private Optional<Usuario> buscarUno(PreparedStatement sentencia) throws SQLException {
        try (ResultSet filas = sentencia.executeQuery()) {
            return filas.next() ? Optional.of(mapear(filas)) : Optional.empty();
        }
    }

    private Usuario mapear(ResultSet filas) throws SQLException {
        return new Usuario(filas.getLong("id"), filas.getString("nombre_usuario"),
                filas.getString("hash_contrasena"), ConversorJdbc.instante(filas, "creado_en"));
    }
}
