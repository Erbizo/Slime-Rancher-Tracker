package com.slimetracker.modelo;

import java.time.Instant;

/** Slime visto desde una partida (LEFT JOIN): descubiertoEn es null si aún no se ha descubierto. */
public record SlimeConEstado(Slime slime, Instant descubiertoEn, String nota) {

    public boolean descubierto() {
        return descubiertoEn != null;
    }
}
