import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.reporte.core.dto.response.MetadataQuery;
import org.zkoss.reporte.core.service.interfaces.DatabaseQueryService;
import org.zkoss.reporte.web.navigation.NavigationFactory;
import org.zkoss.reporte.web.navigation.NavigationMenu;
import org.zkoss.reporte.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zkoss.reporte.web.viewmodel.MainApplicationVM.NAVIGATION;

/**
 * ViewModel para el sidebar de navegación
 * Gestiona menús estáticos y dinámicos (queries)
 */
@Getter
@Setter
@Slf4j
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class sidebar {

    // ===== SERVICIOS =====
    private DatabaseQueryService databaseQueryService;
    private NavigationService navigationModel;

    // ===== DATOS DEL MENÚ =====
    private List<NavigationMenu> menuList; // Lista principal que usa la vista
    private List<MetadataQuery> queriesDisponibles = new ArrayList<>();

    // ===== UI STATE =====
    private String interfazTitle = "";
    private NavigationMenu activeMenu; // Menú actualmente activo

    // ===== INICIALIZACIÓN =====

    /**
     * Inicialización del ViewModel
     * Carga menús estáticos y queries dinámicas
     */
    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        log.info("=== Inicializando Sidebar ===");

        // 1. Inicializar servicios
        inicializarServicios(desktop);

        // 2. Cargar menús estáticos (SIEMPRE crear lista MUTABLE)
        List<NavigationMenu> menuStaticos = NavigationFactory.queryMenu();
        menuList = new ArrayList<>(menuStaticos); // ✅ Crear copia mutable
        log.info("Menús estáticos cargados: {}", menuList.size());

        // 3. Cargar queries dinámicas
        cargarQueriesDisponibles();

        // 4. Agregar menú de queries
        agregarMenuQueries();

        // 5. Log final
        log.info("=== Sidebar inicializado: {} menús totales ===", menuList.size());
        logearEstructuraMenus();
    }

    /**
     * Inicializa los servicios necesarios
     */
    private void inicializarServicios(Desktop desktop) {
        // Inicializar DatabaseQueryService
        try {
            ServletContext servletContext = Executions.getCurrent()
                    .getDesktop().getWebApp().getServletContext();
            WebApplicationContext webAppCtx =
                    WebApplicationContextUtils.getWebApplicationContext(servletContext);
            databaseQueryService = webAppCtx.getBean(DatabaseQueryService.class);
            log.info("DatabaseQueryService inicializado correctamente");
        } catch (Exception e) {
            log.error("Error inicializando DatabaseQueryService: {}", e.getMessage(), e);
        }

        // Inicializar NavigationModel
        this.navigationModel = (NavigationService) desktop.getAttribute(NAVIGATION);
        if (navigationModel == null) {
            log.info("Creando nuevo NavigationService");
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
        } else {
            log.info("NavigationService recuperado del desktop");
        }
    }

    // ===== GESTIÓN DE QUERIES =====

    /**
     * Carga las queries disponibles desde el servicio
     */
    private void cargarQueriesDisponibles() {
        try {
            if (databaseQueryService == null) {
                log.warn("DatabaseQueryService no disponible - skipping queries");
                queriesDisponibles = new ArrayList<>();
                return;
            }

            log.info("Cargando queries disponibles...");
            queriesDisponibles = databaseQueryService.traerQuerys(null);

            if (queriesDisponibles == null) {
                queriesDisponibles = new ArrayList<>();
            }

            log.info("✅ Queries cargadas: {}", queriesDisponibles.size());

        } catch (Exception e) {
            log.error("❌ Error cargando queries: {}", e.getMessage(), e);
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

        // ✅ Verificar que menuList sea mutable
        if (menuList == null) {
            menuList = new ArrayList<>();
        }

        // Crear menú principal de Queries
        NavigationMenu menuQueries = new NavigationMenu();
        menuQueries.setLabel("Consultas");
        menuQueries.setTitle("QUERIES");
        menuQueries.setIcon("fas fa-database icon-menu-color");
        menuQueries.setPath(NavigationService.BLANK_ZUL);
        menuQueries.setExpanded(false);

        // Crear submenús para cada query
        List<NavigationMenu> subMenusQueries = queriesDisponibles.stream()
                .map(this::crearSubmenuQuery)
                .collect(Collectors.toList());

        menuQueries.setSubMenus(subMenusQueries);

        // ✅ Agregar a la lista (ya es mutable por @Init)
        menuList.add(menuQueries);

        log.info("✅ Menú de queries agregado con {} submenús", subMenusQueries.size());
    }

    /**
     * Crea un submenú para una query específica
     */
    private NavigationMenu crearSubmenuQuery(MetadataQuery query) {
        NavigationMenu submenu = new NavigationMenu();
        submenu.setLabel(query.getNombre());
        submenu.setIcon(obtenerIconoQuery(query));
        submenu.setPath("/pages/reporte/ejecutar_query.zul?codigo=" + query.getCodigo());
        submenu.setQueryCodigo(query.getCodigo());
        return submenu;
    }

    /**
     * Determina el icono según el tipo/categoría de query
     */
    private String obtenerIconoQuery(MetadataQuery query) {
        // Query consolidable
        if (Boolean.TRUE.equals(query.getEsConsolidable())) {
            return "fas fa-layer-group icon-submenu-color";
        }

        // Iconos según categoría
        if (query.getCategoria() != null) {
            switch (query.getCategoria().toUpperCase()) {
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

    /**
     * Recarga las queries dinámicamente (útil para desarrollo)
     */
    @Command
    @NotifyChange({"menuList", "queriesDisponibles"})
    public void recargarQueries() {
        log.info("🔄 Recargando queries...");

        // Remover menú de queries anterior
        menuList.removeIf(menu -> "QUERIES".equals(menu.getTitle()));

        // Cargar queries nuevamente
        cargarQueriesDisponibles();
        agregarMenuQueries();

        log.info("✅ Queries recargadas - Total menús: {}", menuList.size());
        logearEstructuraMenus();
    }

    // ===== COMANDOS DE NAVEGACIÓN =====

    /**
     * Navega a un menú específico
     */
    @Command
    @NotifyChange({"activeMenu", "menuList"})
    public void navigate(@BindingParam("menu") NavigationMenu menu) {
        if (menu == null) {
            log.error("❌ Menú es NULL");
            return;
        }

        if (menu.getPath() == null || menu.getPath().isEmpty()) {
            log.error("❌ Menú sin ruta definida: {}", menu.getLabel());
            return;
        }

        String targetPath = menu.getPath();
        log.info("🔗 Navegando a: {} ({})", menu.getLabel(), targetPath);

        // Verificar navigationModel
        if (navigationModel == null) {
            log.error("❌ navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }

        try {
            // Limpiar menú activo anterior
            clearActiveMenu();

            // Establecer nuevo menú activo
            this.activeMenu = menu;

            // Actualizar navegación
            navigationModel.setContentUrl(targetPath);
            navigationModel.setLocation(menu.getLabel());

            // Notificar cambios
            BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
            BindUtils.postNotifyChange(null, null, navigationModel, "location");
            BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);

            log.info("✅ Navegación completada - Menú activo: {}", menu.getLabel());

        } catch (Exception e) {
            log.error("❌ Error durante la navegación: {}", e.getMessage(), e);
        }
    }

    /**
     * Expande/colapsa un menú con submenús
     */
    @Command
    @NotifyChange({"menuList"})
    public void toggleMenu(@BindingParam("menu") NavigationMenu menu) {
        log.debug("🔄 Toggling menu: {} - Estado actual: {}",
                menu.getLabel(), menu.isExpanded());

        // Toggle estado
        menu.setExpanded(!menu.isExpanded());
        log.debug("Nuevo estado: {}", menu.isExpanded());

        // Si el menú tiene path propio, navegar
        if (menu.getPath() != null && !menu.getPath().isEmpty() &&
                !menu.getPath().equals(NavigationService.BLANK_ZUL)) {
            navigate(menu);
        }

        // Colapsar otros menús (accordion behavior)
        if (menu.isExpanded()) {
            menuList.stream()
                    .filter(m -> m != menu && m.isExpanded())
                    .forEach(m -> {
                        m.setExpanded(false);
                        log.debug("Colapsando menú: {}", m.getLabel());
                    });
        }

        // Notificar cambios
        BindUtils.postNotifyChange(null, null, menu, "expanded");
        BindUtils.postNotifyChange(null, null, menuList, ".");
    }

    /**
     * Navega al home/dashboard
     */
    @Command
    @NotifyChange({"activeMenu"})
    public void onNavigate() {
        log.info("🏠 Navegando al home/dashboard");

        clearActiveMenu();

        navigationModel.setLocation("Dashboard");
        navigationModel.setContentUrl(NavigationService.DASHBOARD_PATH);

        BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
        BindUtils.postNotifyChange(null, null, navigationModel, "location");
        BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);
    }

    // ===== MÉTODOS HELPER =====

    /**
     * Limpia el menú activo actual
     */
    private void clearActiveMenu() {
        this.activeMenu = null;
    }

    /**
     * Verifica si un menú está activo
     */
    public boolean isMenuActive(NavigationMenu menu) {
        return activeMenu != null && activeMenu.equals(menu);
    }

    /**
     * ✅ CRÍTICO: Retorna la lista principal de menús
     * Este método es llamado por ZK cuando se hace @load(vm.menuList)
     */
    public List<NavigationMenu> getMenuList() {
        if (menuList == null) {
            menuList = new ArrayList<>();
        }
        return menuList;
    }

    /**
     * Loguea la estructura completa de menús para debugging
     */
    private void logearEstructuraMenus() {
        log.info("=== ESTRUCTURA DE MENÚS ===");
        if (menuList == null || menuList.isEmpty()) {
            log.warn("⚠️ menuList está vacío!");
            return;
        }

        for (int i = 0; i < menuList.size(); i++) {
            NavigationMenu menu = menuList.get(i);
            log.info("  [{}] {} ({})", i, menu.getLabel(),
                    menu.getTitle() != null ? menu.getTitle() : "sin título");

            if (menu.getSubMenus() != null && !menu.getSubMenus().isEmpty()) {
                log.info("      └─ {} submenús", menu.getSubMenus().size());
                for (NavigationMenu sub : menu.getSubMenus()) {
                    log.info("         • {}", sub.getLabel());
                }
            }
        }
        log.info("=== FIN ESTRUCTURA (Total: {}) ===", menuList.size());
    }
}