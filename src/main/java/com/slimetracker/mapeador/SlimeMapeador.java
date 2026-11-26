package com.slimetracker.mapeador;

import com.slimetracker.dto.ProgresoRespuesta.SlimeMini;
import com.slimetracker.dto.SlimeEstadoRespuesta;
import com.slimetracker.dto.SlimeRespuesta;
import com.slimetracker.modelo.Slime;
import com.slimetracker.modelo.SlimeConEstado;

public final class SlimeMapeador {

    private SlimeMapeador() {
    }

    public static SlimeRespuesta aRespuesta(Slime slime, String zonaNombre) {
        return new SlimeRespuesta(slime.id(), slime.nombre(), slime.dieta().name(), slime.comidaFavorita(),
                slime.rareza().name(), slime.peligroso(), slime.descripcion(), slime.color(), slime.zonaId(),
                zonaNombre);
    }

    public static SlimeEstadoRespuesta aEstado(SlimeConEstado estado, String zonaNombre, boolean zonaDesbloqueada) {
        Slime slime = estado.slime();
        return new SlimeEstadoRespuesta(slime.id(), slime.nombre(), slime.dieta().name(), slime.comidaFavorita(),
                slime.rareza().name(), slime.peligroso(), slime.descripcion(), slime.color(), slime.zonaId(),
                zonaNombre, zonaDesbloqueada, estado.descubierto(), estado.descubiertoEn(), estado.nota());
    }

    public static SlimeMini aMini(SlimeConEstado estado) {
        Slime slime = estado.slime();
        return new SlimeMini(slime.id(), slime.nombre(), slime.color(), estado.descubierto());
    }
}
