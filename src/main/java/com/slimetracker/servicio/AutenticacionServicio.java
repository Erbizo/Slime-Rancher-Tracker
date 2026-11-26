package com.slimetracker.servicio;

import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.UsuarioDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dto.LoginPeticion;
import com.slimetracker.dto.RegistroPeticion;
import com.slimetracker.dto.UsuarioRespuesta;
import com.slimetracker.excepcion.EntidadDuplicadaException;
import com.slimetracker.excepcion.NoAutenticadoException;
import com.slimetracker.excepcion.ReglaNegocioException;
import com.slimetracker.excepcion.ValidacionException;
import com.slimetracker.mapeador.UsuarioMapeador;
import com.slimetracker.modelo.Usuario;
import com.slimetracker.seguridad.CifradorContrasenas;
import com.slimetracker.tx.GestorTransacciones;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** R1 (registro) y R2 (login). */
public class AutenticacionServicio {

    static final String ERROR_USUARIO = "Entre 3 y 30 caracteres: letras, números o _.";
    static final String ERROR_CONTRASENA = "Mínimo 8 caracteres, con al menos una letra y un número.";
    static final String ERROR_CONFIRMACION = "Las contraseñas no coinciden.";
    static final String USUARIO_EN_USO = "Ese nombre de usuario ya está en uso.";
    static final String CREDENCIALES_INCORRECTAS = "Usuario o contraseña incorrectos.";
    static final String SESION_NO_VALIDA = "Tu sesión ya no es válida. Vuelve a entrar.";
    static final String NOMBRE_PRIMERA_PARTIDA = "Mi rancho";

    private static final Pattern PATRON_USUARIO = Pattern.compile("[a-zA-Z0-9_]{3,30}");
    private static final int MINIMO_CONTRASENA = 8;

    private final GestorTransacciones gestor;
    private final UsuarioDao usuarioDao;
    private final PartidaDao partidaDao;
    private final ZonaDao zonaDao;
    private final CifradorContrasenas cifrador;
    /** Se compara contra este hash cuando el usuario no existe, para tardar lo mismo que con uno real. */
    private final String hashFicticio;

    public AutenticacionServicio(GestorTransacciones gestor, UsuarioDao usuarioDao, PartidaDao partidaDao,
                                 ZonaDao zonaDao, CifradorContrasenas cifrador) {
        this.gestor = gestor;
        this.usuarioDao = usuarioDao;
        this.partidaDao = partidaDao;
        this.zonaDao = zonaDao;
        this.cifrador = cifrador;
        this.hashFicticio = cifrador.cifrar("contraseña-que-nadie-usa-1");
    }

    /** R1: valida todo a la vez y, en una transacción, crea usuario + "Mi rancho" con la zona inicial. */
    public UsuarioRespuesta registrar(RegistroPeticion peticion) {
        validarRegistro(peticion);
        String nombre = peticion.nombreUsuario();
        String hash = cifrador.cifrar(peticion.contrasena());
        try {
            return gestor.enTransaccion(conexion -> {
                if (usuarioDao.buscarPorNombre(conexion, nombre).isPresent()) {
                    throw new ReglaNegocioException(USUARIO_EN_USO);
                }
                long usuarioId = usuarioDao.insertar(conexion, nombre, hash);
                NuevaPartida.crear(partidaDao, zonaDao, conexion, usuarioId, NOMBRE_PRIMERA_PARTIDA);
                return new UsuarioRespuesta(usuarioId, nombre);
            });
        } catch (EntidadDuplicadaException e) {
            // Otro registro con el mismo nombre se ha colado entre la comprobación y el INSERT.
            throw new ReglaNegocioException(USUARIO_EN_USO);
        }
    }

    /** R2: el mismo mensaje si el usuario no existe o si la contraseña no coincide. */
    public UsuarioRespuesta entrar(LoginPeticion peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (esVacio(peticion.nombreUsuario())) {
            errores.put("nombreUsuario", "Escribe tu usuario.");
        }
        if (esVacio(peticion.contrasena())) {
            errores.put("contrasena", "Escribe tu contraseña.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
        Optional<Usuario> usuario = gestor.soloLectura(
                conexion -> usuarioDao.buscarPorNombre(conexion, peticion.nombreUsuario()));
        String hash = usuario.map(Usuario::hashContrasena).orElse(hashFicticio);
        boolean coincide = cifrador.coincide(peticion.contrasena(), hash);
        if (usuario.isEmpty() || !coincide) {
            throw new NoAutenticadoException(CREDENCIALES_INCORRECTAS);
        }
        return UsuarioMapeador.aRespuesta(usuario.get());
    }

    /** Usuario de la sesión; si lo han borrado, la sesión deja de valer. */
    public UsuarioRespuesta obtener(long usuarioId) {
        return gestor.soloLectura(conexion -> usuarioDao.buscarPorId(conexion, usuarioId))
                .map(UsuarioMapeador::aRespuesta)
                .orElseThrow(() -> new NoAutenticadoException(SESION_NO_VALIDA));
    }

    private void validarRegistro(RegistroPeticion peticion) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombre = peticion.nombreUsuario();
        if (nombre == null || !PATRON_USUARIO.matcher(nombre).matches()) {
            errores.put("nombreUsuario", ERROR_USUARIO);
        }
        String contrasena = peticion.contrasena();
        if (!contrasenaValida(contrasena)) {
            errores.put("contrasena", ERROR_CONTRASENA);
        }
        if (contrasena == null || !contrasena.equals(peticion.confirmacion())) {
            errores.put("confirmacion", ERROR_CONFIRMACION);
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
    }

    private static boolean contrasenaValida(String contrasena) {
        return contrasena != null
                && contrasena.length() >= MINIMO_CONTRASENA
                && contrasena.chars().anyMatch(Character::isLetter)
                && contrasena.chars().anyMatch(Character::isDigit);
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
