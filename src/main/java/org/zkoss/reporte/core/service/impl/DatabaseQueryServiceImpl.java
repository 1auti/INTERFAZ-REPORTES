package org.zkoss.reporte.core.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zkoss.reporte.core.dto.request.ConsultaQuery;
import org.zkoss.reporte.core.dto.request.Query;
import org.zkoss.reporte.core.dto.response.*;
import org.zkoss.reporte.core.service.interfaces.DatabaseQueryService;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DatabaseQueryServiceImpl implements DatabaseQueryService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.backend.url:http://localhost:8080}")
    private String backendUrl;

    private static final String BASE_PATH = "/api/queries-db";

    private String getBaseUrl() {
        return backendUrl + BASE_PATH;
    }

    // =============== REGISTRO Y GESTIÓN DE QUERIES ===============

    @Override
    public MetadataRegistroQuery registrarQuery(Query query) {
        try {
            log.info("Registrando nueva query: {}", query.getCodigo());

            String url = getBaseUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Query> request = new HttpEntity<>(query, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return convertirRespuestaRegistro(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al registrar query: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al registrar query: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error registrando query: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar query: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MetadataQuery> traerQuerys(String categoria) {
        try {
            log.info("Obteniendo queries - Categoría: {}", categoria);

            StringBuilder url = new StringBuilder(getBaseUrl());
            if (categoria != null && !categoria.isEmpty()) {
                url.append("?categoria=").append(categoria);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("queries")) {
                List<Map<String, Object>> queries = (List<Map<String, Object>>) body.get("queries");
                return queries.stream()
                        .map(this::convertirMapAMetadataQuery)
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error obteniendo queries: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener queries: " + e.getMessage(), e);
        }
    }

    @Override
    public MetadataQuery obtenerQuery(String codigo) {
        try {
            log.info("Obteniendo query: {}", codigo);

            String url = getBaseUrl() + "/" + codigo;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("query")) {
                return convertirMapAMetadataQuery((Map<String, Object>) body.get("query"));
            }

            return null;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Query no encontrada: {}", codigo);
            return null;
        } catch (Exception e) {
            log.error("Error obteniendo query '{}': {}", codigo, e.getMessage(), e);
            throw new RuntimeException("Error al obtener query: " + e.getMessage(), e);
        }
    }

    @Override
    public MetadataRegistroQuery actualizarQuery(String codigo, Query query) {
        try {
            log.info("Actualizando query: {}", codigo);

            String url = getBaseUrl() + "/" + codigo;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Query> request = new HttpEntity<>(query, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return convertirRespuestaRegistro(response.getBody());

        } catch (Exception e) {
            log.error("Error actualizando query '{}': {}", codigo, e.getMessage(), e);
            throw new RuntimeException("Error al actualizar query: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> eliminarQuery(String codigo) {
        try {
            log.info("Eliminando query: {}", codigo);

            String url = getBaseUrl() + "/" + codigo;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error eliminando query '{}': {}", codigo, e.getMessage(), e);
            throw new RuntimeException("Error al eliminar query: " + e.getMessage(), e);
        }
    }

    // =============== EJECUCIÓN DE QUERIES ===============

    @Override
    public QueryResponse ejecutarQuery(String codigo, ConsultaQuery consulta) {
        try {
            log.info("Ejecutando query: {}", codigo);

            String url = getBaseUrl() + "/ejecutar/" + codigo;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ConsultaQuery> request = new HttpEntity<>(consulta, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return convertirRespuestaEjecucion(response.getBody(), codigo);

        } catch (Exception e) {
            log.error("Error ejecutando query '{}': {}", codigo, e.getMessage(), e);
            throw new RuntimeException("Error al ejecutar query: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryResponse ejecutarQueryConsolidada(String codigo, ConsultaQuery consulta) {
        try {
            log.info("Ejecutando query consolidada: {}", codigo);

            String url = getBaseUrl() + "/consolidada/" + codigo;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ConsultaQuery> request = new HttpEntity<>(consulta, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return convertirRespuestaEjecucion(response.getBody(), codigo);

        } catch (Exception e) {
            log.error("Error ejecutando query consolidada '{}': {}", codigo, e.getMessage(), e);
            throw new RuntimeException("Error al ejecutar query consolidada: " + e.getMessage(), e);
        }
    }

    // =============== BÚSQUEDA Y FILTRADO ===============

    @Override
    public List<MetadataQuery> buscarQueries(String texto) {
        try {
            log.info("Buscando queries: {}", texto);

            String url = getBaseUrl() + "/buscar";
            if (texto != null && !texto.isEmpty()) {
                url += "?q=" + texto;
            }

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> queries = response.getBody();
            if (queries != null) {
                return queries.stream()
                        .map(this::convertirMapAMetadataQuery)
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error buscando queries: {}", e.getMessage(), e);
            throw new RuntimeException("Error en búsqueda: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MetadataQuery> obtenerQueriesConsolidables() {
        try {
            log.info("Obteniendo queries consolidables");

            String url = getBaseUrl() + "/consolidables";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> queries = response.getBody();
            if (queries != null) {
                return queries.stream()
                        .map(this::convertirMapAMetadataQuery)
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error obteniendo queries consolidables: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener queries consolidables: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MetadataQuery> obtenerQueriesPopulares(int limite) {
        try {
            log.info("Obteniendo queries populares - Límite: {}", limite);

            String url = getBaseUrl() + "/populares?limite=" + limite;

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> queries = response.getBody();
            if (queries != null) {
                return queries.stream()
                        .map(this::convertirMapAMetadataQuery)
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error obteniendo queries populares: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener queries populares: " + e.getMessage(), e);
        }
    }

    // =============== ESTADÍSTICAS ===============

    @Override
    public Map<String, Object> obtenerEstadisticas() {
        try {
            log.info("Obteniendo estadísticas generales");

            String url = getBaseUrl() + "/estadisticas";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener estadísticas: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> obtenerEstadisticasQuery(String codigo) {
        try {
            log.info("Obteniendo estadísticas de query: {}", codigo);

            String url = getBaseUrl() + "/" + codigo + "/estadisticas";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de query '{}': {}", codigo, e.getMessage(), e);
            throw new RuntimeException("Error al obtener estadísticas de query: " + e.getMessage(), e);
        }
    }

    // =============== MÉTODOS PRIVADOS DE CONVERSIÓN ===============

    /**
     * Convierte la respuesta de registro/actualización a MetadataRegistroQuery
     */
    private MetadataQuery convertirRespuesta(Map<String, Object> respuesta) {
        if (respuesta == null) {
            return null;
        }

        MetadataQuery resultado = new MetadataQuery();

        // Query
        if (respuesta.containsKey("query")) {
            Map<String, Object> queryMap = (Map<String, Object>) respuesta.get("query");
            resultado = (convertirMapAMetadataQuery(queryMap));
        }

        return resultado;
    }

    private MetadataRegistroQuery convertirRespuestaRegistro(Map<String, Object> respuesta) {
        if (respuesta == null) {
            return null;
        }

        MetadataRegistroQuery resultado = new MetadataRegistroQuery();

        // Query
        if (respuesta.containsKey("query")) {
            Map<String, Object> queryMap = (Map<String, Object>) respuesta.get("query");
            resultado = (convertirMapAMetadataQueryRegistro(queryMap));
        }

        return resultado;
    }

    /**
     * Convierte un Map a MetadataQueryRegistro
     */
    private MetadataQuery convertirMapAMetadataQuery(Map<String, Object> queryMap) {
        if (queryMap == null) {
            return null;
        }

        // Debug: mostrar todas las claves disponibles
        System.out.println("=== DEBUG: CLAVES EN QUERYMAP ===");
        for (String key : queryMap.keySet()) {
            Object value = queryMap.get(key);
            System.out.println("Key: " + key + " | Value: " + value + " | Type: " +
                    (value != null ? value.getClass().getSimpleName() : "null"));
        }

        MetadataQuery metadata = new MetadataQuery();

        // Setters básicos
        metadata.setId(getIntegerValue(queryMap, "id"));
        metadata.setCodigo(getStringValue(queryMap, "codigo"));
        metadata.setNombre(getStringValue(queryMap, "nombre"));
        metadata.setSqlQuery(getStringValue(queryMap, "sqlQuery"));
        metadata.setDescripcion(getStringValue(queryMap, "descripcion"));
        metadata.setCategoria(getStringValue(queryMap, "categoria"));
        metadata.setActiva(getBooleanValue(queryMap, "activa"));
        metadata.setEsConsolidable(getBooleanValue(queryMap, "esConsolidable"));
        metadata.setEstado(getStringValue(queryMap, "estado"));
        metadata.setContadorUsos(getLongValue(queryMap, "contadorUsos"));


        // Fechas
        if (queryMap.containsKey("fechaCreacion")) {
            metadata.setFechaCreacion(convertirALocalDateTime(queryMap.get("fechaCreacion")));
        }
        if (queryMap.containsKey("fechaActualizacion")) {
            metadata.setFechaActualizacion(convertirALocalDateTime(queryMap.get("fechaActualizacion")));
        }



        // Metadata de consolidación
        if (Boolean.TRUE.equals(metadata.getEsConsolidable())) {
            System.out.println("Procesando campos de consolidación...");

            // Campos agrupación
            if (queryMap.containsKey("camposAgrupacionList")) {
                Object camposObj = queryMap.get("camposAgrupacionList");
                if (camposObj instanceof List) {
                    metadata.setCamposAgrupacionList((List<String>) camposObj);
                    System.out.println("Usando camposAgrupacionList: " + metadata.getCamposAgrupacionList());
                }
            }
            // Opción 2: Si no existe la lista, parsear el string JSON
            else if (queryMap.containsKey("camposAgrupacion")) {
                String camposJson = (String) queryMap.get("camposAgrupacion");
                try {
                    List<String> camposList = parseJsonStringToList(camposJson);
                    metadata.setCamposAgrupacionList(camposList);
                    System.out.println("Parseado desde camposAgrupacion: " + camposList);
                } catch (Exception e) {
                    System.err.println("Error parseando camposAgrupacion: " + e.getMessage());
                }
            }

            // Aplicar la misma lógica para los otros campos
            procesarCampoLista(queryMap, "camposNumericosList", "camposNumericos",
                    metadata::setCamposNumericosList);
            procesarCampoLista(queryMap, "camposUbicacionList", "camposUbicacion",
                    metadata::setCamposUbicacionList);
            procesarCampoLista(queryMap, "camposTiempoList", "camposTiempo",
                    metadata::setCamposTiempoList);
        }

        System.out.println("=== FIN DEBUG ===");
        return metadata;
    }

    // Método auxiliar para procesar ambos formatos
    private void procesarCampoLista(Map<String, Object> queryMap,
                                    String keyList, String keyString,
                                    java.util.function.Consumer<List<String>> setter) {

        if (queryMap.containsKey(keyList)) {
            Object camposObj = queryMap.get(keyList);
            if (camposObj instanceof List) {
                setter.accept((List<String>) camposObj);
                System.out.println("Usando " + keyList + ": " + camposObj);
                return;
            }
        }

        if (queryMap.containsKey(keyString)) {
            String camposJson = (String) queryMap.get(keyString);
            try {
                List<String> camposList = parseJsonStringToList(camposJson);
                setter.accept(camposList);
                System.out.println("Parseado desde " + keyString + ": " + camposList);
            } catch (Exception e) {
                System.err.println("Error parseando " + keyString + ": " + e.getMessage());
            }
        }
    }

    // Método para parsear el string JSON a List usando Jackson
    private List<String> parseJsonStringToList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            System.err.println("Error con ObjectMapper, usando parsing manual: " + e.getMessage());
            // Fallback: parsing manual
            return parseManual(jsonString);
        }
    }

    // Parsing manual como fallback
    private List<String> parseManual(String jsonString) {
        List<String> result = new ArrayList<>();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return result;
        }

        try {
            // Remover corchetes y espacios
            String clean = jsonString.trim();
            if (clean.startsWith("[")) {
                clean = clean.substring(1);
            }
            if (clean.endsWith("]")) {
                clean = clean.substring(0, clean.length() - 1);
            }

            // Dividir por comas y limpiar comillas
            String[] parts = clean.split(",");
            for (String part : parts) {
                String cleanedPart = part.trim()
                        .replace("\"", "")
                        .replace("'", "")
                        .replace("\\", "");
                if (!cleanedPart.isEmpty()) {
                    result.add(cleanedPart);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en parsing manual: " + e.getMessage());
        }

        return result;
    }











    private MetadataRegistroQuery convertirMapAMetadataQueryRegistro(Map<String, Object> queryMap) {
        if (queryMap == null) {
            return null;
        }

        MetadataRegistroQuery metadata = new MetadataRegistroQuery();

        metadata.getMetadataQueryRegistro().setId(getIntegerValue(queryMap, "id"));
        metadata.getMetadataQueryRegistro().setCodigo(getStringValue(queryMap, "codigo"));
        metadata.getMetadataQueryRegistro().setNombre(getStringValue(queryMap, "nombre"));
        metadata.getMetadataQueryRegistro().setDescripcion(getStringValue(queryMap, "descripcion"));
        metadata.getMetadataQueryRegistro().setCategoria(getStringValue(queryMap, "categoria"));
        metadata.getMetadataQueryRegistro().setActiva(getBooleanValue(queryMap, "activa"));
        metadata.getMetadataQueryRegistro().setEsConsolidable(getBooleanValue(queryMap, "esConsolidable"));
        metadata.getMetadataQueryRegistro().setEstado(getStringValue(queryMap, "estado"));

        return metadata;
    }

    /**
     * Convierte la respuesta de ejecución a QueryResponse
     */
    private QueryResponse convertirRespuestaEjecucion(Map<String, Object> respuesta, String codigoQuery) {
        if (respuesta == null) {
            return null;
        }

        QueryResponse queryResponse = new QueryResponse();

        // Datos principales
        if (respuesta.containsKey("datos")) {
            queryResponse.setDatos((List<Map<String, Object>>) respuesta.get("datos"));
        } else {
            // Si la respuesta es directamente la lista
            queryResponse.setDatos((List<Map<String, Object>>) respuesta);
        }

        // Metadata de query
        MetadataQuery metadataQuery = new MetadataQuery();
        metadataQuery.setCodigo(codigoQuery);
        metadataQuery.setFechaEjecucion(LocalDateTime.now());
        queryResponse.setQuery(metadataQuery);

        // Metadata de paginación
        if (respuesta.containsKey("paginacion")) {
            queryResponse.setPaginacion(convertirMapAPaginacion(
                    (Map<String, Object>) respuesta.get("paginacion")));
        } else {
            // Crear metadata básica
            MetadataPaginacion paginacion = new MetadataPaginacion();
            paginacion.setTotalRegistros(
                    (long) (queryResponse.getDatos() != null ? queryResponse.getDatos().size() : 0));
            queryResponse.setPaginacion(paginacion);
        }

        // Metadata de consolidación
        if (respuesta.containsKey("consolidacion")) {
            queryResponse.setConsolidacion(convertirMapAConsolidacion(
                    (Map<String, Object>) respuesta.get("consolidacion")));
        }

        return queryResponse;
    }

    private MetadataPaginacion convertirMapAPaginacion(Map<String, Object> paginacionMap) {
        MetadataPaginacion paginacion = new MetadataPaginacion();

        paginacion.setPaginaActual(getIntegerValue(paginacionMap, "paginaActual"));
        paginacion.setRegistrosPorPagina(getIntegerValue(paginacionMap, "registrosPorPagina"));
        paginacion.setTotalRegistros(getLongValue(paginacionMap, "totalRegistros"));
        paginacion.setTotalPaginas(getIntegerValue(paginacionMap, "totalPaginas"));
        paginacion.setTieneSiguiente(getBooleanValue(paginacionMap, "tieneSiguiente"));
        paginacion.setTieneAnterior(getBooleanValue(paginacionMap, "tieneAnterior"));

        if (paginacionMap.containsKey("lastKey")) {
            paginacion.setLastKey((Map<String, Object>) paginacionMap.get("lastKey"));
        }

        return paginacion;
    }

    private MetadataConsolidacion convertirMapAConsolidacion(Map<String, Object> consolidacionMap) {
        MetadataConsolidacion consolidacion = new MetadataConsolidacion();

        consolidacion.setConsolidado(getBooleanValue(consolidacionMap, "consolidado"));
        consolidacion.setPeriodoTemporal(getStringValue(consolidacionMap, "periodoTemporal"));
        consolidacion.setRegistrosOriginales(getIntegerValue(consolidacionMap, "registrosOriginales"));
        consolidacion.setRegistrosConsolidados(getIntegerValue(consolidacionMap, "registrosConsolidados"));

        if (consolidacionMap.containsKey("camposAgrupacion")) {
            consolidacion.setCamposAgrupacion((List<String>) consolidacionMap.get("camposAgrupacion"));
        }
        if (consolidacionMap.containsKey("camposNumericos")) {
            consolidacion.setCamposNumericos((List<String>) consolidacionMap.get("camposNumericos"));
        }

        return consolidacion;
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

    private LocalDateTime convertirALocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        // Aquí puedes implementar la conversión según el formato que recibas
        // Por ahora retorna la fecha actual como fallback
        return LocalDateTime.now();
    }
}