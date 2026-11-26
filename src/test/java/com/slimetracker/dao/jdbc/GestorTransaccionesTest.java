package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.AccesoDatosException;
import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.modelo.Partida;
import com.slimetracker.tx.GestorTransacciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GestorTransacciones (integración con H2)")
class GestorTransaccionesTest extends BaseDaoTest {

    private final GestorTransacciones gestor = new GestorTransacciones(DATA_SOURCE);
    private final PartidaDaoJdbc partidas = new PartidaDaoJdbc();
    private final ZonaDaoJdbc zonas = new ZonaDaoJdbc();

    @Test
    @DisplayName("si todo va bien, hace commit")
    void commit() throws SQLException {
        long id = gestor.enTransaccion(con -> {
            long nueva = partidas.insertar(con, VECINA, "Confirmada");
            zonas.desbloquear(con, nueva, THE_RANCH);
            return nueva;
        });

        assertThat(contar("SELECT COUNT(*) FROM partida WHERE id = ?", id)).isEqualTo(1);
        assertThat(contar("SELECT COUNT(*) FROM partida_zona WHERE partida_id = ?", id)).isEqualTo(1);
    }

    @Test
    @DisplayName("una excepción a mitad deshace TODO lo hecho en la transacción")
    void rollbackCompleto() throws SQLException {
        long partidasAntes = contar("SELECT COUNT(*) FROM partida");

        assertThatThrownBy(() -> gestor.enTransaccion(con -> {
            long nueva = partidas.insertar(con, VECINA, "Deshecha");
            zonas.desbloquear(con, nueva, THE_RANCH);
            zonas.desbloquear(con, nueva, THE_RANCH); // duplicado: falla aquí
            return nueva;
        })).isInstanceOf(EntidadDuplicadaException.class);

        assertThat(contar("SELECT COUNT(*) FROM partida")).isEqualTo(partidasAntes);
        assertThat(contar("SELECT COUNT(*) FROM partida WHERE nombre = 'Deshecha'")).isZero();
    }

    @Test
    @DisplayName("una excepción de negocio también provoca rollback y se relanza tal cual")
    void rollbackConExcepcionPropia() throws SQLException {
        IllegalStateException propia = new IllegalStateException("regla incumplida");

        assertThatThrownBy(() -> gestor.enTransaccion(con -> {
            partidas.insertar(con, VECINA, "Tampoco");
            throw propia;
        })).isSameAs(propia);

        assertThat(contar("SELECT COUNT(*) FROM partida WHERE nombre = 'Tampoco'")).isZero();
    }

    @Test
    @DisplayName("una SQLException del trabajo se traduce a AccesoDatosException")
    void traduceSqlException() {
        assertThatThrownBy(() -> gestor.enTransaccion(con -> {
            throw new SQLException("fallo", "HY000", 1234);
        })).isInstanceOf(AccesoDatosException.class);
    }

    @Test
    @DisplayName("la conexión vuelve al pool con el autocommit restaurado")
    void restauraAutocommit() {
        boolean autocommitDentro = gestor.enTransaccion(con -> con.getAutoCommit());
        assertThat(autocommitDentro).isFalse();

        boolean autocommitDespues = gestor.soloLectura(con -> con.getAutoCommit());
        assertThat(autocommitDespues).isTrue();
    }

    @Test
    @DisplayName("soloLectura ejecuta la consulta y traduce los errores")
    void soloLectura() {
        List<Partida> deRancher = gestor.soloLectura(con -> partidas.listarPorUsuario(con, RANCHER));
        assertThat(deRancher).hasSize(2);
        assertThatThrownBy(() -> gestor.soloLectura(con -> {
            throw new SQLException("roto", "08S01", 0);
        })).isInstanceOf(AccesoDatosException.class);
    }
}
