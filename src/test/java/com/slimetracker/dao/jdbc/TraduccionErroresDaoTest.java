package com.slimetracker.dao.jdbc;

import com.slimetracker.excepcion.AccesoDatosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regla 5: ningún método DAO deja escapar una SQLException. Con la conexión cerrada, todos
 * tienen que lanzar una excepción propia (AccesoDatosException) en vez de la del driver.
 */
@DisplayName("Todos los DAO traducen la SQLException a una excepción propia")
class TraduccionErroresDaoTest extends BaseDaoTest {

    private static final UsuarioDaoJdbc USUARIOS = new UsuarioDaoJdbc();
    private static final PartidaDaoJdbc PARTIDAS = new PartidaDaoJdbc();
    private static final ZonaDaoJdbc ZONAS = new ZonaDaoJdbc();
    private static final SlimeDaoJdbc SLIMES = new SlimeDaoJdbc();
    private static final LogroDaoJdbc LOGROS = new LogroDaoJdbc();
    private static final ProgresoDaoJdbc PROGRESO = new ProgresoDaoJdbc();
    private static final SaludDaoJdbc SALUD = new SaludDaoJdbc();

    private Connection cerrada;

    @BeforeEach
    void cerrarConexion() throws SQLException {
        cerrada = DATA_SOURCE.getConnection();
        cerrada.close();
    }

    static Stream<Arguments> metodos() {
        return Stream.of(
                caso("UsuarioDao.insertar", c -> USUARIOS.insertar(c, "x", "y")),
                caso("UsuarioDao.buscarPorId", c -> USUARIOS.buscarPorId(c, 1)),
                caso("UsuarioDao.buscarPorNombre", c -> USUARIOS.buscarPorNombre(c, "x")),
                caso("PartidaDao.insertar", c -> PARTIDAS.insertar(c, 1, "x")),
                caso("PartidaDao.buscarPorId", c -> PARTIDAS.buscarPorId(c, 1)),
                caso("PartidaDao.listarPorUsuario", c -> PARTIDAS.listarPorUsuario(c, 1)),
                caso("PartidaDao.renombrar", c -> PARTIDAS.renombrar(c, 1, "x")),
                caso("PartidaDao.borrar", c -> PARTIDAS.borrar(c, 1)),
                caso("PartidaDao.tocar", c -> PARTIDAS.tocar(c, 1)),
                caso("ZonaDao.listar", ZONAS::listar),
                caso("ZonaDao.buscarPorId", c -> ZONAS.buscarPorId(c, 1)),
                caso("ZonaDao.buscarInicial", ZONAS::buscarInicial),
                caso("ZonaDao.listarConEstado", c -> ZONAS.listarConEstado(c, 1)),
                caso("ZonaDao.desbloquear", c -> ZONAS.desbloquear(c, 1, 1)),
                caso("ZonaDao.bloquear", c -> ZONAS.bloquear(c, 1, 1)),
                caso("ZonaDao.estaDesbloqueada", c -> ZONAS.estaDesbloqueada(c, 1, 1)),
                caso("ZonaDao.listarDependientesDesbloqueadas", c -> ZONAS.listarDependientesDesbloqueadas(c, 1, 1)),
                caso("SlimeDao.listar", c -> SLIMES.listar(c, null, null, null)),
                caso("SlimeDao.buscarPorId", c -> SLIMES.buscarPorId(c, 1)),
                caso("SlimeDao.listarConEstado", c -> SLIMES.listarConEstado(c, 1)),
                caso("SlimeDao.buscarConEstado", c -> SLIMES.buscarConEstado(c, 1, 1)),
                caso("SlimeDao.descubrir", c -> SLIMES.descubrir(c, 1, 1)),
                caso("SlimeDao.olvidar", c -> SLIMES.olvidar(c, 1, 1)),
                caso("SlimeDao.actualizarNota", c -> SLIMES.actualizarNota(c, 1, 1, "x")),
                caso("SlimeDao.estaDescubierto", c -> SLIMES.estaDescubierto(c, 1, 1)),
                caso("SlimeDao.listarDescubiertosEnZona", c -> SLIMES.listarDescubiertosEnZona(c, 1, 1)),
                caso("LogroDao.listar", LOGROS::listar),
                caso("LogroDao.listarConEstado", c -> LOGROS.listarConEstado(c, 1)),
                caso("LogroDao.buscarConEstado", c -> LOGROS.buscarConEstado(c, 1, 1)),
                caso("LogroDao.marcar", c -> LOGROS.marcar(c, 1, 1)),
                caso("LogroDao.desmarcar", c -> LOGROS.desmarcar(c, 1, 1)),
                caso("LogroDao.actualizarNota", c -> LOGROS.actualizarNota(c, 1, 1, "x")),
                caso("ProgresoDao.contar", c -> PROGRESO.contar(c, 1)),
                caso("ProgresoDao.contarPorUsuario", c -> PROGRESO.contarPorUsuario(c, 1)),
                caso("ProgresoDao.ultimasActividades", c -> PROGRESO.ultimasActividades(c, 1, 10)),
                caso("SaludDao.comprobar", SALUD::comprobar));
    }

    private static Arguments caso(String nombre, ThrowingConsumer<Connection> llamada) {
        return Arguments.of(nombre, llamada);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("metodos")
    @DisplayName("con la conexión cerrada lanzan AccesoDatosException con la SQLException como causa")
    void traducen(String nombre, ThrowingConsumer<Connection> llamada) {
        assertThatThrownBy(() -> llamada.accept(cerrada))
                .isInstanceOf(AccesoDatosException.class)
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("SaludDao.comprobar con la BD funcionando devuelve true")
    void salud() {
        assertThat(SALUD.comprobar(conexion)).isTrue();
    }
}
