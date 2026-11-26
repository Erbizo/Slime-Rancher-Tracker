package com.slimetracker.dto;

import java.time.Instant;
import java.util.Map;

/** Formato único de error de la API. campos solo se rellena en los 400 de validación. */
public record ErrorRespuesta(int status, String error, String mensaje, Map<String, String> campos, Instant timestamp) {
}
