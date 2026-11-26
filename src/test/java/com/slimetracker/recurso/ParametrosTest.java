package com.slimetracker.recurso;

import com.slimetracker.excepcion.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

@DisplayName("Parametros: validación de entrada en la capa de control")
class ParametrosTest {

    @Test
    @DisplayName("un id numérico y positivo se acepta")
    void idValido() {
        assertThat(Parametros.id("42", "id")).isEqualTo(42L);
    }

    @ParameterizedTest(name = "id \"{0}\" → 400")
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-3", "1.5", "99999999999999999999"})
    @DisplayName("un id que no es un entero positivo → 400 en ese campo")
    void idNoValido(String valor) {
        assertThatThrownBy(() -> Parametros.id(valor, "zonaId"))
                .isInstanceOf(ValidacionException.class)
                .extracting("campos", MAP).containsEntry("zonaId", Parametros.ID_NO_VALIDO);
    }

    @Test
    @DisplayName("los filtros opcionales vacíos son null")
    void opcionales() {
        assertThat(Parametros.idOpcional(null, "zonaId")).isNull();
        assertThat(Parametros.idOpcional(" ", "zonaId")).isNull();
        assertThat(Parametros.idOpcional("3", "zonaId")).isEqualTo(3L);
        assertThat(Parametros.booleanoOpcional(null, "descubierto")).isNull();
        assertThat(Parametros.booleanoOpcional("true", "descubierto")).isTrue();
        assertThat(Parametros.booleanoOpcional("false", "descubierto")).isFalse();
    }

    @Test
    @DisplayName("un booleano que no es true/false → 400")
    void booleanoNoValido() {
        assertThatThrownBy(() -> Parametros.booleanoOpcional("quizas", "descubierto"))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    @DisplayName("falta el cuerpo → 400; si está, se devuelve tal cual")
    void cuerpo() {
        assertThatThrownBy(() -> Parametros.cuerpo(null))
                .isInstanceOf(ValidacionException.class)
                .extracting("campos", MAP).containsOnlyKeys("cuerpo");
        assertThat(Parametros.cuerpo("x")).isEqualTo("x");
    }
}
