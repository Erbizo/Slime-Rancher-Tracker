package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.ZonaEstadoRespuesta;
import com.slimetracker.excepcion.AccesoDenegadoException;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.modelo.ZonaConEstado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ZonaServicio (R4 y R5)")
class ZonaServicioTest extends BaseServicioTest {

    @Mock
    ZonaDao zonaDao;
    @Mock
    SlimeDao slimeDao;
    @Mock
    PartidaDao partidaDao;

    private ZonaServicio servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ZonaServicio(gestor, zonaDao, slimeDao, partidaDao);
    }

    private void partidaPropia() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
    }

    /** The Ranch y Dry Reef abiertas; Indigo Quarry y Moss Blanket cerradas. */
    private List<ZonaConEstado> estados() {
        return List.of(new ZonaConEstado(THE_RANCH, AHORA, 0, 0), new ZonaConEstado(DRY_REEF, AHORA, 5, 2),
                new ZonaConEstado(INDIGO_QUARRY, null, 3, 0), new ZonaConEstado(MOSS_BLANKET, null, 4, 0));
    }

    @Test
    @DisplayName("listarCatalogo devuelve las zonas como DTO")
    void catalogo() {
        when(zonaDao.listar(conexion)).thenReturn(List.of(THE_RANCH, DRY_REEF));
        assertThat(servicio.listarCatalogo()).extracting("nombre").containsExactly("The Ranch", "Dry Reef");
    }

    @Test
    @DisplayName("listarDePartida calcula el requisito y si ya se puede desbloquear")
    void listarDePartida() {
        partidaPropia();
        when(zonaDao.listarConEstado(conexion, PARTIDA)).thenReturn(estados());

        List<ZonaEstadoRespuesta> zonas = servicio.listarDePartida(YO, PARTIDA);

        assertThat(zonas.get(0).puedeDesbloquear()).isFalse();
        assertThat(zonas.get(2).requisitoNombre()).isEqualTo("Dry Reef");
        assertThat(zonas.get(2).puedeDesbloquear()).isTrue();
        assertThat(zonas.get(1).slimesDescubiertos()).isEqualTo(2);
    }

    @Test
    @DisplayName("listarDePartida de otro usuario → 403")
    void listarDeOtro() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));
        assertThatThrownBy(() -> servicio.listarDePartida(YO, PARTIDA)).isInstanceOf(AccesoDenegadoException.class);
    }

    @Nested
    @DisplayName("R4: desbloquear")
    class Desbloquear {

        @Test
        @DisplayName("desbloquea si el requisito está abierto y actualiza la partida (R9)")
        void caminoFeliz() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 3L)).thenReturn(Optional.of(INDIGO_QUARRY));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 3L)).thenReturn(false);
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);
            when(zonaDao.listarConEstado(conexion, PARTIDA)).thenReturn(estados());

            ZonaEstadoRespuesta zona = servicio.desbloquear(YO, PARTIDA, 3L);

            assertThat(zona.nombre()).isEqualTo("Indigo Quarry");
            verify(zonaDao).desbloquear(conexion, PARTIDA, 3L);
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("zona que no existe → 404")
        void noExiste() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.desbloquear(YO, PARTIDA, 99L))
                    .isInstanceOf(NoEncontradoException.class).hasMessage("La zona no existe.");
        }

        @Test
        @DisplayName("ya desbloqueada → 409")
        void yaDesbloqueada() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(DRY_REEF));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);

            assertThatThrownBy(() -> servicio.desbloquear(YO, PARTIDA, 2L))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage("Dry Reef ya está desbloqueada.");
            verify(zonaDao, never()).desbloquear(any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("requisito sin desbloquear → 409 'Primero tienes que desbloquear X.'")
        void requisitoCerrado() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 3L)).thenReturn(Optional.of(INDIGO_QUARRY));
            when(zonaDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(DRY_REEF));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 3L)).thenReturn(false);
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.desbloquear(YO, PARTIDA, 3L))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage("Primero tienes que desbloquear Dry Reef.");
            verify(zonaDao, never()).desbloquear(any(), anyLong(), anyLong());
            verify(partidaDao, never()).tocar(any(), anyLong());
        }

        @Test
        @DisplayName("partida de otro usuario → 403 y no se toca nada")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.desbloquear(YO, PARTIDA, 3L)).isInstanceOf(AccesoDenegadoException.class);
            verify(zonaDao, never()).desbloquear(any(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("R5: bloquear")
    class Bloquear {

        @Test
        @DisplayName("bloquea una zona sin dependientes ni slimes y actualiza la partida (R9)")
        void caminoFeliz() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 3L)).thenReturn(Optional.of(INDIGO_QUARRY));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 3L)).thenReturn(true);
            when(zonaDao.listarDependientesDesbloqueadas(conexion, PARTIDA, 3L)).thenReturn(List.of());
            when(slimeDao.listarDescubiertosEnZona(conexion, PARTIDA, 3L)).thenReturn(List.of());

            servicio.bloquear(YO, PARTIDA, 3L);

            verify(zonaDao).bloquear(conexion, PARTIDA, 3L);
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("la zona inicial no se puede bloquear → 409")
        void inicial() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 1L)).thenReturn(Optional.of(THE_RANCH));

            assertThatThrownBy(() -> servicio.bloquear(YO, PARTIDA, 1L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("The Ranch es la zona inicial y no se puede bloquear.");
            verify(zonaDao, never()).bloquear(any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("una zona que no estaba desbloqueada → 409")
        void yaBloqueada() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 4L)).thenReturn(Optional.of(MOSS_BLANKET));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 4L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.bloquear(YO, PARTIDA, 4L))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage("Moss Blanket ya está bloqueada.");
        }

        @Test
        @DisplayName("con zonas dependientes desbloqueadas → 409 diciendo cuáles (como en el diseño)")
        void conDependientes() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(DRY_REEF));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);
            when(zonaDao.listarDependientesDesbloqueadas(conexion, PARTIDA, 2L))
                    .thenReturn(List.of(INDIGO_QUARRY, MOSS_BLANKET));

            assertThatThrownBy(() -> servicio.bloquear(YO, PARTIDA, 2L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Indigo Quarry y Moss Blanket dependen de Dry Reef.");
            verify(zonaDao, never()).bloquear(any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("con una sola dependiente el verbo va en singular")
        void unaDependiente() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(DRY_REEF));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);
            when(zonaDao.listarDependientesDesbloqueadas(conexion, PARTIDA, 2L)).thenReturn(List.of(INDIGO_QUARRY));

            assertThatThrownBy(() -> servicio.bloquear(YO, PARTIDA, 2L))
                    .hasMessage("Indigo Quarry depende de Dry Reef.");
        }

        @Test
        @DisplayName("con slimes descubiertos en la zona → 409 diciendo cuáles")
        void conSlimes() {
            partidaPropia();
            when(zonaDao.buscarPorId(conexion, 2L)).thenReturn(Optional.of(DRY_REEF));
            when(zonaDao.estaDesbloqueada(conexion, PARTIDA, 2L)).thenReturn(true);
            when(zonaDao.listarDependientesDesbloqueadas(conexion, PARTIDA, 2L)).thenReturn(List.of());
            when(slimeDao.listarDescubiertosEnZona(conexion, PARTIDA, 2L)).thenReturn(List.of("Pink", "Rock", "Tabby"));

            assertThatThrownBy(() -> servicio.bloquear(YO, PARTIDA, 2L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Tienes en la Slimepedia slimes de Dry Reef: Pink, Rock y Tabby.");
            verify(zonaDao, never()).bloquear(any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("partida de otro usuario → 403")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));
            assertThatThrownBy(() -> servicio.bloquear(YO, PARTIDA, 2L)).isInstanceOf(AccesoDenegadoException.class);
        }
    }
}
