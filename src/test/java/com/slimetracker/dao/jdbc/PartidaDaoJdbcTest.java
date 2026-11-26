package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.excepcion.IntegridadReferencialException;
import com.slimetracker.modelo.Partida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PartidaDaoJdbc (integración con H2)")
class PartidaDaoJdbcTest extends BaseDaoTest {

    private final PartidaDaoJdbc dao = new PartidaDaoJdbc();

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("inserta la partida y devuelve su id")
        void inserta() {
            long id = dao.insertar(conexion, VECINA, "Segunda");

            Partida partida = dao.buscarPorId(conexion, id).orElseThrow();
            assertThat(partida.usuarioId()).isEqualTo(VECINA);
            assertThat(partida.nombre()).isEqualTo("Segunda");
            assertThat(partida.creadaEn()).isNotNull();
        }

        @Test
        @DisplayName("un nombre repetido para el mismo usuario lanza EntidadDuplicadaException")
        void nombreRepetidoMismoUsuario() {
            assertThatThrownBy(() -> dao.insertar(conexion, RANCHER, "Modo Rush"))
                    .isInstanceOf(EntidadDuplicadaException.class);
        }

        @Test
        @DisplayName("el mismo nombre en otro usuario está permitido")
        void mismoNombreOtroUsuario() {
            long id = dao.insertar(conexion, VECINA, "Modo Rush");

            assertThat(dao.buscarPorId(conexion, id)).isPresent();
        }

        @Test
        @DisplayName("un usuario que no existe lanza IntegridadReferencialException")
        void usuarioInexistente() {
            assertThatThrownBy(() -> dao.insertar(conexion, INEXISTENTE, "Huérfana"))
                    .isInstanceOf(IntegridadReferencialException.class);
        }
    }

    @Nested
    @DisplayName("leer")
    class Leer {

        @Test
        @DisplayName("buscarPorId de una partida que no existe devuelve Optional.empty()")
        void inexistente() {
            assertThat(dao.buscarPorId(conexion, INEXISTENTE)).isEmpty();
        }

        @Test
        @DisplayName("listarPorUsuario devuelve solo las suyas, la última modificada primero")
        void listarPorUsuario() {
            assertThat(dao.listarPorUsuario(conexion, RANCHER))
                    .extracting(Partida::nombre)
                    .containsExactly("Primera partida", "Modo Rush");
        }

        @Test
        @DisplayName("listarPorUsuario de un usuario sin partidas devuelve una lista vacía")
        void listarSinPartidas() {
            assertThat(dao.listarPorUsuario(conexion, INEXISTENTE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("renombrar cambia el nombre")
        void renombrar() {
            assertThat(dao.renombrar(conexion, MODO_RUSH, "Rush 2")).isEqualTo(1);
            assertThat(dao.buscarPorId(conexion, MODO_RUSH).orElseThrow().nombre()).isEqualTo("Rush 2");
        }

        @Test
        @DisplayName("renombrar una partida que no existe afecta a 0 filas")
        void renombrarInexistente() {
            assertThat(dao.renombrar(conexion, INEXISTENTE, "Nada")).isZero();
        }

        @Test
        @DisplayName("renombrar a un nombre que ya tiene el usuario lanza EntidadDuplicadaException")
        void renombrarRepetido() {
            assertThatThrownBy(() -> dao.renombrar(conexion, MODO_RUSH, "Primera partida"))
                    .isInstanceOf(EntidadDuplicadaException.class);
        }

        @Test
        @DisplayName("tocar pone actualizada_en a la hora actual")
        void tocar() {
            Partida antes = dao.buscarPorId(conexion, MODO_RUSH).orElseThrow();

            assertThat(dao.tocar(conexion, MODO_RUSH)).isEqualTo(1);

            Partida despues = dao.buscarPorId(conexion, MODO_RUSH).orElseThrow();
            assertThat(despues.actualizadaEn()).isAfter(antes.actualizadaEn());
            assertThat(dao.listarPorUsuario(conexion, RANCHER).get(0).id()).isEqualTo(MODO_RUSH);
        }

        @Test
        @DisplayName("tocar una partida que no existe afecta a 0 filas")
        void tocarInexistente() {
            assertThat(dao.tocar(conexion, INEXISTENTE)).isZero();
        }
    }

    @Nested
    @DisplayName("borrar")
    class Borrar {

        @Test
        @DisplayName("borra la partida y, en cascada, sus zonas, slimes y logros")
        void cascada() throws SQLException {
            assertThat(contar("SELECT COUNT(*) FROM partida_slime WHERE partida_id = ?", PRIMERA_PARTIDA)).isEqualTo(11);

            assertThat(dao.borrar(conexion, PRIMERA_PARTIDA)).isEqualTo(1);

            assertThat(dao.buscarPorId(conexion, PRIMERA_PARTIDA)).isEmpty();
            assertThat(contar("SELECT COUNT(*) FROM partida_zona WHERE partida_id = ?", PRIMERA_PARTIDA)).isZero();
            assertThat(contar("SELECT COUNT(*) FROM partida_slime WHERE partida_id = ?", PRIMERA_PARTIDA)).isZero();
            assertThat(contar("SELECT COUNT(*) FROM partida_logro WHERE partida_id = ?", PRIMERA_PARTIDA)).isZero();
            assertThat(contar("SELECT COUNT(*) FROM partida_slime WHERE partida_id = ?", MODO_RUSH)).isEqualTo(3);
        }

        @Test
        @DisplayName("borrar una partida que no existe afecta a 0 filas")
        void inexistente() {
            assertThat(dao.borrar(conexion, INEXISTENTE)).isZero();
        }
    }
}
