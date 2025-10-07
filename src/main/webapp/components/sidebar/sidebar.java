import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.reporte.core.dto.response.MetadataQueryRegistro;
import org.zkoss.reporte.core.service.interfaces.DatabaseQueryService;
import org.zkoss.reporte.web.navigation.NavigationFactory;
import org.zkoss.reporte.web.navigation.NavigationMenu;
import org.zkoss.reporte.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.spring.SpringUtil;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;

import static org.zkoss.reporte.web.viewmodel.MainApplicationVM.NAVIGATION;

@Getter
@Setter
@Slf4j
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class sidebar {


    private DatabaseQueryService databaseQueryService;

    private NavigationService navigationModel;
    private List<NavigationMenu> menuList;
    private List<NavigationMenu> dashboardMenus = new ArrayList<>();
    private List<NavigationMenu> pageMenus = new ArrayList<>();
    private List<MetadataQueryRegistro> queriesDisponibles = new ArrayList<>();

    public String interfazTitle = "";

    // Variable para tracking del menú activo
    private NavigationMenu activeMenu;

    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        log.info("Inicializando Sidebar");

//        databaseQueryService = (DatabaseQueryService) SpringUtil.getBean("DatabaseQueryServiceImpl");
        ServletContext servletContext = Executions.getCurrent().getDesktop().getWebApp().getServletContext();
        WebApplicationContext webAppCtx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        databaseQueryService = webAppCtx.getBean(DatabaseQueryService.class);

        this.navigationModel = (NavigationService) desktop.getAttribute(NAVIGATION);

        if (navigationModel == null) {
            log.info("Creando nuevo NavigationModel en SidebarVM");
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
        } else {
            log.info("NavigationModel recuperado correctamente en SidebarVM");
        }

        // Cargar menús estáticos
        menuList = NavigationFactory.queryMenu();
        // Cargar queries dinámicas
        cargarQueriesDisponibles();

        // Agregar menú de queries al menuList
        agregarMenuQueries();
    }

    private void procesarMenus() {
        if (menuList == null) {
            return;
        }

        for (NavigationMenu menu : menuList) {
            if (menu.getTitle() != null && menu.getTitle().contains("Dashboard")) {
                dashboardMenus.add(menu);
            } else {
                pageMenus.add(menu);
            }
        }
    }

    /**
     * Carga las queries disponibles desde el servicio
     */
    private void cargarQueriesDisponibles() {
        try {
            if (databaseQueryService != null) {
                log.info("Cargando queries disponibles...");

                // Obtener todas las queries activas
                queriesDisponibles = databaseQueryService.traerQuerys(null);

                log.info("Queries cargadas: {}", queriesDisponibles.size());
            } else {
                log.warn("DatabaseQueryService no disponible");
                queriesDisponibles = new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error cargando queries: {}", e.getMessage(), e);
            queriesDisponibles = new ArrayList<>();
        }
    }

    /**
     * Agrega el menú de queries dinámicamente al menuList
     */
    private void agregarMenuQueries() {
        if (queriesDisponibles == null || queriesDisponibles.isEmpty()) {
            log.info("No hay queries para agregar al menú");
            return;
        }

        // Crear menú principal de Queries
        NavigationMenu menuQueries = new NavigationMenu();
        menuQueries.setLabel("Consultas");
        menuQueries.setTitle("QUERIES");
        menuQueries.setIcon("fas fa-database icon-menu-color");
        menuQueries.setPath(NavigationService.BLANK_ZUL); // No tiene path propio
        menuQueries.setExpanded(false);

        // Crear submenús para cada query
        List<NavigationMenu> subMenusQueries = new ArrayList<>();

        for (MetadataQueryRegistro query : queriesDisponibles) {
            NavigationMenu submenu = new NavigationMenu();
            submenu.setLabel(query.getNombre());
            submenu.setIcon(obtenerIconoQuery(query));
            submenu.setPath("/pages/reportes/ejecutar_query.zul?codigo=" + query.getCodigo());
            submenu.setQueryCodigo(query.getCodigo()); // Guardar código para referencia

            subMenusQueries.add(submenu);
        }

        menuQueries.setSubMenus(subMenusQueries);

        if (menuList == null) {
            menuList = new ArrayList<>();
        } else {
            // Si menuList es inmutable, crear una nueva lista mutable
            try {
                menuList.add(null); // Prueba si es mutable
                menuList.remove(menuList.size() - 1); // Limpiar la prueba
            } catch (UnsupportedOperationException e) {
                // Es inmutable, crear nueva lista mutable
                menuList = new ArrayList<>(menuList);
            }
        }

        menuList.add(menuQueries);

        log.info("Menú de queries agregado con {} submenús", subMenusQueries.size());
    }

    /**
     * Determina el icono según el tipo de query
     */
    private String obtenerIconoQuery(MetadataQueryRegistro query) {
        if (query.getEsConsolidable() != null && query.getEsConsolidable()) {
            return "fas fa-layer-group icon-submenu-color";
        }

        // Iconos según categoría
        if (query.getCatergoria() != null) {
            switch (query.getCatergoria().toUpperCase()) {
                case "INFRACCIONES":
                    return "fas fa-exclamation-triangle icon-submenu-color";
                case "VEHICULOS":
                    return "fas fa-car icon-submenu-color";
                case "ESTADISTICAS":
                    return "fas fa-chart-bar icon-submenu-color";
                case "REPORTES":
                    return "fas fa-file-alt icon-submenu-color";
                default:
                    return "fas fa-file-alt icon-submenu-color";
            }
        }

        return "fas fa-file-alt icon-submenu-color";
    }

    @Command
    @NotifyChange({"activeMenu", "menuList"})
    public void navigate(@BindingParam("menu") NavigationMenu menu) {
        if (menu == null) {
            log.error("ERROR: Menú es NULL");
            return;
        }

        if (menu.getPath() == null || menu.getPath().isEmpty()) {
            log.error("ERROR: Menú sin ruta definida");
            return;
        }

        String targetPath = menu.getPath();
        log.info("Navegando a: {}", targetPath);

        if (navigationModel == null) {
            log.error("ERROR: navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }

        try {
            // Limpiar menú activo anterior
            clearActiveMenu();

            // Establecer nuevo menú activo
            this.activeMenu = menu;

            navigationModel.setContentUrl(targetPath);
            navigationModel.setLocation(menu.getLabel());

            // Notificar cambios específicos
            BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
            BindUtils.postNotifyChange(null, null, navigationModel, "location");

            // Comando global para refrescar toda la navegación
            BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);

            log.info("Navegación completada - Menú activo: {}", menu.getLabel());
        } catch (Exception e) {
            log.error("Error durante la navegación: {}", e.getMessage(), e);
        }
    }

    @Command
    @NotifyChange({"menuList"})
    public void toggleMenu(@BindingParam("menu") NavigationMenu menu) {
        log.debug("Toggling menu: {} - Estado: {}", menu.getLabel(), menu.isExpanded());

        menu.setExpanded(!menu.isExpanded());
        log.debug("Nuevo estado: {}", menu.isExpanded());

        // Si el menú tiene path propio, navegar
        if (menu.getPath() != null && !menu.getPath().isEmpty() &&
                !menu.getPath().equals(NavigationService.BLANK_ZUL)) {
            navigate(menu);
        }

        // Colapsar otros menús si este se expande
        if (menu.isExpanded()) {
            for (NavigationMenu m : menuList) {
                if (m != menu && m.isExpanded()) {
                    m.setExpanded(false);
                    log.debug("Colapsando menú: {}", m.getLabel());
                }
            }
        }

        BindUtils.postNotifyChange(null, null, menu, "expanded");
        BindUtils.postNotifyChange(null, null, menuList, ".");
    }

    @Command
    @NotifyChange({"activeMenu"})
    public void onNavigate() {
        log.info("Navegando al home/dashboard");

        // Limpiar menú activo
        clearActiveMenu();

        navigationModel.setLocation("Dashboard");
        navigationModel.setContentUrl(NavigationService.DASHBOARD_PATH);

        BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
        BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);
        BindUtils.postNotifyChange(null, null, navigationModel, "location");
    }

    /**
     * Recargar queries dinámicamente
     */
    @Command
    @NotifyChange({"menuList", "queriesDisponibles"})
    public void recargarQueries() {
        log.info("Recargando queries...");

        // Remover menú de queries anterior
        menuList.removeIf(menu -> "QUERIES".equals(menu.getTitle()));

        // Cargar queries nuevamente
        cargarQueriesDisponibles();
        agregarMenuQueries();

        log.info("Queries recargadas exitosamente");
    }

    // ===== MÉTODOS HELPER =====

    private void clearActiveMenu() {
        this.activeMenu = null;
    }

    public boolean isMenuActive(NavigationMenu menu) {
        return activeMenu != null && activeMenu.equals(menu);
    }

    public List<NavigationMenu> getDashboardMenus() {
        return dashboardMenus;
    }

    public List<NavigationMenu> getPageMenus() {
        return pageMenus;
    }

    public List<NavigationMenu> getMenuList() {
        if (menuList != null) {
            return menuList;
        }

        List<NavigationMenu> allMenus = new ArrayList<>();
        if (dashboardMenus != null) {
            allMenus.addAll(dashboardMenus);
        }
        if (pageMenus != null) {
            allMenus.addAll(pageMenus);
        }
        return allMenus;
    }
}