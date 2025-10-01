package batch;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.Lote;
import org.zkoss.dominial.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Messagebox;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.zkoss.dominial.web.viewmodel.MainApplicationVM.NAVIGATION;

@Getter @Setter
public class batch {
    private NavigationService navigationModel;

    private String searchText = "";
    private List<Lote> loteList;
    private List<Lote> originalLoteList;
    private Date fechaDesde;
    private Date fechaHasta;
    private FilterOption selectedPendientesFilter;
    private List<FilterOption> pendientesFilterOptions;

    // Estadísticas
    private int totalLotes;
    private int lotesConPendientes;
    private String ultimaActualizacion;

    public batch() {
        initializeFilters();
        loadData();
        updateStatistics();
    }

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

    private void initializeFilters() {
        pendientesFilterOptions = Arrays.asList(
                new FilterOption("Todos", null),
                new FilterOption("Sin pendientes", 0L),
                new FilterOption("Con pendientes", -1L), // -1 significa > 0
                new FilterOption("Más de 5 pendientes", 5L)
        );
        selectedPendientesFilter = pendientesFilterOptions.get(0);
    }

    public String getPaginationText() {
        if (loteList == null || loteList.isEmpty()) {
            return "Sin registros";
        }
        return "Mostrando " + loteList.size() + " de " + totalLotes + " lotes";
    }

    // Comandos
    @Command
    @NotifyChange({"loteList", "totalLotes", "lotesConPendientes"})
    public void search() {
        applyFilters();
    }

    @Command
    @NotifyChange({"loteList", "totalLotes", "lotesConPendientes"})
    public void filterByPendientes() {
        applyFilters();
    }

    @Command
    @NotifyChange({"loteList", "totalLotes", "lotesConPendientes"})
    public void filterByDate() {
        applyFilters();
    }

    @Command
    @NotifyChange({"loteList", "searchText", "fechaDesde", "fechaHasta", "selectedPendientesFilter",
            "totalLotes", "lotesConPendientes"})
    public void clearFilters() {
        searchText = "";
        fechaDesde = null;
        fechaHasta = null;
        selectedPendientesFilter = pendientesFilterOptions.get(0);
        loteList = new ArrayList<>(originalLoteList);
        updateStatistics();
    }

    @Command
    public void newLote() {
        // Implementar navegación a pantalla de nuevo lote
        Messagebox.show("Funcionalidad: Crear nuevo lote", "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void viewLote(@BindingParam("lote") Lote lote) {
        // Implementar navegación a pantalla de detalle del lote
        Messagebox.show("Ver detalles del lote ID: " + lote.getId(), "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void editLote(@BindingParam("lote") Lote lote) {
        // Implementar navegación a pantalla de edición del lote
        Messagebox.show("Editar lote ID: " + lote.getId(), "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"loteList", "totalLotes", "lotesConPendientes", "totalLotesText",
            "lotesConPendientesText", "ultimaActualizacionText", "paginationText"})
    public void deleteLote(@BindingParam("lote") Lote lote) {
        Messagebox.show("¿Está seguro de que desea eliminar el lote ID: " + lote.getId() + "?",
                "Confirmar eliminación",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        // Simular eliminación
                        originalLoteList.remove(lote);
                        applyFilters();
                        Messagebox.show("Lote eliminado correctamente", "Información",
                                Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    @Command
    public void viewDominios(@BindingParam("lote") Lote lote) {
        // Implementar navegación a pantalla de dominios del lote
        Messagebox.show("Ver dominios del lote ID: " + lote.getId(), "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void export() {
        // Implementar exportación a Excel/CSV
        Messagebox.show("Exportando " + loteList.size() + " registros...", "Exportación", Messagebox.OK, Messagebox.INFORMATION);
    }

    private void loadData() {
        // Simular carga de datos - en la implementación real, esto vendría de un servicio
        originalLoteList = new ArrayList<>();

        // Datos de ejemplo
        Random random = new Random();
        Calendar cal = Calendar.getInstance();

        for (int i = 1; i <= 50; i++) {
            int min = 900000000;
            int max = 999999999;
            Lote lote = new Lote();
            lote.setId((int) (Math.random() * ((max - min) + 1)) + min);

            lote.setIdMunicipio("MUN" + String.format("%04d", i));

            lote.setPendientes((long) random.nextInt(10000)); // 0-9 pendientes

            // Fecha de emisión aleatoria en los últimos 2 años
            cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(730));
            lote.setFechaEmision(new Date(cal.getTimeInMillis()));

            // Fecha de modificación posterior a emisión
            cal.add(Calendar.DAY_OF_YEAR, random.nextInt(30));
            lote.setFechaModificacion(new Date(cal.getTimeInMillis()));

            // Reset calendar
            cal = Calendar.getInstance();

            originalLoteList.add(lote);
        }

        loteList = new ArrayList<>(originalLoteList);
    }

    private void applyFilters() {
        List<Lote> filtered = new ArrayList<>(originalLoteList);

        // Filtrar por texto de búsqueda (ID Municipio)
        if (searchText != null && !searchText.trim().isEmpty()) {
            filtered = filtered.stream()
                    .filter(lote -> lote.getIdMunicipio().toLowerCase()
                            .contains(searchText.toLowerCase()))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por pendientes
        if (selectedPendientesFilter != null && selectedPendientesFilter.getValue() != null) {
            Long filterValue = selectedPendientesFilter.getValue();
            if (filterValue == -1L) { // Con pendientes
                filtered = filtered.stream()
                        .filter(lote -> lote.getPendientes() > 0)
                        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
            } else if (filterValue == 0L) { // Sin pendientes
                filtered = filtered.stream()
                        .filter(lote -> lote.getPendientes() == 0)
                        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
            } else { // Más de X pendientes
                filtered = filtered.stream()
                        .filter(lote -> lote.getPendientes() > filterValue)
                        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
            }
        }

        // Filtrar por rango de fechas
        if (fechaDesde != null) {
            filtered = filtered.stream()
                    .filter(lote -> lote.getFechaEmision().compareTo(fechaDesde) >= 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        if (fechaHasta != null) {
            // Agregar 1 día para incluir todo el día de fechaHasta
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaHasta);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Date fechaHastaInclusive = cal.getTime();

            filtered = filtered.stream()
                    .filter(lote -> lote.getFechaEmision().compareTo(fechaHastaInclusive) < 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        loteList = filtered;
        updateStatistics();
    }

    private void updateStatistics() {
        if (loteList != null) {
            totalLotes = loteList.size();
            lotesConPendientes = (int) loteList.stream()
                    .mapToLong(Lote::getPendientes)
                    .filter(p -> p > 0)
                    .count();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        ultimaActualizacion = sdf.format(new Date());
    }

    // Clase auxiliar para las opciones de filtro
    public static class FilterOption {
        private String label;
        private Long value;

        public FilterOption(String label, Long value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Long getValue() { return value; }
        public void setValue(Long value) { this.value = value; }
    }

    // Métodos @Command para navegación directa (redireccionan)
    @Command
    public void navigateDetalleLote() {
        navigateUsingService(NavigationService.DETALLE_BATCH_PATH, "Detalle Lote");
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