package navbar;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.reporte.web.navigation.NavigationFactory;
import org.zkoss.reporte.web.navigation.NavigationMenu;
import org.zkoss.reporte.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;

import java.util.List;

import static org.zkoss.reporte.web.viewmodel.MainApplicationVM.NAVIGATION;

@Getter @Setter
public class navbar {
    private List<NavigationMenu> menuList;
    private NavigationService navigationModel;
    private NavigationFactory navigationFactory;

    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        this.navigationModel = (NavigationService) desktop.getAttribute(NAVIGATION);

        if (navigationModel == null) {
            System.out.println("Creando nuevo NavigationModel en navbar");
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
        } else {
            System.out.println("NavigationModel recuperado correctamente en navbar");
        }

        menuList = NavigationFactory.queryMenu();
    }

    @AfterCompose
    public void afterCompose(){
        if (navigationModel == null) {
            System.out.println("ERROR: navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }
    }

    public String getLocation(){
        String currentLocation = navigationModel.getLocation();
        return currentLocation;
    }

    public String getTitle(){
        String title = navigationModel.getTitle();
        System.out.println("Obteniendo title del navbar: " + title);
        return title;
    }

    @Command("navigationToHome")
    @NotifyChange({"location", "title"})
    public void navigationToHome(){
        navigateUsingService(NavigationService.DASHBOARD_PATH, "Dashboard");
    }

    @GlobalCommand("refreshNavigation")
    @NotifyChange({"location", "title"})
    public void refreshNavigation() {
        System.out.println("Refrescando navegación - title: " + getTitle() + ", location: " + getLocation());
    }


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