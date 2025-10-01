package history;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.History;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zul.Messagebox;

import java.text.SimpleDateFormat;
import java.util.*;

@Getter @Setter
public class history {
    private String searchText = "";
    private List<History> historialList;
    private List<History> originalHistorialList;

    // Filtros
    private Date fechaDesde;
    private Date fechaHasta;
    private FilterOption selectedOperacionFilter;
    private List<FilterOption> operacionFilterOptions;
    private FilterOption selectedAutorFilter;
    private List<FilterOption> autorFilterOptions;

    // Estadísticas
    private int totalRegistros;
    private String ultimaActualizacion;

    public history() {
        initializeFilters();
        loadData();
        updateStatistics();
    }

    private void initializeFilters() {
        // Filtros de operación
        operacionFilterOptions = Arrays.asList(
                new FilterOption("Todas las operaciones", null),
                new FilterOption("CREAR", "CREAR"),
                new FilterOption("EDITAR", "EDITAR"),
                new FilterOption("ELIMINAR", "ELIMINAR"),
                new FilterOption("CONSULTAR", "CONSULTAR"),
                new FilterOption("EXPORTAR", "EXPORTAR"),
                new FilterOption("IMPORTAR", "IMPORTAR"),
                new FilterOption("LOGIN", "LOGIN"),
                new FilterOption("LOGOUT", "LOGOUT")
        );
        selectedOperacionFilter = operacionFilterOptions.get(0);

        // Filtros de autor
        autorFilterOptions = Arrays.asList(
                new FilterOption("Todos los autores", null),
                new FilterOption("CAMILA.C", "CAMILA.C"),
                new FilterOption("ABRIL.Q", "ABRIL.Q"),
                new FilterOption("PEDRO", "PEDRO"),
                new FilterOption("ADMIN", "ADMIN"),
                new FilterOption("SISTEMA", "SISTEMA")
        );
        selectedAutorFilter = autorFilterOptions.get(0);
    }

    // Comandos de búsqueda y filtros
    @Command
    @NotifyChange({"historialList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void search() {
        applyFilters();
    }

    @Command
    @NotifyChange({"historialList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void filterByOperacion() {
        applyFilters();
    }

    @Command
    @NotifyChange({"historialList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void filterByAutor() {
        applyFilters();
    }

    @Command
    @NotifyChange({"historialList", "totalRegistros", "totalRegistrosText", "paginationText"})
    public void filterByDate() {
        applyFilters();
    }

    @Command
    @NotifyChange({"historialList", "searchText", "fechaDesde", "fechaHasta",
            "selectedOperacionFilter", "selectedAutorFilter", "totalRegistros",
            "totalRegistrosText", "paginationText"})
    public void clearFilters() {
        searchText = "";
        fechaDesde = null;
        fechaHasta = null;
        selectedOperacionFilter = operacionFilterOptions.get(0);
        selectedAutorFilter = autorFilterOptions.get(0);
        historialList = new ArrayList<>(originalHistorialList);
        updateStatistics();
    }

    @Command
    public void exportToExcel() {
        Messagebox.show("Exportando " + historialList.size() + " registros de historial a Excel...",
                "Exportación", Messagebox.OK, Messagebox.INFORMATION);
    }

    private void loadData() {
        originalHistorialList = new ArrayList<>();

        // Datos realistas de historial del sistema
        generateHistorialData();

        // Ordenar por fecha descendente (más reciente primero)
        originalHistorialList.sort((h1, h2) -> h2.getFecha().compareTo(h1.getFecha()));

        historialList = new ArrayList<>(originalHistorialList);
    }

    private void generateHistorialData() {
        long id = 1;
        Calendar cal = Calendar.getInstance();
        Random random = new Random();

        // Operaciones recientes del sistema (últimos 30 días)
        for (int dia = 0; dia < 30; dia++) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -dia);

            // Sesiones de usuarios
            if (random.nextBoolean()) {
                addHistorial(id++, "LOGIN", "SISTEMA", "CAMILA.C",
                        "Inicio de sesión - IP: 192.168.1." + (100 + random.nextInt(50)), cal);

                // Actividades durante la sesión
                for (int actividad = 0; actividad < random.nextInt(5) + 1; actividad++) {
                    cal.add(Calendar.MINUTE, random.nextInt(30) + 5);

                    if (random.nextBoolean()) {
                        addHistorial(id++, "CONSULTAR", "LOTES", "CAMILA.C",
                                "Consulta de lotes - Filtros: pendientes > 0", cal);
                    } else if (random.nextBoolean()) {
                        addHistorial(id++, "EDITAR", "REGLAS", "CAMILA.C",
                                "Modificación regla básica: " + generateRuleName(), cal);
                    } else {
                        addHistorial(id++, "CREAR", "MAPEO", "CAMILA.C",
                                "Nuevo mapeo creado: " + generateMapeoName(), cal);
                    }
                }

                cal.add(Calendar.HOUR_OF_DAY, random.nextInt(4) + 1);
                addHistorial(id++, "LOGOUT", "SISTEMA", "CAMILA.C",
                        "Cierre de sesión - Duración: " + (random.nextInt(4) + 1) + "h " + random.nextInt(60) + "m", cal);
            }

            // Actividades de ABRIL.Q
            if (random.nextBoolean()) {
                cal.add(Calendar.HOUR_OF_DAY, random.nextInt(8) + 1);
                addHistorial(id++, "LOGIN", "SISTEMA", "ABRIL.Q",
                        "Inicio de sesión - IP: 192.168.1." + (150 + random.nextInt(50)), cal);

                addHistorial(id++, "EXPORTAR", "LOTES", "ABRIL.Q",
                        "Exportación a Excel - " + (random.nextInt(500) + 100) + " registros", cal);

                if (random.nextBoolean()) {
                    addHistorial(id++, "ELIMINAR", "REGLAS", "ABRIL.Q",
                            "Eliminación regla obsoleta ID: " + (random.nextInt(50) + 1), cal);
                }

                addHistorial(id++, "LOGOUT", "SISTEMA", "ABRIL.Q",
                        "Cierre de sesión - Duración: " + (random.nextInt(3) + 1) + "h " + random.nextInt(60) + "m", cal);
            }

            // Actividades administrativas de PEDRO
            if (dia % 3 == 0) {
                cal.add(Calendar.HOUR_OF_DAY, random.nextInt(6) + 2);
                addHistorial(id++, "LOGIN", "SISTEMA", "PEDRO",
                        "Inicio de sesión administrativo - IP: 192.168.1.10", cal);

                addHistorial(id++, "CONSULTAR", "REPORTES", "PEDRO",
                        "Generación reporte mensual - Módulos: LOTES, REGLAS, MAPEO", cal);

                if (random.nextBoolean()) {
                    addHistorial(id++, "IMPORTAR", "LOTES", "PEDRO",
                            "Importación masiva - " + (random.nextInt(1000) + 500) + " lotes procesados", cal);
                }

                addHistorial(id++, "LOGOUT", "SISTEMA", "PEDRO",
                        "Cierre de sesión - Duración: " + (random.nextInt(2) + 1) + "h " + random.nextInt(60) + "m", cal);
            }

            // Operaciones automáticas del sistema
            if (dia % 7 == 0) {
                cal.set(Calendar.HOUR_OF_DAY, 2);
                cal.set(Calendar.MINUTE, 0);
                addHistorial(id++, "EXPORTAR", "SISTEMA", "SISTEMA",
                        "Backup automático - Base de datos completa", cal);

                cal.add(Calendar.HOUR_OF_DAY, 1);
                addHistorial(id++, "CONSULTAR", "SISTEMA", "SISTEMA",
                        "Verificación integridad datos - Estado: OK", cal);
            }
        }
    }

    private void addHistorial(long id, String operacion, String modulo, String autor, String detalles, Calendar fecha) {
        History historial = new History();
        historial.setId(id);
        historial.setOperacion(operacion);
        historial.setModulo(modulo);
        historial.setAutor(autor);
        historial.setDetalles(detalles);
        historial.setFecha(fecha.getTime());
        originalHistorialList.add(historial);
    }

    private String generateRuleName() {
        String[] reglas = {"CALLE", "DOMINIO", "LOCALIDAD", "CP", "CELULAR", "MARCA", "MODELO"};
        return "Regla " + reglas[new Random().nextInt(reglas.length)];
    }

    private String generateMapeoName() {
        String[] mapeos = {"CAPITAL FEDERAL", "C.A.B.A.", "TRES DE FEBRERO", "SAN MIGUEL", "ELDORADO"};
        return mapeos[new Random().nextInt(mapeos.length)];
    }

    private void applyFilters() {
        List<History> filtered = new ArrayList<>(originalHistorialList);

        // Filtrar por texto de búsqueda
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.toLowerCase();
            filtered = filtered.stream()
                    .filter(historial ->
                            historial.getOperacion().toLowerCase().contains(search) ||
                                    historial.getAutor().toLowerCase().contains(search) ||
                                    historial.getModulo().toLowerCase().contains(search) ||
                                    historial.getDetalles().toLowerCase().contains(search))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por operación
        if (selectedOperacionFilter != null && selectedOperacionFilter.getValue() != null) {
            String operacionFilter = selectedOperacionFilter.getValue();
            filtered = filtered.stream()
                    .filter(historial -> historial.getOperacion().equals(operacionFilter))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por autor
        if (selectedAutorFilter != null && selectedAutorFilter.getValue() != null) {
            String autorFilter = selectedAutorFilter.getValue();
            filtered = filtered.stream()
                    .filter(historial -> historial.getAutor().equals(autorFilter))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por rango de fechas
        if (fechaDesde != null) {
            filtered = filtered.stream()
                    .filter(historial -> historial.getFecha().compareTo(fechaDesde) >= 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        if (fechaHasta != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaHasta);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Date fechaHastaInclusive = cal.getTime();

            filtered = filtered.stream()
                    .filter(historial -> historial.getFecha().compareTo(fechaHastaInclusive) < 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        historialList = filtered;
        updateStatistics();
    }

    private void updateStatistics() {
        totalRegistros = historialList != null ? historialList.size() : 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        ultimaActualizacion = sdf.format(new Date());
    }

    // Métodos para obtener textos de estadísticas
    public String getTotalRegistrosText() {
        return "Total de registros: " + totalRegistros;
    }

    public String getUltimaActualizacionText() {
        return "Última actualización: " + ultimaActualizacion;
    }

    public String getPaginationText() {
        if (historialList == null || historialList.isEmpty()) {
            return "Sin registros";
        }
        return "Mostrando " + historialList.size() + " registros de historial";
    }

    // Método para obtener la clase CSS del badge según el tipo de operación
    public String getOperacionBadgeClass(String operacion) {
        if (operacion == null) return "badge-operacion badge-default";

        switch (operacion.toUpperCase()) {
            case "CREAR":
                return "badge-operacion badge-crear";
            case "EDITAR":
                return "badge-operacion badge-editar";
            case "ELIMINAR":
                return "badge-operacion badge-eliminar";
            case "CONSULTAR":
                return "badge-operacion badge-consultar";
            case "EXPORTAR":
            case "IMPORTAR":
                return "badge-operacion badge-archivo";
            case "LOGIN":
            case "LOGOUT":
                return "badge-operacion badge-sesion";
            default:
                return "badge-operacion badge-default";
        }
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
}