package com.slimetracker.tx;

import java.sql.SQLException;

/** Como java.util.function.Function, pero puede lanzar SQLException. */
@FunctionalInterface
public interface FuncionSql<T, R> {

    R aplicar(T entrada) throws SQLException;
}
