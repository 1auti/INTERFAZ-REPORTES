package detalleLote;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.DetalleLote;
import org.zkoss.dominial.core.entity.Lote;
import org.zkoss.dominial.web.navigation.NavigationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Messagebox;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.zkoss.dominial.web.viewmodel.MainApplicationVM.NAVIGATION;

@Getter @Setter
public class detalleLote {

    // Navigation
    private NavigationService navigationModel;

    // Lote seleccionado
    private Lote loteSeleccionado;

    // Search and filters
    private String searchText = "";
    private List<DetalleLote> detalleLoteList;
    private List<DetalleLote> originalDetalleLoteList;
    private Date fechaDesde;
    private Date fechaHasta;
    private FilterOption selectedEstadoFilter;
    private List<FilterOption> estadoFilterOptions;
    private FilterOption selectedAutorFilter;
    private List<FilterOption> autorFilterOptions;

    // Estadísticas
    private int totalRegistros;
    private int totalVerificados;
    private int totalRechazados;
    private String ultimaActualizacion;

    @Init
    public void init() {
        System.out.println("=== INICIALIZANDO DetalleLote ViewModel ===");
        System.out.println("Clase actual: " + this.getClass().getName());

        initializeNavigationService();
        initializeLoteSeleccionado();
        initializeFilters();
        loadData();
        updateStatistics();

        System.out.println("=== DetalleLote ViewModel INICIALIZADO CORRECTAMENTE ===");
    }

    private void initializeNavigationService() {
        navigationModel = (NavigationService) Executions.getCurrent().getDesktop().getAttribute(NAVIGATION);
        if (navigationModel == null) {
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }
    }

    private void initializeLoteSeleccionado() {
        try {
            // Recuperar el lote seleccionado desde la sesión
            loteSeleccionado = (Lote) Executions.getCurrent().getSession().getAttribute("selectedLote");

            if (loteSeleccionado != null) {
                System.out.println("Lote cargado en detalle: ID=" + loteSeleccionado.getId() +
                        ", Municipio=" + loteSeleccionado.getIdMunicipio());

                // Actualizar el título de navegación
                String location = "Detalle Lote > " + loteSeleccionado.getIdMunicipio() + " (ID: " + loteSeleccionado.getId() + ")";
                if (navigationModel != null) {
                    navigationModel.setLocation(location);
                }
            } else {
                System.out.println("No se encontró lote en sesión, creando lote de ejemplo");
                // Crear lote de ejemplo si no hay uno en sesión
                loteSeleccionado = new Lote();
                loteSeleccionado.setId(123456789);
                loteSeleccionado.setIdMunicipio("MUN0001");
                loteSeleccionado.setPendientes(150L);
            }
        } catch (Exception e) {
            System.out.println("Error al recuperar lote de sesión: " + e.getMessage());
            // Crear lote de ejemplo en caso de error
            loteSeleccionado = new Lote();
            loteSeleccionado.setId(123456789);
            loteSeleccionado.setIdMunicipio("MUN0001");
            loteSeleccionado.setPendientes(150L);
        }
    }

    private void initializeFilters() {
        // Filtros de estado
        estadoFilterOptions = Arrays.asList(
                new FilterOption("Todos los estados", null),
                new FilterOption("Procesado correctamente", "PROCESADO"),
                new FilterOption("Con errores", "ERROR"),
                new FilterOption("Pendiente", "PENDIENTE"),
                new FilterOption("Reprocesando", "REPROCESANDO")
        );
        selectedEstadoFilter = estadoFilterOptions.get(0);

        // Filtros de autor
        autorFilterOptions = Arrays.asList(
                new FilterOption("Todos los autores", null),
                new FilterOption("CAMILA.C", "CAMILA.C"),
                new FilterOption("ABRIL.Q", "ABRIL.Q"),
                new FilterOption("PEDRO.M", "PEDRO.M"),
                new FilterOption("SISTEMA", "SISTEMA")
        );
        selectedAutorFilter = autorFilterOptions.get(0);
    }

    // Comandos de búsqueda y filtros
    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void search() {
        applyFilters();
    }

    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void filterByEstado() {
        applyFilters();
    }

    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void filterByAutor() {
        applyFilters();
    }

    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void filterByDate() {
        applyFilters();
    }

    @Command
    @NotifyChange({"detalleLoteList", "searchText", "fechaDesde", "fechaHasta",
            "selectedEstadoFilter", "selectedAutorFilter", "totalRegistros",
            "totalRegistrosText", "paginationText"})
    public void clearFilters() {
        searchText = "";
        fechaDesde = null;
        fechaHasta = null;
        selectedEstadoFilter = estadoFilterOptions.get(0);
        selectedAutorFilter = autorFilterOptions.get(0);
        detalleLoteList = new ArrayList<>(originalDetalleLoteList);
        updateStatistics();
    }

    // Comandos de acciones
    @Command
    public void subirExcel() {
        if (loteSeleccionado != null) {
            Messagebox.show("Funcionalidad: Subir archivo Excel para el lote " + loteSeleccionado.getId(),
                    "Subir Excel", Messagebox.OK, Messagebox.INFORMATION);
        }
    }

    @Command
    public void exportToExcel() {
        Messagebox.show("Exportando " + detalleLoteList.size() + " registros de detalle de lote...",
                "Exportación", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalVerificados", "totalRechazados",
            "totalRegistrosText", "totalVerificadosText", "totalRechazadosText",
            "ultimaActualizacionText"})
    public void reprocesar(@BindingParam("detalle") DetalleLote detalle) {
        Messagebox.show("¿Está seguro de que desea reprocesar el archivo: " + detalle.getNombreArchivo() + "?",
                "Confirmar reprocesamiento",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        // Simular reprocesamiento
                        detalle.setEstado("REPROCESANDO");
                        detalle.setFechaModificacion(new Date());

                        // Simular nuevos resultados después de un delay
                        Random random = new Random();
                        detalle.setVerificados(random.nextInt(100) + 50);
                        detalle.setRechazados(random.nextInt(20));
                        detalle.setEstado("PROCESADO");

                        updateStatistics();
                        Messagebox.show("Archivo reprocesado correctamente", "Información",
                                Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    @Command
    public void subirExcelDetalle(@BindingParam("detalle") DetalleLote detalle) {
        Messagebox.show("Subir nuevo archivo Excel para reemplazar: " + detalle.getNombreArchivo(),
                "Subir Excel", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void bajarExcel(@BindingParam("detalle") DetalleLote detalle) {
        Messagebox.show("Descargando archivo: " + detalle.getNombreArchivo() + "\n" +
                        "Verificados: " + detalle.getVerificados() + "\n" +
                        "Rechazados: " + detalle.getRechazados(),
                "Descargar Excel", Messagebox.OK, Messagebox.INFORMATION);
    }

    // COMANDOS ADICIONALES PARA EL HEADER

    @Command
    public void bajarTodosExcel() {
        Messagebox.show("Descargando todos los archivos del lote " +
                        (loteSeleccionado != null ? loteSeleccionado.getId() : ""),
                "Descargar Todos", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalVerificados", "totalRechazados",
            "totalRegistrosText", "totalVerificadosText", "totalRechazadosText",
            "ultimaActualizacionText"})
    public void reprocesarSeleccionados() {
        Messagebox.show("¿Está seguro de que desea reprocesar los archivos seleccionados?",
                "Confirmar reprocesamiento",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        // Simular reprocesamiento de archivos seleccionados
                        int archivosReprocesados = 0;
                        Random random = new Random();

                        for (DetalleLote detalle : detalleLoteList) {
                            if (!"PROCESADO".equals(detalle.getEstado())) {
                                detalle.setEstado("REPROCESANDO");
                                detalle.setFechaModificacion(new Date());

                                // Simular nuevos resultados
                                detalle.setVerificados(random.nextInt(100) + 50);
                                detalle.setRechazados(random.nextInt(20));
                                detalle.setEstado("PROCESADO");
                                archivosReprocesados++;
                            }
                        }

                        updateStatistics();
                        Messagebox.show(archivosReprocesados + " archivos reprocesados correctamente",
                                "Información", Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    @Command
    @NotifyChange({"detalleLoteList", "totalRegistros", "totalVerificados", "totalRechazados",
            "totalRegistrosText", "totalVerificadosText", "totalRechazadosText",
            "ultimaActualizacionText"})
    public void reprocesarTodo() {
        Messagebox.show("¿Está seguro de que desea reprocesar TODOS los archivos del lote?",
                "Confirmar reprocesamiento completo",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        // Simular reprocesamiento de todos los archivos
                        Random random = new Random();

                        for (DetalleLote detalle : detalleLoteList) {
                            detalle.setEstado("REPROCESANDO");
                            detalle.setFechaModificacion(new Date());

                            // Simular nuevos resultados
                            detalle.setVerificados(random.nextInt(100) + 50);
                            detalle.setRechazados(random.nextInt(20));
                            detalle.setEstado("PROCESADO");
                        }

                        updateStatistics();
                        Messagebox.show("Todos los archivos han sido reprocesados correctamente",
                                "Información", Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    // COMANDO PARA VOLVER A LA LISTA DE LOTES
    @Command
    public void volverALotes() {
        try {
            // Limpiar la sesión
            Executions.getCurrent().getSession().removeAttribute("selectedLote");

            // Navegar de vuelta a la lista de lotes
            if (navigationModel != null) {
                navigationModel.setContentUrl(NavigationService.BATCH_PATH);
                navigationModel.setLocation("Gestión de Lotes");

                // Notificar cambios
                BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
                BindUtils.postNotifyChange(null, null, navigationModel, "location");
                BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);
            }

        } catch (Exception e) {
            System.out.println("Error al volver a lotes: " + e.getMessage());
        }
    }

    private void loadData() {
        originalDetalleLoteList = new ArrayList<>();

        // Usar el ID del lote seleccionado para los nombres de archivo
        String loteId = loteSeleccionado != null ? String.valueOf(loteSeleccionado.getId()) : "123456789";

        String[] nombresArchivos = {
                "lote_" + loteId + "_datos_principales.xlsx",
                "lote_" + loteId + "_validacion_completa.xlsx",
                "lote_" + loteId + "_correccion_errores.xlsx",
                "lote_" + loteId + "_actualizacion_final.xlsx",
                "lote_" + loteId + "_revision_tecnica.xlsx",
                "lote_" + loteId + "_verificacion_dominios.xlsx",
                "lote_" + loteId + "_control_calidad.xlsx",
                "lote_" + loteId + "_proceso_masivo.xlsx",
                "lote_" + loteId + "_auditoria_datos.xlsx",
                "lote_" + loteId + "_consolidacion.xlsx"
        };

        String[] autores = {"CAMILA.C", "ABRIL.Q", "PEDRO.M", "SISTEMA"};
        String[] estados = {"PROCESADO", "ERROR", "PENDIENTE", "REPROCESANDO"};

        Random random = new Random();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < nombresArchivos.length; i++) {
            DetalleLote detalle = new DetalleLote();
            detalle.setId((long) (i + 1));
            detalle.setIdLote(loteSeleccionado.getId());
            detalle.setNombreArchivo(nombresArchivos[i]);

            // Datos de verificación/rechazo realistas
            int totalRegistros = random.nextInt(500) + 100; // 100-600 registros
            int rechazados = random.nextInt(50); // 0-50 rechazados
            int verificados = totalRegistros - rechazados;

            detalle.setVerificados(verificados);
            detalle.setRechazados(rechazados);
            detalle.setAutor(autores[random.nextInt(autores.length)]);
            detalle.setEstado(estados[random.nextInt(estados.length)]);

            // Fechas de alta en los últimos 30 días
            cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(30));
            detalle.setFechaAlta(new Date(cal.getTimeInMillis()));

            // Fecha de modificación posterior a alta
            cal.add(Calendar.HOUR_OF_DAY, random.nextInt(72)); // 0-72 horas después
            detalle.setFechaModificacion(new Date(cal.getTimeInMillis()));

            // Reset calendar
            cal = Calendar.getInstance();

            originalDetalleLoteList.add(detalle);
        }

        detalleLoteList = new ArrayList<>(originalDetalleLoteList);
    }

    private void applyFilters() {
        List<DetalleLote> filtered = new ArrayList<>(originalDetalleLoteList);

        // Filtrar por texto de búsqueda (nombre archivo)
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.toLowerCase();
            filtered = filtered.stream()
                    .filter(detalle -> detalle.getNombreArchivo().toLowerCase().contains(search) ||
                            detalle.getAutor().toLowerCase().contains(search))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por estado
        if (selectedEstadoFilter != null && selectedEstadoFilter.getValue() != null) {
            String estadoFilter = selectedEstadoFilter.getValue();
            filtered = filtered.stream()
                    .filter(detalle -> detalle.getEstado().equals(estadoFilter))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por autor
        if (selectedAutorFilter != null && selectedAutorFilter.getValue() != null) {
            String autorFilter = selectedAutorFilter.getValue();
            filtered = filtered.stream()
                    .filter(detalle -> detalle.getAutor().equals(autorFilter))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por rango de fechas
        if (fechaDesde != null) {
            filtered = filtered.stream()
                    .filter(detalle -> detalle.getFechaAlta().compareTo(fechaDesde) >= 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        if (fechaHasta != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaHasta);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Date fechaHastaInclusive = cal.getTime();

            filtered = filtered.stream()
                    .filter(detalle -> detalle.getFechaAlta().compareTo(fechaHastaInclusive) < 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        detalleLoteList = filtered;
        updateStatistics();
    }

    private void updateStatistics() {
        if (detalleLoteList != null) {
            totalRegistros = detalleLoteList.size();
            totalVerificados = detalleLoteList.stream()
                    .mapToInt(DetalleLote::getVerificados)
                    .sum();
            totalRechazados = detalleLoteList.stream()
                    .mapToInt(DetalleLote::getRechazados)
                    .sum();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        ultimaActualizacion = sdf.format(new Date());
    }

    // Métodos para obtener textos de estadísticas
    public String getTotalRegistrosText() {
        return "Total archivos: " + totalRegistros;
    }

    public String getTotalVerificadosText() {
        return "Verificados: " + totalVerificados;
    }

    public String getTotalRechazadosText() {
        return "Rechazados: " + totalRechazados;
    }

    public String getUltimaActualizacionText() {
        return "Última actualización: " + ultimaActualizacion;
    }

    public String getPaginationText() {
        if (detalleLoteList == null || detalleLoteList.isEmpty()) {
            return "Sin registros";
        }
        return "Mostrando " + detalleLoteList.size() + " archivos del lote";
    }

    /**
     * Método para obtener el título dinámico del detalle lote
     */
    public String getTituloDetalle() {
        if (loteSeleccionado != null && loteSeleccionado.getIdMunicipio() != null) {
            return "Detalle de Lote - " + loteSeleccionado.getIdMunicipio() + " (ID: " + loteSeleccionado.getId() + ")";
        }
        return "Detalle de Lote";
    }

    /**
     * Método para obtener la clase CSS del estado
     */
    public String getEstadoClass(String estado) {
        if (estado == null) return "estado-default";
        return "estado-" + estado.toLowerCase();
    }

    // Clase auxiliar para las opciones de filtro
    public static class FilterOption {
        private String label;
        private String value;

        public FilterOption(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    @Command
    public void verVerificados(@BindingParam("detalle") DetalleLote detalle) {
        try {
            // Guardar el detalle de lote seleccionado en la sesión
            if (detalle != null) {
                Executions.getCurrent().getSession().setAttribute("selectedDetalleLote", detalle);
                System.out.println("DetalleLote guardado en sesión para rechazados: " + detalle.getNombreArchivo() +
                        " - Rechazados: " + detalle.getRechazados());
            }

            // Navegar a la vista de rechazados usando el patrón correcto
            navigateUsingService(NavigationService.DETALLE_BATCH_VERIFICADOS_PATH,
                    "Lote Verificados" + (detalle != null ? detalle.getNombreArchivo() : ""));

        } catch (Exception e) {
            System.out.println("Error al navegar a rechazados: " + e.getMessage());
            e.printStackTrace();
            Messagebox.show("Error al navegar a los rechazados", "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    @Command
    public void verRechazados(@BindingParam("detalle") DetalleLote detalle) {
        try {
            // Guardar el detalle de lote seleccionado en la sesión
            if (detalle != null) {
                Executions.getCurrent().getSession().setAttribute("selectedDetalleLote", detalle);
                System.out.println("DetalleLote guardado en sesión para rechazados: " + detalle.getNombreArchivo() +
                        " - Rechazados: " + detalle.getRechazados());
            }

            // Navegar a la vista de rechazados usando el patrón correcto
            navigateUsingService(NavigationService.DETALLE_BATCH_RECHAZADOS_PATH,
                    "Lote Rechazados" + (detalle != null ? detalle.getNombreArchivo() : ""));

        } catch (Exception e) {
            System.out.println("Error al navegar a rechazados: " + e.getMessage());
            e.printStackTrace();
            Messagebox.show("Error al navegar a los rechazados", "Error", Messagebox.OK, Messagebox.ERROR);
        }
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