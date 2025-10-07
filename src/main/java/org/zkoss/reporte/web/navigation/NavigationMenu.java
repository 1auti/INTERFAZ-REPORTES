package org.zkoss.reporte.web.navigation;

import lombok.Data;
import java.util.List;

@Data
public class NavigationMenu {
    private String label;
    private String title;
    private String icon;
    private String path;
    private Integer counter;
    private boolean expanded;
    private List<NavigationMenu> subMenus;

    // ===== NUEVO =====
    private String queryCodigo; // Código de la query asociada

    public NavigationMenu() {
        this.expanded = false;
    }

    // Constructor con parámetros si lo necesitas
    public NavigationMenu(String label, String icon, String path) {
        this.label = label;
        this.icon = icon;
        this.path = path;
        this.expanded = false;
    }

    public NavigationMenu(String label,String icon){
        this.label = label;
        this.icon = icon;
    }
}