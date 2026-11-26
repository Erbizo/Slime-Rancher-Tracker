package com.slimetracker.dto;

import java.time.Instant;
import java.util.List;

/** Todo lo que necesita el panel de una partida. */
public record ProgresoRespuesta(long partidaId, String partidaNombre, Instant actualizadaEn,
                                int porcentajeGlobal,
                                Conteo zonas, Conteo slimes, Conteo logros,
                                List<ZonaSlimes> slimesPorZona,
                                List<ActividadRespuesta> ultimasActividades,
                                SiguientesPasos siguientesPasos) {

    public record Conteo(int conseguidos, int total, int porcentaje) {
    }

    /** Slimes de una zona con su estado, para la tarjeta "Slimepedia por zona". */
    public record ZonaSlimes(long zonaId, String zonaNombre, boolean desbloqueada,
                             int descubiertos, int total, List<SlimeMini> slimes) {
    }

    public record SlimeMini(long id, String nombre, String color, boolean descubierto) {
    }

    /** tipo: ZONA, SLIME o LOGRO. */
    public record ActividadRespuesta(String tipo, String nombre, Instant fecha) {
    }

    /** "Ahora puedes": zonas que ya se pueden desbloquear y slimes que ya se pueden descubrir. */
    public record SiguientesPasos(List<PasoZona> zonas, List<PasoSlime> slimes) {
    }

    public record PasoZona(long zonaId, String zonaNombre, String requisitoNombre) {
    }

    public record PasoSlime(long slimeId, String slimeNombre, String zonaNombre) {
    }
}
