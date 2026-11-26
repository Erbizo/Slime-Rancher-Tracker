package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.ProgresoDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.ProgresoRespuesta;
import com.slimetracker.excepcion.AccesoDenegadoException;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.modelo.Actividad;
import com.slimetracker.modelo.Actividad.TipoActividad;
import com.slimetracker.modelo.ConteoProgreso;
import com.slimetracker.modelo.SlimeConEstado;
import com.slimetracker.modelo.ZonaConEstado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ProgresoServicio (R10)")
class ProgresoServicioTest extends BaseServicioTest {

    @Mock
    ProgresoDao progresoDao;
    @Mock
    ZonaDao zonaDao;
    @Mock
    SlimeDao slimeDao;
    @Mock
    PartidaDao partidaDao;

    private ProgresoServicio servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ProgresoServicio(gestor, progresoDao, zonaDao, slimeDao, partidaDao);
    }

    @Test
    @DisplayName("la Primera partida del demo da 58 % (media de 5/7, 11/18 y 6/15 con HALF_UP)")
    void porcentajesDelDemo() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
        when(progresoDao.contar(conexion, PARTIDA)).thenReturn(new ConteoProgreso(7, 5, 18, 11, 15, 6));
        when(zonaDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of());
        when(slimeDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of());
        when(progresoDao.ultimasActividades(conexion, PARTIDA, 10)).thenReturn(List.of());

        ProgresoRespuesta progreso = servicio.obtener(YO, PARTIDA);

        assertThat(progreso.porcentajeGlobal()).isEqualTo(58);
        assertThat(progreso.zonas()).isEqualTo(new ProgresoRespuesta.Conteo(5, 7, 71));
        assertThat(progreso.slimes()).isEqualTo(new ProgresoRespuesta.Conteo(11, 18, 61));
        assertThat(progreso.logros()).isEqualTo(new ProgresoRespuesta.Conteo(6, 15, 40));
    }

    @Test
    @DisplayName("desglose por zona, siguientes pasos y últimas actividades")
    void desgloseYPasos() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
        when(progresoDao.contar(conexion, PARTIDA)).thenReturn(new ConteoProgreso(4, 2, 3, 1, 15, 0));
        when(zonaDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of(
                new ZonaConEstado(THE_RANCH, AHORA, 0, 0), new ZonaConEstado(DRY_REEF, AHORA, 2, 1),
                new ZonaConEstado(INDIGO_QUARRY, null, 1, 0), new ZonaConEstado(MOSS_BLANKET, null, 0, 0)));
        when(slimeDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of(
                new SlimeConEstado(PINK, AHORA, null), new SlimeConEstado(ROCK, null, null), new SlimeConEstado(RAD, null, null)));
        when(progresoDao.ultimasActividades(conexion, PARTIDA, 10))
                .thenReturn(List.of(new Actividad(TipoActividad.SLIME, "Pink", AHORA)));

        ProgresoRespuesta progreso = servicio.obtener(YO, PARTIDA);

        // Solo zonas con slimes; cada una con sus slimes en su color o en silueta
        assertThat(progreso.slimesPorZona()).extracting(ProgresoRespuesta.ZonaSlimes::zonaNombre)
                .containsExactly("Dry Reef", "Indigo Quarry");
        assertThat(progreso.slimesPorZona().get(0).slimes())
                .extracting(ProgresoRespuesta.SlimeMini::descubierto).containsExactly(true, false);
        assertThat(progreso.slimesPorZona().get(1).desbloqueada()).isFalse();
        // Ahora puedes: zonas cuyo requisito está abierto y slimes de zonas abiertas
        assertThat(progreso.siguientesPasos().zonas()).extracting(ProgresoRespuesta.PasoZona::zonaNombre)
                .containsExactly("Indigo Quarry", "Moss Blanket");
        assertThat(progreso.siguientesPasos().zonas().get(0).requisitoNombre()).isEqualTo("Dry Reef");
        assertThat(progreso.siguientesPasos().slimes()).extracting(ProgresoRespuesta.PasoSlime::slimeNombre)
                .containsExactly("Rock");
        assertThat(progreso.ultimasActividades()).singleElement()
                .extracting(ProgresoRespuesta.ActividadRespuesta::tipo).isEqualTo("SLIME");
    }

    @Test
    @DisplayName("partida de otro usuario → 403; que no existe → 404")
    void propiedad() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));
        when(partidaDao.buscarPorId(conexion, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(YO, PARTIDA)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> servicio.obtener(YO, 99L)).isInstanceOf(NoEncontradoException.class);
        verifyNoInteractions(progresoDao);
    }
}
