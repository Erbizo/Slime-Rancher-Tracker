package com.slimetracker.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Crea el pool de conexiones HikariCP a partir de la configuración. */
public final class ProveedorDataSource {

    private ProveedorDataSource() {
    }

    public static HikariDataSource crear(ConfigBD config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("slime-tracker");
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setJdbcUrl(config.urlJdbc());
        hikari.setUsername(config.usuario());
        hikari.setPassword(config.contrasena());
        hikari.setMaximumPoolSize(10);
        // Si la BD cae, la petición falla en 5 s (por defecto serían 30) y la web puede avisar a tiempo.
        hikari.setConnectionTimeout(5_000);
        return new HikariDataSource(hikari);
    }
}
