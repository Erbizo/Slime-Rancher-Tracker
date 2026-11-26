package com.slimetracker.modelo;

import java.time.Instant;

/**
 * Zona vista desde una partida (resultado de un LEFT JOIN): desbloqueadaEn es null si sigue bloqueada.
 * Incluye cuántos slimes tiene la zona y cuántos se han descubierto en esa partida.
 */
public record ZonaConEstado(Zona zona, Instant desbloqueadaEn, int slimesTotal, int slimesDescubiertos) {

    public boolean desbloqueada() {
        return desbloqueadaEn != null;
    }
}
