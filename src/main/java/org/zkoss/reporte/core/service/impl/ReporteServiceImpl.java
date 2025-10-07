package org.zkoss.reporte.core.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.response.*;
import org.zkoss.reporte.core.service.interfaces.ReporteService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.backend.url:http://localhost:8080}")
    private String backendUrl;

    private static final String BASE_PATH = "/api/infracciones";

    private String getBaseUrl() {
        return backendUrl + BASE_PATH;
    }

    // =============== EJECUCIÓN DE CONSULTAS ===============

    @Override
    public QueryResponse ejecutarConsulta(String tipoConsulta, ConsultaQuery consulta) {
        try {
            log.info("Ejecutando consulta de infracciones - Tipo: {}", tipoConsulta);

            String url = getBaseUrl() + "/" + tipoConsulta;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ConsultaQuery> request = new HttpEntity<>(consulta, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // Extraer headers informativos
            boolean consolidado = "true".equals(response.getHeaders().getFirst("X-Consolidado"));
            String tipoConsultaHeader = response.getHeaders().getFirst("X-Tipo-Consulta");

            log.info("Consulta ejecutada - Tipo: {}, Consolidado: {}", tipoConsultaHeader, consolidado);

            return convertirRespuestaAQueryResponse(response.getBody(), tipoConsulta, consolidado);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP en consulta '{}': {} - {}",
                    tipoConsulta, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new RuntimeException("Consulta no válida: " + e.getMessage(), e);
            } else {
                throw new RuntimeException("Error al ejecutar consulta: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error ejecutando consulta '{}': {}", tipoConsulta, e.getMessage(), e);
            throw new RuntimeException("Error al ejecutar consulta: " + e.getMessage(), e);
        }
    }

    // =============== DESCARGA DE ARCHIVOS ===============

    @Override
    public byte[] descargarConsulta(String tipoConsulta, ConsultaQuery consulta) {
        try {
            log.info("Descargando consulta - Tipo: {}, Formato: {}", tipoConsulta, consulta.getFormato());

            String url = getBaseUrl() + "/" + tipoConsulta + "/descargar";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

            HttpEntity<ConsultaQuery> request = new HttpEntity<>(consulta, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            byte[] archivo = response.getBody();

            if (archivo == null || archivo.length == 0) {
                log.warn("Archivo descargado está vacío");
                throw new RuntimeException("No se pudo descargar el archivo");
            }

            log.info("Archivo descargado exitosamente - Tamaño: {} bytes", archivo.length);
            return archivo;

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP en descarga '{}': {} - {}",
                    tipoConsulta, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al descargar archivo: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Error descargando consulta '{}': {}", tipoConsulta, e.getMessage(), e);
            throw new RuntimeException("Error al descargar archivo: " + e.getMessage(), e);
        }
    }

    // =============== MÉTODOS PRIVADOS DE CONVERSIÓN ===============

    /**
     * Convierte la respuesta del controller a QueryResponse
     */
    private QueryResponse convertirRespuestaAQueryResponse(
            Map<String, Object> respuesta,
            String tipoConsulta,
            boolean consolidado) {

        if (respuesta == null) {
            return null;
        }

        QueryResponse queryResponse = new QueryResponse();

        // Extraer datos principales
        List<Map<String, Object>> datos = extraerDatos(respuesta);
        queryResponse.setDatos(datos);

        // Metadata de query
        MetadataQuery metadataQuery = MetadataQuery.builder()
                .codigoQuery(tipoConsulta)
                .nombreQuery(generarNombreQuery(tipoConsulta))
                .categoria("INFRACCIONES")
                .fechaEjecucion(LocalDateTime.now())
                .build();
        queryResponse.setQuery(metadataQuery);

        // Metadata de paginación
        MetadataPaginacion paginacion = extraerMetadataPaginacion(respuesta, datos);
        queryResponse.setPaginacion(paginacion);

        // Metadata de consolidación
        if (consolidado) {
            MetadataConsolidacion consolidacion = extraerMetadataConsolidacion(respuesta);
            queryResponse.setConsolidacion(consolidacion);
        }

        return queryResponse;
    }

    /**
     * Extrae los datos de la respuesta
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extraerDatos(Map<String, Object> respuesta) {
        if (respuesta == null) {
            return new ArrayList<>();
        }

        // Intentar extraer de diferentes estructuras posibles
        if (respuesta.containsKey("datos")) {
            Object datos = respuesta.get("datos");
            if (datos instanceof List) {
                return (List<Map<String, Object>>) datos;
            }
        }

        // Si no hay key "datos", asumir que toda la respuesta son los datos
        if (respuesta.values().stream().allMatch(v -> v instanceof Map)) {
            return Collections.singletonList(respuesta);
        }

        // Intentar como lista directa
        try {
            return (List<Map<String, Object>>) respuesta;
        } catch (ClassCastException e) {
            log.warn("No se pudieron extraer datos de la respuesta correctamente");
            return new ArrayList<>();
        }
    }

    /**
     * Extrae metadata de paginación
     */
    @SuppressWarnings("unchecked")
    private MetadataPaginacion extraerMetadataPaginacion(
            Map<String, Object> respuesta,
            List<Map<String, Object>> datos) {

        MetadataPaginacion paginacion = new MetadataPaginacion();

        if (respuesta.containsKey("paginacion")) {
            Map<String, Object> paginacionMap = (Map<String, Object>) respuesta.get("paginacion");

            paginacion.setPaginaActual(getIntegerValue(paginacionMap, "paginaActual"));
            paginacion.setRegistrosPorPagina(getIntegerValue(paginacionMap, "registrosPorPagina"));
            paginacion.setTotalRegistros(getLongValue(paginacionMap, "totalRegistros"));
            paginacion.setTotalPaginas(getIntegerValue(paginacionMap, "totalPaginas"));
            paginacion.setTieneSiguiente(getBooleanValue(paginacionMap, "tieneSiguiente"));
            paginacion.setTieneAnterior(getBooleanValue(paginacionMap, "tieneAnterior"));

            if (paginacionMap.containsKey("lastKey")) {
                paginacion.setLastKey((Map<String, Object>) paginacionMap.get("lastKey"));
            }
        } else {
            // Crear metadata básica basada en los datos
            paginacion.setTotalRegistros((long) datos.size());
            paginacion.setPaginaActual(1);
            paginacion.setRegistrosPorPagina(datos.size());
            paginacion.setTotalPaginas(1);
            paginacion.setTieneSiguiente(false);
            paginacion.setTieneAnterior(false);
        }

        return paginacion;
    }

    /**
     * Extrae metadata de consolidación
     */
    @SuppressWarnings("unchecked")
    private MetadataConsolidacion extraerMetadataConsolidacion(Map<String, Object> respuesta) {
        MetadataConsolidacion consolidacion = new MetadataConsolidacion();
        consolidacion.setConsolidado(true);

        if (respuesta.containsKey("consolidacion")) {
            Map<String, Object> consolidacionMap = (Map<String, Object>) respuesta.get("consolidacion");

            consolidacion.setCamposAgrupacion(
                    (List<String>) consolidacionMap.get("camposAgrupacion"));
            consolidacion.setCamposNumericos(
                    (List<String>) consolidacionMap.get("camposNumericos"));
            consolidacion.setPeriodoTemporal(
                    (String) consolidacionMap.get("periodoTemporal"));
            consolidacion.setRegistrosOriginales(
                    getIntegerValue(consolidacionMap, "registrosOriginales"));
            consolidacion.setRegistrosConsolidados(
                    getIntegerValue(consolidacionMap, "registrosConsolidados"));
        }

        return consolidacion;
    }

    /**
     * Genera un nombre legible para la query basado en el tipo
     */
    private String generarNombreQuery(String tipoConsulta) {
        if (tipoConsulta == null) {
            return "Consulta de Infracciones";
        }

        // Convertir de snake_case o kebab-case a Title Case
        String[] palabras = tipoConsulta
                .replace("_", " ")
                .replace("-", " ")
                .split(" ");

        StringBuilder nombre = new StringBuilder();
        for (String palabra : palabras) {
            if (palabra.length() > 0) {
                nombre.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return nombre.toString().trim();
    }

    // =============== MÉTODOS AUXILIARES ===============

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}