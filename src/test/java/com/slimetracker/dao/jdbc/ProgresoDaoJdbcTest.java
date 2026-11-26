package com.slimetracker.dao.jdbc;

import com.slimetracker.modelo.Actividad;
import com.slimetracker.modelo.Actividad.TipoActividad;
import com.slimetracker.modelo.ConteoProgreso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgresoDaoJdbc (integración con H2)")
class ProgresoDaoJdbcTest extends BaseDaoTest {

    private final ProgresoDaoJdbc dao = new ProgresoDaoJdbc();

    @Test
    @DisplayName("contar da los totales del catálogo y lo conseguido en la Primera partida")
    void contar() {
        assertThat(dao.contar(conexion, PRIMERA_PARTIDA)).isEqualTo(new ConteoProgreso(7, 5, 18, 11, 15, 6));
    }

    @Test
    @DisplayName("contar una partida que no existe devuelve todo a cero")
    void contarInexistente() {
        assertThat(dao.contar(conexion, INEXISTENTE)).isEqualTo(new ConteoProgreso(0, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("contarPorUsuario devuelve el conteo de cada partida del usuario en una consulta")
    void contarPorUsuario() {
        Map<Long, ConteoProgreso> conteos = dao.contarPorUsuario(conexion, RANCHER);

        assertThat(conteos).containsOnlyKeys(PRIMERA_PARTIDA, MODO_RUSH);
        assertThat(conteos.get(MODO_RUSH)).isEqualTo(new ConteoProgreso(7, 3, 18, 3, 15, 1));
    }

    @Test
    @DisplayName("ultimasActividades une zonas, slimes y logros, de la más reciente a la más antigua")
    void ultimasActividades() {
        List<Actividad> actividades = dao.ultimasActividades(conexion, PRIMERA_PARTIDA, 10);

        assertThat(actividades).hasSize(10);
        assertThat(actividades.get(0)).extracting(Actividad::tipo, Actividad::nombre)
                .containsExactly(TipoActividad.LOGRO, "Lo que dejaron los antiguos");
        assertThat(actividades.get(1)).extracting(Actividad::tipo, Actividad::nombre)
                .containsExactly(TipoActividad.ZONA, "Ancient Ruins");
        assertThat(actividades.get(2).nombre()).isEqualTo("Quantum");
        assertThat(actividades).extracting(Actividad::fecha).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    @DisplayName("ultimasActividades respeta el límite")
    void limite() {
        assertThat(dao.ultimasActividades(conexion, PRIMERA_PARTIDA, 3)).hasSize(3);
        assertThat(dao.ultimasActividades(conexion, MI_RANCHO, 10)).hasSize(1);
    }
}
