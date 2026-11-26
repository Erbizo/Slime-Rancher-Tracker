package com.slimetracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigBD: configuración desde variables de entorno")
class ConfigBDTest {

    private static final Map<String, String> COMPLETO = Map.of("DB_HOST", "db", "DB_PORT", "3306",
            "DB_NAME", "slime_tracker", "DB_USER", "slime_app", "DB_PASSWORD", "secreta");

    @Test
    @DisplayName("con todas las variables construye la URL JDBC")
    void completa() {
        ConfigBD config = ConfigBD.desde(COMPLETO);

        assertThat(config.urlJdbc()).isEqualTo("jdbc:mariadb://db:3306/slime_tracker");
        assertThat(config.usuario()).isEqualTo("slime_app");
    }

    @Test
    @DisplayName("si faltan variables falla al arrancar diciendo todas las que faltan")
    void faltan() {
        assertThatThrownBy(() -> ConfigBD.desde(Map.of("DB_HOST", "db", "DB_PORT", " ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PORT, DB_NAME, DB_USER, DB_PASSWORD")
                .hasMessageContaining(".env.example");
    }

    @Test
    @DisplayName("toString no enseña la contraseña")
    void sinContrasenaEnLogs() {
        assertThat(ConfigBD.desde(COMPLETO).toString()).doesNotContain("secreta").contains("slime_app");
    }
}
