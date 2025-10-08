package org.zkoss.reporte.web.navigation;

import lombok.Getter;
import org.zkoss.bind.BindUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;

@Getter
public class NavigationService {

    /**
     * INICIO DE SESION - REGISTRO
     * */
    public static final String BLANK_ZUL = "/src/main/webapp/layouts/page_not_found/not_found.zul";
    private static final String PAGES_BASE = "/pages";

    /**
     * DECLARACION DE PANTALLAS
     * */
    public static final String DASHBOARD_PATH = PAGES_BASE + "/home/home.zul";
    public static final String REPORTE_PATH = PAGES_BASE + "/reporte/ejecutar_query.zul";
    public static final String ADMIN_PATH = PAGES_BASE + "/admin/admin.zul";


    /**
     * Variables
     * */
    private String contentUrl = DASHBOARD_PATH;
    private String location = "dashboard";
    private String title = "Interfaz Reporte";
    /**
     * Getters
     * */
    public String getContentUrl() {
        return contentUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        BindUtils.postNotifyChange(null, null, this, "title");

    }
    public void setLocation(String location){
        this.location = location;
        BindUtils.postNotifyChange(null, null, this, "location");
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
        title = "Interfaz Reporte";

        if (contentUrl.equals(DASHBOARD_PATH)) {
            // Solo cambiar location si no hay uno ya establecido o si es explícitamente home
            if (location == null || location.isEmpty()) {
                setLocation("Dashboard");
            }
        }

        // Actualizar la URL del navegador
        String contextPath = Executions.getCurrent().getContextPath();
        String newUrl = contextPath + "/" + title;
        Clients.evalJavaScript("history.pushState({}, '', '" + newUrl + "');");

        // Notificar el cambio
        BindUtils.postNotifyChange(null, null, this, "contentUrl");
        BindUtils.postNotifyChange(null, null, this, "location");
        BindUtils.postNotifyChange(null, null, this, "title");
    }
}