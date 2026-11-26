package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.modelo.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UsuarioDaoJdbc (integración con H2)")
class UsuarioDaoJdbcTest extends BaseDaoTest {

    private final UsuarioDaoJdbc dao = new UsuarioDaoJdbc();

    @Nested
    @DisplayName("insertar")
    class Insertar {

        @Test
        @DisplayName("guarda el usuario y devuelve el id generado")
        void guardaYDevuelveId() {
            long id = dao.insertar(conexion, "nuevo_rancher", "$2a$10$hash");

            assertThat(id).isEqualTo(3);
            Usuario guardado = dao.buscarPorId(conexion, id).orElseThrow();
            assertThat(guardado.nombreUsuario()).isEqualTo("nuevo_rancher");
            assertThat(guardado.hashContrasena()).isEqualTo("$2a$10$hash");
            assertThat(guardado.creadoEn()).isNotNull();
        }

        @Test
        @DisplayName("un nombre de usuario repetido lanza EntidadDuplicadaException")
        void nombreRepetido() {
            assertThatThrownBy(() -> dao.insertar(conexion, "rancher", "$2a$10$otro"))
                    .isInstanceOf(EntidadDuplicadaException.class);
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("por id devuelve el usuario del demo")
        void porId() {
            assertThat(dao.buscarPorId(conexion, RANCHER)).get()
                    .extracting(Usuario::nombreUsuario).isEqualTo("rancher");
        }

        @Test
        @DisplayName("por nombre devuelve el usuario con su hash")
        void porNombre() {
            Usuario vecina = dao.buscarPorNombre(conexion, "vecina").orElseThrow();
            assertThat(vecina.id()).isEqualTo(VECINA);
            assertThat(vecina.hashContrasena()).startsWith("$2a$10$");
        }

        @Test
        @DisplayName("un id que no existe devuelve Optional.empty()")
        void idInexistente() {
            assertThat(dao.buscarPorId(conexion, INEXISTENTE)).isEmpty();
        }

        @Test
        @DisplayName("un nombre que no existe devuelve Optional.empty()")
        void nombreInexistente() {
            assertThat(dao.buscarPorNombre(conexion, "nadie")).isEmpty();
        }
    }
}
