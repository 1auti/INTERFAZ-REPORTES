package detalleLoteVerificados;

import lombok.Getter;
import lombok.Setter;
import org.zkoss.dominial.core.entity.*;
import org.zkoss.dominial.core.enums.EstadoDominial;
import org.zkoss.dominial.core.enums.TipoVehiculo;
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
import java.util.stream.Collectors;

import static org.zkoss.dominial.web.viewmodel.MainApplicationVM.NAVIGATION;

@Getter @Setter
public class detalleLoteVerificados {
    // Navigation
    private NavigationService navigationModel;

    // Información del contexto
    private DetalleLote detalleLoteSeleccionado;
    private Lote loteSeleccionado;

    // Search and filters
    private String searchText = "";
    private List<Dominial> dominialesList;
    private List<Dominial> originalDominialesList;
    private FilterOption selectedEstadoFilter;
    private List<FilterOption> estadoFilterOptions;

    // Estadísticas
    private int totalDominios;
    private int verificados;
    private int pendientes;
    private int rechazados;
    private String ultimaActualizacion;

    private Date fechaVerificacionDesde = new Date();
    private Date fechaVerificacionHasta = new Date();

    @Init
    public void init() {
        System.out.println("=== INICIALIZANDO DetalleDominiosVerificados ViewModel ===");

        initializeNavigationService();
        initializeContext();
        initializeFilters();
        loadData();
        updateStatistics();

        System.out.println("=== DetalleDominiosVerificados ViewModel INICIALIZADO CORRECTAMENTE ===");
    }

    private void initializeNavigationService() {
        navigationModel = (NavigationService) Executions.getCurrent().getDesktop().getAttribute(NAVIGATION);
        if (navigationModel == null) {
            navigationModel = new NavigationService();
            Executions.getCurrent().getDesktop().setAttribute(NAVIGATION, navigationModel);
        }
    }

    private void initializeContext() {
        try {
            // Recuperar el detalle de lote seleccionado desde la sesión
            detalleLoteSeleccionado = (DetalleLote) Executions.getCurrent().getSession().getAttribute("selectedDetalleLote");
            loteSeleccionado = (Lote) Executions.getCurrent().getSession().getAttribute("selectedLote");

            if (detalleLoteSeleccionado != null && loteSeleccionado != null) {
                System.out.println("Contexto cargado - Lote: " + loteSeleccionado.getId() +
                        ", Archivo: " + detalleLoteSeleccionado.getNombreArchivo());

                // Actualizar el título de navegación
                String location = "Dominios Verificados > " + detalleLoteSeleccionado.getNombreArchivo();
                if (navigationModel != null) {
                    navigationModel.setLocation(location);
                }
            } else {
                System.out.println("No se encontró contexto en sesión, creando datos de ejemplo");
                // Crear datos de ejemplo
                loteSeleccionado = new Lote();
                loteSeleccionado.setId(123456789);
                loteSeleccionado.setIdMunicipio("MUN0001");

                detalleLoteSeleccionado = new DetalleLote();
                detalleLoteSeleccionado.setId(1L);
                detalleLoteSeleccionado.setNombreArchivo("lote_123456789_datos_principales.xlsx");
                detalleLoteSeleccionado.setVerificados(25);
            }
        } catch (Exception e) {
            System.out.println("Error al recuperar contexto de sesión: " + e.getMessage());
            // Crear datos de ejemplo en caso de error
            loteSeleccionado = new Lote();
            loteSeleccionado.setId(123456789);
            loteSeleccionado.setIdMunicipio("MUN0001");

            detalleLoteSeleccionado = new DetalleLote();
            detalleLoteSeleccionado.setId(1L);
            detalleLoteSeleccionado.setNombreArchivo("lote_123456789_datos_principales.xlsx");
            detalleLoteSeleccionado.setVerificados(25);
        }
    }

    private void initializeFilters() {
        estadoFilterOptions = Arrays.asList(
                new FilterOption("Todos los estados", null),
                new FilterOption("Verificado", EstadoDominial.VERIFICADO),
                new FilterOption("Pendiente", EstadoDominial.PENDIENTE),
                new FilterOption("Rechazado", EstadoDominial.RECHAZADO)
        );
        selectedEstadoFilter = estadoFilterOptions.get(1); // Por defecto, mostrar solo verificados
    }

    // Comandos de búsqueda y filtros
    @Command
    @NotifyChange({"dominialesList", "totalDominios", "paginationText"})
    public void search() {
        applyFilters();
    }

    @Command
    @NotifyChange({"dominialesList", "totalDominios", "paginationText"})
    public void filterByEstado() {
        applyFilters();
    }

    @Command
    @NotifyChange({"dominialesList", "searchText", "selectedEstadoFilter", "totalDominios", "paginationText"})
    public void clearFilters() {
        searchText = "";
        selectedEstadoFilter = estadoFilterOptions.get(1); // Mantener filtro de verificados
        dominialesList = new ArrayList<>(originalDominialesList.stream()
                .filter(d -> d.getEstado() == EstadoDominial.VERIFICADO)
                .collect(Collectors.toList()));
        updateStatistics();
    }

    // Comandos de acciones del header
    @Command
    public void importarExcel() {
        Messagebox.show("Funcionalidad: Importar archivo Excel con dominios verificados para " +
                        (detalleLoteSeleccionado != null ? detalleLoteSeleccionado.getNombreArchivo() : ""),
                "Importar Excel", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void exportarExcel() {
        Messagebox.show("Exportando " + dominialesList.size() + " registros de dominios verificados...",
                "Exportar Excel", Messagebox.OK, Messagebox.INFORMATION);
    }

    // Comandos de acciones por fila
    @Command
    public void editarDominio(@BindingParam("dominio") Dominial dominio) {
        Messagebox.show("Funcionalidad: Editar dominio verificado " + dominio.getDominio() +
                        "\nPersona: " + dominio.getPersona().getNombre() + " " + dominio.getPersona().getApellido(),
                "Editar Dominio Verificado", Messagebox.OK, Messagebox.INFORMATION);
    }

    @Command
    public void verDetalleDominio(@BindingParam("dominio") Dominial dominio) {
        StringBuilder detalle = new StringBuilder();
        detalle.append("DOMINIO: ").append(dominio.getDominio()).append("\n");
        detalle.append("ESTADO: ").append(dominio.getEstado().name()).append("\n");
        detalle.append("PERSONA: ").append(dominio.getPersona().getNombre()).append(" ").append(dominio.getPersona().getApellido()).append("\n");
        detalle.append("DNI: ").append(dominio.getPersona().getDni()).append("\n");
        if (dominio.getPersona().getContacto() != null && dominio.getPersona().getContacto().getMail() != null) {
            detalle.append("EMAIL: ").append(dominio.getPersona().getContacto().getMail()).append("\n");
        }
        if (dominio.getPersona().getVehiculo() != null) {
            detalle.append("VEHÍCULO: ").append(dominio.getPersona().getVehiculo().getMarca())
                    .append(" ").append(dominio.getPersona().getVehiculo().getModelo()).append("\n");
        }
        if (dominio.getObservacion() != null && !dominio.getObservacion().isEmpty()) {
            detalle.append("OBSERVACIÓN: ").append(dominio.getObservacion()).append("\n");
        }

        Messagebox.show(detalle.toString(), "Detalle del Dominio Verificado", Messagebox.OK, Messagebox.INFORMATION);
    }

    // Comando para volver al detalle de lote
    @Command
    public void volverADetalleLote() {
        try {
            // Limpiar la sesión de dominios
            Executions.getCurrent().getSession().removeAttribute("selectedDetalleLote");

            // Navegar de vuelta al detalle de lote
            if (navigationModel != null) {
                navigationModel.setContentUrl(NavigationService.DETALLE_BATCH_PATH);
                navigationModel.setLocation("Detalle Lote");

                // Notificar cambios
                BindUtils.postNotifyChange(null, null, navigationModel, "contentUrl");
                BindUtils.postNotifyChange(null, null, navigationModel, "location");
                BindUtils.postGlobalCommand(null, null, "refreshNavigation", null);
            }

        } catch (Exception e) {
            System.out.println("Error al volver al detalle de lote: " + e.getMessage());
        }
    }

    private void loadData() {
        originalDominialesList = new ArrayList<>();

        // Generar datos de ejemplo basados en el contexto - priorizando VERIFICADOS
        String[] dominios = {"VER001", "VER002", "VER003", "VER004", "VER005", "VER006", "VER007", "VER008",
                "VER009", "VER010", "VER011", "VER012", "VER013", "VER014", "VER015", "VER016",
                "VER017", "VER018", "ABC123", "DEF456", "GHI789", "JKL012", "MNO345", "PQR678", "STU901"};

        String[] nombres = {"Juan", "María", "Carlos", "Ana", "Luis", "Carmen", "José", "Elena", "Pedro", "Laura",
                "Miguel", "Rosa", "Diego", "Patricia", "Rafael"};
        String[] apellidos = {"García", "Rodríguez", "González", "Fernández", "López", "Martínez", "Sánchez",
                "Pérez", "Gómez", "Martín", "Jiménez", "Ruiz", "Hernández", "Díaz", "Moreno"};

        String[] marcas = {"Ford", "Chevrolet", "Toyota", "Honda", "Nissan", "Volkswagen", "Peugeot", "Renault", "Fiat", "Hyundai"};
        String[] modelos = {"Focus", "Corolla", "Civic", "Gol", "208", "Clio", "Palio", "Elantra", "Fiesta", "Onix"};

        Random random = new Random();
        Calendar cal = Calendar.getInstance();

        // Crear dominios con diferentes estados, priorizando verificados
        for (int i = 0; i < 25; i++) {
            Dominial dominial = new Dominial();

            dominial.setId((long) (i + 1));
            dominial.setIdLote(loteSeleccionado != null ? loteSeleccionado.getId() : 123456789L);
            dominial.setDominio(dominios[i]);

            // Fecha de trámite en los últimos 60 días
            cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(60));
            dominial.setFechaTramite(new Date(cal.getTimeInMillis()));
            cal = Calendar.getInstance(); // Reset

            // Estado: mayoría verificados
            if (i < 18) { // Primeros 18 verificados
                dominial.setEstado(EstadoDominial.VERIFICADO);
                dominial.setObservacion("Documentación completa y validada");
            } else if (i < 22) { // Algunos pendientes
                dominial.setEstado(EstadoDominial.PENDIENTE);
                dominial.setObservacion("Pendiente de verificación");
            } else { // Resto rechazados
                dominial.setEstado(EstadoDominial.RECHAZADO);
                dominial.setObservacion("Error en validación: " + getRandomError());
            }

            // Crear persona
            Persona persona = new Persona();
            persona.setId((long) (i + 1));
            persona.setNombre(nombres[random.nextInt(nombres.length)]);
            persona.setApellido(apellidos[random.nextInt(apellidos.length)]);
            persona.setDni(String.valueOf(10000000 + random.nextInt(40000000)));
            persona.setTipoDni("DNI");
            persona.setCuilCuit("20" + persona.getDni() + "5");

            // Crear contacto
            Contacto contacto = new Contacto();
            contacto.setId((long) (i + 1));
            contacto.setTelefono("011" + String.valueOf(10000000 + random.nextInt(90000000)));
            contacto.setCelular("11" + String.valueOf(10000000 + random.nextInt(90000000)));
            contacto.setMail(persona.getNombre().toLowerCase() + "." + persona.getApellido().toLowerCase() + "@email.com");
            persona.setContacto(contacto);

            // Crear domicilio
            Domicilio domicilio = new Domicilio();
            domicilio.setId((long) (i + 1));
            domicilio.setCalle("Av. Corrientes");
            domicilio.setNumero(String.valueOf(1000 + random.nextInt(9000)));
            domicilio.setLocalidad("CABA");
            domicilio.setProvincia("Buenos Aires");
            domicilio.setCp("1000");
            persona.setDomicilio(domicilio);

            // Crear vehículo
            Vehiculo vehiculo = new Vehiculo();
            vehiculo.setMarca(marcas[random.nextInt(marcas.length)]);
            vehiculo.setModelo(modelos[random.nextInt(modelos.length)]);
            vehiculo.setTipoVehiculo(TipoVehiculo.values()[random.nextInt(TipoVehiculo.values().length)]);
            persona.setVehiculo(vehiculo);

            dominial.setPersona(persona);

            originalDominialesList.add(dominial);
        }

        // Por defecto, mostrar solo los verificados
        dominialesList = originalDominialesList.stream()
                .filter(d -> d.getEstado() == EstadoDominial.VERIFICADO)
                .collect(Collectors.toList());
    }

    private String getRandomError() {
        String[] errores = {
                "DNI no válido", "Domicilio incompleto", "Email inválido",
                "Datos del vehículo inconsistentes", "Documentación faltante",
                "Error en formato de fecha", "Información duplicada"
        };
        return errores[new Random().nextInt(errores.length)];
    }

    private void applyFilters() {
        List<Dominial> filtered = new ArrayList<>(originalDominialesList);

        // Filtrar por texto de búsqueda
        if (searchText != null && !searchText.trim().isEmpty()) {
            String search = searchText.toLowerCase();
            filtered = filtered.stream()
                    .filter(dominial -> dominial.getDominio().toLowerCase().contains(search) ||
                            dominial.getPersona().getNombre().toLowerCase().contains(search) ||
                            dominial.getPersona().getApellido().toLowerCase().contains(search) ||
                            dominial.getPersona().getDni().contains(search))
                    .collect(Collectors.toList());
        }

        // Filtrar por estado
        if (selectedEstadoFilter != null && selectedEstadoFilter.getValue() != null) {
            EstadoDominial estadoFilter = selectedEstadoFilter.getValue();
            filtered = filtered.stream()
                    .filter(dominial -> dominial.getEstado().equals(estadoFilter))
                    .collect(Collectors.toList());
        }

        dominialesList = filtered;
        updateStatistics();
    }

    private void updateStatistics() {
        // Estadísticas sobre la lista completa original
        if (originalDominialesList != null) {
            totalDominios = originalDominialesList.size();
            verificados = (int) originalDominialesList.stream().filter(d -> d.getEstado() == EstadoDominial.VERIFICADO).count();
            pendientes = (int) originalDominialesList.stream().filter(d -> d.getEstado() == EstadoDominial.PENDIENTE).count();
            rechazados = (int) originalDominialesList.stream().filter(d -> d.getEstado() == EstadoDominial.RECHAZADO).count();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        ultimaActualizacion = sdf.format(new Date());
    }

    // Métodos para textos de estadísticas
    public String getTotalDominiosText() {
        return "Total dominios: " + totalDominios;
    }

    public String getVerificadosText() {
        return "Verificados: " + verificados;
    }

    public String getPendientesText() {
        return "Pendientes: " + pendientes;
    }

    public String getRechazadosText() {
        return "Rechazados: " + rechazados;
    }

    public String getUltimaActualizacionText() {
        return "Última actualización: " + ultimaActualizacion;
    }

    public String getPaginationText() {
        if (dominialesList == null || dominialesList.isEmpty()) {
            return "Sin registros";
        }
        return "Mostrando " + dominialesList.size() + " dominios";
    }

    public String getTituloDetalle() {
        if (detalleLoteSeleccionado != null) {
            return "Dominios Verificados - " + detalleLoteSeleccionado.getNombreArchivo();
        }
        return "Dominios Verificados";
    }

    public String getEstadoClass(EstadoDominial estado) {
        if (estado == null) return "estado-default";
        return "estado-" + estado.name().toLowerCase();
    }

    // Clase auxiliar para las opciones de filtro
    public static class FilterOption {
        private String label;
        private EstadoDominial value;

        public FilterOption(String label, EstadoDominial value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public EstadoDominial getValue() { return value; }
        public void setValue(EstadoDominial value) { this.value = value; }
    }

    // Método auxiliar para formatear fechas
    public String formatearFecha(Date fecha) {
        if (fecha == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(fecha);
    }
}