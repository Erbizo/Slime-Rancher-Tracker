package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.ZonaEstadoRespuesta;
import com.slimetracker.dto.ZonaRespuesta;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.mapeador.ZonaMapeador;
import com.slimetracker.modelo.Zona;
import com.slimetracker.modelo.ZonaConEstado;
import com.slimetracker.tx.GestorTransacciones;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** R4 (desbloquear) y R5 (bloquear) zonas. */
public class ZonaServicio {

    static final String ZONA_NO_EXISTE = "La zona no existe.";

    private final GestorTransacciones gestor;
    private final ZonaDao zonaDao;
    private final SlimeDao slimeDao;
    private final PartidaDao partidaDao;

    public ZonaServicio(GestorTransacciones gestor, ZonaDao zonaDao, SlimeDao slimeDao, PartidaDao partidaDao) {
        this.gestor = gestor;
        this.zonaDao = zonaDao;
        this.slimeDao = slimeDao;
        this.partidaDao = partidaDao;
    }

    public List<ZonaRespuesta> listarCatalogo() {
        return gestor.soloLectura(conexion -> zonaDao.listar(conexion).stream()
                .map(ZonaMapeador::aRespuesta)
                .toList());
    }

    public List<ZonaEstadoRespuesta> listarDePartida(long usuarioId, long partidaId) {
        return gestor.soloLectura(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            return listarEstados(conexion, partidaId);
        });
    }

    /** R4: la zona existe, no está ya desbloqueada y su requisito sí lo está. */
    public ZonaEstadoRespuesta desbloquear(long usuarioId, long partidaId, long zonaId) {
        return gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            Zona zona = buscarZona(conexion, zonaId);
            if (zonaDao.estaDesbloqueada(conexion, partidaId, zonaId)) {
                throw new ReglaNegocioException(zona.nombre() + " ya está desbloqueada.");
            }
            Long requisitoId = zona.zonaRequisitoId();
            if (requisitoId != null && !zonaDao.estaDesbloqueada(conexion, partidaId, requisitoId)) {
                Zona requisito = buscarZona(conexion, requisitoId);
                throw new ReglaNegocioException("Primero tienes que desbloquear " + requisito.nombre() + ".");
            }
            zonaDao.desbloquear(conexion, partidaId, zonaId);
            partidaDao.tocar(conexion, partidaId);
            return listarEstados(conexion, partidaId).stream()
                    .filter(estado -> estado.id() == zonaId)
                    .findFirst()
                    .orElseThrow();
        });
    }

    /**
     * R5: no se puede bloquear la zona inicial, ni una de la que dependan zonas desbloqueadas,
     * ni una con slimes descubiertos.
     */
    public void bloquear(long usuarioId, long partidaId, long zonaId) {
        gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            Zona zona = buscarZona(conexion, zonaId);
            if (zona.esInicial()) {
                throw new ReglaNegocioException(zona.nombre() + " es la zona inicial y no se puede bloquear.");
            }
            if (!zonaDao.estaDesbloqueada(conexion, partidaId, zonaId)) {
                throw new ReglaNegocioException(zona.nombre() + " ya está bloqueada.");
            }
            List<String> dependientes = zonaDao.listarDependientesDesbloqueadas(conexion, partidaId, zonaId)
                    .stream().map(Zona::nombre).toList();
            if (!dependientes.isEmpty()) {
                String verbo = dependientes.size() == 1 ? " depende de " : " dependen de ";
                throw new ReglaNegocioException(Textos.enumerar(dependientes) + verbo + zona.nombre() + ".");
            }
            List<String> descubiertos = slimeDao.listarDescubiertosEnZona(conexion, partidaId, zonaId);
            if (!descubiertos.isEmpty()) {
                throw new ReglaNegocioException("Tienes en la Slimepedia slimes de " + zona.nombre() + ": "
                        + Textos.enumerar(descubiertos) + ".");
            }
            zonaDao.bloquear(conexion, partidaId, zonaId);
            partidaDao.tocar(conexion, partidaId);
            return null;
        });
    }

    private Zona buscarZona(Connection conexion, long zonaId) {
        return zonaDao.buscarPorId(conexion, zonaId).orElseThrow(() -> new NoEncontradoException(ZONA_NO_EXISTE));
    }

    private List<ZonaEstadoRespuesta> listarEstados(Connection conexion, long partidaId) {
        List<ZonaConEstado> zonas = zonaDao.listarConEstado(conexion, partidaId);
        Map<Long, ZonaConEstado> porId = zonas.stream()
                .collect(Collectors.toMap(estado -> estado.zona().id(), Function.identity()));
        return zonas.stream().map(estado -> {
            ZonaConEstado requisito = porId.get(estado.zona().zonaRequisitoId());
            boolean requisitoAbierto = requisito == null || requisito.desbloqueada();
            return ZonaMapeador.aEstado(estado, requisito == null ? null : requisito.zona().nombre(),
                    !estado.desbloqueada() && requisitoAbierto);
        }).toList();
    }
}
