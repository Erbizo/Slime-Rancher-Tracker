package com.slimetracker.dao.jdbc;

import com.slimetracker.dao.ProgresoDao;
import com.slimetracker.excepcion.TraductorSqlException;
import com.slimetracker.modelo.Actividad;
import com.slimetracker.modelo.Actividad.TipoActividad;
import com.slimetracker.modelo.ConteoProgreso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgresoDaoJdbc implements ProgresoDao {

    private static final String SQL_CONTEO = """
            SELECT p.id,
                   (SELECT COUNT(*) FROM zona) AS zonas_total,
                   (SELECT COUNT(*) FROM partida_zona pz WHERE pz.partida_id = p.id) AS zonas_conseguidas,
                   (SELECT COUNT(*) FROM slime) AS slimes_total,
                   (SELECT COUNT(*) FROM partida_slime ps WHERE ps.partida_id = p.id) AS slimes_conseguidos,
                   (SELECT COUNT(*) FROM logro) AS logros_total,
                   (SELECT COUNT(*) FROM partida_logro pl WHERE pl.partida_id = p.id) AS logros_conseguidos
            FROM partida p""";
    private static final String SQL_CONTAR = SQL_CONTEO + " WHERE p.id = ?";
    private static final String SQL_CONTAR_POR_USUARIO = SQL_CONTEO + " WHERE p.usuario_id = ?";
    private static final String SQL_ACTIVIDADES = """
            SELECT tipo, nombre, fecha FROM (
                SELECT 'ZONA' AS tipo, z.nombre AS nombre, pz.desbloqueada_en AS fecha
                FROM partida_zona pz JOIN zona z ON z.id = pz.zona_id WHERE pz.partida_id = ?
                UNION ALL
                SELECT 'SLIME', s.nombre, ps.descubierto_en
                FROM partida_slime ps JOIN slime s ON s.id = ps.slime_id WHERE ps.partida_id = ?
                UNION ALL
                SELECT 'LOGRO', l.nombre, pl.conseguido_en
                FROM partida_logro pl JOIN logro l ON l.id = pl.logro_id WHERE pl.partida_id = ?
            ) actividad
            ORDER BY fecha DESC, nombre
            LIMIT ?""";

    @Override
    public ConteoProgreso contar(Connection conexion, long partidaId) {
        return contarPorClave(conexion, SQL_CONTAR, partidaId).values().stream()
                .findFirst()
                .orElse(new ConteoProgreso(0, 0, 0, 0, 0, 0));
    }

    @Override
    public Map<Long, ConteoProgreso> contarPorUsuario(Connection conexion, long usuarioId) {
        return contarPorClave(conexion, SQL_CONTAR_POR_USUARIO, usuarioId);
    }

    @Override
    public List<Actividad> ultimasActividades(Connection conexion, long partidaId, int limite) {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTIVIDADES)) {
            sentencia.setLong(1, partidaId);
            sentencia.setLong(2, partidaId);
            sentencia.setLong(3, partidaId);
            sentencia.setInt(4, limite);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<Actividad> actividades = new ArrayList<>();
                while (filas.next()) {
                    actividades.add(new Actividad(TipoActividad.valueOf(filas.getString("tipo")),
                            filas.getString("nombre"), ConversorJdbc.instante(filas, "fecha")));
                }
                return actividades;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }

    private Map<Long, ConteoProgreso> contarPorClave(Connection conexion, String sql, long clave) {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, clave);
            try (ResultSet filas = sentencia.executeQuery()) {
                Map<Long, ConteoProgreso> conteos = new LinkedHashMap<>();
                while (filas.next()) {
                    conteos.put(filas.getLong("id"), new ConteoProgreso(
                            filas.getInt("zonas_total"), filas.getInt("zonas_conseguidas"),
                            filas.getInt("slimes_total"), filas.getInt("slimes_conseguidos"),
                            filas.getInt("logros_total"), filas.getInt("logros_conseguidos")));
                }
                return conteos;
            }
        } catch (SQLException e) {
            throw TraductorSqlException.traducir(e);
        }
    }
}
