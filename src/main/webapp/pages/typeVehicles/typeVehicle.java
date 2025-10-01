package typeVehicles;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.TipoVehiculo;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zul.Messagebox;

import java.text.SimpleDateFormat;
import java.util.*;

@Getter @Setter
public class typeVehicle {
    private String searchText = "";
    private List<TipoVehiculo> tipoVehiculoList;
    private List<TipoVehiculo> originalTipoVehiculoList;

    // Filtros
    private Date fechaDesde;
    private Date fechaHasta;
    private FilterOption selectedOrdenFilter;
    private List<FilterOption> ordenFilterOptions;

    // Modal para CRUD
    private boolean modalVisible = false;
    private String modalTitle = "";
    private TipoVehiculo currentTipoVehiculo;
    private boolean isEditing = false;

    // Estadísticas
    private int totalTipos;
    private String ultimaActualizacion;

    public typeVehicle() {
        initializeFilters();
        loadData();
        updateStatistics();
    }

    private void initializeFilters() {
        // Filtros de ordenamiento
        ordenFilterOptions = Arrays.asList(
                new FilterOption("Más recientes primero", "FECHA_DESC"),
                new FilterOption("Más antiguos primero", "FECHA_ASC"),
                new FilterOption("Alfabético A-Z", "TIPO_ASC"),
                new FilterOption("Alfabético Z-A", "TIPO_DESC")
        );
        selectedOrdenFilter = ordenFilterOptions.get(0);
    }

    // Comandos de búsqueda y filtros
    @Command
    @NotifyChange({"tipoVehiculoList", "totalTipos", "totalTiposText", "paginationText"})
    public void search() {
        applyFilters();
    }

    @Command
    @NotifyChange({"tipoVehiculoList", "totalTipos", "totalTiposText", "paginationText"})
    public void filterByOrden() {
        applyFilters();
    }

    @Command
    @NotifyChange({"tipoVehiculoList", "totalTipos", "totalTiposText", "paginationText"})
    public void filterByDate() {
        applyFilters();
    }

    @Command
    @NotifyChange({"tipoVehiculoList", "searchText", "fechaDesde", "fechaHasta",
            "selectedOrdenFilter", "totalTipos", "totalTiposText", "paginationText"})
    public void clearFilters() {
        searchText = "";
        fechaDesde = null;
        fechaHasta = null;
        selectedOrdenFilter = ordenFilterOptions.get(0);
        tipoVehiculoList = new ArrayList<>(originalTipoVehiculoList);
        applyOrdering();
        updateStatistics();
    }

    // Comandos CRUD
    @Command
    @NotifyChange({"modalVisible", "modalTitle", "currentTipoVehiculo", "isEditing"})
    public void newTipoVehiculo() {
        currentTipoVehiculo = new TipoVehiculo();
        modalTitle = "Nuevo Tipo de Vehículo";
        isEditing = false;
        modalVisible = true;
    }

    @Command
    public void viewTipoVehiculo(@BindingParam("tipoVehiculo") TipoVehiculo tipoVehiculo) {
        Messagebox.show("Tipo de Vehículo: " + tipoVehiculo.getTipo() +
                        "\nFecha Alta: " + new SimpleDateFormat("dd/MM/yyyy").format(tipoVehiculo.getFechaAlta()) +
                        "\nID: " + tipoVehiculo.getId(),
                "Detalles del Tipo de Vehículo", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"modalVisible", "modalTitle", "currentTipoVehiculo", "isEditing"})
    public void editTipoVehiculo(@BindingParam("tipoVehiculo") TipoVehiculo tipoVehiculo) {
        currentTipoVehiculo = cloneTipoVehiculo(tipoVehiculo);
        modalTitle = "Editar Tipo de Vehículo";
        isEditing = true;
        modalVisible = true;
    }

    @Command
    @NotifyChange({"tipoVehiculoList", "totalTipos", "totalTiposText", "paginationText", "ultimaActualizacionText"})
    public void deleteTipoVehiculo(@BindingParam("tipoVehiculo") TipoVehiculo tipoVehiculo) {
        Messagebox.show("¿Está seguro de que desea eliminar el tipo de vehículo: " + tipoVehiculo.getTipo() + "?",
                "Confirmar eliminación",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        originalTipoVehiculoList.remove(tipoVehiculo);
                        applyFilters();
                        updateStatistics();
                        Messagebox.show("Tipo de vehículo eliminado correctamente", "Información",
                                Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    @Command
    @NotifyChange({"modalVisible", "tipoVehiculoList", "totalTipos", "totalTiposText",
            "paginationText", "ultimaActualizacionText"})
    public void saveTipoVehiculo() {
        if (currentTipoVehiculo == null || currentTipoVehiculo.getTipo() == null ||
                currentTipoVehiculo.getTipo().trim().isEmpty()) {
            Messagebox.show("El tipo de vehículo es obligatorio", "Error de validación",
                    Messagebox.OK, Messagebox.ERROR);
            return;
        }

        // Validar que no exista el tipo (solo en creación o si cambió el nombre)
        String tipoUpper = currentTipoVehiculo.getTipo().trim().toUpperCase();
        boolean existe = originalTipoVehiculoList.stream()
                .anyMatch(tv -> tv.getTipo().toUpperCase().equals(tipoUpper) &&
                        (!isEditing || !tv.getId().equals(currentTipoVehiculo.getId())));

        if (existe) {
            Messagebox.show("Ya existe un tipo de vehículo con ese nombre", "Error de validación",
                    Messagebox.OK, Messagebox.ERROR);
            return;
        }

        currentTipoVehiculo.setTipo(tipoUpper);

        if (isEditing) {
            // Actualizar en la lista original
            TipoVehiculo original = originalTipoVehiculoList.stream()
                    .filter(tv -> tv.getId().equals(currentTipoVehiculo.getId()))
                    .findFirst().orElse(null);
            if (original != null) {
                original.setTipo(currentTipoVehiculo.getTipo());
            }
            Messagebox.show("Tipo de vehículo actualizado correctamente", "Información",
                    Messagebox.OK, Messagebox.INFORMATION);
        } else {
            // Crear nuevo
            currentTipoVehiculo.setId(getNextId());
            currentTipoVehiculo.setFechaAlta(new Date());
            originalTipoVehiculoList.add(currentTipoVehiculo);
            Messagebox.show("Tipo de vehículo creado correctamente", "Información",
                    Messagebox.OK, Messagebox.INFORMATION);
        }

        modalVisible = false;
        applyFilters();
        updateStatistics();
    }

    @Command
    @NotifyChange({"modalVisible"})
    public void closeModal() {
        modalVisible = false;
        currentTipoVehiculo = null;
    }

    @Command
    public void export() {
        Messagebox.show("Exportando " + tipoVehiculoList.size() + " tipos de vehículos...",
                "Exportación", Messagebox.OK, Messagebox.INFORMATION);
    }

    private void loadData() {
        originalTipoVehiculoList = new ArrayList<>();

        // Tipos de vehículos comunes en Argentina
        String[] tiposVehiculos = {
                "AUTOMÓVIL", "MOTOCICLETA", "CAMIÓN", "COLECTIVO", "REMOLQUE", "SEMIRREMOLQUE",
                "ACOPLADO", "ÓMNIBUS", "MICROÓMNIBUS", "CAMIONETA", "FURGÓN", "CHASIS CON CABINA",
                "TRACTOR DE CARRETERA", "CASA RODANTE", "MAQUINARIA VIAL", "MAQUINARIA AGRÍCOLA",
                "CICLOMOTOR", "CUATRICICLO", "TRICARGO", "AMBULANCIA"
        };

        Random random = new Random();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < tiposVehiculos.length; i++) {
            TipoVehiculo tipo = new TipoVehiculo();
            tipo.setId((long) (i + 1));
            tipo.setTipo(tiposVehiculos[i]);

            // Fechas aleatorias en los últimos 2 años
            cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(730));
            tipo.setFechaAlta(new Date(cal.getTimeInMillis()));

            // Reset calendar
            cal = Calendar.getInstance();

            originalTipoVehiculoList.add(tipo);
        }

        tipoVehiculoList = new ArrayList<>(originalTipoVehiculoList);
        applyOrdering();
    }

    private void applyFilters() {
        List<TipoVehiculo> filtered = new ArrayList<>(originalTipoVehiculoList);

        // Filtrar por texto de búsqueda
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.toUpperCase();
            filtered = filtered.stream()
                    .filter(tipo -> tipo.getTipo().contains(search))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por rango de fechas
        if (fechaDesde != null) {
            filtered = filtered.stream()
                    .filter(tipo -> tipo.getFechaAlta().compareTo(fechaDesde) >= 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        if (fechaHasta != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaHasta);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Date fechaHastaInclusive = cal.getTime();

            filtered = filtered.stream()
                    .filter(tipo -> tipo.getFechaAlta().compareTo(fechaHastaInclusive) < 0)
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        tipoVehiculoList = filtered;
        applyOrdering();
        updateStatistics();
    }

    private void applyOrdering() {
        if (selectedOrdenFilter == null || tipoVehiculoList == null) return;

        switch (selectedOrdenFilter.getValue()) {
            case "FECHA_DESC":
                tipoVehiculoList.sort((t1, t2) -> t2.getFechaAlta().compareTo(t1.getFechaAlta()));
                break;
            case "FECHA_ASC":
                tipoVehiculoList.sort((t1, t2) -> t1.getFechaAlta().compareTo(t2.getFechaAlta()));
                break;
            case "TIPO_ASC":
                tipoVehiculoList.sort((t1, t2) -> t1.getTipo().compareTo(t2.getTipo()));
                break;
            case "TIPO_DESC":
                tipoVehiculoList.sort((t1, t2) -> t2.getTipo().compareTo(t1.getTipo()));
                break;
        }
    }

    private TipoVehiculo cloneTipoVehiculo(TipoVehiculo original) {
        TipoVehiculo clone = new TipoVehiculo();
        clone.setId(original.getId());
        clone.setTipo(original.getTipo());
        clone.setFechaAlta(original.getFechaAlta());
        return clone;
    }

    private Long getNextId() {
        return originalTipoVehiculoList.stream()
                .mapToLong(TipoVehiculo::getId)
                .max()
                .orElse(0L) + 1;
    }

    private void updateStatistics() {
        totalTipos = tipoVehiculoList != null ? tipoVehiculoList.size() : 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        ultimaActualizacion = sdf.format(new Date());
    }

    // Métodos para obtener textos de estadísticas
    public String getTotalTiposText() {
        return "Total de tipos: " + totalTipos;
    }

    public String getUltimaActualizacionText() {
        return "Última actualización: " + ultimaActualizacion;
    }

    public String getPaginationText() {
        if (tipoVehiculoList == null || tipoVehiculoList.isEmpty()) {
            return "Sin registros";
        }
        return "Mostrando " + tipoVehiculoList.size() + " tipos de vehículos";
    }

    // Método para obtener la clase CSS del badge según el tipo de vehículo
    public String getTipoVehiculoBadgeClass(String tipo) {
        if (tipo == null) return "badge-tipo-vehiculo badge-default";

        String tipoUpper = tipo.toUpperCase();
        if (tipoUpper.contains("AUTO") || tipoUpper.contains("CAMIONETA")) {
            return "badge-tipo-vehiculo badge-automovil";
        } else if (tipoUpper.contains("MOTO") || tipoUpper.contains("CICLO")) {
            return "badge-tipo-vehiculo badge-motocicleta";
        } else if (tipoUpper.contains("CAMIÓN") || tipoUpper.contains("CHASIS")) {
            return "badge-tipo-vehiculo badge-camion";
        } else if (tipoUpper.contains("ÓMNIBUS") || tipoUpper.contains("COLECTIVO")) {
            return "badge-tipo-vehiculo badge-omnibus";
        } else if (tipoUpper.contains("REMOLQUE") || tipoUpper.contains("ACOPLADO")) {
            return "badge-tipo-vehiculo badge-remolque";
        } else if (tipoUpper.contains("MAQUINARIA")) {
            return "badge-tipo-vehiculo badge-maquinaria";
        } else {
            return "badge-tipo-vehiculo badge-otros";
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