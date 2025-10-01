package mapeo;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.Mapeo;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zul.Messagebox;

import java.util.*;

@Getter @Setter
public class mapeo {
    private String searchText = "";
    private List<Mapeo> mapeoList;
    private List<Mapeo> originalMapeoList;

    // Filtros
    private FilterOption selectedColumnaFilter;
    private List<FilterOption> columnaFilterOptions;
    private FilterOption selectedCreadorFilter;
    private List<FilterOption> creadorFilterOptions;

    public mapeo() {
        initializeFilters();
        loadData();
    }

    private void initializeFilters() {
        // Filtros de columna
        columnaFilterOptions = Arrays.asList(
                new FilterOption("Todas las columnas", null),
                new FilterOption("PROVINCIA", "PROVINCIA"),
                new FilterOption("PARTIDO", "PARTIDO"),
                new FilterOption("LOCALIDAD", "LOCALIDAD"),
                new FilterOption("CALLE", "CALLE")
        );
        selectedColumnaFilter = columnaFilterOptions.get(0);

        // Filtros de creador
        creadorFilterOptions = Arrays.asList(
                new FilterOption("Todos los creadores", null),
                new FilterOption("CAMILA.C", "CAMILA.C"),
                new FilterOption("ABRIL.Q", "ABRIL.Q")
        );
        selectedCreadorFilter = creadorFilterOptions.get(0);
    }

    // Comandos de búsqueda y filtros
    @Command
    @NotifyChange({"mapeoList"})
    public void search() {
        applyFilters();
    }

    @Command
    @NotifyChange({"mapeoList"})
    public void filterByColumna() {
        applyFilters();
    }

    @Command
    @NotifyChange({"mapeoList"})
    public void filterByCreador() {
        applyFilters();
    }

    @Command
    @NotifyChange({"mapeoList", "searchText", "selectedColumnaFilter", "selectedCreadorFilter"})
    public void clearFilters() {
        searchText = "";
        selectedColumnaFilter = columnaFilterOptions.get(0);
        selectedCreadorFilter = creadorFilterOptions.get(0);
        mapeoList = new ArrayList<>(originalMapeoList);
    }

    // Comandos CRUD
    @Command
    public void newMapeo() {
        Messagebox.show("Funcionalidad: Crear nuevo mapeo", "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void editMapeo(@BindingParam("mapeo") Mapeo mapeo) {
        Messagebox.show("Editar mapeo: " + mapeo.getDatoClave(), "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"mapeoList"})
    public void deleteMapeo(@BindingParam("mapeo") Mapeo mapeo) {
        Messagebox.show("¿Está seguro de que desea eliminar el mapeo: " + mapeo.getDatoClave() + "?",
                "Confirmar eliminación",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        originalMapeoList.remove(mapeo);
                        applyFilters();
                        Messagebox.show("Mapeo eliminado correctamente", "Información",
                                Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    @Command
    public void export() {
        Messagebox.show("Exportando " + mapeoList.size() + " mapeos...", "Exportación", Messagebox.OK, Messagebox.INFORMATION);
    }

    private void loadData() {
        originalMapeoList = new ArrayList<>();

        // Datos basados en las imágenes
        createMapeoWithMultipleReemplazos("CAPITAL FEDERAL",
                Arrays.asList("CIUDAD AUTONOMA DE BS. AS.", "CIUDAD AUTONOMA", "CIUDAD AUTONOMA DE BUENOS AIRES"),
                "PROVINCIA", "CAMILA.C");

        createMapeoWithMultipleReemplazos("C.A.B.A.",
                Arrays.asList("CIUDAD AUTONOMA DE BS. AS.", "CABA", "CIUDAD DE BUENOS AIRES", "CIUDAD AUTONOMA DE BUENOS AIRES",
                        "C. AUTONOMA DE BS.AS.", "CIUDAD AUTONOMA DE BS.AS.", "CIUDAD AUTONOMA DE BS AS", "CAPITAL FEDERAL"),
                "PARTIDO", "CAMILA.C");

        createMapeoWithMultipleReemplazos("CIUDAD AUTONOMA BUENOS AIRES",
                Arrays.asList("C.A.B.A.", "C. AUTONOMA DE BS.AS.", "C.A.B.A", "C.AUTONOMA DE BS. AS.", "C. AUTONOMA DE BS AS", "CABA",
                        "CIUDAD AUTONOMA DE BUENOS AIRES", "CIUDAD AUTONOMA DE BUENOS AIRES", "CAPITAL FEDERAL"),
                "LOCALIDAD", "");

        createMapeoWithMultipleReemplazos("TRES DE FEBRERO",
                Arrays.asList("3 DE FEBRERO", "TRES DE FEBERO", "TRES DE FEBRERO -", "PARTIDO DE TRES DE FEBRERO",
                        "- 3 DE FEBRERO", "TRES DE FEBRERO", "3DE FEBRERO", "TRE DE FEBRERO", "TRES DE FEBRRRO"),
                "PARTIDO", "CAMILA.C");

        createMapeoSimple("9 DE JULIO", Arrays.asList("10 DE JULIO"), "PARTIDO", "CAMILA.C");

        createMapeoSimple("ELDORADO", Arrays.asList("EL DORADO", "ELDORDO"), "PARTIDO", "");

        createMapeoSimple("CONSTITUCION", Arrays.asList("VILLA CONSTITUCION"), "PARTIDO", "CAMILA.C");

        createMapeoSimple("LA COSTA", Arrays.asList("DE LA COSTA", "PARTIDO DE LA COSTA"), "PARTIDO", "CAMILA.C");

        // Datos adicionales de la segunda imagen
        createMapeoWithMultipleReemplazos("CALLE",
                Arrays.asList("SIN DATOS", "SIN INFORMAR", "SIN NOMBRE", "S/C S/NO", "S/NOMBRE S/NRO", "S/N S/N", "S/CALLE S/NO",
                        "SIN IDENTIFICACION", "SC", "S/N.", "S/NOMBRE", "S CALLE", "S/ CALLE", "<S/C>", "S/CALLE -",
                        "S/CALLE S/N", "S/N", "S/CALLE"),
                "CALLE", "ABRIL.Q");

        createMapeoSimple("PILAR", Arrays.asList("DEL PILAR"), "PARTIDO", "ABRIL.Q");

        createMapeoSimple("DOS DE ABRIL", Arrays.asList("2 DE ABRIL"), "PARTIDO", "ABRIL.Q");

        createMapeoSimple("SANTO TOME", Arrays.asList("VIRASORO"), "PARTIDO", "CAMILA.C");

        createMapeoSimple("DOCE DE OCTUBRE", Arrays.asList("12 DE OCTUBRE"), "PARTIDO", "ABRIL.Q");

        createMapeoSimple("PRIMERO DE MAYO", Arrays.asList("1 DE MAYO"), "PARTIDO", "CAMILA.C");

        createMapeoSimple("ESCOBAR", Arrays.asList("BELEN DE ESCOBAR"), "PARTIDO", "CAMILA.C");

        createMapeoWithMultipleReemplazos("SAN MIGUEL DE TUCUMAN",
                Arrays.asList("S.M. DE TUCUMAN", "S. M. DE TUCUMAN", "S.M.TUC.", "S.M. DE TUCUMAN-TUCUMAN.", "S.M. DE TUCUMAN-TUCUMAN",
                        "S.M. DE TUCUMAN - TUCUMAN.", "SM DE TUCUMAN", "S M DE TUCUMAN", "S M DE TUCUMAN", "S.M TUC", "S.M DE TUC",
                        "S.M.DE TUCUMAN", "S.M.TUCUMAN", "S.M DE TUCUMAN", "CAPITAL/SAN MIGUEL DE TUCUMAN", "CAPITAL S. M. DE"),
                "LOCALIDAD", "CAMILA.C");

        mapeoList = new ArrayList<>(originalMapeoList);
    }

    private void createMapeoWithMultipleReemplazos(String datoClave, List<String> reemplazos, String columna, String creador) {
        Mapeo mapeo = new Mapeo();
        mapeo.setId((long) (originalMapeoList.size() + 1));
        mapeo.setDatoClave(datoClave);
        mapeo.setReemplazos(reemplazos);
        mapeo.setColumna(columna);
        mapeo.setCreador(creador);
        mapeo.setFechaCreacion(new Date());
        originalMapeoList.add(mapeo);
    }

    private void createMapeoSimple(String datoClave, List<String> reemplazos, String columna, String creador) {
        createMapeoWithMultipleReemplazos(datoClave, reemplazos, columna, creador);
    }

    private void applyFilters() {
        List<Mapeo> filtered = new ArrayList<>(originalMapeoList);

        // Filtrar por texto de búsqueda
        if (searchText != null && !searchText.trim().isEmpty()) {
            filtered = filtered.stream()
                    .filter(mapeo -> mapeo.getDatoClave().toLowerCase().contains(searchText.toLowerCase()) ||
                            (mapeo.getReemplazos() != null && mapeo.getReemplazos().stream()
                                    .anyMatch(reemplazo -> reemplazo.toLowerCase().contains(searchText.toLowerCase()))))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por columna
        if (selectedColumnaFilter != null && selectedColumnaFilter.getValue() != null) {
            String columnaFilter = selectedColumnaFilter.getValue();
            filtered = filtered.stream()
                    .filter(mapeo -> mapeo.getColumna().equals(columnaFilter))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        // Filtrar por creador
        if (selectedCreadorFilter != null && selectedCreadorFilter.getValue() != null) {
            String creadorFilter = selectedCreadorFilter.getValue();
            filtered = filtered.stream()
                    .filter(mapeo -> mapeo.getCreador().equals(creadorFilter))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }

        mapeoList = filtered;
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