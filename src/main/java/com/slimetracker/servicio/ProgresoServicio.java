package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.ProgresoDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.ProgresoRespuesta;
import com.slimetracker.dto.ProgresoRespuesta.Conteo;
import com.slimetracker.dto.ProgresoRespuesta.PasoSlime;
import com.slimetracker.dto.ProgresoRespuesta.PasoZona;
import com.slimetracker.dto.ProgresoRespuesta.SiguientesPasos;
import com.slimetracker.dto.ProgresoRespuesta.ZonaSlimes;
import com.slimetracker.mapeador.ProgresoMapeador;
import com.slimetracker.mapeador.SlimeMapeador;
import com.slimetracker.modelo.ConteoProgreso;
import com.slimetracker.modelo.Partida;
import com.slimetracker.modelo.SlimeConEstado;
import com.slimetracker.modelo.ZonaConEstado;
import com.slimetracker.tx.GestorTransacciones;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** R10: panel de progreso de una partida. */
public class ProgresoServicio {

    static final int ULTIMAS_ACTIVIDADES = 10;

    private final GestorTransacciones gestor;
    private final ProgresoDao progresoDao;
    private final ZonaDao zonaDao;
    private final SlimeDao slimeDao;
    private final PartidaDao partidaDao;

    public ProgresoServicio(GestorTransacciones gestor, ProgresoDao progresoDao, ZonaDao zonaDao, SlimeDao slimeDao,
                            PartidaDao partidaDao) {
        this.gestor = gestor;
        this.progresoDao = progresoDao;
        this.zonaDao = zonaDao;
        this.slimeDao = slimeDao;
        this.partidaDao = partidaDao;
    }

    public ProgresoRespuesta obtener(long usuarioId, long partidaId) {
        return gestor.soloLectura(conexion -> {
            Partida partida = PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            ConteoProgreso conteo = progresoDao.contar(conexion, partidaId);
            List<ZonaConEstado> zonas = zonaDao.listarConEstado(conexion, partidaId);
            List<SlimeConEstado> slimes = slimeDao.listarConEstado(conexion, partidaId);
            List<ProgresoRespuesta.ActividadRespuesta> actividades =
                    progresoDao.ultimasActividades(conexion, partidaId, ULTIMAS_ACTIVIDADES).stream()
                            .map(ProgresoMapeador::aRespuesta)
                            .toList();
            return new ProgresoRespuesta(partida.id(), partida.nombre(), partida.actualizadaEn(),
                    CalculadoraProgreso.porcentajeGlobal(conteo),
                    conteo(conteo.zonasConseguidas(), conteo.zonasTotal()),
                    conteo(conteo.slimesConseguidos(), conteo.slimesTotal()),
                    conteo(conteo.logrosConseguidos(), conteo.logrosTotal()),
                    slimesPorZona(zonas, slimes),
                    actividades,
                    siguientesPasos(zonas, slimes));
        });
    }

    private static Conteo conteo(int conseguidos, int total) {
        return new Conteo(conseguidos, total, CalculadoraProgreso.porcentaje(conseguidos, total));
    }

    /** Zonas con slimes, en orden, cada una con sus slimes en su color o en silueta. */
    private static List<ZonaSlimes> slimesPorZona(List<ZonaConEstado> zonas, List<SlimeConEstado> slimes) {
        Map<Long, List<SlimeConEstado>> porZona = slimes.stream()
                .collect(Collectors.groupingBy(estado -> estado.slime().zonaId()));
        return zonas.stream()
                .filter(zona -> zona.slimesTotal() > 0)
                .map(zona -> new ZonaSlimes(zona.zona().id(), zona.zona().nombre(), zona.desbloqueada(),
                        zona.slimesDescubiertos(), zona.slimesTotal(),
                        porZona.getOrDefault(zona.zona().id(), List.of()).stream()
                                .map(SlimeMapeador::aMini)
                                .toList()))
                .toList();
    }

    /** Zonas que ya se pueden desbloquear y slimes sin descubrir de zonas abiertas. */
    private static SiguientesPasos siguientesPasos(List<ZonaConEstado> zonas, List<SlimeConEstado> slimes) {
        Map<Long, ZonaConEstado> porId = zonas.stream()
                .collect(Collectors.toMap(zona -> zona.zona().id(), Function.identity()));
        List<PasoZona> pasosZona = zonas.stream()
                .filter(zona -> !zona.desbloqueada())
                .filter(zona -> {
                    ZonaConEstado requisito = porId.get(zona.zona().zonaRequisitoId());
                    return requisito == null || requisito.desbloqueada();
                })
                .map(zona -> {
                    ZonaConEstado requisito = porId.get(zona.zona().zonaRequisitoId());
                    return new PasoZona(zona.zona().id(), zona.zona().nombre(),
                            requisito == null ? null : requisito.zona().nombre());
                })
                .toList();
        List<PasoSlime> pasosSlime = slimes.stream()
                .filter(slime -> !slime.descubierto())
                .filter(slime -> porId.get(slime.slime().zonaId()).desbloqueada())
                .map(slime -> new PasoSlime(slime.slime().id(), slime.slime().nombre(),
                        porId.get(slime.slime().zonaId()).zona().nombre()))
                .toList();
        return new SiguientesPasos(pasosZona, pasosSlime);
    }
}
