package com.slimetracker.dao;

import java.sql.Connection;

public interface SaludDao {

    /** Devuelve true si la base de datos responde a una consulta trivial. */
    boolean comprobar(Connection conexion);
}
