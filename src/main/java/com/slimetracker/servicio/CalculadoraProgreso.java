package com.slimetracker.servicio;

import com.slimetracker.modelo.ConteoProgreso;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * R10: porcentajes redondeados a entero con HALF_UP.
 * El global es la media de las tres fracciones exactas (no de los porcentajes ya redondeados):
 * (5/7 + 11/18 + 6/15) / 3 = 57,51 % → 58 %.
 */
final class CalculadoraProgreso {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal TRES = BigDecimal.valueOf(3);

    private CalculadoraProgreso() {
    }

    static int porcentaje(int conseguidos, int total) {
        return fraccion(conseguidos, total).multiply(CIEN).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    static int porcentajeGlobal(ConteoProgreso conteo) {
        BigDecimal suma = fraccion(conteo.zonasConseguidas(), conteo.zonasTotal())
                .add(fraccion(conteo.slimesConseguidos(), conteo.slimesTotal()))
                .add(fraccion(conteo.logrosConseguidos(), conteo.logrosTotal()));
        return suma.multiply(CIEN).divide(TRES, 0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal fraccion(int conseguidos, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(conseguidos).divide(BigDecimal.valueOf(total), MathContext.DECIMAL64);
    }
}
