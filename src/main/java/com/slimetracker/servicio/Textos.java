package com.slimetracker.servicio;

import java.util.List;

/** Utilidades de texto para los mensajes de error. */
final class Textos {

    private Textos() {
    }

    /** ["A"] → "A"; ["A","B"] → "A y B"; ["A","B","C"] → "A, B y C". */
    static String enumerar(List<String> nombres) {
        if (nombres.size() == 1) {
            return nombres.get(0);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    /** Recorta los espacios de los extremos; null se queda en null. */
    static String recortar(String texto) {
        return texto == null ? null : texto.strip();
    }
}
