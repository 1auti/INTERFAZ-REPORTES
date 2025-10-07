package home;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.reporte.core.model.ActividadItem;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.reporte.core.dto.response.MetadataQueryRegistro;
import org.zkoss.reporte.core.service.interfaces.DatabaseQueryService;
import org.zkoss.reporte.web.navigation.NavigationService;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.zkoss.reporte.web.viewmodel.MainApplicationVM.NAVIGATION;

@Component("home.home")
public class home {

    private NavigationService navigationModel;

    @Autowired(required = false)
    private DatabaseQueryService databaseQueryService;

    // ===== PROPIEDADES PARA ESTADÍSTICAS =====

    @Getter @Setter
    private boolean showStats = true;

    @Getter @Setter
    private String totalQueries = "0";

    @Getter @Setter
    private String queriesConsolidables = "0";

    @Getter @Setter
    private String totalReportesGenerados = "0";

    @Getter @Setter
    private String reportesHoy = "0";

    @Getter @Setter
    private String ultimaEjecucion = "N/A";

    // ===== ACTIVIDAD RECIENTE =====

    @Getter @Setter
    private List<ActividadItem> actividadReciente = new ArrayList<>();

    // ===== QUERIES POPULARES =====

    @Getter @Setter
    private List<MetadataQueryRegistro> queriesPopulares = new ArrayList<>();

    // ===== INICIALIZACIÓN =====

    @Init
    public void init(@ContextParam(ContextType.DESKTOP) Desktop desktop) {
        System.out.println("Inicializando Home Reportes");

        this.navigationModel = (NavigationService) desktop.getAttribute(NAVIGATION);

        if (navigationModel == null) {
            System.out.println("Creando nuevo NavigationModel en Home");
            navigationModel = new NavigationService();
            desktop.setAttribute(NAVIGATION, navigationModel);
        }

        // Cargar datos
        cargarEstadisticas();
        cargarActividadReciente();
        cargarQueriesPopulares();
    }

    @AfterCompose
    public void afterCompose() {
        if (navigationModel == null) {
            System.out.println("ERROR: navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }
    }

    // ===== MÉTODOS PARA CARGAR DATOS =====

    private void cargarEstadisticas() {
        try {
            if (databaseQueryService != null) {
                // Obtener estadísticas del servicio
                Map<String, Object> stats = databaseQueryService.obtenerEstadisticas();

                if (stats != null) {
                    totalQueries = String.valueOf(stats.getOrDefault("total_queries_activas", 0));
                    queriesConsolidables = String.valueOf(stats.getOrDefault("queries_consolidables", 0));

                    // Calcular porcentaje si quieres mostrarlo
                    Object porcentaje = stats.get("porcentaje_consolidables");
                    if (porcentaje != null) {
                        System.out.println("Porcentaje consolidables: " + porcentaje);
                    }
                }
            } else {
                // Datos de ejemplo si el servicio no está disponible
                totalQueries = "12";
                queriesConsolidables = "8";
                reportesHoy = "5";
            }

            // Última ejecución
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            ultimaEjecucion = sdf.format(new Date());

            System.out.println("Estadísticas cargadas - Queries: " + totalQueries +
                    ", Consolidables: " + queriesConsolidables);

        } catch (Exception e) {
            System.out.println("Error cargando estadísticas: " + e.getMessage());
            e.printStackTrace();
            showStats = false;

            // Valores por defecto en caso de error
            totalQueries = "0";
            queriesConsolidables = "0";
            reportesHoy = "0";
        }
    }

    private void cargarActividadReciente() {
        try {
            actividadReciente = new ArrayList<>();

            if (databaseQueryService != null) {
                // Aquí puedes obtener la actividad real del servicio
                // Por ahora usamos datos de ejemplo

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                Date ahora = new Date();

                actividadReciente.add(new ActividadItem(
                        "Infracciones por Municipio",
                        sdf.format(new Date(ahora.getTime() - 3600000)), // Hace 1 hora
                        "1,250"
                ));

                actividadReciente.add(new ActividadItem(
                        "Consolidado Provincial",
                        sdf.format(new Date(ahora.getTime() - 7200000)), // Hace 2 horas
                        "850"
                ));

                actividadReciente.add(new ActividadItem(
                        "Reporte Mensual",
                        sdf.format(new Date(ahora.getTime() - 10800000)), // Hace 3 horas
                        "3,420"
                ));
            }

            System.out.println("Actividad reciente cargada: " + actividadReciente.size() + " items");

        } catch (Exception e) {
            System.out.println("Error cargando actividad reciente: " + e.getMessage());
            actividadReciente = new ArrayList<>();
        }
    }

    private void cargarQueriesPopulares() {
        try {
            if (databaseQueryService != null) {
                // Obtener queries más populares (limitado a 5)
                queriesPopulares = databaseQueryService.obtenerQueriesPopulares(5);

                System.out.println("Queries populares cargadas: " + queriesPopulares.size());
            } else {
                queriesPopulares = new ArrayList<>();
            }

        } catch (Exception e) {
            System.out.println("Error cargando queries populares: " + e.getMessage());
            queriesPopulares = new ArrayList<>();
        }
    }

    // ===== COMANDOS DE NAVEGACIÓN =====

    @Command
    @NotifyChange("*")
    public void navegarNuevaConsulta() {
        navigateUsingService("/pages/reportes/nueva_consulta.zul", "Nueva Consulta");
    }

    @Command
    @NotifyChange("*")
    public void navegarUltimaConsulta() {
        navigateUsingService("/pages/reportes/ultima_consulta.zul", "Última Consulta");
    }

    @Command
    @NotifyChange("*")
    public void navegarExportar() {
        navigateUsingService("/pages/reportes/exportar.zul", "Exportar Datos");
    }

    @Command
    @NotifyChange("*")
    public void navegarDashboard() {
        navigateUsingService("/pages/reportes/dashboard.zul", "Dashboard");
    }

    @Command
    @NotifyChange("*")
    public void navegarGestionQueries() {
        navigateUsingService("/pages/reportes/gestion_queries.zul", "Gestión de Queries");
    }

    @Command
    @NotifyChange("*")
    public void navegarAyuda() {
        navigateUsingService("/pages/reportes/ayuda.zul", "Centro de Ayuda");
    }

    @Command
    @NotifyChange("*")
    public void navigateConsultasDinamicas() {
        navigateUsingService("/pages/reportes/consultas_dinamicas.zul", "Consultas Dinámicas");
    }

    @Command
    @NotifyChange("*")
    public void navigateReportesInfracciones() {
        navigateUsingService("/pages/reportes/infracciones.zul", "Infracciones");
    }

    @Command
    @NotifyChange("*")
    public void navigateDashboard() {
        navigateUsingService("/pages/reportes/dashboard.zul", "Dashboard");
    }

    @Command
    @NotifyChange("*")
    public void navigateConsolidacion() {
        navigateUsingService("/pages/reportes/consolidacion.zul", "Consolidación");
    }

    @Command
    @NotifyChange("*")
    public void navigateExportar() {
        navigateUsingService("/pages/reportes/exportar.zul", "Exportar Datos");
    }

    @Command
    @NotifyChange("*")
    public void navigateGestionQueries() {
        navigateUsingService("/pages/reportes/gestion_queries.zul", "Gestión de Queries");
    }

    @Command
    @NotifyChange("*")
    public void navigateAnalisisTemporal() {
        navigateUsingService("/pages/reportes/analisis_temporal.zul", "Análisis Temporal");
    }

    @Command
    @NotifyChange("*")
    public void navigateAnalisisGeografico() {
        navigateUsingService("/pages/reportes/analisis_geografico.zul", "Análisis Geográfico");
    }

    @Command
    @NotifyChange("*")
    public void navigateHistorial() {
        navigateUsingService("/pages/reportes/historial.zul", "Historial de Reportes");
    }

    @Command
    @NotifyChange("*")
    public void navigateConsultaRapida() {
        navigateUsingService("/pages/reportes/consulta_rapida.zul", "Consulta Rápida");
    }

    @Command
    @NotifyChange("*")
    public void navigateAyuda() {
        navigateUsingService("/pages/reportes/ayuda.zul", "Centro de Ayuda");
    }

    // ===== COMANDO PARA EJECUTAR QUERY DESDE POPULARES =====

    @Command
    @NotifyChange("*")
    public void ejecutarQuery(@BindingParam("query") MetadataQueryRegistro query) {
        if (query != null) {
            System.out.println("Ejecutando query popular: " + query.getNombre());
            // Navegar a la página de ejecución con el código de la query
            navigateUsingService(
                    "/pages/reportes/ejecutar_query.zul?codigo=" + query.getCodigo(),
                    "Ejecutar: " + query.getNombre()
            );
        }
    }

    // ===== MÉTODO AUXILIAR DE NAVEGACIÓN =====

    private void navigateUsingService(String targetPath, String label) {
        if (navigationModel == null) {
            System.out.println("ERROR: navigationModel es NULL. Inicializando...");
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }

        try {
            System.out.println("Navegando a: " + targetPath + " - Label: " + label);

            navigationModel.setContentUrl(targetPath);
            navigationModel.setLocation(label);

            BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
            BindUtils.postNotifyChange(null, null, navigationModel, "location");

            BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);

            System.out.println("Navegación completada a: " + targetPath);

        } catch (Exception e) {
            System.out.println("Error durante la navegación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== CLASE INTERNA PARA ACTIVIDAD RECIENTE =====


}