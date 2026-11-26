package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.excepcion.IntegridadReferencialException;
import com.slimetracker.modelo.Zona;
import com.slimetracker.modelo.ZonaConEstado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ZonaDaoJdbc (integración con H2)")
class ZonaDaoJdbcTest extends BaseDaoTest {

    private final ZonaDaoJdbc dao = new ZonaDaoJdbc();

    @Nested
    @DisplayName("catálogo")
    class Catalogo {

        @Test
        @DisplayName("listar devuelve las 7 zonas en orden")
        void listar() {
            assertThat(dao.listar(conexion)).extracting(Zona::nombre).containsExactly(
                    "The Ranch", "Dry Reef", "Indigo Quarry", "Moss Blanket", "Ancient Ruins", "Glass Desert", "The Wilds");
        }

        @Test
        @DisplayName("buscarPorId devuelve la zona con su requisito")
        void buscarPorId() {
            Zona indigo = dao.buscarPorId(conexion, INDIGO_QUARRY).orElseThrow();
            assertThat(indigo.zonaRequisitoId()).isEqualTo(DRY_REEF);
            assertThat(indigo.esInicial()).isFalse();
        }

        @Test
        @DisplayName("buscarPorId de una zona que no existe devuelve Optional.empty()")
        void inexistente() {
            assertThat(dao.buscarPorId(conexion, INEXISTENTE)).isEmpty();
        }

        @Test
        @DisplayName("buscarInicial devuelve The Ranch, sin requisito")
        void inicial() {
            Zona inicial = dao.buscarInicial(conexion).orElseThrow();
            assertThat(inicial.nombre()).isEqualTo("The Ranch");
            assertThat(inicial.zonaRequisitoId()).isNull();
        }
    }

    @Nested
    @DisplayName("estado en una partida")
    class Estado {

        @Test
        @DisplayName("listarConEstado marca las desbloqueadas y cuenta los slimes de cada zona")
        void listarConEstado() {
            List<ZonaConEstado> zonas = dao.listarConEstado(conexion, PRIMERA_PARTIDA);

            assertThat(zonas).hasSize(7);
            assertThat(zonas).filteredOn(ZonaConEstado::desbloqueada).hasSize(5);
            ZonaConEstado dryReef = zonas.get(1);
            assertThat(dryReef.slimesTotal()).isEqualTo(5);
            assertThat(dryReef.slimesDescubiertos()).isEqualTo(4);
            assertThat(zonas.get(5).desbloqueada()).isFalse();
        }

        @Test
        @DisplayName("estaDesbloqueada distingue zonas abiertas y cerradas")
        void estaDesbloqueada() {
            assertThat(dao.estaDesbloqueada(conexion, PRIMERA_PARTIDA, DRY_REEF)).isTrue();
            assertThat(dao.estaDesbloqueada(conexion, PRIMERA_PARTIDA, GLASS_DESERT)).isFalse();
        }

        @Test
        @DisplayName("listarDependientesDesbloqueadas devuelve las zonas abiertas que requieren a la dada")
        void dependientes() {
            assertThat(dao.listarDependientesDesbloqueadas(conexion, PRIMERA_PARTIDA, DRY_REEF))
                    .extracting(Zona::nombre).containsExactly("Indigo Quarry", "Moss Blanket");
            assertThat(dao.listarDependientesDesbloqueadas(conexion, MODO_RUSH, DRY_REEF))
                    .extracting(Zona::nombre).containsExactly("Indigo Quarry");
            assertThat(dao.listarDependientesDesbloqueadas(conexion, PRIMERA_PARTIDA, ANCIENT_RUINS)).isEmpty();
        }
    }

    @Nested
    @DisplayName("desbloquear y bloquear")
    class Cambios {

        @Test
        @DisplayName("desbloquear y bloquear cambian el estado")
        void desbloquearYBloquear() {
            dao.desbloquear(conexion, MI_RANCHO, DRY_REEF);
            assertThat(dao.estaDesbloqueada(conexion, MI_RANCHO, DRY_REEF)).isTrue();

            assertThat(dao.bloquear(conexion, MI_RANCHO, DRY_REEF)).isEqualTo(1);
            assertThat(dao.estaDesbloqueada(conexion, MI_RANCHO, DRY_REEF)).isFalse();
        }

        @Test
        @DisplayName("desbloquear dos veces la misma zona lanza EntidadDuplicadaException")
        void desbloquearDosVeces() {
            assertThatThrownBy(() -> dao.desbloquear(conexion, PRIMERA_PARTIDA, DRY_REEF))
                    .isInstanceOf(EntidadDuplicadaException.class);
        }

        @Test
        @DisplayName("desbloquear una zona que no existe lanza IntegridadReferencialException")
        void zonaInexistente() {
            assertThatThrownBy(() -> dao.desbloquear(conexion, MI_RANCHO, INEXISTENTE))
                    .isInstanceOf(IntegridadReferencialException.class);
        }

        @Test
        @DisplayName("bloquear una zona que no estaba desbloqueada afecta a 0 filas")
        void bloquearNoDesbloqueada() {
            assertThat(dao.bloquear(conexion, MI_RANCHO, GLASS_DESERT)).isZero();
        }
    }
}
