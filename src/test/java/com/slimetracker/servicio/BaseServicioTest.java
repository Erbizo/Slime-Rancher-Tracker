package com.slimetracker.servicio;

import com.slimetracker.modelo.Logro;
import com.slimetracker.modelo.CategoriaLogro;
import com.slimetracker.modelo.Dieta;
import com.slimetracker.modelo.Partida;
import com.slimetracker.modelo.Rareza;
import com.slimetracker.modelo.Slime;
import com.slimetracker.modelo.Zona;
import com.slimetracker.tx.FuncionSql;
import com.slimetracker.tx.GestorTransacciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Base de los tests de servicio: el GestorTransacciones mockeado ejecuta la lambda con una conexión simulada,
 * así se prueba la lógica de negocio sin base de datos. Incluye datos de ejemplo parecidos a los del demo.
 */
@ExtendWith(MockitoExtension.class)
abstract class BaseServicioTest {

    static final long YO = 1;
    static final long OTRO = 2;
    static final long PARTIDA = 10;
    static final Instant AHORA = Instant.parse("2026-11-26T18:42:00Z");

    static final Zona THE_RANCH = new Zona(1L, "The Ranch", "Tu rancho.", 1, true, null);
    static final Zona DRY_REEF = new Zona(2L, "Dry Reef", "Islas.", 2, false, 1L);
    static final Zona INDIGO_QUARRY = new Zona(3L, "Indigo Quarry", "Cantera.", 3, false, 2L);
    static final Zona MOSS_BLANKET = new Zona(4L, "Moss Blanket", "Bosque.", 4, false, 2L);

    static final Slime PINK = new Slime(1L, "Pink", Dieta.OMNIVORO, null, Rareza.COMUN, false, null, "#FF7EB6", 2L);
    static final Slime ROCK = new Slime(2L, "Rock", Dieta.VERDURA, null, Rareza.COMUN, false, null, "#6F8FC0", 2L);
    static final Slime RAD = new Slime(6L, "Rad", Dieta.VERDURA, null, Rareza.COMUN, false, null, "#4FD63A", 3L);

    static final Logro PRIMER_CONTACTO = new Logro(7L, "Primer contacto", "Descubre tu primer slime.", CategoriaLogro.SLIMES);

    @Mock
    GestorTransacciones gestor;

    @Mock
    Connection conexion;

    @BeforeEach
    void gestorEjecutaLaLambda() {
        lenient().when(gestor.enTransaccion(any())).thenAnswer(inv -> inv.<FuncionSql<Connection, Object>>getArgument(0).aplicar(conexion));
        lenient().when(gestor.soloLectura(any())).thenAnswer(inv -> inv.<FuncionSql<Connection, Object>>getArgument(0).aplicar(conexion));
    }

    static Partida partidaDe(long usuarioId) {
        return new Partida(PARTIDA, usuarioId, "Primera partida", AHORA, AHORA);
    }
}
