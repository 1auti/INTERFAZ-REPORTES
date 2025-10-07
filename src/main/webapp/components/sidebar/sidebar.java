package sidebar;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.reporte.web.navigation.NavigationFactory;
import org.zkoss.reporte.web.navigation.NavigationMenu;
import org.zkoss.reporte.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;

import java.util.ArrayList;
import java.util.List;

import static org.zkoss.reporte.web.viewmodel.MainApplicationVM.NAVIGATION;

@Getter
@Setter
public class sidebar {
    private static final Logger log = LoggerFactory.getLogger(sidebar.class);
    private NavigationService navigationModel;
    private List<NavigationMenu> menuList;
    private List<NavigationMenu> dashboardMenus = new ArrayList<>();
    private List<NavigationMenu> pageMenus = new ArrayList<>();
    public String interfazTitle = "";

    // Agregar variable para tracking del menú activo
    private NavigationMenu activeMenu;

    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        this.navigationModel = (NavigationService) desktop.getAttribute(NAVIGATION);

        if (navigationModel == null) {
            System.out.println("Creando nuevo NavigationMdel en SidebarVM");
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
        } else {
            System.out.println("NavigationMdel recuperado correctamente en SidebarVM");
        }

        menuList = NavigationFactory.queryMenu();
        procesarMenus();
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

    @Command
    @NotifyChange({"activeMenu", "menuList"})
    public void navigate(@BindingParam("menu") NavigationMenu menu) {
        if (menu == null) {
            System.out.println("ERROR: Menú es NULL");
            return;
        }

        if (menu.getPath() == null || menu.getPath().isEmpty()) {
            System.out.println("ERROR: Menú sin ruta definida");
            return;
        }

        String targetPath = menu.getPath();
        System.out.println("Intentando navegar a: " + targetPath);

        if (navigationModel == null) {
            System.out.println("ERROR: navigationModel es NULL. Inicializando...");
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

            System.out.println("Navegación completada a: " + targetPath);
            System.out.println("Menú activo establecido: " + menu.getLabel());
        } catch (Exception e) {
            System.out.println("Error durante la navegación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Command
    @NotifyChange({"menuList"})
    public void toggleMenu(@BindingParam("menu") NavigationMenu menu) {
        System.out.println("Toggling menu: " + menu.getLabel() + " - Actual estado: " + menu.isExpanded());
        menu.setExpanded(!menu.isExpanded());
        System.out.println("Nuevo estado: " + menu.isExpanded());

        if (menu.getPath() != null && !menu.getPath().isEmpty() &&
                !menu.getPath().equals(NavigationService.BLANK_ZUL)) {
            navigate(menu);
        }

        if (menu.isExpanded()) {
            for (NavigationMenu m : menuList) {
                if (m != menu) {
                    boolean cambio = m.isExpanded();
                    m.setExpanded(false);
                    if (cambio) {
                        System.out.println("Colapsando menú: " + m.getLabel());
                    }
                }
            }
        }

        BindUtils.postNotifyChange(null, null, menu, "expanded");
        BindUtils.postNotifyChange(null, null, menuList, ".");
    }

    @Command
    @NotifyChange({"activeMenu"})
    public void onNavigate() {
        // Limpiar menú activo al navegar al home
        clearActiveMenu();

        navigationModel.setLocation("Dashboard");

        navigationModel.setContentUrl(NavigationService.DASHBOARD_PATH);
        BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
        BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);
        BindUtils.postNotifyChange(null, null, navigationModel, "location");
    }

    // Método helper para limpiar menú activo
    private void clearActiveMenu() {
        this.activeMenu = null;
    }

    // Método para verificar si un menú está activo
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