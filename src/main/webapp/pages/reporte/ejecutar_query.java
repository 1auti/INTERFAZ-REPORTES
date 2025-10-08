package reporte;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;

import org.zkoss.reporte.core.dto.response.MetadataQuery;
import org.zkoss.reporte.core.service.interfaces.ReporteService;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.request.ParametrosFiltros;
import org.zkoss.reporte.core.dto.response.MetadataQueryRegistro;
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

        ServletContext servletContext = Executions.getCurrent().getDesktop().getWebApp().getServletContext();
        WebApplicationContext webAppCtx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        databaseQueryService = webAppCtx.getBean(DatabaseQueryService.class);
        infraccionesService = webAppCtx.getBean(ReporteService.class);

        // Inicializar filtros
        filtros = new ParametrosFiltros();

        // Obtener código de la query desde parámetro URL
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

    private void cargarMetadataQuery() {
        try {
            if (databaseQueryService != null && codigoQuery != null) {
                log.info("Cargando metadata de query: {}", codigoQuery);

                queryMetadata = databaseQueryService.obtenerQuery(codigoQuery);

                if (queryMetadata != null) {
                    queryNombre = queryMetadata.getNombre();
                    queryDescripcion = queryMetadata.getDescripcion() != null
                            ? queryMetadata.getDescripcion()
                            : "Ejecute la consulta para ver los resultados";

                    log.info("Metadata cargada - Query: {} ({})",
                            queryNombre,
                            queryMetadata.getEsConsolidable() ? "Consolidable" : "Normal");

                    // Notificar cambios
                    BindUtils.postNotifyChange(null, null, this, "queryNombre");
                    BindUtils.postNotifyChange(null, null, this, "queryDescripcion");
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

    // ===== COMANDOS =====

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

            // Preparar filtros con fechas
            prepararFiltros();

            // Crear consulta
            ConsultaQuery consulta = ConsultaQuery.builder()
                    .parametrosFiltros(filtros)
                    .formato("json")
                    .build();

            // Medir tiempo de ejecución
            long inicio = System.currentTimeMillis();

            // Ejecutar query
            resultadoQuery = databaseQueryService.ejecutarQuery(codigoQuery, consulta);

            long fin = System.currentTimeMillis();
            tiempoEjecucion = (fin - inicio) + " ms";

            // Procesar resultados
            if (resultadoQuery != null && resultadoQuery.getDatos() != null) {
                List<Map<String, Object>> datos = resultadoQuery.getDatos();

                resultados = new ListModelList<>(datos);
                hayResultados = !datos.isEmpty();
                totalRegistros = datos.size();

                // Extraer columnas
                if (!datos.isEmpty()) {
                    columnasResultado = new ArrayList<>(datos.get(0).keySet());
                }

                log.info("Query ejecutada exitosamente - {} registros en {}",
                        totalRegistros, tiempoEjecucion);

                mostrarInformacion("Consulta ejecutada exitosamente");
            } else {
                hayResultados = false;
                totalRegistros = 0;
                resultados = new ListModelList<>();

                log.warn("La query no devolvió resultados");
                mostrarAdvertencia("La consulta no devolvió resultados");
            }

        } catch (Exception e) {
            log.error("Error ejecutando query: {}", e.getMessage(), e);
            mostrarError("Error al ejecutar la consulta: " + e.getMessage());

            hayResultados = false;
            totalRegistros = 0;
            resultados = new ListModelList<>();
        } finally {
            ejecutando = false;
        }
    }

    @Command
    public void exportarExcel() {
        if (!hayResultados) {
            mostrarAdvertencia("No hay resultados para exportar");
            return;
        }

        try {
            log.info("Exportando resultados a Excel - Query: {}", codigoQuery);

            // Preparar consulta para descarga
            prepararFiltros();

            ConsultaQuery consulta = ConsultaQuery.builder()
                    .parametrosFiltros(filtros)
                    .formato("excel")
                    .build();

            // Descargar archivo
            byte[] archivo = infraccionesService != null
                    ? infraccionesService.descargarConsulta(codigoQuery, consulta)
                    : null;

            if (archivo != null && archivo.length > 0) {
                // Generar nombre de archivo
                String nombreArchivo = generarNombreArchivo();

                // Descargar
                org.zkoss.zul.Filedownload.save(
                        new ByteArrayInputStream(archivo),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        nombreArchivo
                );

                log.info("Archivo exportado exitosamente: {}", nombreArchivo);
                mostrarInformacion("Archivo descargado exitosamente");
            } else {
                mostrarAdvertencia("No se pudo generar el archivo de exportación");
            }

        } catch (Exception e) {
            log.error("Error exportando a Excel: {}", e.getMessage(), e);
            mostrarError("Error al exportar: " + e.getMessage());
        }
    }

    @Command
    @NotifyChange({"filtros", "fechaDesde", "fechaHasta", "resultados",
            "hayResultados", "totalRegistros", "tiempoEjecucion"})
    public void limpiar() {
        log.info("Limpiando filtros y resultados");

        // Limpiar filtros
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

    @Command
    @NotifyChange("*")
    public void recargarQuery() {
        log.info("Recargando query");
        cargarMetadataQuery();
    }

    // ===== MÉTODOS AUXILIARES =====

    private void prepararFiltros() {
        if (filtros == null) {
            filtros = new ParametrosFiltros();
        }

        // Convertir fechas a String
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        if (fechaDesde != null) {
            filtros.setFechaInicio(sdf.format(fechaDesde));
        }

        if (fechaHasta != null) {
            filtros.setFechaFin(sdf.format(fechaHasta));
        }

        log.debug("Filtros preparados - Fecha Inicio: {}, Fecha Fin: {}",
                filtros.getFechaInicio(), filtros.getFechaFin());
    }

    private String generarNombreArchivo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String nombreBase = queryNombre != null
                ? queryNombre.replaceAll("[^a-zA-Z0-9]", "_")
                : "reporte";

        return nombreBase + "_" + timestamp + ".xlsx";
    }

    // ===== GETTERS COMPUTADOS =====

    public boolean isQueryConsolidable() {
        boolean consolidable = queryMetadata != null &&
                Boolean.TRUE.equals(queryMetadata.getEsConsolidable());
        log.debug("isQueryConsolidable() llamado - valor: {}", consolidable);
        return consolidable;
    }

    public String getCategoria() {
        String categoria = queryMetadata != null ? queryMetadata.getCategoria() : null;
        log.debug("getCategoria() llamado - valor: {}", categoria);
        return categoria;
    }

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