package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.ZonaDao;
import com.slimetracker.excepcion.TraductorSqlException;
import com.slimetracker.modelo.Zona;
import com.slimetracker.modelo.ZonaConEstado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ZonaDaoJdbc implements ZonaDao {

    private static final String COLUMNAS =
            "SELECT z.id, z.nombre, z.descripcion, z.orden, z.es_inicial, z.zona_requisito_id FROM zona z";
    private static final String SQL_LISTAR = COLUMNAS + " ORDER BY z.orden";
    private static final String SQL_POR_ID = COLUMNAS + " WHERE z.id = ?";
    private static final String SQL_INICIAL = COLUMNAS + " WHERE z.es_inicial = TRUE ORDER BY z.orden LIMIT 1";
    private static final String SQL_CON_ESTADO = """
            SELECT z.id, z.nombre, z.descripcion, z.orden, z.es_inicial, z.zona_requisito_id,
                   pz.desbloqueada_en,
                   (SELECT COUNT(*) FROM slime s WHERE s.zona_id = z.id) AS slimes_total,
                   (SELECT COUNT(*) FROM slime s JOIN partida_slime ps ON ps.slime_id = s.id
                     WHERE s.zona_id = z.id AND ps.partida_id = ?) AS slimes_descubiertos
            FROM zona z
            LEFT JOIN partida_zona pz ON pz.zona_id = z.id AND pz.partida_id = ?
            ORDER BY z.orden""";
    private static final String SQL_DESBLOQUEAR = "INSERT INTO partida_zona (partida_id, zona_id) VALUES (?, ?)";
    private static final String SQL_BLOQUEAR = "DELETE FROM partida_zona WHERE partida_id = ? AND zona_id = ?";
    private static final String SQL_ESTA_DESBLOQUEADA =
            "SELECT 1 FROM partida_zona WHERE partida_id = ? AND zona_id = ?";
    private static final String SQL_DEPENDIENTES_DESBLOQUEADAS = COLUMNAS
            + " JOIN partida_zona pz ON pz.zona_id = z.id AND pz.partida_id = ?"
            + " WHERE z.zona_requisito_id = ? ORDER BY z.orden";

    @Override
    public List<Zona> listar(Connection conexion) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR)) {
            return listarZonas(sentencia);
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<Zona> buscarPorId(Connection conexion, long id) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_POR_ID)) {
            sentencia.setLong(1, id);
            return listarZonas(sentencia).stream().findFirst();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public Optional<Zona> buscarInicial(Connection conexion) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_INICIAL)) {
            return listarZonas(sentencia).stream().findFirst();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public List<ZonaConEstado> listarConEstado(Connection conexion, long partidaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_CON_ESTADO)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, partidaId);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<ZonaConEstado> zonas = new ArrayList<>();
                while (filas.next()) {
                    zonas.add(new ZonaConEstado(mapear(filas), ConversorJdbc.instante(filas, "desbloqueada_en"),
                            filas.getInt("slimes_total"), filas.getInt("slimes_descubiertos")));
                }
                return zonas;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public void desbloquear(Connection conexion, long partidaId, long zonaId) {
        ejecutar(conexion, SQL_DESBLOQUEAR, partidaId, zonaId);
    }

    @Override
    public int bloquear(Connection conexion, long partidaId, long zonaId) {
        return ejecutar(conexion, SQL_BLOQUEAR, partidaId, zonaId);
    }

    @Override
    public boolean estaDesbloqueada(Connection conexion, long partidaId, long zonaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ESTA_DESBLOQUEADA)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, zonaId);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next();
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    @Override
    public List<Zona> listarDependientesDesbloqueadas(Connection conexion, long partidaId, long zonaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_DEPENDIENTES_DESBLOQUEADAS)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, zonaId);
            return listarZonas(sentencia);
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private int ejecutar(Connection conexion, String sql, long partidaId, long zonaId) {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, zonaId);
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private List<Zona> listarZonas(PreparedStatement sentencia) throws SQLException {
        try (ResultSet filas = sentencia.executeQuery()) {
            List<Zona> zonas = new ArrayList<>();
            while (filas.next()) {
                zonas.add(mapear(filas));
            }
            return zonas;
        }
    }

    private Zona mapear(ResultSet filas) throws SQLException {
        return new Zona(filas.getLong("id"), filas.getString("nombre"), filas.getString("descripcion"),
                filas.getInt("orden"), filas.getBoolean("es_inicial"),
                ConversorJdbc.largoONulo(filas, "zona_requisito_id"));
    }
}
