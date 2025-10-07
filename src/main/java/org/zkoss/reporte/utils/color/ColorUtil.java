package org.zkoss.reporte.utils.color;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para manejar colores en la aplicación
 */
public class ColorUtil {

    private static final List<String> COLOR_CODES = new ArrayList<>();

    static {
        // Colores utilizados en el gráfico
        COLOR_CODES.add("#5470c6"); // Azul
        COLOR_CODES.add("#fac858"); // Amarillo
        COLOR_CODES.add("#ee6666"); // Rojo
        COLOR_CODES.add("#91cc75"); // Verde
        COLOR_CODES.add("#73c0de"); // Azul claro
        COLOR_CODES.add("#fc8452"); // Naranja
        COLOR_CODES.add("#9a60b4"); // Morado
    }

    /**
     * Obtiene un color por índice (con ciclo si se excede la cantidad de colores)
     */
    public static String getColor(int index) {
        return COLOR_CODES.get(index % COLOR_CODES.size());
    }

    /**
     * Obtiene todos los colores disponibles
     */
    public static List<String> getAllColors() {
        return new ArrayList<>(COLOR_CODES);
    }
}