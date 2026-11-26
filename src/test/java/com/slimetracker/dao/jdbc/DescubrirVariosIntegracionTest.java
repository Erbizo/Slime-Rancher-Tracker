package com.slimetracker.dao.jdbc;

import com.slimetracker.dto.DescubrirVariosPeticion;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.servicio.SlimeServicio;
import com.slimetracker.tx.GestorTransacciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** R7 de punta a punta (servicio real + DAOs JDBC + H2): si un slime falla, no se guarda ninguno. */
@DisplayName("R7 descubrir varios con base de datos real")
class DescubrirVariosIntegracionTest extends BaseDaoTest {

    private static final long ROCK = 2;
    private static final long TABBY = 3;

    private final SlimeServicio servicio = new SlimeServicio(new GestorTransacciones(DATA_SOURCE),
            new SlimeDaoJdbc(), new ZonaDaoJdbc(), new PartidaDaoJdbc());

    @Test
    @DisplayName("Rock (zona abierta) + Rad (zona cerrada) → 409 y Rock tampoco se guarda")
    void todoONada() throws SQLException {
        new ZonaDaoJdbc().desbloquear(conexion, MI_RANCHO, DRY_REEF);

        assertThatThrownBy(() -> servicio.descubrirVarios(VECINA, MI_RANCHO, new DescubrirVariosPeticion(List.of(ROCK, RAD))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Rad");

        assertThat(contar("SELECT COUNT(*) FROM partida_slime WHERE partida_id = ?", MI_RANCHO)).isZero();
    }

    @Test
    @DisplayName("si todos cumplen R6 se guardan todos")
    void todos() throws SQLException {
        new ZonaDaoJdbc().desbloquear(conexion, MI_RANCHO, DRY_REEF);

        assertThat(servicio.descubrirVarios(VECINA, MI_RANCHO, new DescubrirVariosPeticion(List.of(ROCK, TABBY)))).hasSize(2);

        assertThat(contar("SELECT COUNT(*) FROM partida_slime WHERE partida_id = ?", MI_RANCHO)).isEqualTo(2);
    }
}
