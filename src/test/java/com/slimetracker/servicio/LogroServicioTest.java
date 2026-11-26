package com.slimetracker.servicio;

import com.slimetracker.dao.LogroDao;
import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dto.NotaPeticion;
import com.slimetracker.excepcion.AccesoDenegadoException;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.modelo.LogroConEstado;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("LogroServicio (R8)")
class LogroServicioTest extends BaseServicioTest {

    private static final long LOGRO = 7;
    private static final LogroConEstado SIN_CONSEGUIR = new LogroConEstado(PRIMER_CONTACTO, null, null);
    private static final LogroConEstado CONSEGUIDO = new LogroConEstado(PRIMER_CONTACTO, AHORA, null);

    @Mock
    LogroDao logroDao;
    @Mock
    PartidaDao partidaDao;

    private LogroServicio servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new LogroServicio(gestor, logroDao, partidaDao);
    }

    private void partidaPropia() {
        when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(YO)));
    }

    @Test
    @DisplayName("listarCatalogo y listarDePartida devuelven DTOs")
    void listar() {
        partidaPropia();
        when(logroDao.listar(conexion)).thenReturn(List.of(PRIMER_CONTACTO));
        when(logroDao.listarConEstado(conexion, PARTIDA)).thenReturn(List.of(CONSEGUIDO));

        assertThat(servicio.listarCatalogo()).singleElement().extracting("categoria").isEqualTo("SLIMES");
        assertThat(servicio.listarDePartida(YO, PARTIDA)).singleElement().extracting("conseguido").isEqualTo(true);
    }

    @Nested
    @DisplayName("marcar y desmarcar")
    class Marcar {

        @Test
        @DisplayName("marcar un logro no conseguido lo guarda y actualiza la partida (R9)")
        void marcar() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, LOGRO)).thenReturn(Optional.of(SIN_CONSEGUIR), Optional.of(CONSEGUIDO));

            assertThat(servicio.marcar(YO, PARTIDA, LOGRO).conseguido()).isTrue();
            verify(logroDao).marcar(conexion, PARTIDA, LOGRO);
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("marcar uno ya conseguido → 409")
        void yaConseguido() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, LOGRO)).thenReturn(Optional.of(CONSEGUIDO));

            assertThatThrownBy(() -> servicio.marcar(YO, PARTIDA, LOGRO))
                    .isInstanceOf(ReglaNegocioException.class).hasMessage("Ya habías conseguido «Primer contacto».");
            verify(logroDao, never()).marcar(any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("un logro que no existe → 404")
        void noExiste() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.marcar(YO, PARTIDA, 99L)).isInstanceOf(NoEncontradoException.class);
        }

        @Test
        @DisplayName("desmarcar uno conseguido lo quita y actualiza la partida")
        void desmarcar() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, LOGRO)).thenReturn(Optional.of(CONSEGUIDO));

            servicio.desmarcar(YO, PARTIDA, LOGRO);

            verify(logroDao).desmarcar(conexion, PARTIDA, LOGRO);
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("desmarcar uno no conseguido → 409")
        void desmarcarSinConseguir() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, LOGRO)).thenReturn(Optional.of(SIN_CONSEGUIR));

            assertThatThrownBy(() -> servicio.desmarcar(YO, PARTIDA, LOGRO)).isInstanceOf(ReglaNegocioException.class);
            verify(logroDao, never()).desmarcar(any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("marcar o desmarcar en la partida de otro → 403")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));

            assertThatThrownBy(() -> servicio.marcar(YO, PARTIDA, LOGRO)).isInstanceOf(AccesoDenegadoException.class);
            assertThatThrownBy(() -> servicio.desmarcar(YO, PARTIDA, LOGRO)).isInstanceOf(AccesoDenegadoException.class);
            verifyNoInteractions(logroDao);
        }
    }

    @Nested
    @DisplayName("nota")
    class Nota {

        @Test
        @DisplayName("se guarda en un logro conseguido")
        void guardar() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, LOGRO))
                    .thenReturn(Optional.of(CONSEGUIDO), Optional.of(new LogroConEstado(PRIMER_CONTACTO, AHORA, "Fue el Pink")));

            assertThat(servicio.actualizarNota(YO, PARTIDA, LOGRO, new NotaPeticion("Fue el Pink")).nota()).isEqualTo("Fue el Pink");
            verify(logroDao).actualizarNota(conexion, PARTIDA, LOGRO, "Fue el Pink");
            verify(partidaDao).tocar(conexion, PARTIDA);
        }

        @Test
        @DisplayName("más de 200 caracteres → 400 sin tocar la BD")
        void larga() {
            assertThatThrownBy(() -> servicio.actualizarNota(YO, PARTIDA, LOGRO, new NotaPeticion("b".repeat(201))))
                    .isInstanceOf(ValidacionException.class);
            verifyNoInteractions(gestor, logroDao);
        }

        @Test
        @DisplayName("en un logro no conseguido → 409")
        void sinConseguir() {
            partidaPropia();
            when(logroDao.buscarConEstado(conexion, PARTIDA, LOGRO)).thenReturn(Optional.of(SIN_CONSEGUIR));

            assertThatThrownBy(() -> servicio.actualizarNota(YO, PARTIDA, LOGRO, new NotaPeticion("x")))
                    .isInstanceOf(ReglaNegocioException.class);
            verify(partidaDao, never()).tocar(any(), anyLong());
        }

        @Test
        @DisplayName("en la partida de otro → 403")
        void deOtro() {
            when(partidaDao.buscarPorId(conexion, PARTIDA)).thenReturn(Optional.of(partidaDe(OTRO)));
            assertThatThrownBy(() -> servicio.actualizarNota(YO, PARTIDA, LOGRO, new NotaPeticion("x")))
                    .isInstanceOf(AccesoDenegadoException.class);
        }
    }
}
