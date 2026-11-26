package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.DescubrirVariosPeticion;
import com.slimetracker.dto.NotaPeticion;
import com.slimetracker.dto.SlimeEstadoRespuesta;
import com.slimetracker.excepcion.AccesoDenegadoException;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.modelo.Dieta;
import com.slimetracker.modelo.Rareza;
import com.slimetracker.modelo.SlimeConEstado;
import com.slimetracker.modelo.ZonaConEstado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("SlimeServicio (R6, R7 y notas)")
class SlimeServicioTest extends BaseServicioTest {

    @Mock
    SlimeDao slimeDao;
    @Mock
    ZonaDao zonaDao;
    @Mock
    PartidaDao partidaDao;

    private SlimeServicio servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new SlimeServicio(gestor, slimeDao, zonaDao, partidaDao);
    }

    private void partidaPropia() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
    }

    /** Dry Reef abierta e Indigo Quarry cerrada. */
    private void zonas() {
        when(zonaDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of(
                new ZonaConEstado(THE_RANCH, AHORA, 0, 0), new ZonaConEstado(DRY_REEF, AHORA, 2, 1),
                new ZonaConEstado(INDIGO_QUARRY, null, 1, 0)));
    }

    private void slimeDescubrible(long id) {
        when(slimeDao.buscarPorId(conexion, id)).thenReturn(Optional.of(id == 1 ? PINK : ROCK));
        when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);
        when(slimeDao.estaDescubierto(conexion, PARTIDA, id)).thenReturn(false);
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("el catálogo pasa los filtros al DAO y añade el nombre de la zona")
        void catalogo() {
            when(zonaDao.listar(conexion)).thenReturn(List.of(THE_RANCH, DRY_REEF));
            when(slimeDao.listar(conexion, 2L, Dieta.OMNIVORO, null)).thenReturn(List.of(PINK));

            assertThat(servicio.listarCatalogo(2L, "omnivoro", " ")).singleElement()
                    .extracting("zonaNombre").isEqualTo("Dry Reef");
        }

        @Test
        @DisplayName("dieta y rareza desconocidas → 400 con los dos campos")
        void filtrosNoValidos() {
            assertThatThrownBy(() -> servicio.listarCatalogo(null, "PESCADO", "LEGENDARIO"))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsOnlyKeys("dieta", "rareza");
            verifyNoInteractions(gestor);
        }

        @Test
        @DisplayName("la Slimepedia de la partida se puede filtrar por zona y por estado")
        void slimepediaFiltrada() {
            partidaPropia();
            zonas();
            when(slimeDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of(
                    new SlimeConEstado(PINK, AHORA, null), new SlimeConEstado(ROCK, null, null),
                    new SlimeConEstado(RAD, null, null)));

            assertThat(servicio.listarDePartida(YO, PARTIDA, 2L, false))
                    .extracting(SlimeEstadoRespuesta::nombre).containsExactly("Rock");
            assertThat(servicio.listarDePartida(YO, PARTIDA, null, null)).hasSize(3)
                    .last().extracting(SlimeEstadoRespuesta::zonaDesbloqueada).isEqualTo(false);
        }

        @Test
        @DisplayName("filtrar por una zona que no existe → 404")
        void zonaInexistente() {
            partidaPropia();
            zonas();
            assertThatThrownBy(() -> servicio.listarDePartida(YO, PARTIDA, 99L, null))
                    .isInstanceOf(NoEncontradoException.class);
        }

        @Test
        @DisplayName("la Slimepedia de otro usuario → 403")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));
            assertThatThrownBy(() -> servicio.listarDePartida(YO, PARTIDA, null, null))
                    .isInstanceOf(AccesoDenegadoException.class);
        }
    }

    @Nested
    @DisplayName("R6: descubrir")
    class Descubrir {

        @Test
        @DisplayName("descubre el slime y actualiza la partida (R9)")
        void caminoFeliz() {
            partidaPropia();
            slimeDescubrible(1L);
            zonas();
            when(slimeDao.buscarConEstado(conexion, PARTIDA, 1L)).thenReturn(Optional.of(new SlimeConEstado(PINK, AHORA, null)));

            SlimeEstadoRespuesta pink = servicio.descubrir(YO, PARTIDA, 1L);

            assertThat(pink.descubierto()).isTrue();
            assertThat(pink.color()).isEqualTo("#FF7EB6");
            verify(slimeDao).descubrir(conexion, PARTIDA, 1L);
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("slime que no existe → 404")
        void noExiste() {
            partidaPropia();
            when(slimeDao.buscarPorId(conexion, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.descubrir(YO, PARTIDA, 99L))
                    .isInstanceOf(NoEncontradoException.class).hasMessage("El slime 99 no existe.");
        }

        @Test
        @DisplayName("su zona está bloqueada → 409 diciendo qué zona falta")
        void zonaBloqueada() {
            partidaPropia();
            when(slimeDao.buscarPorId(conexion, 6L)).thenReturn(Optional.of(RAD));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 3L)).thenReturn(false);
            when(zonaDao.buscarPorId(conexion, 3L)).thenReturn(Optional.of(INDIGO_QUARRY));

            assertThatThrownBy(() -> servicio.descubrir(YO, PARTIDA, 6L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Para descubrir al Rad primero tienes que desbloquear Indigo Quarry.");
            verify(slimeDao, never()).descubrir(any(), anyLong(), anyLong());
            verify(partidaDao, never()).tocar(any(), anyLong());
        }

        @Test
        @DisplayName("ya estaba descubierto → 409")
        void yaDescubierto() {
            partidaPropia();
            when(slimeDao.buscarPorId(conexion, 1L)).thenReturn(Optional.of(PINK));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);
            when(slimeDao.estaDescubierto(conexion, PARTIDA, 1L)).thenReturn(true);

            assertThatThrownBy(() -> servicio.descubrir(YO, PARTIDA, 1L))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage("Ya habías descubierto al Pink.");
        }

        @Test
        @DisplayName("partida de otro usuario → 403 y no se descubre nada")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.descubrir(YO, PARTIDA, 1L)).isInstanceOf(AccesoDenegadoException.class);
            verifyNoInteractions(slimeDao);
        }
    }

    @Nested
    @DisplayName("R7: descubrir varios (todo o nada)")
    class DescubrirVarios {

        @Test
        @DisplayName("descubre todos en UNA sola transacción y toca la partida una vez")
        void todos() {
            partidaPropia();
            slimeDescubrible(1L);
            slimeDescubrible(2L);
            zonas();
            when(slimeDao.buscarConEstado(conexion, PARTIDA, 1L)).thenReturn(Optional.of(new SlimeConEstado(PINK, AHORA, null)));
            when(slimeDao.buscarConEstado(conexion, PARTIDA, 2L)).thenReturn(Optional.of(new SlimeConEstado(ROCK, AHORA, null)));

            assertThat(servicio.descubrirVarios(YO, PARTIDA, new DescubrirVariosPeticion(List.of(1L, 2L)))).hasSize(2);

            verify(gestor).enTransaccion(any());
            verify(slimeDao).descubrir(conexion, PARTIDA, 1L);
            verify(slimeDao).descubrir(conexion, PARTIDA, 2L);
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("si uno falla, la excepción sale de la transacción (rollback) diciendo cuál y por qué")
        void unoFalla() {
            partidaPropia();
            slimeDescubrible(2L);
            when(slimeDao.buscarPorId(conexion, 6L)).thenReturn(Optional.of(RAD));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 3L)).thenReturn(false);
            when(zonaDao.buscarPorId(conexion, 3L)).thenReturn(Optional.of(INDIGO_QUARRY));

            assertThatThrownBy(() -> servicio.descubrirVarios(YO, PARTIDA, new DescubrirVariosPeticion(List.of(2L, 6L))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Rad").hasMessageContaining("Indigo Quarry");
            verify(gestor).enTransaccion(any());
            verify(partidaDao, never()).tocar(any(), anyLong());
        }

        static Stream<List<Long>> listasNoValidas() {
            return Stream.of(null, List.of(), Arrays.asList(1L, null), List.of(1L, 1L));
        }

        @ParameterizedTest(name = "slimeIds = {0}")
        @MethodSource("listasNoValidas")
        @DisplayName("lista vacía, con null o con repetidos → 400 sin abrir transacción")
        void listaNoValida(List<Long> ids) {
            assertThatThrownBy(() -> servicio.descubrirVarios(YO, PARTIDA, new DescubrirVariosPeticion(ids)))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsOnlyKeys("slimeIds");
            verifyNoInteractions(gestor, slimeDao);
        }
    }

    @Nested
    @DisplayName("notas y olvidar")
    class Notas {

        @Test
        @DisplayName("guarda la nota recortada y actualiza la partida")
        void nota() {
            partidaPropia();
            zonas();
            when(slimeDao.buscarPorId(conexion, 1L)).thenReturn(Optional.of(PINK));
            when(slimeDao.actualizarNota(conexion, PARTIDA, 1L, "Le gustan")).thenReturn(1);
            when(slimeDao.buscarConEstado(conexion, PARTIDA, 1L)).thenReturn(Optional.of(new SlimeConEstado(PINK, AHORA, "Le gustan")));

            assertThat(servicio.actualizarNota(YO, PARTIDA, 1L, new NotaPeticion("  Le gustan  ")).nota()).isEqualTo("Le gustan");
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("una nota vacía se guarda como null (borra la nota)")
        void notaVacia() {
            partidaPropia();
            zonas();
            when(slimeDao.buscarPorId(conexion, 1L)).thenReturn(Optional.of(PINK));
            when(slimeDao.actualizarNota(conexion, PARTIDA, 1L, null)).thenReturn(1);
            when(slimeDao.buscarConEstado(conexion, PARTIDA, 1L)).thenReturn(Optional.of(new SlimeConEstado(PINK, AHORA, null)));

            servicio.actualizarNota(YO, PARTIDA, 1L, new NotaPeticion("   "));

            verify(slimeDao).actualizarNota(conexion, PARTIDA, 1L, null);
        }

        @Test
        @DisplayName("nota de más de 200 caracteres → 400 sin escribir")
        void notaLarga() {
            assertThatThrownBy(() -> servicio.actualizarNota(YO, PARTIDA, 1L, new NotaPeticion("a".repeat(201))))
                    .isInstanceOf(ValidacionException.class)
                    .extracting("campos", MAP).containsOnlyKeys("nota");
            verifyNoInteractions(gestor, slimeDao);
        }

        @Test
        @DisplayName("nota de un slime sin descubrir → 409")
        void notaSinDescubrir() {
            partidaPropia();
            when(slimeDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(ROCK));
            when(slimeDao.actualizarNota(conexion, PARTIDA, 2L, "x")).thenReturn(0);

            assertThatThrownBy(() -> servicio.actualizarNota(YO, PARTIDA, 2L, new NotaPeticion("x")))
                    .isInstanceOf(ReglaNegocioException.class);
            verify(partidaDao, never()).tocar(any(), anyLong());
        }

        @Test
        @DisplayName("olvidar quita el slime y actualiza la partida")
        void olvidar() {
            partidaPropia();
            when(slimeDao.buscarPorId(conexion, 1L)).thenReturn(Optional.of(PINK));
            when(slimeDao.olvidar(conexion, PARTIDA, 1L)).thenReturn(1);

            servicio.olvidar(YO, PARTIDA, 1L);

            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("olvidar un slime que no estaba descubierto → 409; uno que no existe → 404")
        void olvidarErrores() {
            partidaPropia();
            when(slimeDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(ROCK));
            when(slimeDao.olvidar(conexion, PARTIDA, 2L)).thenReturn(0);
            when(slimeDao.buscarPorId(conexion, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.olvidar(YO, PARTIDA, 2L)).isInstanceOf(ReglaNegocioException.class);
            assertThatThrownBy(() -> servicio.olvidar(YO, PARTIDA, 99L)).isInstanceOf(NoEncontradoException.class);
            verify(partidaDao, never()).tocar(any(), anyLong());
        }

        @Test
        @DisplayName("notas y olvidar en la partida de otro → 403")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.actualizarNota(YO, PARTIDA, 1L, new NotaPeticion("x")))
                    .isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> servicio.olvidar(YO, PARTIDA, 1L)).isInstanceOf(AccesoDenegadoException.class);
            verify(slimeDao, never()).actualizarNota(any(), anyLong(), anyLong(), isNull());
        }
    }

    @Test
    @DisplayName("los valores de dieta y rareza se aceptan sin distinguir mayúsculas")
    void enumsSinMayusculas() {
        when(zonaDao.listar(conexion)).thenReturn(List.of());
        when(slimeDao.listar(conexion, null, Dieta.CARNE, Rareza.RARO)).thenReturn(List.of());

        assertThat(servicio.listarCatalogo(null, "carne", "Raro")).isEmpty();
    }
}
