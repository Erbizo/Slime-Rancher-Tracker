package com.slimetracker.seguridad;

import org.mindrot.jbcrypt.BCrypt;

/** Hash y comprobación de contraseñas con BCrypt. */
public class CifradorContrasenas {

    private static final int COSTE = 10;

    public String cifrar(String contrasena) {
        return BCrypt.hashpw(contrasena, BCrypt.gensalt(COSTE));
    }

    public boolean coincide(String contrasena, String hash) {
        try {
            return BCrypt.checkpw(contrasena, hash);
        } catch (IllegalArgumentException e) {
            // Hash con formato no válido: se trata como contraseña incorrecta.
            return false;
        }
    }
}
