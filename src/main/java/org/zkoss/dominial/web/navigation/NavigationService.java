package org.zkoss.dominial.web.navigation;

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
    public static final String BATCH_PATH = PAGES_BASE + "/batch/batch.zul";
    public static final String HISTORY_PATH = PAGES_BASE + "/history/history.zul";
    public static final String LOADEXCEL_PATH = PAGES_BASE + "/loadExcel/loadExcel.zul";
    public static final String MAPEO_PATH = PAGES_BASE + "/mapeo/mapeo.zul";
    public static final String RULES_PATH = PAGES_BASE + "/rules/rules.zul";
    public static final String TYPEVEHICLES_PATH = PAGES_BASE + "/typeVehicles/typeVehicle.zul";

    /**
     * DECLARACION DE SUBPANTALLAS
     * */
    public static final String DETALLE_BATCH_PATH = PAGES_BASE + "/detalleLote/detalleLote.zul";
    public static final String DETALLE_BATCH_RECHAZADOS_PATH = PAGES_BASE + "/detalleLoteRechazados/detalleLoteRechazados.zul";
    public static final String DETALLE_BATCH_VERIFICADOS_PATH = PAGES_BASE + "/detalleLoteVerificados/detalleLoteVerificados.zul";

    /**
     * Variables
     * */
    private String contentUrl = DASHBOARD_PATH;
    private String location = "dashboard";
    private String title = "Datos Dominiales";
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
        title = "Datos Dominiales";

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