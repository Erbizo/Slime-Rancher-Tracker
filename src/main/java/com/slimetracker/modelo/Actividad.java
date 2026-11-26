package com.slimetracker.modelo;

import java.time.Instant;

/** Un cambio de progreso: zona desbloqueada, slime descubierto o logro conseguido. */
public record Actividad(TipoActividad tipo, String nombre, Instant fecha) {

    public enum TipoActividad { ZONA, SLIME, LOGRO }
}
