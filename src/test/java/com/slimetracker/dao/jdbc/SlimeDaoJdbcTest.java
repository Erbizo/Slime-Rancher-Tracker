package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.modelo.Dieta;
import com.slimetracker.modelo.Rareza;
import com.slimetracker.modelo.Slime;
import com.slimetracker.modelo.SlimeConEstado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SlimeDaoJdbc (integración con H2)")
class SlimeDaoJdbcTest extends BaseDaoTest {

    private final SlimeDaoJdbc dao = new SlimeDaoJdbc();

    @Nested
    @DisplayName("catálogo con filtros")
    class Catalogo {

        @Test
        @DisplayName("sin filtros devuelve los 18 slimes en el orden de las zonas")
        void sinFiltros() {
            List<Slime> slimes = dao.listar(conexion, null, null, null);
            assertThat(slimes).hasSize(18);
            assertThat(slimes.get(0).nombre()).isEqualTo("Pink");
            assertThat(slimes.get(17).nombre()).isEqualTo("Saber");
        }

        @Test
        @DisplayName("filtra por zona")
        void porZona() {
            assertThat(dao.listar(conexion, DRY_REEF, null, null)).extracting(Slime::nombre)
                    .containsExactly("Pink", "Rock", "Tabby", "Phosphor", "Lucky");
        }

        @Test
        @DisplayName("filtra por dieta y rareza a la vez")
        void porDietaYRareza() {
            assertThat(dao.listar(conexion, null, Dieta.CARNE, Rareza.RARO)).extracting(Slime::nombre)
                    .containsExactly("Hunter", "Tangle");
        }

        @Test
        @DisplayName("combina los tres filtros")
        void tresFiltros() {
            assertThat(dao.listar(conexion, DRY_REEF, Dieta.CARNE, Rareza.COMUN)).extracting(Slime::nombre)
                    .containsExactly("Tabby");
        }

        @Test
        @DisplayName("buscarPorId devuelve el slime con su color y su zona")
        void buscarPorId() {
            Slime quantum = dao.buscarPorId(conexion, QUANTUM).orElseThrow();
            assertThat(quantum.color()).isEqualTo("#A070FF");
            assertThat(quantum.zonaId()).isEqualTo(ANCIENT_RUINS);
            assertThat(quantum.dieta()).isEqualTo(Dieta.FRUTA);
        }

        @Test
        @DisplayName("buscarPorId de un slime que no existe devuelve Optional.empty()")
        void inexistente() {
            assertThat(dao.buscarPorId(conexion, INEXISTENTE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Slimepedia de una partida")
    class Slimepedia {

        @Test
        @DisplayName("listarConEstado devuelve los 18 con 11 descubiertos (una consulta con LEFT JOIN)")
        void listarConEstado() {
            List<SlimeConEstado> slimes = dao.listarConEstado(conexion, PRIMERA_PARTIDA);
            assertThat(slimes).hasSize(18);
            assertThat(slimes).filteredOn(SlimeConEstado::descubierto).hasSize(11);
            assertThat(slimes).filteredOn(s -> s.slime().id() == QUANTUM).first()
                    .extracting(SlimeConEstado::nota).isEqualTo("Aparece al lado de los teletransportes de las ruinas.");
        }

        @Test
        @DisplayName("buscarConEstado de un slime sin descubrir lo devuelve con descubiertoEn null")
        void buscarConEstadoSinDescubrir() {
            SlimeConEstado lucky = dao.buscarConEstado(conexion, PRIMERA_PARTIDA, LUCKY).orElseThrow();
            assertThat(lucky.descubierto()).isFalse();
            assertThat(dao.buscarConEstado(conexion, PRIMERA_PARTIDA, INEXISTENTE)).isEmpty();
        }

        @Test
        @DisplayName("descubrir y olvidar cambian el estado")
        void descubrirYOlvidar() {
            dao.descubrir(conexion, PRIMERA_PARTIDA, LUCKY);
            assertThat(dao.estaDescubierto(conexion, PRIMERA_PARTIDA, LUCKY)).isTrue();

            assertThat(dao.olvidar(conexion, PRIMERA_PARTIDA, LUCKY)).isEqualTo(1);
            assertThat(dao.estaDescubierto(conexion, PRIMERA_PARTIDA, LUCKY)).isFalse();
        }

        @Test
        @DisplayName("descubrir dos veces el mismo slime lanza EntidadDuplicadaException")
        void descubrirDosVeces() {
            assertThatThrownBy(() -> dao.descubrir(conexion, PRIMERA_PARTIDA, PINK))
                    .isInstanceOf(EntidadDuplicadaException.class);
        }

        @Test
        @DisplayName("olvidar un slime no descubierto afecta a 0 filas")
        void olvidarNoDescubierto() {
            assertThat(dao.olvidar(conexion, PRIMERA_PARTIDA, LUCKY)).isZero();
        }

        @Test
        @DisplayName("actualizarNota cambia la nota de un slime descubierto")
        void nota() {
            assertThat(dao.actualizarNota(conexion, PRIMERA_PARTIDA, PINK, "Le gustan las zanahorias")).isEqualTo(1);
            assertThat(dao.buscarConEstado(conexion, PRIMERA_PARTIDA, PINK).orElseThrow().nota())
                    .isEqualTo("Le gustan las zanahorias");
        }

        @Test
        @DisplayName("actualizarNota de un slime no descubierto afecta a 0 filas")
        void notaNoDescubierto() {
            assertThat(dao.actualizarNota(conexion, PRIMERA_PARTIDA, LUCKY, "Nada")).isZero();
        }

        @Test
        @DisplayName("listarDescubiertosEnZona da los nombres descubiertos de esa zona")
        void descubiertosEnZona() {
            assertThat(dao.listarDescubiertosEnZona(conexion, PRIMERA_PARTIDA, DRY_REEF))
                    .containsExactly("Pink", "Rock", "Tabby", "Phosphor");
            assertThat(dao.listarDescubiertosEnZona(conexion, MI_RANCHO, DRY_REEF)).isEmpty();
        }
    }
}
