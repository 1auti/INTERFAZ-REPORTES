package org.zkoss.dominial.web.viewmodel;

import org.zkoss.dominial.web.navigation.NavigationService;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Sessions;

public class MainApplicationVM {

    // Propiedades
    public static final String NAVIGATION = "navigation";
    private NavigationService navigationModel;

    // Métodos
    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        NavigationService navModel = (NavigationService) desktop.getAttribute(NAVIGATION);
        if (navModel == null) {
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
            Sessions.getCurrent().setAttribute(NAVIGATION, navigationModel);
            System.out.println("MainApplicationVM: Nuevo NavigationController creado y establecido");
        } else {
            navigationModel = navModel;
            System.out.println("MainApplicationVM: Usando NavigationController existente");
        }
    }

    @GlobalCommand
    @NotifyChange("contentUrl")
    public void refreshNavigation() {
        System.out.println("MainApplicationVM: Actualizando navegación a " + navigationModel.getContentUrl());
    }

    // Getters y Setters
    public String getContentUrl() {
        String url = navigationModel.getContentUrl();
        System.out.println("MainApplicationVM.getContentUrl(): " + url);
        return url;
    }

    public NavigationService getNavigationModel() {
        return navigationModel;
    }
}
