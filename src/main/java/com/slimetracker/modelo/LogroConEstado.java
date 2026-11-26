package com.slimetracker.modelo;

import java.time.Instant;

/** Logro visto desde una partida (LEFT JOIN): conseguidoEn es null si no se ha conseguido. */
public record LogroConEstado(Logro logro, Instant conseguidoEn, String nota) {

    public boolean conseguido() {
        return conseguidoEn != null;
    }
}
