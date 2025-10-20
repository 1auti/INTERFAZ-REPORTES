package reporte;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.reporte.core.dto.response.MetadataQuery;
import org.zkoss.reporte.core.model.ColumnaDef;
import org.zkoss.reporte.core.service.interfaces.ReporteService;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zul.*;
import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.request.ParametrosFiltros;
import org.zkoss.reporte.core.dto.response.QueryResponse;
import org.zkoss.reporte.core.service.interfaces.DatabaseQueryService;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ViewModel para la ejecución de queries dinámicas con soporte de consolidación.
 * Maneja la carga de metadata, ejecución de consultas, filtros y exportación.
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
@Getter
@Setter
public class ejecutar_query {

    // ===== SERVICIOS =====
    private DatabaseQueryService databaseQueryService;
    private ReporteService infraccionesService;

    // ===== PROPIEDADES DE LA QUERY =====
    private String codigoQuery;
    private String queryNombre = "Ejecutar Consulta";
    private String queryDescripcion = "Seleccione los filtros y ejecute la consulta";
    private MetadataQuery queryMetadata;

    // ===== CAMPOS DE CONSOLIDACIÓN =====
    private List<String> camposAgrupacion = new ArrayList<>();
    private List<String> camposNumericos = new ArrayList<>();
    private List<String> camposUbicacion = new ArrayList<>();
    private List<String> camposTiempo = new ArrayList<>();

    // Campos seleccionados por el usuario
    private List<String> camposAgrupacionSeleccionados = new ArrayList<>();
    private List<String> camposNumericosSeleccionados = new ArrayList<>();
    private List<String> camposUbicacionSeleccionados = new ArrayList<>();
    private List<String> camposTiempoSeleccionados = new ArrayList<>();

    // ⭐ CAMPO PRINCIPAL: Lista de strings para grupos de consolidación
    private List<String> consolidacion = new ArrayList<>();
    private boolean consolidado;

    // ===== FILTROS =====
    private ParametrosFiltros filtros;
    private Date fechaDesde;
    private Date fechaHasta;
    private boolean mostrarFiltros = true;

    // ===== RESULTADOS =====
    private QueryResponse resultadoQuery;
    private ListModelList<Map<String, Object>> resultados;
    private List<ColumnaDef> columnas;
    private List<String> columnasResultado = new ArrayList<>();
    private boolean hayResultados = false;
    private Integer totalRegistros = 0;
    private String tiempoEjecucion = "0 ms";

    // ===== ESTADO =====
    private boolean ejecutando = false;
    private String formatoExportar = "excel";

    // Funciones de agregación
    private boolean funcionSum = true;
    private boolean funcionAvg = false;
    private boolean funcionCount = false;
    private boolean funcionMax = false;
    private boolean funcionMin = false;

    // Opciones de consolidación
    private String nivelConsolidacion = "completo";
    private boolean incluirSubtotales = false;

    // ===== INICIALIZACIÓN =====

    /**
     * Inicializa el ViewModel con el código de query proporcionado.
     * Carga los servicios desde el contexto de Spring y la metadata de la query.
     */
    @Init
    public void init(@QueryParam("codigo") String codigo) {
        log.info("🚀 Inicializando ViewModel de Ejecución de Query - Código: {}", codigo);

        // Obtener servicios de Spring desde el contexto web
        ServletContext servletContext = Executions.getCurrent().getDesktop().getWebApp().getServletContext();
        WebApplicationContext webAppCtx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        databaseQueryService = webAppCtx.getBean(DatabaseQueryService.class);
        infraccionesService = webAppCtx.getBean(ReporteService.class);

        // Inicializar filtros vacíos
        filtros = new ParametrosFiltros();

        // Validar y cargar query si se proporcionó código
        if (codigo != null && !codigo.isEmpty()) {
            this.codigoQuery = codigo;
            cargarMetadataQuery();
        } else {
            log.warn("⚠️ No se proporcionó código de query");
            mostrarAdvertencia("No se especificó una query para ejecutar");
        }
    }

    /**
     * Método ejecutado después de que todos los componentes ZUL están compuestos.
     */
    @AfterCompose
    public void afterCompose() {
        log.info("✅ ViewModel inicializado correctamente");
    }

    // ===== MÉTODOS DE CARGA =====

    /**
     * Carga la metadata de la query desde el servicio y actualiza
     * todas las propiedades relacionadas, incluyendo los campos de consolidación.
     */
    @NotifyChange({"queryMetadata", "queryNombre", "queryDescripcion",
            "camposAgrupacion", "camposNumericos", "camposUbicacion", "camposTiempo"})
    private void cargarMetadataQuery() {
        try {
            if (databaseQueryService != null && codigoQuery != null) {
                log.info("📥 Cargando metadata de query: {}", codigoQuery);

                // Obtener metadata del servicio
                queryMetadata = databaseQueryService.obtenerQuery(codigoQuery);

                if (queryMetadata != null) {
                    // Actualizar información básica
                    queryNombre = queryMetadata.getNombre();
                    queryDescripcion = queryMetadata.getDescripcion() != null
                            ? queryMetadata.getDescripcion()
                            : "Ejecute la consulta para ver los resultados";

                    // Actualizar las propiedades de campos de consolidación
                    actualizarCamposConsolidacion();

                    log.info("✅ Metadata cargada - Query: {} ({})",
                            queryNombre,
                            queryMetadata.getEsConsolidable() ? "Consolidable" : "Normal");

                } else {
                    log.warn("⚠️ No se encontró metadata para la query: {}", codigoQuery);
                    mostrarAdvertencia("No se encontró información de la query");
                }
            }
        } catch (Exception e) {
            log.error("❌ Error cargando metadata de query: {}", e.getMessage(), e);
            mostrarError("Error al cargar información de la query");
        }
    }

    /**
     * Actualiza las propiedades de campos de consolidación desde queryMetadata.
     * Extrae los diferentes tipos de campos disponibles para consolidación.
     */
    private void actualizarCamposConsolidacion() {
        // Inicializar con listas vacías por defecto
        camposAgrupacion = new ArrayList<>();
        camposNumericos = new ArrayList<>();
        camposUbicacion = new ArrayList<>();
        camposTiempo = new ArrayList<>();

        // Solo actualizar si la metadata existe y es consolidable
        if (queryMetadata != null && Boolean.TRUE.equals(queryMetadata.getEsConsolidable())) {

            // Extraer campos de agrupación
            if (queryMetadata.getCamposAgrupacionList() != null) {
                camposAgrupacion = new ArrayList<>(queryMetadata.getCamposAgrupacionList());
                log.debug("📋 Campos agrupación actualizados: {}", camposAgrupacion.size());
            }

            // Extraer campos numéricos
            if (queryMetadata.getCamposNumericosList() != null) {
                camposNumericos = new ArrayList<>(queryMetadata.getCamposNumericosList());
                log.debug("🔢 Campos numéricos actualizados: {}", camposNumericos.size());
            }

            // Extraer campos de ubicación
            if (queryMetadata.getCamposUbicacionList() != null) {
                camposUbicacion = new ArrayList<>(queryMetadata.getCamposUbicacionList());
                log.debug("📍 Campos ubicación actualizados: {}", camposUbicacion.size());
            }

            // Extraer campos de tiempo
            if (queryMetadata.getCamposTiempoList() != null) {
                camposTiempo = new ArrayList<>(queryMetadata.getCamposTiempoList());
                log.debug("⏰ Campos tiempo actualizados: {}", camposTiempo.size());
            }
        }
    }

    // ===== COMANDOS =====

    /**
     * Ejecuta la query con los filtros especificados y muestra los resultados.
     * Incluye manejo de consolidación si está activado.
     */
    @Command
    @NotifyChange({"resultados", "hayResultados", "totalRegistros",
            "tiempoEjecucion", "ejecutando", "columnasResultado"})
    public void ejecutarQuery() {
        if (codigoQuery == null || codigoQuery.isEmpty()) {
            mostrarAdvertencia("No se ha especificado una query para ejecutar");
            return;
        }

        try {
            ejecutando = true;
            log.debug("▶️ Ejecutando query: {}", codigoQuery);

            // Preparar filtros incluyendo consolidación
            prepararFiltros();

            // Construir objeto de consulta
            ConsultaQuery consulta = ConsultaQuery.builder()
                    .parametrosFiltros(filtros)
                    .formato("json")
                    .build();

            // Medir tiempo de ejecución
            long inicio = System.currentTimeMillis();

            // Ejecutar consulta
            resultadoQuery = infraccionesService.ejecutarConsulta(codigoQuery, consulta);

            long fin = System.currentTimeMillis();
            tiempoEjecucion = (fin - inicio) + " ms";

            // Procesar resultados
            if (resultadoQuery != null && resultadoQuery.getDatos() != null) {
                List<Map<String, Object>> datos = resultadoQuery.getDatos();

                // Extraer columnas del primer registro
                if (!datos.isEmpty()) {
                    columnasResultado = new ArrayList<>(datos.get(0).keySet());
                    log.info("🔍 Columnas encontradas: {}", columnasResultado);
                }

                // Convertir datos a ListModelList para ZK
                resultados = new ListModelList<>(datos);
                hayResultados = !datos.isEmpty();
                totalRegistros = datos.size();

//                crearHeadersListbox();

                log.debug("✅ Query ejecutada exitosamente - {} registros en {}",
                        totalRegistros, tiempoEjecucion);

                // Log adicional si se aplicó consolidación
                if (Boolean.TRUE.equals(filtros.getConsolidado()) &&
                        filtros.getConsolidacion() != null &&
                        !filtros.getConsolidacion().isEmpty()) {
                    log.debug("📊 Consolidación aplicada con {} campos: {}",
                            filtros.getConsolidacion().size(),
                            filtros.getConsolidacion());
                }

                mostrarInformacion("Consulta ejecutada exitosamente");
            } else {
                // Sin resultados
                hayResultados = false;
                totalRegistros = 0;
                resultados = new ListModelList<>();
                columnasResultado = new ArrayList<>();
                log.warn("⚠️ La consulta no devolvió resultados");
                mostrarAdvertencia("La consulta no devolvió resultados");
            }

        } catch (Exception e) {
            log.error("❌ Error ejecutando query: {}", e.getMessage(), e);
            mostrarError("Error al ejecutar la consulta: " + e.getMessage());
            hayResultados = false;
            totalRegistros = 0;
            resultados = new ListModelList<>();
            columnasResultado = new ArrayList<>();
        } finally {
            ejecutando = false;
        }
    }

    /**
     * Extrae los valores string de una lista de Listitem seleccionados.
     * ZK devuelve objetos Listitem, necesitamos extraer el valor real.
     */
    private List<String> extraerValoresSeleccionados(List<?> selectedItems) {
        List<String> valores = new ArrayList<>();

        if (selectedItems != null && !selectedItems.isEmpty()) {
            for (Object item : selectedItems) {
                if (item instanceof org.zkoss.zul.Listitem) {
                    org.zkoss.zul.Listitem listitem = (org.zkoss.zul.Listitem) item;
                    Object value = listitem.getValue();
                    if (value != null) {
                        valores.add(value.toString());
                    }
                } else if (item instanceof String) {
                    // Si ya es String directamente
                    valores.add((String) item);
                }
            }
        }

        return valores;
    }

    /**
     * Crea los headers del listbox dinámicamente después de ejecutar la query.
     * Este método busca el listbox y crea los headers basándose en columnasResultado.
     */
    /**
     * Crea los headers del listbox dinámicamente después de ejecutar la query.
     * Usa Events.postEvent para asegurar que el listbox ya está renderizado.
     */
    /**
     * Crea los headers del listbox dinámicamente después de ejecutar la query.
     * Usa un timer para asegurar que el componente está disponible.
     */
    private void crearHeadersListbox() {
        if (columnasResultado == null || columnasResultado.isEmpty()) {
            log.warn("⚠️ No hay columnas para crear headers");
            return;
        }

        // ⭐ Crear un timer para ejecutar después del renderizado
        org.zkoss.zk.ui.util.Clients.evalJavaScript(
                "setTimeout(function() { zAu.send(new zk.Event(zk.Widget.$('#listboxResultados'), 'onCreateHeaders')); }, 100);"
        );
    }

    /**
     * Crea y retorna un Listhead con headers dinámicos.
     * Este método se llama desde un getter para que ZK lo renderice automáticamente.
     */
    public Listhead getListheadDinamico() {
        if (columnasResultado == null || columnasResultado.isEmpty()) {
            return null;
        }

        Listhead listhead = new Listhead();
        listhead.setStyle("background: linear-gradient(to bottom, #f8f9fa 0%, #e9ecef 100%);");

        for (String nombreColumna : columnasResultado) {
            Listheader header = new Listheader();
            header.setLabel(nombreColumna);
            header.setHflex("1");
            header.setAlign("left");
            header.setStyle("font-weight: 600; padding: 12px 8px; border-right: 1px solid #dee2e6;");
            header.setParent(listhead);
        }

        return listhead;
    }

    /**
     * Evento personalizado para crear headers después del renderizado.
     */
    @Command
    public void crearHeaders() {
        try {
            Component listboxComp = Executions.getCurrent().getDesktop()
                    .getFirstPage().getFellow("listboxResultados");

            if (listboxComp instanceof Listbox) {
                Listbox listbox = (Listbox) listboxComp;

                // Limpiar listhead existente
                if (listbox.getListhead() != null) {
                    listbox.getListhead().detach();
                }

                // Crear nuevo listhead
                Listhead listhead = new Listhead();
                listhead.setStyle("background: linear-gradient(to bottom, #f8f9fa 0%, #e9ecef 100%);");
                listhead.setParent(listbox);

                // Crear listheader para cada columna
                for (String nombreColumna : columnasResultado) {
                    Listheader header = new Listheader();
                    header.setLabel(nombreColumna);
                    header.setHflex("1");
                    header.setAlign("left");
                    header.setStyle("font-weight: 600; padding: 12px 8px; border-right: 1px solid #dee2e6;");
                    header.setParent(listhead);
                }

                log.info("✅ {} headers creados", columnasResultado.size());
            }
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
        }
    }

    /**
     * Obtiene las columnas de los resultados.
     * Este método DEBE ser público para que ZK pueda accederlo.
     */
    public List<String> getColumnasResultado() {
        return columnasResultado != null ? columnasResultado : new ArrayList<>();
    }

    /**
     * Actualiza las columnas del grid de resultados dinámicamente.
     * Se ejecuta después de obtener los datos de la query.
     */
    private void actualizarColumnasGrid() {
        if (columnasResultado == null || columnasResultado.isEmpty()) {
            log.warn("⚠️ No hay columnas para actualizar");
            return;
        }

        try {
            // Buscar el grid en la página
            Component comp = Executions.getCurrent().getDesktop().getFirstPage()
                    .getFellow("gridResultados");

            if (comp instanceof Grid) {
                Grid grid = (Grid) comp;

                // Limpiar columnas existentes
                if (grid.getColumns() != null) {
                    grid.getColumns().detach();
                }

                // Crear nuevas columnas
                Columns columns = new Columns();
                columns.setParent(grid);

                for (String nombreColumna : columnasResultado) {
                    Column col = new Column();
                    col.setLabel(nombreColumna);
                    col.setHflex("1");
                    col.setAlign("left");
                    col.setParent(columns);
                }

                log.info("✅ {} columnas creadas en el grid", columnasResultado.size());
            }
        } catch (Exception e) {
            log.error("❌ Error actualizando columnas del grid: {}", e.getMessage(), e);
        }
    }

    /**
     * Establece las columnas de los resultados.
     */
    public void setColumnasResultado(List<String> columnasResultado) {
        this.columnasResultado = columnasResultado;
        log.debug("🔍 Columnas establecidas: {}", columnasResultado);
    }

    /**
     * Exporta los resultados actuales a un archivo Excel.
     * Genera un archivo con formato y nombre descriptivo.
     */
    @Command
    public void exportarExcel() {
        if (!hayResultados) {
            mostrarAdvertencia("No hay resultados para exportar");
            return;
        }

        try {
            log.info("📊 Exportando resultados a Excel - Query: {}", codigoQuery);

            // Preparar filtros para la descarga
            prepararFiltros();

            // Construir consulta con formato Excel
            ConsultaQuery consulta = ConsultaQuery.builder()
                    .parametrosFiltros(filtros)
                    .formato("excel")
                    .build();

            // Generar archivo Excel a través del servicio
            byte[] archivo = infraccionesService != null
                    ? infraccionesService.descargarConsulta(codigoQuery, consulta)
                    : null;

            if (archivo != null && archivo.length > 0) {
                // Generar nombre descriptivo para el archivo
                String nombreArchivo = generarNombreArchivo();

                // Iniciar descarga en el navegador
                org.zkoss.zul.Filedownload.save(
                        new ByteArrayInputStream(archivo),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        nombreArchivo
                );

                log.info("✅ Archivo exportado exitosamente: {}", nombreArchivo);
                mostrarInformacion("Archivo descargado exitosamente");
            } else {
                log.warn("⚠️ No se pudo generar el archivo de exportación");
                mostrarAdvertencia("No se pudo generar el archivo de exportación");
            }

        } catch (Exception e) {
            log.error("❌ Error exportando a Excel: {}", e.getMessage(), e);
            mostrarError("Error al exportar: " + e.getMessage());
        }
    }

    /**
     * Limpia todos los filtros, selecciones y resultados actuales.
     * Resetea el estado del formulario a su estado inicial.
     */
    @Command
    @NotifyChange({"filtros", "fechaDesde", "fechaHasta", "resultados",
            "hayResultados", "totalRegistros", "tiempoEjecucion",
            "consolidacion", "consolidado", "camposAgrupacionSeleccionados",
            "camposNumericosSeleccionados"})
    public void limpiar() {
        log.info("🧹 Limpiando filtros y resultados");

        // Resetear filtros
        filtros = new ParametrosFiltros();
        fechaDesde = null;
        fechaHasta = null;

        // Limpiar consolidación
        consolidacion = new ArrayList<>();
        consolidado = false;
        camposAgrupacionSeleccionados = new ArrayList<>();
        camposNumericosSeleccionados = new ArrayList<>();

        // Limpiar resultados
        resultados = new ListModelList<>();
        hayResultados = false;
        totalRegistros = 0;
        tiempoEjecucion = "0 ms";
        columnasResultado = new ArrayList<>();

        mostrarInformacion("Filtros y resultados limpiados");
    }

    /**
     * Recarga la metadata de la query actual.
     * Útil para actualizar información después de cambios en la configuración.
     */
    @Command
    @NotifyChange("*")
    public void recargarQuery() {
        log.info("🔄 Recargando metadata de la query");
        cargarMetadataQuery();
        mostrarInformacion("Query recargada");
    }

    /**
     * Crea las columnas dinámicamente después de obtener los resultados.
     * Este método se llama desde el ZUL mediante el atributo afterCompose.
     */
    @Command
    @NotifyChange("*")
    public void crearColumnasGrid(@BindingParam("grid") Component grid) {
        if (!(grid instanceof Grid) || columnasResultado == null || columnasResultado.isEmpty()) {
            return;
        }

        Grid gridComponent = (Grid) grid;

        // Limpiar columnas existentes
        if (gridComponent.getColumns() != null) {
            gridComponent.getColumns().getChildren().clear();
        }

        // Crear columnas dinámicamente
        Columns columns = new Columns();
        columns.setParent(gridComponent);

        for (String nombreColumna : columnasResultado) {
            Column col = new Column();
            col.setLabel(nombreColumna);
            col.setHflex("1");
            col.setParent(columns);
        }

        log.info("✅ Columnas creadas dinámicamente: {}", columnasResultado.size());
    }

    /**
     * Comando ejecutado cuando cambia el estado del checkbox de consolidado.
     * Limpia las selecciones de consolidación si se desmarca.
     */
    @Command
    @NotifyChange({"consolidacion", "camposAgrupacionSeleccionados",
            "camposNumericosSeleccionados", "camposUbicacionSeleccionados",
            "camposTiempoSeleccionados"})
    public void onConsolidadoChange() {
        if (!consolidado) {
            // Limpiar todas las selecciones de consolidación
            consolidacion = new ArrayList<>();
            camposAgrupacionSeleccionados = new ArrayList<>();
            camposNumericosSeleccionados = new ArrayList<>();
            camposUbicacionSeleccionados = new ArrayList<>();
            camposTiempoSeleccionados = new ArrayList<>();

            log.info("🧹 Selecciones de consolidación limpiadas");
        } else {
            log.info("📊 Modo consolidación activado");
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Prepara los filtros para la ejecución de la query.
     * Convierte las fechas a formato String y agrega los parámetros de consolidación.
     */
    /**
     * Prepara los filtros para la ejecución de la query.
     * Convierte las fechas a formato String y combina TODOS los campos seleccionados
     * en una única lista 'consolidacion' que el backend espera.
     */
    private void prepararFiltros() {
        if (filtros == null) {
            filtros = new ParametrosFiltros();
        }

        // ===== FORMATO DE FECHAS =====
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Convertir fecha inicio si está definida
        if (fechaDesde != null) {
            filtros.setFechaInicio(sdf.format(fechaDesde));
            log.debug("📅 Fecha inicio: {}", filtros.getFechaInicio());
        }

        // Convertir fecha fin si está definida
        if (fechaHasta != null) {
            filtros.setFechaFin(sdf.format(fechaHasta));
            log.debug("📅 Fecha fin: {}", filtros.getFechaFin());
        }

        // ===== PARÁMETROS DE CONSOLIDACIÓN =====
        if (consolidado) {
            // ⭐ Marcar que está consolidado
            filtros.setConsolidado(true);

            // ⭐ COMBINAR todos los campos seleccionados en UNA SOLA LISTA
            List<String> camposCombinados = new ArrayList<>();

            // Agregar campos de agrupación
            if (camposAgrupacionSeleccionados != null && !camposAgrupacionSeleccionados.isEmpty()) {
                camposCombinados.addAll(camposAgrupacionSeleccionados);
                log.debug("   ✓ Agregados {} campos de agrupación", camposAgrupacionSeleccionados.size());
            }

            // Agregar campos numéricos
            if (camposNumericosSeleccionados != null && !camposNumericosSeleccionados.isEmpty()) {
                camposCombinados.addAll(camposNumericosSeleccionados);
                log.debug("   ✓ Agregados {} campos numéricos", camposNumericosSeleccionados.size());
            }

            // Agregar campos de ubicación
            if (camposUbicacionSeleccionados != null && !camposUbicacionSeleccionados.isEmpty()) {
                camposCombinados.addAll(camposUbicacionSeleccionados);
                log.debug("   ✓ Agregados {} campos de ubicación", camposUbicacionSeleccionados.size());
            }

            // Agregar campos de tiempo
            if (camposTiempoSeleccionados != null && !camposTiempoSeleccionados.isEmpty()) {
                camposCombinados.addAll(camposTiempoSeleccionados);
                log.debug("   ✓ Agregados {} campos de tiempo", camposTiempoSeleccionados.size());
            }

            // ⭐ Asignar la lista combinada al filtro
            if (!camposCombinados.isEmpty()) {
                filtros.setConsolidacion(camposCombinados);
                log.info("📊 Lista de consolidación preparada con {} campos: {}",
                        camposCombinados.size(), camposCombinados);
            } else {
                log.warn("⚠️ Consolidación activada pero no hay campos seleccionados");
                filtros.setConsolidacion(new ArrayList<>());
            }

            // Log detallado de lo que se enviará
            log.info("✅ Parámetros de consolidación:");
            log.info("   - consolidado: true");
            log.info("   - consolidacion: {}", camposCombinados);

        } else {
            // Si no está consolidado, limpiar parámetros de consolidación
            filtros.setConsolidado(false);
            filtros.setConsolidacion(null);
            log.info("ℹ️ Modo consolidación: DESACTIVADO");
        }
    }

    /**
     * Genera un nombre de archivo descriptivo con timestamp.
     * Formato: nombreQuery_YYYYMMDD_HHMMSS.xlsx
     */
    private String generarNombreArchivo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());

        // Sanitizar nombre de la query para usar como nombre de archivo
        String nombreBase = queryNombre != null
                ? queryNombre.replaceAll("[^a-zA-Z0-9]", "_")
                : "reporte";

        return nombreBase + "_" + timestamp + ".xlsx";
    }

    // ===== GETTERS Y SETTERS PERSONALIZADOS =====

    /**
     * Setter de consolidado con notificación de cambios.
     */
    @NotifyChange({"consolidado"})
    public void setConsolidado(boolean consolidado) {
        boolean cambio = this.consolidado != consolidado;
        this.consolidado = consolidado;

        if (cambio) {
            log.info("✓ Consolidado: {}", consolidado);

            // Si se desmarca, ejecutar limpieza
            if (!consolidado) {
                onConsolidadoChange();
            }
        }
    }

    /**
     * Getter para la lista de consolidación con log de debug.
     */
    public List<String> getConsolidacion() {
        log.debug("📋 Obteniendo lista de consolidación: {} elementos",
                consolidacion != null ? consolidacion.size() : 0);
        return consolidacion;
    }

    /**
     * Setter para la lista de consolidación con log informativo.
     */
    public void setConsolidacion(List<String> consolidacion) {
        this.consolidacion = consolidacion;
        log.info("✓ Lista de consolidación actualizada: {} elementos - {}",
                consolidacion != null ? consolidacion.size() : 0,
                consolidacion);
    }

    /**
     * Setter para campos de agrupación seleccionados con log.
     */
    public void setCamposAgrupacionSeleccionados(List<String> camposAgrupacionSeleccionados) {
        this.camposAgrupacionSeleccionados = camposAgrupacionSeleccionados;
        log.info("✓ Campos agrupación seleccionados: {}", camposAgrupacionSeleccionados);
    }

    /**
     * Setter para campos numéricos seleccionados con log.
     */
    public void setCamposNumericosSeleccionados(List<String> camposNumericosSeleccionados) {
        this.camposNumericosSeleccionados = camposNumericosSeleccionados;
        log.info("✓ Campos numéricos seleccionados: {}", camposNumericosSeleccionados);
    }

    // ===== GETTERS COMPUTADOS =====

    /**
     * Verifica si la query actual es consolidable.
     */
    public boolean isQueryConsolidable() {
        return queryMetadata != null &&
                Boolean.TRUE.equals(queryMetadata.getEsConsolidable());
    }

    /**
     * Obtiene la categoría de la query actual.
     */
    public String getCategoria() {
        return queryMetadata != null ? queryMetadata.getCategoria() : null;
    }

    /**
     * Verifica si existen datos en los resultados.
     */
    public boolean hayDatos() {
        return resultados != null && !resultados.isEmpty();
    }

    // ===== MENSAJES AL USUARIO =====

    /**
     * Muestra un mensaje informativo al usuario.
     */
    private void mostrarInformacion(String mensaje) {
        Messagebox.show(mensaje, "Información",
                Messagebox.OK, Messagebox.INFORMATION);
    }

    /**
     * Muestra un mensaje de advertencia al usuario.
     */
    private void mostrarAdvertencia(String mensaje) {
        Messagebox.show(mensaje, "Advertencia",
                Messagebox.OK, Messagebox.EXCLAMATION);
    }

    /**
     * Muestra un mensaje de error al usuario.
     */
    private void mostrarError(String mensaje) {
        Messagebox.show(mensaje, "Error",
                Messagebox.OK, Messagebox.ERROR);
    }
}