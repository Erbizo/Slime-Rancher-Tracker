package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.UsuarioDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.LoginPeticion;
import com.slimetracker.dto.RegistroPeticion;
import com.slimetracker.dto.UsuarioRespuesta;
import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.excepcion.NoAutenticadoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.modelo.Usuario;
import com.slimetracker.seguridad.CifradorContrasenas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AutenticacionServicio")
class AutenticacionServicioTest extends BaseServicioTest {

    private static final String HASH = "$2a$10$hashDePrueba";
    private static final Usuario RANCHER = new Usuario(YO, "rancher", HASH, AHORA);

    @Mock
    UsuarioDao usuarioDao;
    @Mock
    PartidaDao partidaDao;
    @Mock
    ZonaDao zonaDao;
    @Mock
    CifradorContrasenas cifrador;

    private AutenticacionServicio servicio;

    @BeforeEach
    void crearServicio() {
        lenient().when(cifrador.cifrar(anyString())).thenReturn(HASH);
        servicio = new AutenticacionServicio(gestor, usuarioDao, partidaDao, zonaDao, cifrador);
    }

    private static RegistroPeticion registro(String usuario, String contrasena, String confirmacion) {
        return new RegistroPeticion(usuario, contrasena, confirmacion);
    }

    @Nested
    @DisplayName("R1: registro")
    class Registro {

        @Test
        @DisplayName("crea el usuario con la contraseña cifrada y una partida 'Mi rancho' con la zona inicial, en una transacción")
        void caminoFeliz() {
            when(usuarioDao.buscarPorNombre(conexion, "nuevo_1")).thenReturn(Optional.empty());
            when(usuarioDao.insertar(conexion, "nuevo_1", HASH)).thenReturn(7L);
            when(partidaDao.insertar(conexion, 7L, "Mi rancho")).thenReturn(30L);
            when(zonaDao.buscarInicial(conexion)).thenReturn(Optional.of(THE_RANCH));

            UsuarioRespuesta respuesta = servicio.registrar(registro("nuevo_1", "slime1234", "slime1234"));

            assertThat(respuesta).isEqualTo(new UsuarioRespuesta(7L, "nuevo_1"));
            verify(cifrador).cifrar("slime1234");
            verify(zonaDao).desbloquear(conexion, 30L, THE_RANCH.id());
            verify(gestor).enTransaccion(any());
        }

        @ParameterizedTest(name = "usuario \"{0}\" no es válido")
        @NullAndEmptySource
        @ValueSource(strings = {"ab", "un_nombre_de_usuario_muy_largo_", "con espacio", "ñandú", "guion-medio"})
        @DisplayName("nombre de usuario no válido → 400 en el campo nombreUsuario y no se escribe nada")
        void usuarioNoValido(String usuario) {
            assertThatThrownBy(() -> servicio.registrar(registro(usuario, "slime1234", "slime1234")))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsOnlyKeys("nombreUsuario");
            verifyNoInteractions(usuarioDao, partidaDao);
        }

        @ParameterizedTest(name = "contraseña \"{0}\" no es válida")
        @NullAndEmptySource
        @ValueSource(strings = {"corta12", "sinnumeros", "12345678", "        "})
        @DisplayName("contraseña sin 8 caracteres, letra y número → 400 en el campo contrasena")
        void contrasenaNoValida(String contrasena) {
            assertThatThrownBy(() -> servicio.registrar(registro("rancher_2", contrasena, contrasena)))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsKey("contrasena").doesNotContainKey("nombreUsuario");
            verifyNoInteractions(usuarioDao);
        }

        @Test
        @DisplayName("devuelve los tres errores a la vez, con los textos del diseño")
        void tresErroresALaVez() {
            assertThatThrownBy(() -> servicio.registrar(registro("ab", "slime", "slimes")))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP)
                    .containsEntry("nombreUsuario", "Entre 3 y 30 caracteres: letras, números o _.")
                    .containsEntry("contrasena", "Mínimo 8 caracteres, con al menos una letra y un número.")
                    .containsEntry("confirmacion", "Las contraseñas no coinciden.");
            verify(cifrador, never()).cifrar("slime");
        }

        @Test
        @DisplayName("contraseñas distintas → 400 solo en confirmacion")
        void confirmacionDistinta() {
            assertThatThrownBy(() -> servicio.registrar(registro("rancher_2", "slime1234", "slime12345")))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsOnlyKeys("confirmacion");
        }

        @Test
        @DisplayName("nombre ya usado → 409 y no se inserta")
        void duplicado() {
            when(usuarioDao.buscarPorNombre(conexion, "rancher")).thenReturn(Optional.of(RANCHER));

            assertThatThrownBy(() -> servicio.registrar(registro("rancher", "slime1234", "slime1234")))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage(AutenticacionServicio.USUARIO_EN_USO);
            verify(usuarioDao, never()).insertar(any(), anyString(), anyString());
            verifyNoInteractions(partidaDao);
        }

        @Test
        @DisplayName("si otro registro se cuela con el mismo nombre (clave única) → 409")
        void duplicadoPorCarrera() {
            when(usuarioDao.buscarPorNombre(conexion, "rancher_2")).thenReturn(Optional.empty());
            when(usuarioDao.insertar(conexion, "rancher_2", HASH)).thenThrow(new EntidadDuplicadaException("dup", null));

            assertThatThrownBy(() -> servicio.registrar(registro("rancher_2", "slime1234", "slime1234")))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage(AutenticacionServicio.USUARIO_EN_USO);
        }

        @Test
        @DisplayName("sin zona inicial en el catálogo falla (y la transacción se deshace)")
        void sinZonaInicial() {
            when(usuarioDao.buscarPorNombre(conexion, "rancher_2")).thenReturn(Optional.empty());
            when(zonaDao.buscarInicial(conexion)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrar(registro("rancher_2", "slime1234", "slime1234")))
                    .isInstanceOf(IllegalStateException.class);
            verify(zonaDao, never()).desbloquear(any(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("R2: login")
    class Login {

        @Test
        @DisplayName("con usuario y contraseña correctos devuelve el usuario (sin hash)")
        void correcto() {
            when(usuarioDao.buscarPorNombre(conexion, "rancher")).thenReturn(Optional.of(RANCHER));
            when(cifrador.coincide("slime1234", HASH)).thenReturn(true);

            assertThat(servicio.entrar(new LoginPeticion("rancher", "slime1234")))
                    .isEqualTo(new UsuarioRespuesta(YO, "rancher"));
        }

        @Test
        @DisplayName("contraseña incorrecta → 401 con el mensaje genérico")
        void contrasenaIncorrecta() {
            when(usuarioDao.buscarPorNombre(conexion, "rancher")).thenReturn(Optional.of(RANCHER));
            when(cifrador.coincide("mala12345", HASH)).thenReturn(false);

            assertThatThrownBy(() -> servicio.entrar(new LoginPeticion("rancher", "mala12345")))
                    .isInstanceOf(NoAutenticadoException.class)
                    .hasMessage("Usuario o contraseña incorrectos.");
        }

        @Test
        @DisplayName("usuario que no existe → 401 con el MISMO mensaje y comprobando igualmente un hash")
        void usuarioInexistente() {
            when(usuarioDao.buscarPorNombre(conexion, "nadie")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.entrar(new LoginPeticion("nadie", "slime1234")))
                    .isInstanceOf(NoAutenticadoException.class)
                    .hasMessage("Usuario o contraseña incorrectos.");
            verify(cifrador).coincide(eq("slime1234"), anyString());
        }

        @Test
        @DisplayName("campos vacíos → 400 en los dos campos, sin consultar la BD")
        void vacios() {
            assertThatThrownBy(() -> servicio.entrar(new LoginPeticion(" ", null)))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsOnlyKeys("nombreUsuario", "contrasena");
            verifyNoInteractions(usuarioDao);
        }
    }

    @Nested
    @DisplayName("usuario de la sesión")
    class Sesion {

        @Test
        @DisplayName("obtener devuelve el usuario")
        void obtener() {
            when(usuarioDao.buscarPorId(conexion, YO)).thenReturn(Optional.of(RANCHER));
            assertThat(servicio.obtener(YO).nombreUsuario()).isEqualTo("rancher");
        }

        @Test
        @DisplayName("si el usuario ya no existe → 401")
        void borrado() {
            when(usuarioDao.buscarPorId(conexion, YO)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.obtener(YO)).isInstanceOf(NoAutenticadoException.class);
        }
    }
}
