package com.slimetracker.seguridad;

import com.slimetracker.excepcion.NoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Seguridad: filtro de autenticación, sesión y BCrypt")
class SeguridadTest {

    @Mock
    HttpServletRequest peticion;
    @Mock
    HttpSession sesion;
    @Mock
    ContainerRequestContext contexto;
    @Mock
    UriInfo uriInfo;

    @Nested
    @DisplayName("FiltroAutenticacion")
    class Filtro {

        private FiltroAutenticacion filtro;

        @BeforeEach
        void crear() {
            filtro = new FiltroAutenticacion(peticion);
            when(contexto.getUriInfo()).thenReturn(uriInfo);
        }

        @ParameterizedTest(name = "/api/{0} es pública")
        @ValueSource(strings = {"auth/login", "auth/registro", "salud", "/salud"})
        @DisplayName("registro, login y salud no piden sesión")
        void rutasPublicas(String ruta) {
            when(uriInfo.getPath()).thenReturn(ruta);

            assertThatCode(() -> filtro.filter(contexto)).doesNotThrowAnyException();
            verify(peticion, never()).getSession(false);
        }

        @ParameterizedTest(name = "/api/{0} sin sesión → 401")
        @ValueSource(strings = {"partidas", "partidas/1/progreso", "zonas", "auth/yo", "auth/logout"})
        @DisplayName("el resto de la API sin sesión → NoAutenticadoException (401)")
        void sinSesion(String ruta) {
            when(uriInfo.getPath()).thenReturn(ruta);
            when(peticion.getSession(false)).thenReturn(null);

            assertThatThrownBy(() -> filtro.filter(contexto)).isInstanceOf(NoAutenticadoException.class);
        }

        @Test
        @DisplayName("una sesión sin usuario tampoco vale → 401")
        void sesionSinUsuario() {
            when(uriInfo.getPath()).thenReturn("partidas");
            when(peticion.getSession(false)).thenReturn(sesion);
            when(sesion.getAttribute(GestorSesion.ATRIBUTO)).thenReturn(null);

            assertThatThrownBy(() -> filtro.filter(contexto)).isInstanceOf(NoAutenticadoException.class);
        }

        @Test
        @DisplayName("con usuario en sesión deja pasar")
        void conSesion() {
            when(uriInfo.getPath()).thenReturn("partidas");
            when(peticion.getSession(false)).thenReturn(sesion);
            when(sesion.getAttribute(GestorSesion.ATRIBUTO)).thenReturn(new UsuarioSesion(1, "rancher"));

            assertThatCode(() -> filtro.filter(contexto)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("GestorSesion")
    class Sesion {

        @Test
        @DisplayName("al entrar con una sesión previa cambia su id (fijación de sesión) y guarda solo id y nombre")
        void cambiaId() {
            when(peticion.getSession(false)).thenReturn(sesion);
            when(peticion.getSession(true)).thenReturn(sesion);

            GestorSesion.iniciar(peticion, 1, "rancher");

            var orden = inOrder(peticion, sesion);
            orden.verify(peticion).changeSessionId();
            orden.verify(sesion).setAttribute(GestorSesion.ATRIBUTO, new UsuarioSesion(1, "rancher"));
        }

        @Test
        @DisplayName("sin sesión previa crea una nueva")
        void sesionNueva() {
            when(peticion.getSession(false)).thenReturn(null);
            when(peticion.getSession(true)).thenReturn(sesion);

            GestorSesion.iniciar(peticion, 2, "vecina");

            verify(peticion, never()).changeSessionId();
            verify(sesion).setAttribute(any(), any());
        }

        @Test
        @DisplayName("salir invalida la sesión; sin sesión no hace nada")
        void cerrar() {
            when(peticion.getSession(false)).thenReturn(sesion, (HttpSession) null);

            GestorSesion.cerrar(peticion);
            GestorSesion.cerrar(peticion);

            verify(sesion).invalidate();
        }

        @Test
        @DisplayName("usuarioId devuelve el id de la sesión o 401")
        void usuarioId() {
            when(peticion.getSession(false)).thenReturn(sesion, (HttpSession) null);
            when(sesion.getAttribute(GestorSesion.ATRIBUTO)).thenReturn(new UsuarioSesion(7, "x"));

            assertThat(GestorSesion.usuarioId(peticion)).isEqualTo(7);
            assertThatThrownBy(() -> GestorSesion.usuarioId(peticion)).isInstanceOf(NoAutenticadoException.class);
        }
    }

    @Nested
    @DisplayName("CifradorContrasenas")
    class Cifrador {

        private final CifradorContrasenas cifrador = new CifradorContrasenas();

        @Test
        @DisplayName("el hash es BCrypt ($2a$, 60 caracteres) y solo coincide con su contraseña")
        void cifraYComprueba() {
            String hash = cifrador.cifrar("slime1234");

            assertThat(hash).startsWith("$2a$10$").hasSize(60).doesNotContain("slime1234");
            assertThat(cifrador.coincide("slime1234", hash)).isTrue();
            assertThat(cifrador.coincide("slime12345", hash)).isFalse();
        }

        @Test
        @DisplayName("el hash del usuario demo de init.sql es real (rancher / slime1234)")
        void hashDelDemo() {
            assertThat(cifrador.coincide("slime1234", "$2a$10$MykW0NEv4E/iaFyco5Xnfe7.f.0kplCU96mzs7Em6CUUm3DEsVUkK")).isTrue();
            assertThat(cifrador.coincide("slime5678", "$2a$10$qM.9K1mHxtSX6y0ql8CYm.eNYkW8P9TnTeTQljQyoru0dwM7ZcZMm")).isTrue();
        }

        @Test
        @DisplayName("un hash con formato roto cuenta como contraseña incorrecta")
        void hashRoto() {
            assertThat(cifrador.coincide("slime1234", "no-es-un-hash")).isFalse();
        }
    }
}
