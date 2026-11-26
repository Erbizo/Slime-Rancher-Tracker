package com.slimetracker.servicio;

import com.slimetracker.modelo.ConteoProgreso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CalculadoraProgreso y Textos")
class CalculadoraProgresoTest {

    @ParameterizedTest(name = "zonas {0}/7, slimes {1}/18, logros {2}/15 → {3} %")
    @CsvSource({
            "5, 11, 6, 58",   // Primera partida del demo: 57,51 %
            "3, 3, 1, 22",    // Modo Rush: 22,06 %
            "1, 0, 0, 5",     // Partida recién creada: solo The Ranch
            "0, 0, 0, 0",
            "7, 18, 15, 100"
    })
    @DisplayName("el global es la media de las tres fracciones exactas, redondeada con HALF_UP")
    void global(int zonas, int slimes, int logros, int esperado) {
        assertThat(CalculadoraProgreso.porcentajeGlobal(new ConteoProgreso(7, zonas, 18, slimes, 15, logros)))
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("redondeo HALF_UP: 1/8 = 12,5 % → 13 %; sin total → 0 %")
    void porcentaje() {
        assertThat(CalculadoraProgreso.porcentaje(1, 8)).isEqualTo(13);
        assertThat(CalculadoraProgreso.porcentaje(5, 7)).isEqualTo(71);
        assertThat(CalculadoraProgreso.porcentaje(0, 0)).isZero();
        assertThat(CalculadoraProgreso.porcentajeGlobal(new ConteoProgreso(0, 0, 0, 0, 0, 0))).isZero();
    }

    @Test
    @DisplayName("Textos.enumerar une nombres con comas e 'y'")
    void enumerar() {
        assertThat(Textos.enumerar(List.of("Pink"))).isEqualTo("Pink");
        assertThat(Textos.enumerar(List.of("Pink", "Rock"))).isEqualTo("Pink y Rock");
        assertThat(Textos.enumerar(List.of("Pink", "Rock", "Tabby"))).isEqualTo("Pink, Rock y Tabby");
    }
}
