package com.slimetracker.dao.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base de los tests de integración de los DAO: H2 en memoria en modo MariaDB.
 * Antes de cada test recrea el esquema y los datos de prueba (schema-h2.sql), así los tests no se pisan.
 */
abstract class BaseDaoTest {

    static final long RANCHER = 1;
    static final long VECINA = 2;
    static final long PRIMERA_PARTIDA = 1;
    static final long MODO_RUSH = 2;
    static final long MI_RANCHO = 3;
    static final long THE_RANCH = 1;
    static final long DRY_REEF = 2;
    static final long INDIGO_QUARRY = 3;
    static final long MOSS_BLANKET = 4;
    static final long ANCIENT_RUINS = 5;
    static final long GLASS_DESERT = 6;
    static final long PINK = 1;
    static final long LUCKY = 5;
    static final long RAD = 6;
    static final long QUANTUM = 13;
    static final long INEXISTENTE = 999_999;

    protected static final DataSource DATA_SOURCE = crearDataSource();

    protected Connection conexion;

    private static DataSource crearDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    @BeforeEach
    void recrearBaseDeDatos() throws SQLException {
        try (Connection inicial = DATA_SOURCE.getConnection(); Statement sentencia = inicial.createStatement()) {
            sentencia.execute("RUNSCRIPT FROM 'classpath:/schema-h2.sql' CHARSET 'UTF-8'");
        }
        conexion = DATA_SOURCE.getConnection();
    }

    @AfterEach
    void cerrarConexion() throws SQLException {
        conexion.close();
    }

    /** Ejecuta un SELECT COUNT(*) (solo para comprobar resultados en los tests). */
    protected long contar(String sql, long... parametros) throws SQLException {
        try (Connection otra = DATA_SOURCE.getConnection(); PreparedStatement sentencia = otra.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setLong(i + 1, parametros[i]);
            }
            try (ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                return filas.getLong(1);
            }
        }
    }
}
