package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.ProgresoDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.PartidaPeticion;
import com.slimetracker.dto.PartidaRespuesta;
import com.slimetracker.excepcion.AccesoDenegadoException;
import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.modelo.ConteoProgreso;
import com.slimetracker.modelo.Partida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("PartidaServicio (R3)")
class PartidaServicioTest extends BaseServicioTest {

    private static final ConteoProgreso CONTEO_DEMO = new ConteoProgreso(7, 5, 18, 11, 15, 6);

    @Mock
    PartidaDao partidaDao;
    @Mock
    ZonaDao zonaDao;
    @Mock
    ProgresoDao progresoDao;

    private PartidaServicio servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new PartidaServicio(gestor, partidaDao, zonaDao, progresoDao);
    }

    @Nested
    @DisplayName("leer")
    class Leer {

        @Test
        @DisplayName("listar devuelve las partidas del usuario con su porcentaje")
        void listar() {
            Partida rush = new Partida(11L, YO, "Modo Rush", AHORA, AHORA);
            when(partidaDao.listarPorUsuario(conexion, YO)).thenReturn(List.of(partidaDe(YO), rush));
            when(progresoDao.contarPorUsuario(conexion, YO)).thenReturn(Map.of(PARTIDA, CONTEO_DEMO));

            List<PartidaRespuesta> partidas = servicio.listar(YO);

            assertThat(partidas).extracting(PartidaRespuesta::porcentaje).containsExactly(58, 0);
        }

        @Test
        @DisplayName("obtener una partida propia → 200 con su porcentaje")
        void obtener() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
            when(progresoDao.contar(conexion, PARTIDA)).thenReturn(CONTEO_DEMO);

            assertThat(servicio.obtener(YO, PARTIDA).porcentaje()).isEqualTo(58);
        }

        @Test
        @DisplayName("obtener una partida que no existe → 404")
        void noExiste() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.obtener(YO, PARTIDA))
                    .isInstanceOf(NoEncontradoException.class).hasMessage("La partida no existe.");
        }

        @Test
        @DisplayName("obtener la partida de otro usuario → 403")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.obtener(YO, PARTIDA))
                    .isInstanceOf(AccesoDenegadoException.class).hasMessage("Esa partida no es tuya.");
            verifyNoInteractions(progresoDao);
        }
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("inserta la partida (sin espacios en los extremos) y desbloquea la zona inicial")
        void crea() {
            when(partidaDao.insertar(conexion, YO, "Nueva")).thenReturn(PARTIDA);
            when(zonaDao.buscarInicial(conexion)).thenReturn(Optional.of(THE_RANCH));
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
            when(progresoDao.contar(conexion, PARTIDA)).thenReturn(new ConteoProgreso(7, 1, 18, 0, 15, 0));

            PartidaRespuesta creada = servicio.crear(YO, new PartidaPeticion("  Nueva  "));

            assertThat(creada.porcentaje()).isEqualTo(5);
            verify(zonaDao).desbloquear(conexion, PARTIDA, THE_RANCH.id());
            verify(gestor).enTransaccion(any());
        }

        @ParameterizedTest(name = "nombre \"{0}\" no es válido")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "Una partida con un nombre larguísimo, 41c"})
        @DisplayName("nombre vacío o de más de 40 caracteres → 400 y no se escribe nada")
        void nombreNoValido(String nombre) {
            assertThatThrownBy(() -> servicio.crear(YO, new PartidaPeticion(nombre)))
                    .isInstanceOf(ValidacionException.class);
            verify(partidaDao, never()).insertar(any(), anyLong(), anyString());
            verifyNoInteractions(gestor);
        }

        @Test
        @DisplayName("nombre repetido para el mismo usuario → 409")
        void repetido() {
            when(partidaDao.insertar(conexion, YO, "Modo Rush")).thenThrow(new EntidadDuplicadaException("dup", null));

            assertThatThrownBy(() -> servicio.crear(YO, new PartidaPeticion("Modo Rush")))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Ya tienes una partida con ese nombre.");
            verify(zonaDao, never()).desbloquear(any(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("renombrar y borrar")
    class Modificar {

        @Test
        @DisplayName("renombrar una partida propia")
        void renombrar() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
            when(progresoDao.contar(conexion, PARTIDA)).thenReturn(CONTEO_DEMO);

            servicio.renombrar(YO, PARTIDA, new PartidaPeticion("Otra"));

            verify(partidaDao).renombrar(conexion, PARTIDA, "Otra");
        }

        @Test
        @DisplayName("renombrar la partida de otro → 403 y no se cambia")
        void renombrarDeOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.renombrar(YO, PARTIDA, new PartidaPeticion("Mía")))
                    .isInstanceOf(AccesoDenegadoException.class);
            verify(partidaDao, never()).renombrar(any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("renombrar a un nombre que ya tienes → 409")
        void renombrarRepetido() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
            when(partidaDao.renombrar(conexion, PARTIDA, "Modo Rush")).thenThrow(new EntidadDuplicadaException("dup", null));

            assertThatThrownBy(() -> servicio.renombrar(YO, PARTIDA, new PartidaPeticion("Modo Rush")))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("borrar una partida propia")
        void borrar() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));

            servicio.borrar(YO, PARTIDA);

            verify(partidaDao).borrar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("borrar la partida de otro → 403 y no se borra")
        void borrarDeOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.borrar(YO, PARTIDA)).isInstanceOf(AccesoDenegadoException.class);
            verify(partidaDao, never()).borrar(any(), anyLong());
        }

        @Test
        @DisplayName("borrar una partida que no existe → 404")
        void borrarInexistente() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.borrar(YO, PARTIDA)).isInstanceOf(NoEncontradoException.class);
        }
    }
}
