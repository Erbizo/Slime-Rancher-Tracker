package com.slimetracker.dao;

import com.slimetracker.modelo.Usuario;

import java.sql.Connection;
import java.util.Optional;

public interface UsuarioDao {

    /** Inserta el usuario y devuelve su id generado. */
    long insertar(Connection conexion, String nombreUsuario, String hashContrasena);

    Optional<Usuario> buscarPorId(Connection conexion, long id);

    Optional<Usuario> buscarPorNombre(Connection conexion, String nombreUsuario);
}
