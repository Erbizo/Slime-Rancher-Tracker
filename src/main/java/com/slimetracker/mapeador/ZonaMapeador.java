package com.slimetracker.mapeador;

import com.slimetracker.dto.ZonaEstadoRespuesta;
import com.slimetracker.dto.ZonaRespuesta;
import com.slimetracker.modelo.Zona;
import com.slimetracker.modelo.ZonaConEstado;

public final class ZonaMapeador {

    private ZonaMapeador() {
    }

    public static ZonaRespuesta aRespuesta(Zona zona) {
        return new ZonaRespuesta(zona.id(), zona.nombre(), zona.descripcion(), zona.orden(), zona.esInicial(),
                zona.zonaRequisitoId());
    }

    public static ZonaEstadoRespuesta aEstado(ZonaConEstado estado, String requisitoNombre, boolean puedeDesbloquear) {
        Zona zona = estado.zona();
        return new ZonaEstadoRespuesta(zona.id(), zona.nombre(), zona.descripcion(), zona.orden(), zona.esInicial(),
                zona.zonaRequisitoId(), requisitoNombre, estado.desbloqueada(), estado.desbloqueadaEn(),
                puedeDesbloquear, estado.slimesTotal(), estado.slimesDescubiertos());
    }
}
