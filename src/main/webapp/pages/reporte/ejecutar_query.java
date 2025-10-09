package reporte;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.reporte.core.dto.response.MetadataQuery;
import org.zkoss.reporte.core.service.interfaces.ReporteService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.request.ParametrosFiltros;
import org.zkoss.reporte.core.dto.response.QueryResponse;
import org.zkoss.reporte.core.service.interfaces.DatabaseQueryService;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
@Slf4j
@Getter
@Setter
public class ejecutar_query {

    private DatabaseQueryService databaseQueryService;
    private ReporteService infraccionesService;

    // ===== PROPIEDADES DE LA QUERY =====
    private String codigoQuery;
    private String queryNombre = "Ejecutar Consulta";
    private String queryDescripcion = "Seleccione los filtros y ejecute la consulta";
    private MetadataQuery queryMetadata;

    // ===== CAMPOS DE CONSOLIDACIÓN (NUEVAS PROPIEDADES EXPLÍCITAS) =====
    // IMPORTANTE: Estas propiedades reemplazan los getters computados
    // para que ZK pueda trackear los cambios correctamente
    private List<String> camposAgrupacion = new ArrayList<>();
    private List<String> camposNumericos = new ArrayList<>();
    private List<String> camposUbicacion = new ArrayList<>();
    private List<String> camposTiempo = new ArrayList<>();

    // ===== FILTROS =====
    private ParametrosFiltros filtros;
    private Date fechaDesde;
    private Date fechaHasta;
    private boolean mostrarFiltros = true;

    // ===== RESULTADOS =====
    private QueryResponse resultadoQuery;
    private ListModelList<Map<String, Object>> resultados;
    private List<String> columnasResultado = new ArrayList<>();
    private boolean hayResultados = false;
    private Integer totalRegistros = 0;
    private String tiempoEjecucion = "0 ms";

    // ===== ESTADO =====
    private boolean ejecutando = false;
    private String formatoExportar = "excel";

    // ===== INICIALIZACIÓN =====

    @Init
    public void init(@QueryParam("codigo") String codigo) {
        log.info("Inicializando ViewModel de Ejecución de Query - Código: {}", codigo);

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
            log.warn("No se proporcionó código de query");
            mostrarAdvertencia("No se especificó una query para ejecutar");
        }
    }

    @AfterCompose
    public void afterCompose() {
        log.info("ViewModel inicializado correctamente");
    }

    // ===== MÉTODOS DE CARGA =====

    /**
     * Carga la metadata de la query desde el servicio y actualiza
     * todas las propiedades relacionadas, incluyendo los campos de consolidación.
     *
     * Este método es crítico para el funcionamiento correcto de la vista,
     * ya que notifica TODOS los cambios necesarios a ZK Framework.
     */
    @NotifyChange({"queryMetadata", "queryNombre", "queryDescripcion",
            "camposAgrupacion", "camposNumericos", "camposUbicacion", "camposTiempo"})
    private void cargarMetadataQuery() {
        try {
            if (databaseQueryService != null && codigoQuery != null) {
                log.info("Cargando metadata de query: {}", codigoQuery);

                // Obtener metadata del servicio
                queryMetadata = databaseQueryService.obtenerQuery(codigoQuery);

                if (queryMetadata != null) {
                    // Actualizar información básica
                    queryNombre = queryMetadata.getNombre();
                    queryDescripcion = queryMetadata.getDescripcion() != null
                            ? queryMetadata.getDescripcion()
                            : "Ejecute la consulta para ver los resultados";

                    // ⭐ CRUCIAL: Actualizar las propiedades de campos de consolidación
                    // Esto asegura que ZK Framework detecte los cambios
                    actualizarCamposConsolidacion();

                    log.info("Metadata cargada - Query: {} ({})",
                            queryNombre,
                            queryMetadata.getEsConsolidable() ? "Consolidable" : "Normal");

                    // DEBUG: Mostrar campos cargados
                    if (Boolean.TRUE.equals(queryMetadata.getEsConsolidable())) {
                        log.info("✅ Campos Agrupación: {}", camposAgrupacion);
                        log.info("✅ Campos Numéricos: {}", camposNumericos);
                        log.info("✅ Campos Ubicación: {}", camposUbicacion);
                        log.info("✅ Campos Tiempo: {}", camposTiempo);
                    }

                } else {
                    log.warn("No se encontró metadata para la query: {}", codigoQuery);
                    mostrarAdvertencia("No se encontró información de la query");
                }
            }
        } catch (Exception e) {
            log.error("Error cargando metadata de query: {}", e.getMessage(), e);
            mostrarError("Error al cargar información de la query");
        }
    }

    /**
     * Actualiza las propiedades de campos de consolidación desde queryMetadata.
     *
     * Este método separa la lógica de extracción de campos para mantener
     * el código más limpio y facilitar el debugging.
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
                log.debug("Campos agrupación actualizados: {}", camposAgrupacion.size());
            }

            // Extraer campos numéricos
            if (queryMetadata.getCamposNumericosList() != null) {
                camposNumericos = new ArrayList<>(queryMetadata.getCamposNumericosList());
                log.debug("Campos numéricos actualizados: {}", camposNumericos.size());
            }

            // Extraer campos de ubicación
            if (queryMetadata.getCamposUbicacionList() != null) {
                camposUbicacion = new ArrayList<>(queryMetadata.getCamposUbicacionList());
                log.debug("Campos ubicación actualizados: {}", camposUbicacion.size());
            }

            // Extraer campos de tiempo
            if (queryMetadata.getCamposTiempoList() != null) {
                camposTiempo = new ArrayList<>(queryMetadata.getCamposTiempoList());
                log.debug("Campos tiempo actualizados: {}", camposTiempo.size());
            }
        }
    }

    // ===== COMANDOS =====

    /**
     * Ejecuta la query con los filtros especificados y muestra los resultados.
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
            log.info("Ejecutando query: {}", codigoQuery);

            // Preparar filtros con fechas convertidas
            prepararFiltros();

            // Crear objeto de consulta
            ConsultaQuery consulta = ConsultaQuery.builder()
                    .parametrosFiltros(filtros)
                    .formato("json")
                    .build();

            // Medir tiempo de ejecución
            long inicio = System.currentTimeMillis();

            // Ejecutar query a través del servicio
            resultadoQuery = databaseQueryService.ejecutarQuery(codigoQuery, consulta);

            long fin = System.currentTimeMillis();
            tiempoEjecucion = (fin - inicio) + " ms";

            // Procesar y mostrar resultados
            if (resultadoQuery != null && resultadoQuery.getDatos() != null) {
                List<Map<String, Object>> datos = resultadoQuery.getDatos();

                resultados = new ListModelList<>(datos);
                hayResultados = !datos.isEmpty();
                totalRegistros = datos.size();

                // Extraer nombres de columnas del primer registro
                if (!datos.isEmpty()) {
                    columnasResultado = new ArrayList<>(datos.get(0).keySet());
                }

                log.info("✅ Query ejecutada exitosamente - {} registros en {}",
                        totalRegistros, tiempoEjecucion);

                mostrarInformacion("Consulta ejecutada exitosamente");
            } else {
                // No hay resultados
                hayResultados = false;
                totalRegistros = 0;
                resultados = new ListModelList<>();

                log.warn("⚠️ La query no devolvió resultados");
                mostrarAdvertencia("La consulta no devolvió resultados");
            }

        } catch (Exception e) {
            log.error("❌ Error ejecutando query: {}", e.getMessage(), e);
            mostrarError("Error al ejecutar la consulta: " + e.getMessage());

            // Limpiar resultados en caso de error
            hayResultados = false;
            totalRegistros = 0;
            resultados = new ListModelList<>();
        } finally {
            ejecutando = false;
        }
    }

    /**
     * Exporta los resultados actuales a un archivo Excel.
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
                mostrarAdvertencia("No se pudo generar el archivo de exportación");
            }

        } catch (Exception e) {
            log.error("❌ Error exportando a Excel: {}", e.getMessage(), e);
            mostrarError("Error al exportar: " + e.getMessage());
        }
    }

    /**
     * Limpia todos los filtros y resultados actuales.
     */
    @Command
    @NotifyChange({"filtros", "fechaDesde", "fechaHasta", "resultados",
            "hayResultados", "totalRegistros", "tiempoEjecucion"})
    public void limpiar() {
        log.info("🧹 Limpiando filtros y resultados");

        // Resetear filtros
        filtros = new ParametrosFiltros();
        fechaDesde = null;
        fechaHasta = null;

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
     */
    @Command
    @NotifyChange("*")
    public void recargarQuery() {
        log.info("🔄 Recargando query");
        cargarMetadataQuery();
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Convierte las fechas seleccionadas a formato String para el filtro.
     */
    private void prepararFiltros() {
        if (filtros == null) {
            filtros = new ParametrosFiltros();
        }

        // Formato estándar para fechas SQL
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Convertir fecha inicio si está definida
        if (fechaDesde != null) {
            filtros.setFechaInicio(sdf.format(fechaDesde));
        }

        // Convertir fecha fin si está definida
        if (fechaHasta != null) {
            filtros.setFechaFin(sdf.format(fechaHasta));
        }

        log.debug("Filtros preparados - Fecha Inicio: {}, Fecha Fin: {}",
                filtros.getFechaInicio(), filtros.getFechaFin());
    }

    /**
     * Genera un nombre de archivo descriptivo con timestamp.
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

    private void mostrarInformacion(String mensaje) {
        Messagebox.show(mensaje, "Información",
                Messagebox.OK, Messagebox.INFORMATION);
    }

    private void mostrarAdvertencia(String mensaje) {
        Messagebox.show(mensaje, "Advertencia",
                Messagebox.OK, Messagebox.EXCLAMATION);
    }

    private void mostrarError(String mensaje) {
        Messagebox.show(mensaje, "Error",
                Messagebox.OK, Messagebox.ERROR);
    }
}