package com.slimetracker.config;

import com.slimetracker.dao.LogroDao;
import com.slimetracker.dao.PartidaDao;
import com.slimetracker.dao.ProgresoDao;
import com.slimetracker.dao.SlimeDao;
import com.slimetracker.dao.UsuarioDao;
import com.slimetracker.dao.ZonaDao;
import com.slimetracker.dao.jdbc.LogroDaoJdbc;
import com.slimetracker.dao.jdbc.PartidaDaoJdbc;
import com.slimetracker.dao.jdbc.ProgresoDaoJdbc;
import com.slimetracker.dao.jdbc.SaludDaoJdbc;
import com.slimetracker.dao.jdbc.SlimeDaoJdbc;
import com.slimetracker.dao.jdbc.UsuarioDaoJdbc;
import com.slimetracker.dao.jdbc.ZonaDaoJdbc;
import com.slimetracker.seguridad.CifradorContrasenas;
import com.slimetracker.servicio.AutenticacionServicio;
import com.slimetracker.servicio.LogroServicio;
import com.slimetracker.servicio.PartidaServicio;
import com.slimetracker.servicio.ProgresoServicio;
import com.slimetracker.servicio.SaludServicio;
import com.slimetracker.servicio.SlimeServicio;
import com.slimetracker.servicio.ZonaServicio;
import com.slimetracker.tx.GestorTransacciones;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Construye a mano las piezas de la aplicación: DataSource → GestorTransacciones → DAOs → Servicios.
 * Se crea una vez al arrancar y se cierra al parar.
 */
public final class ContextoApp {

    /** Nombre del atributo del ServletContext donde se guarda. */
    public static final String ATRIBUTO = ContextoApp.class.getName();

    private final HikariDataSource dataSource;
    private final SaludServicio saludServicio;
    private final AutenticacionServicio autenticacionServicio;
    private final PartidaServicio partidaServicio;
    private final ZonaServicio zonaServicio;
    private final SlimeServicio slimeServicio;
    private final LogroServicio logroServicio;
    private final ProgresoServicio progresoServicio;

    public ContextoApp(ConfigBD config) {
        this.dataSource = ProveedorDataSource.crear(config);
        GestorTransacciones gestor = new GestorTransacciones(dataSource);

        UsuarioDao usuarioDao = new UsuarioDaoJdbc();
        PartidaDao partidaDao = new PartidaDaoJdbc();
        ZonaDao zonaDao = new ZonaDaoJdbc();
        SlimeDao slimeDao = new SlimeDaoJdbc();
        LogroDao logroDao = new LogroDaoJdbc();
        ProgresoDao progresoDao = new ProgresoDaoJdbc();

        this.saludServicio = new SaludServicio(gestor, new SaludDaoJdbc());
        this.autenticacionServicio = new AutenticacionServicio(gestor, usuarioDao, partidaDao, zonaDao,
                new CifradorContrasenas());
        this.partidaServicio = new PartidaServicio(gestor, partidaDao, zonaDao, progresoDao);
        this.zonaServicio = new ZonaServicio(gestor, zonaDao, slimeDao, partidaDao);
        this.slimeServicio = new SlimeServicio(gestor, slimeDao, zonaDao, partidaDao);
        this.logroServicio = new LogroServicio(gestor, logroDao, partidaDao);
        this.progresoServicio = new ProgresoServicio(gestor, progresoDao, zonaDao, slimeDao, partidaDao);
    }

    public SaludServicio saludServicio() {
        return saludServicio;
    }

    public AutenticacionServicio autenticacionServicio() {
        return autenticacionServicio;
    }

    public PartidaServicio partidaServicio() {
        return partidaServicio;
    }

    public ZonaServicio zonaServicio() {
        return zonaServicio;
    }

    public SlimeServicio slimeServicio() {
        return slimeServicio;
    }

    public LogroServicio logroServicio() {
        return logroServicio;
    }

    public ProgresoServicio progresoServicio() {
        return progresoServicio;
    }

    public void cerrar() {
        dataSource.close();
    }
}
