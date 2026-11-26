package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.LogroDao;
import com.slimetracker.excepcion.TraductorSqlException;
import com.slimetracker.modelo.CategoriaLogro;
import com.slimetracker.modelo.Logro;
import com.slimetracker.modelo.LogroConEstado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LogroDaoJdbc implements LogroDao {

    private static final String COLUMNAS = "l.id, l.nombre, l.descripcion, l.categoria";
    private static final String SQL_LISTAR = "SELECT " + COLUMNAS + " FROM logro l ORDER BY l.id";
    private static final String SQL_CON_ESTADO = "SELECT " + COLUMNAS + ", pl.conseguido_en, pl.nota FROM logro l"
            + " LEFT JOIN partida_logro pl ON pl.logro_id = l.id AND pl.partida_id = ?";
    private static final String SQL_LISTAR_CON_ESTADO = SQL_CON_ESTADO + " ORDER BY l.id";
    private static final String SQL_BUSCAR_CON_ESTADO = SQL_CON_ESTADO + " WHERE l.id = ?";
    private static final String SQL_MARCAR = "INSERT INTO partida_logro (partida_id, logro_id) VALUES (?, ?)";
    private static final String SQL_DESMARCAR = "DELETE FROM partida_logro WHERE partida_id = ? AND logro_id = ?";
    private static final String SQL_NOTA = "UPDATE partida_logro SET nota = ? WHERE partida_id = ? AND logro_id = ?";

    @Override
    public List<Logro> listar(Connection conexion) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {
            List<Logro> logros = new ArrayList<>();
            while (filas.next()) {
                logros.add(mapear(filas));
            }
            return logros;
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public List<LogroConEstado> listarConEstado(Connection conexion, long partidaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_CON_ESTADO)) {
            sentencia.setLong(1, partidaId);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<LogroConEstado> logros = new ArrayList<>();
                while (filas.next()) {
                    logros.add(mapearConEstado(filas));
                }
                return logros;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<LogroConEstado> buscarConEstado(Connection conexion, long partidaId, long logroId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_CON_ESTADO)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, logroId);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapearConEstado(filas)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public void marcar(Connection conexion, long partidaId, long logroId) {
        ejecutar(conexion, SQL_MARCAR, partidaId, logroId);
    }

    @Override
    public int desmarcar(Connection conexion, long partidaId, long logroId) {
        return ejecutar(conexion, SQL_DESMARCAR, partidaId, logroId);
    }

    @Override
    public int actualizarNota(Connection conexion, long partidaId, long logroId, String nota) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_NOTA)) {
            sentencia.setString(1, nota);
            sentencia.setLong(2, partidaId);
            sentencia.setLong(3, logroId);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private int ejecutar(Connection conexion, String sql, long partidaId, long logroId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, logroId);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private LogroConEstado mapearConEstado(ResultSet filas) throws SQLException {
        return new LogroConEstado(mapear(filas), ConversorJdbc.instante(filas, "conseguido_en"),
                filas.getString("nota"));
    }

    private Logro mapear(ResultSet filas) throws SQLException {
        return new Logro(filas.getLong("id"), filas.getString("nombre"), filas.getString("descripcion"),
                CategoriaLogro.valueOf(filas.getString("categoria")));
    }
}
