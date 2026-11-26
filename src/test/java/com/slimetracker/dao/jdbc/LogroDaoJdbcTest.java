package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.modelo.CategoriaLogro;
import com.slimetracker.modelo.Logro;
import com.slimetracker.modelo.LogroConEstado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LogroDaoJdbc (integración con H2)")
class LogroDaoJdbcTest extends BaseDaoTest {

    private static final long MAS_ALLA_DEL_RANCHO = 1;
    private static final long ARENA_DE_CRISTAL = 5;

    private final LogroDaoJdbc dao = new LogroDaoJdbc();

    @Test
    @DisplayName("listar devuelve los 15 logros del catálogo")
    void listar() {
        assertThat(dao.listar(conexion)).hasSize(15).first()
                .extracting(Logro::categoria).isEqualTo(CategoriaLogro.EXPLORACION);
    }

    @Test
    @DisplayName("buscarConEstado devuelve el logro o Optional.empty() si no existe")
    void buscarConEstado() {
        assertThat(dao.buscarConEstado(conexion, PRIMERA_PARTIDA, ARENA_DE_CRISTAL)).get()
                .extracting(estado -> estado.logro().nombre()).isEqualTo("Arena de cristal");
        assertThat(dao.buscarConEstado(conexion, PRIMERA_PARTIDA, INEXISTENTE)).isEmpty();
    }

    @Test
    @DisplayName("listarConEstado marca los 6 conseguidos de la Primera partida")
    void listarConEstado() {
        assertThat(dao.listarConEstado(conexion, PRIMERA_PARTIDA))
                .hasSize(15)
                .filteredOn(LogroConEstado::conseguido).hasSize(6);
    }

    @Test
    @DisplayName("marcar, anotar y desmarcar un logro")
    void marcarAnotarDesmarcar() {
        dao.marcar(conexion, MI_RANCHO, MAS_ALLA_DEL_RANCHO);
        assertThat(dao.buscarConEstado(conexion, MI_RANCHO, MAS_ALLA_DEL_RANCHO).orElseThrow().conseguido()).isTrue();

        assertThat(dao.actualizarNota(conexion, MI_RANCHO, MAS_ALLA_DEL_RANCHO, "Primera salida")).isEqualTo(1);
        assertThat(dao.buscarConEstado(conexion, MI_RANCHO, MAS_ALLA_DEL_RANCHO).orElseThrow().nota())
                .isEqualTo("Primera salida");

        assertThat(dao.desmarcar(conexion, MI_RANCHO, MAS_ALLA_DEL_RANCHO)).isEqualTo(1);
        assertThat(dao.buscarConEstado(conexion, MI_RANCHO, MAS_ALLA_DEL_RANCHO).orElseThrow().conseguido()).isFalse();
    }

    @Test
    @DisplayName("marcar dos veces el mismo logro lanza EntidadDuplicadaException")
    void marcarDosVeces() {
        assertThatThrownBy(() -> dao.marcar(conexion, PRIMERA_PARTIDA, MAS_ALLA_DEL_RANCHO))
                .isInstanceOf(EntidadDuplicadaException.class);
    }

    @Test
    @DisplayName("desmarcar o anotar un logro no conseguido afecta a 0 filas")
    void noConseguido() {
        assertThat(dao.desmarcar(conexion, PRIMERA_PARTIDA, ARENA_DE_CRISTAL)).isZero();
        assertThat(dao.actualizarNota(conexion, PRIMERA_PARTIDA, ARENA_DE_CRISTAL, "x")).isZero();
        assertThat(dao.buscarConEstado(conexion, PRIMERA_PARTIDA, INEXISTENTE)).isEmpty();
    }
}
