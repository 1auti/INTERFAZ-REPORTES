package rules;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.ReglaBasica;
import org.zkoss.dominial.core.entity.ReglaAvanzada;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zul.Messagebox;

import java.util.*;

@Getter @Setter
public class rules {
    private String searchText = "";
    private List<ReglaBasica> reglasBasicasList;
    private List<ReglaBasica> originalReglasBasicasList;
    private List<ReglaAvanzada> reglasAvanzadasList;
    private List<ReglaAvanzada> originalReglasAvanzadasList;
    private boolean isBasicTabActive = true;

    public rules() {
        loadData();
    }

    // Getter público para isBasicTabActive
    public boolean getIsBasicTabActive() {
        return isBasicTabActive;
    }

    public void setIsBasicTabActive(boolean isBasicTabActive) {
        this.isBasicTabActive = isBasicTabActive;
    }

    // Comandos para navegación entre tabs
    @Command
    @NotifyChange({"isBasicTabActive"})
    public void showBasicRules() {
        isBasicTabActive = true;
    }

    @Command
    @NotifyChange({"isBasicTabActive"})
    public void showAdvancedRules() {
        isBasicTabActive = false;
    }

    // Comandos de búsqueda
    @Command
    @NotifyChange({"reglasBasicasList", "reglasAvanzadasList"})
    public void search() {
        applyFilters();
    }

    // Comandos para Reglas Básicas
    @Command
    public void newRegla() {
        Messagebox.show("Funcionalidad: Crear nueva regla básica", "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void editRegla(@BindingParam("regla") ReglaBasica regla) {
        Messagebox.show("Editar regla básica: " + regla.getNombre(), "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"reglasBasicasList"})
    public void deleteRegla(@BindingParam("regla") ReglaBasica regla) {
        Messagebox.show("¿Está seguro de que desea eliminar la regla: " + regla.getNombre() + "?",
                "Confirmar eliminación",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        originalReglasBasicasList.remove(regla);
                        applyFilters();
                        Messagebox.show("Regla eliminada correctamente", "Información",
                                Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    // Comandos para Reglas Avanzadas
    @Command
    public void newReglaAvanzada() {
        Messagebox.show("Funcionalidad: Crear nueva regla avanzada", "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void editReglaAvanzada(@BindingParam("regla") ReglaAvanzada regla) {
        Messagebox.show("Editar regla avanzada: " + regla.getColumna() + " - " + regla.getOperacion(),
                "Información", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    @NotifyChange({"reglasAvanzadasList"})
    public void deleteReglaAvanzada(@BindingParam("regla") ReglaAvanzada regla) {
        Messagebox.show("¿Está seguro de que desea eliminar la regla avanzada: " + regla.getColumna() + "?",
                "Confirmar eliminación",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        originalReglasAvanzadasList.remove(regla);
                        applyFilters();
                        Messagebox.show("Regla avanzada eliminada correctamente", "Información",
                                Messagebox.OK, Messagebox.INFORMATION);
                    }
                });
    }

    private void loadData() {
        loadReglasBasicas();
        loadReglasAvanzadas();
    }

    private void loadReglasBasicas() {
        originalReglasBasicasList = new ArrayList<>();

        // Datos de ejemplo para reglas básicas
        String[] nombres = {"Regla CALLE", "Regla CELULAR", "Regla CP", "Regla DEPTO", "Regla DOMINIO",
                "Regla LOCALIDAD", "Regla MARCA", "Regla MODELO", "Regla NRO_DOCUMENTO", "Regla NUMERO", "Regla PARTIDO"};
        String[] columnas = {"CALLE", "CELULAR", "CP", "DEPTO", "DOMINIO", "LOCALIDAD", "MARCA", "MODELO", "NRO_DOCUMENTO", "NUMERO", "PARTIDO"};

        for (int i = 0; i < nombres.length; i++) {
            ReglaBasica regla = new ReglaBasica();
            regla.setId((long) (i + 1));
            regla.setNombre(nombres[i]);
            regla.setColumna(columnas[i]);
            regla.setCreador("Pedro");
            regla.setModificadoPor("Pedro");
            regla.setFechaCreacion(new Date());
            regla.setFechaModificacion(new Date());

            // Configurar tipos de reglas aleatoriamente
            Random random = new Random();
            regla.setTieneNoNulo(random.nextBoolean());
            regla.setTieneTexto(random.nextBoolean());
            regla.setTieneNumero(random.nextBoolean());

            // Asegurar que al menos una esté activa
            if (!regla.isTieneNoNulo() && !regla.isTieneTexto() && !regla.isTieneNumero()) {
                regla.setTieneTexto(true);
            }

            originalReglasBasicasList.add(regla);
        }

        reglasBasicasList = new ArrayList<>(originalReglasBasicasList);
    }

    private void loadReglasAvanzadas() {
        originalReglasAvanzadasList = new ArrayList<>();

        // Datos de ejemplo para reglas avanzadas
        String[][] datosAvanzadas = {
                {"CP", "Contiene", ""},
                {"NRO_DOCUMENTO", "Contiene", ""},
                {"NRO_CUIT", "Contiene", ""},
                {"TIPO", "Distinto a", "0"},
                {"CP", "Distinto a", "9999"},
                {"NRO_DOCUMENTO", "Distinto a", "99999999"},
                {"MODELO", "No Contiene", ""},
                {"CALLE", "No Contiene", "NO CONSTA"}
        };

        for (int i = 0; i < datosAvanzadas.length; i++) {
            ReglaAvanzada regla = new ReglaAvanzada();
            regla.setId((long) (i + 1));
            regla.setColumna(datosAvanzadas[i][0]);
            regla.setOperacion(datosAvanzadas[i][1]);
            regla.setValor(datosAvanzadas[i][2]);
            regla.setCreador(i < 3 ? "Pedro" : (i == 7 ? "Admin" : "Pedro"));
            regla.setModificadoPor("");
            regla.setFechaCreacion(new Date());
            regla.setFechaModificacion(new Date());

            originalReglasAvanzadasList.add(regla);
        }

        reglasAvanzadasList = new ArrayList<>(originalReglasAvanzadasList);
    }

    private void applyFilters() {
        // Filtrar reglas básicas
        List<ReglaBasica> filteredBasicas = new ArrayList<>(originalReglasBasicasList);
        if (searchText != null && !searchText.trim().isEmpty()) {
            filteredBasicas = filteredBasicas.stream()
                    .filter(regla -> regla.getNombre().toLowerCase().contains(searchText.toLowerCase()) ||
                            regla.getColumna().toLowerCase().contains(searchText.toLowerCase()))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }
        reglasBasicasList = filteredBasicas;

        // Filtrar reglas avanzadas
        List<ReglaAvanzada> filteredAvanzadas = new ArrayList<>(originalReglasAvanzadasList);
        if (searchText != null && !searchText.trim().isEmpty()) {
            filteredAvanzadas = filteredAvanzadas.stream()
                    .filter(regla -> regla.getColumna().toLowerCase().contains(searchText.toLowerCase()) ||
                            regla.getOperacion().toLowerCase().contains(searchText.toLowerCase()) ||
                            (regla.getValor() != null && regla.getValor().toLowerCase().contains(searchText.toLowerCase())))
                    .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
        }
        reglasAvanzadasList = filteredAvanzadas;
    }
}