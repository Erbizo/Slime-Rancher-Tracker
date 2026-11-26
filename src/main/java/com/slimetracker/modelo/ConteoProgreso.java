package com.slimetracker.modelo;

/** Totales del catálogo y conseguidos en una partida. */
public record ConteoProgreso(int zonasTotal, int zonasConseguidas,
                             int slimesTotal, int slimesConseguidos,
                             int logrosTotal, int logrosConseguidos) {
}
