package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.modelo.Zona;

import java.sql.Connection;

/**
 * Crear una partida = insertarla + desbloquear la zona inicial (R1 y R3).
 * Lo usan AutenticacionServicio y PartidaServicio sin depender uno del otro.
 */
final class NuevaPartida {

    private NuevaPartida() {
    }

    static long crear(PartidaDao partidaDao, ZonaDao zonaDao, Connection conexion, long usuarioId, String nombre) {
        long partidaId = partidaDao.insertar(conexion, usuarioId, nombre);
        Zona inicial = zonaDao.buscarInicial(conexion)
                .orElseThrow(() -> new IllegalStateException("El catálogo no tiene zona inicial"));
        zonaDao.desbloquear(conexion, partidaId, inicial.id());
        return partidaId;
    }
}
