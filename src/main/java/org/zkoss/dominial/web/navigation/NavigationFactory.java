package org.zkoss.dominial.web.navigation;

import java.util.*;

import lombok.Getter;

/**
 * Factory para crear la estructura de menús de navegación del sistema.
 * Utiliza el patrón Factory para encapsular la lógica de construcción
 * de la jerarquía de menús.
 */
@Getter
public class NavigationFactory {
    private static final List<NavigationMenu> menuList = new ArrayList<>();
    public static List<NavigationMenu> menuInstance;

    static {
        buildMenuStructure();
    }

    /**
     * Construye la estructura completa de menús del sistema.
     */
    private static void buildMenuStructure() {
        menuList.clear();

        menuList.add(createDashboardMenu());

        menuInstance = menuList;
    }

    /**
     * Crea el menú principal del Dashboard.
     */
    private static NavigationMenu createDashboardMenu() {
        NavigationMenu menu = new NavigationMenu("Menu", "fa fa-home");
        menu.setExpanded(true);
        menu.setTitle("Datos Dominiales");

        List<NavigationMenu> subMenus = Arrays.asList(


                // 1. OPERACIÓNES (uso frecuente)
                createMenuItem("Carga de Excel", "fa fa-file-excel", NavigationService.LOADEXCEL_PATH),
                createMenuItem("Gestión de Lotes", "fa fa-boxes", NavigationService.BATCH_PATH),

                // 2. CONFIGURACIÓN
                createMenuItem("Tipo Vehiculo", "fa fa-car", NavigationService.TYPEVEHICLES_PATH),
                createMenuItem("Reglas de Datos", "fa fa-cogs", NavigationService.RULES_PATH),
                createMenuItem("Mapeo de Datos", "fa fa-map", NavigationService.MAPEO_PATH),

                // 3. CONSULTA/ANÁLISIS (consulta posterior)
                createMenuItem("Historial Actividad", "fa fa-history", NavigationService.HISTORY_PATH)
        );

        return buildMenuWithSubMenus(menu, subMenus);
    }

    /**
     * Método auxiliar para crear un elemento de menú con path.
     */
    private static NavigationMenu createMenuItem(String name, String icon, String path) {
        NavigationMenu menuItem = new NavigationMenu(name, icon);
        menuItem.setPath(path);
        return menuItem;
    }

    /**
     * Método auxiliar para construir un menú con sus submenús.
     */
    private static NavigationMenu buildMenuWithSubMenus(NavigationMenu menu, List<NavigationMenu> subMenus) {
        menu.setCounter(subMenus.size());
        menu.setSubMenus(subMenus);
        return menu;
    }

    /**
     * Retorna la lista completa de menús de navegación.
     * Retorna una copia defensiva para evitar modificaciones externas.
     *
     * @return Lista inmutable de menús de navegación
     */
    public static List<NavigationMenu> createNavigationMenus() {
        return Collections.unmodifiableList(new ArrayList<>(menuList));
    }

    /**
     * Método de compatibilidad con el código existente.
     * @deprecated Usar {@link #createNavigationMenus()} en su lugar
     */
    @Deprecated
    public static List<NavigationMenu> queryMenu() {
        return createNavigationMenus();
    }

    /**
     * Permite reconstruir los menús (útil para testing o recarga dinámica).
     */
    public static void rebuildMenus() {
        buildMenuStructure();
    }
}