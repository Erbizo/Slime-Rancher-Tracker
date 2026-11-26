package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.SlimeDao;
import com.slimetracker.excepcion.TraductorSqlException;
import com.slimetracker.modelo.Dieta;
import com.slimetracker.modelo.Rareza;
import com.slimetracker.modelo.Slime;
import com.slimetracker.modelo.SlimeConEstado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlimeDaoJdbc implements SlimeDao {

    private static final String COLUMNAS_SLIME = "s.id, s.nombre, s.dieta, s.comida_favorita, s.rareza,"
            + " s.peligroso, s.descripcion, s.color, s.zona_id";
    private static final String DESDE_SLIME = " FROM slime s JOIN zona z ON z.id = s.zona_id";
    private static final String ORDEN = " ORDER BY z.orden, s.id";

    private static final String SQL_LISTAR = "SELECT " + COLUMNAS_SLIME + DESDE_SLIME;
    private static final String SQL_POR_ID = SQL_LISTAR + " WHERE s.id = ?";
    private static final String SQL_CON_ESTADO = "SELECT " + COLUMNAS_SLIME + ", ps.descubierto_en, ps.nota"
            + DESDE_SLIME + " LEFT JOIN partida_slime ps ON ps.slime_id = s.id AND ps.partida_id = ?";
    private static final String SQL_LISTAR_CON_ESTADO = SQL_CON_ESTADO + ORDEN;
    private static final String SQL_BUSCAR_CON_ESTADO = SQL_CON_ESTADO + " WHERE s.id = ?";
    private static final String SQL_DESCUBRIR = "INSERT INTO partida_slime (partida_id, slime_id) VALUES (?, ?)";
    private static final String SQL_OLVIDAR = "DELETE FROM partida_slime WHERE partida_id = ? AND slime_id = ?";
    private static final String SQL_NOTA = "UPDATE partida_slime SET nota = ? WHERE partida_id = ? AND slime_id = ?";
    private static final String SQL_ESTA_DESCUBIERTO =
            "SELECT 1 FROM partida_slime WHERE partida_id = ? AND slime_id = ?";
    private static final String SQL_DESCUBIERTOS_EN_ZONA = "SELECT s.nombre FROM slime s"
            + " JOIN partida_slime ps ON ps.slime_id = s.id AND ps.partida_id = ?"
            + " WHERE s.zona_id = ? ORDER BY s.id";

    @Override
    public List<Slime> listar(Connection conexion, Long zonaId, Dieta dieta, Rareza rareza) {
        // Solo se añaden fragmentos fijos de SQL; los valores van siempre como parámetros.
        List<String> condiciones = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        if (zonaId != null) {
            condiciones.add("s.zona_id = ?");
            valores.add(zonaId);
        }
        if (dieta != null) {
            condiciones.add("s.dieta = ?");
            valores.add(dieta.name());
        }
        if (rareza != null) {
            condiciones.add("s.rareza = ?");
            valores.add(rareza.name());
        }
        String sql = SQL_LISTAR
                + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones))
                + ORDEN;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < valores.size(); i++) {
                sentencia.setObject(i + 1, valores.get(i));
            }
            try (ResultSet filas = sentencia.executeQuery()) {
                List<Slime> slimes = new ArrayList<>();
                while (filas.next()) {
                    slimes.add(mapear(filas));
                }
                return slimes;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<Slime> buscarPorId(Connection conexion, long id) {
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
    public List<SlimeConEstado> listarConEstado(Connection conexion, long partidaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR_CON_ESTADO)) {
            sentencia.setLong(1, partidaId);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<SlimeConEstado> slimes = new ArrayList<>();
                while (filas.next()) {
                    slimes.add(mapearConEstado(filas));
                }
                return slimes;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<SlimeConEstado> buscarConEstado(Connection conexion, long partidaId, long slimeId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_CON_ESTADO)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, slimeId);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapearConEstado(filas)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public void descubrir(Connection conexion, long partidaId, long slimeId) {
        ejecutar(conexion, SQL_DESCUBRIR, partidaId, slimeId);
    }

    @Override
    public int olvidar(Connection conexion, long partidaId, long slimeId) {
        return ejecutar(conexion, SQL_OLVIDAR, partidaId, slimeId);
    }

    @Override
    public int actualizarNota(Connection conexion, long partidaId, long slimeId, String nota) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_NOTA)) {
            sentencia.setString(1, nota);
            sentencia.setLong(2, partidaId);
            sentencia.setLong(3, slimeId);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public boolean estaDescubierto(Connection conexion, long partidaId, long slimeId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ESTA_DESCUBIERTO)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, slimeId);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next();
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public List<String> listarDescubiertosEnZona(Connection conexion, long partidaId, long zonaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_DESCUBIERTOS_EN_ZONA)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, zonaId);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<String> nombres = new ArrayList<>();
                while (filas.next()) {
                    nombres.add(filas.getString("nombre"));
                }
                return nombres;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private int ejecutar(Connection conexion, String sql, long partidaId, long slimeId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, slimeId);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private SlimeConEstado mapearConEstado(ResultSet filas) throws SQLException {
        return new SlimeConEstado(mapear(filas), ConversorJdbc.instante(filas, "descubierto_en"),
                filas.getString("nota"));
    }

    private Slime mapear(ResultSet filas) throws SQLException {
        return new Slime(filas.getLong("id"), filas.getString("nombre"), Dieta.valueOf(filas.getString("dieta")),
                filas.getString("comida_favorita"), Rareza.valueOf(filas.getString("rareza")),
                filas.getBoolean("peligroso"), filas.getString("descripcion"), filas.getString("color"),
                filas.getLong("zona_id"));
    }
}
