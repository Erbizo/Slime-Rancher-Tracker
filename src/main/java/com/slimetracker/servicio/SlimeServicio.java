package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.DescubrirVariosPeticion;
import com.slimetracker.dto.NotaPeticion;
import com.slimetracker.dto.SlimeEstadoRespuesta;
import com.slimetracker.dto.SlimeRespuesta;
import com.slimetracker.excepcion.NoEncontradoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.mapeador.SlimeMapeador;
import com.slimetracker.modelo.Dieta;
import com.slimetracker.modelo.Rareza;
import com.slimetracker.modelo.Slime;
import com.slimetracker.modelo.SlimeConEstado;
import com.slimetracker.modelo.Zona;
import com.slimetracker.modelo.ZonaConEstado;
import com.slimetracker.tx.GestorTransacciones;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** R6 (descubrir), R7 (descubrir varios) y notas de la Slimepedia. */
public class SlimeServicio {

    static final String SLIME_NO_EXISTE = "El slime no existe.";
    static final String ERROR_LISTA = "Indica al menos un slime, sin repetir.";

    private final GestorTransacciones gestor;
    private final SlimeDao slimeDao;
    private final ZonaDao zonaDao;
    private final PartidaDao partidaDao;

    public SlimeServicio(GestorTransacciones gestor, SlimeDao slimeDao, ZonaDao zonaDao, PartidaDao partidaDao) {
        this.gestor = gestor;
        this.slimeDao = slimeDao;
        this.zonaDao = zonaDao;
        this.partidaDao = partidaDao;
    }

    /** Catálogo con filtros opcionales. dieta y rareza tienen que ser valores conocidos. */
    public List<SlimeRespuesta> listarCatalogo(Long zonaId, String dieta, String rareza) {
        Map<String, String> errores = new LinkedHashMap<>();
        Dieta filtroDieta = convertir(Dieta.class, dieta, "dieta", errores);
        Rareza filtroRareza = convertir(Rareza.class, rareza, "rareza", errores);
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
        return gestor.soloLectura(conexion -> {
            Map<Long, String> nombresZona = zonaDao.listar(conexion).stream()
                    .collect(Collectors.toMap(Zona::id, Zona::nombre));
            return slimeDao.listar(conexion, zonaId, filtroDieta, filtroRareza).stream()
                    .map(slime -> SlimeMapeador.aRespuesta(slime, nombresZona.get(slime.zonaId())))
                    .toList();
        });
    }

    /** Slimepedia de la partida, con filtros opcionales de zona y de estado. */
    public List<SlimeEstadoRespuesta> listarDePartida(long usuarioId, long partidaId, Long zonaId,
                                                      Boolean descubierto) {
        return gestor.soloLectura(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            Map<Long, ZonaConEstado> zonas = zonasPorId(conexion, partidaId);
            if (zonaId != null && !zonas.containsKey(zonaId)) {
                throw new NoEncontradoException(ZonaServicio.ZONA_NO_EXISTE);
            }
            return slimeDao.listarConEstado(conexion, partidaId).stream()
                    .filter(estado -> zonaId == null || estado.slime().zonaId().equals(zonaId))
                    .filter(estado -> descubierto == null || estado.descubierto() == descubierto)
                    .map(estado -> aEstado(estado, zonas))
                    .toList();
        });
    }

    /** R6: la zona del slime está desbloqueada y el slime no estaba ya descubierto. */
    public SlimeEstadoRespuesta descubrir(long usuarioId, long partidaId, long slimeId) {
        return gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            descubrirUno(conexion, partidaId, slimeId);
            partidaDao.tocar(conexion, partidaId);
            return estadoDe(conexion, partidaId, slimeId);
        });
    }

    /**
     * R7: todo o nada. Una sola transacción; si un slime no cumple R6, la excepción
     * (que dice cuál y por qué) hace rollback y no se guarda ninguno.
     */
    public List<SlimeEstadoRespuesta> descubrirVarios(long usuarioId, long partidaId,
                                                      DescubrirVariosPeticion peticion) {
        List<Long> ids = peticion.slimeIds();
        // anyMatch en vez de contains(null): contains lanza NullPointerException con listas inmutables (List.of).
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) {
            throw ValidacionException.deCampo("slimeIds", ERROR_LISTA);
        }
        return gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            for (long slimeId : ids) {
                descubrirUno(conexion, partidaId, slimeId);
            }
            partidaDao.tocar(conexion, partidaId);
            List<SlimeEstadoRespuesta> resultado = new ArrayList<>();
            for (long slimeId : ids) {
                resultado.add(estadoDe(conexion, partidaId, slimeId));
            }
            return resultado;
        });
    }

    public SlimeEstadoRespuesta actualizarNota(long usuarioId, long partidaId, long slimeId, NotaPeticion peticion) {
        String nota = ValidacionNota.validar(peticion);
        return gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            Slime slime = buscarSlime(conexion, slimeId);
            if (slimeDao.actualizarNota(conexion, partidaId, slimeId, nota) == 0) {
                throw new ReglaNegocioException("Aún no has descubierto al " + slime.nombre()
                        + ": solo se pueden anotar slimes descubiertos.");
            }
            partidaDao.tocar(conexion, partidaId);
            return estadoDe(conexion, partidaId, slimeId);
        });
    }

    /** Quita el slime de la Slimepedia de la partida. */
    public void olvidar(long usuarioId, long partidaId, long slimeId) {
        gestor.enTransaccion(conexion -> {
            PropiedadPartida.comprobar(partidaDao, conexion, usuarioId, partidaId);
            Slime slime = buscarSlime(conexion, slimeId);
            if (slimeDao.olvidar(conexion, partidaId, slimeId) == 0) {
                throw new ReglaNegocioException("No tenías al " + slime.nombre() + " en la Slimepedia.");
            }
            partidaDao.tocar(conexion, partidaId);
            return null;
        });
    }

    private void descubrirUno(Connection conexion, long partidaId, long slimeId) {
        Slime slime = slimeDao.buscarPorId(conexion, slimeId)
                .orElseThrow(() -> new NoEncontradoException("El slime " + slimeId + " no existe."));
        if (!zonaDao.estaDesbloqueada(conexion, partidaId, slime.zonaId())) {
            String zona = zonaDao.buscarPorId(conexion, slime.zonaId()).map(Zona::nombre).orElse("su zona");
            throw new ReglaNegocioException("Para descubrir al " + slime.nombre()
                    + " primero tienes que desbloquear " + zona + ".");
        }
        if (slimeDao.estaDescubierto(conexion, partidaId, slimeId)) {
            throw new ReglaNegocioException("Ya habías descubierto al " + slime.nombre() + ".");
        }
        slimeDao.descubrir(conexion, partidaId, slimeId);
    }

    private Slime buscarSlime(Connection conexion, long slimeId) {
        return slimeDao.buscarPorId(conexion, slimeId).orElseThrow(() -> new NoEncontradoException(SLIME_NO_EXISTE));
    }

    private SlimeEstadoRespuesta estadoDe(Connection conexion, long partidaId, long slimeId) {
        SlimeConEstado estado = slimeDao.buscarConEstado(conexion, partidaId, slimeId)
                .orElseThrow(() -> new NoEncontradoException(SLIME_NO_EXISTE));
        return aEstado(estado, zonasPorId(conexion, partidaId));
    }

    private Map<Long, ZonaConEstado> zonasPorId(Connection conexion, long partidaId) {
        return zonaDao.listarConEstado(conexion, partidaId).stream()
                .collect(Collectors.toMap(estado -> estado.zona().id(), Function.identity()));
    }

    private static SlimeEstadoRespuesta aEstado(SlimeConEstado estado, Map<Long, ZonaConEstado> zonas) {
        ZonaConEstado zona = zonas.get(estado.slime().zonaId());
        return SlimeMapeador.aEstado(estado, zona.zona().nombre(), zona.desbloqueada());
    }

    private static <E extends Enum<E>> E convertir(Class<E> tipo, String valor, String campo,
                                                   Map<String, String> errores) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            String validos = Arrays.stream(tipo.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            errores.put(campo, "Valor no válido. Usa uno de estos: " + validos + ".");
            return null;
        }
    }
}
