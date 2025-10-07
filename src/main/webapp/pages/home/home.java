package home;

import org.zkoss.reporte.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.springframework.stereotype.Component;
import static org.zkoss.reporte.web.viewmodel.MainApplicationVM.NAVIGATION;

@Component("home.home")
public class home {

    private NavigationService navigationModel;

    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        this.navigationModel = (NavigationService) desktop.getAttribute(NAVIGATION);

        if (navigationModel == null) {
            System.out.println("Creando nuevo NavigationModel en HomeVM");
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
        } else {
            System.out.println("NavigationModel recuperado correctamente en HomeVM");
        }
    }

    @AfterCompose
    public void afterCompose(){
        if (navigationModel == null) {
            System.out.println("ERROR: navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }
    }

    // Métodos @Command para navegación directa (redireccionan)
    @Command
    public void navigateDashboardHome() {
        navigateUsingService(NavigationService.DASHBOARD_PATH, "Dashboard");
    }

    @Command
    public void navigateReporte(){ navigateUsingService(NavigationService.REPORTE_PATH, "Reportes");}

    @Command
    public void navigateAdmin(){ navigateUsingService(NavigationService.ADMIN_PATH, "Admin");}

    /**
     * Método para cambiar el contenido usando NavigationService
     * @param targetPath - Ruta del contenido a mostrar
     * @param label - Etiqueta para mostrar en la ubicación
     */
    public void navigateUsingService(String targetPath, String label) {
        if (navigationModel == null) {
            System.out.println("ERROR: navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }

        try {
            System.out.println("Navegando a: " + targetPath + " - Label: " + label);

            // Cambiar el contenido y la ubicación
            navigationModel.setContentUrl(targetPath);
            navigationModel.setLocation(label);

            // Notificar cambios específicos
            BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
            BindUtils.postNotifyChange(null, null, navigationModel, "location");

            // Comando global para refrescar toda la navegación
            BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);

            System.out.println("Navegación completada a: " + targetPath);


        } catch (Exception e) {
            System.out.println("Error durante la navegación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}