package com.slimetracker.excepcion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraductorSqlException")
class TraductorSqlExceptionTest {

    @ParameterizedTest(name = "código {0}, SQLState {1} → {2}")
    @CsvSource({
            "1062, 23000, EntidadDuplicadaException",       // MariaDB: clave duplicada
            "1451, 23000, IntegridadReferencialException",  // MariaDB: borrar un padre con hijos
            "1452, 23000, IntegridadReferencialException",  // MariaDB: insertar un hijo sin padre
            "23505, 23505, EntidadDuplicadaException",      // H2: clave duplicada
            "23503, 23503, IntegridadReferencialException", // H2: hijo con padre inexistente
            "23506, 23506, IntegridadReferencialException", // H2: padre con hijos
            "4025, 23000, AccesoDatosException",            // MariaDB: CHECK incumplido
            "0, 08S01, AccesoDatosException"                // conexión perdida
    })
    @DisplayName("traduce por código de MariaDB o por SQLState estándar")
    void traduce(int codigo, String estado, String esperada) {
        SQLException original = new SQLException("mensaje", estado, codigo);

        AccesoDatosException traducida = TraductorSqlException.traducir(original);

        assertThat(traducida.getClass().getSimpleName()).isEqualTo(esperada);
        assertThat(traducida.getCause()).isSameAs(original);
    }
}
