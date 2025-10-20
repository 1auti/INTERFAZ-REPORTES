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

/**
 * Implementación del servicio para gestión y ejecución de queries de base de datos.
 *
 * Este servicio actúa como intermediario entre el frontend ZK y el backend REST,
 * manejando el registro, consulta, actualización y ejecución de queries dinámicas.
 *
 * Funcionalidades principales:
 * - Registro y gestión de queries (CRUD)
 * - Ejecución de queries normales y consolidadas
 * - Búsqueda y filtrado de queries
 * - Obtención de estadísticas
 *
 * @author Tu Nombre
 * @version 1.0
 */
@Service
@Slf4j
public class DatabaseQueryServiceImpl implements DatabaseQueryService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.backend.url:http://localhost:8080}")
    private String backendUrl;

    private static final String BASE_PATH = "/api/queries-db";

    /**
     * Construye la URL base para las peticiones al backend
     * @return URL completa del endpoint base
     */
    private String getBaseUrl() {
        return backendUrl + BASE_PATH;
    }

    // =============== REGISTRO Y GESTIÓN DE QUERIES ===============

    /**
     * Registra una nueva query en el sistema
     *
     * @param query Objeto Query con los datos de la nueva consulta
     * @return MetadataRegistroQuery con la información del registro
     * @throws RuntimeException si ocurre un error en el registro
     */
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
            log.error("Error HTTP al registrar query: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al registrar query: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error registrando query: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar query: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene la lista de queries, opcionalmente filtradas por categoría
     *
     * @param categoria Categoría para filtrar (puede ser null)
     * @return Lista de MetadataQuery
     */
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

    /**
     * Obtiene los detalles de una query específica por su código
     *
     * @param codigo Código único de la query
     * @return MetadataQuery con los detalles, o null si no existe
     */
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

    /**
     * Actualiza una query existente
     *
     * @param codigo Código de la query a actualizar
     * @param query Objeto Query con los nuevos datos
     * @return MetadataRegistroQuery con la información actualizada
     */
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

    /**
     * Elimina una query del sistema
     *
     * @param codigo Código de la query a eliminar
     * @return Map con el resultado de la operación
     */
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

    /**
     * Ejecuta una query registrada con parámetros específicos
     *
     * @param codigo Código de la query a ejecutar
     * @param consulta Parámetros de filtros y paginación
     * @return QueryResponse con los resultados
     */
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

    /**
     * Ejecuta una query consolidada (agrupada y agregada)
     *
     * @param codigo Código de la query a ejecutar
     * @param consulta Parámetros de consolidación y filtros
     * @return QueryResponse con los resultados consolidados
     */
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

    /**
     * Busca queries por texto en nombre, descripción o código
     *
     * @param texto Texto a buscar
     * @return Lista de queries que coinciden con la búsqueda
     */
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

    /**
     * Obtiene la lista de queries que soportan consolidación
     *
     * @return Lista de queries consolidables
     */
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

    /**
     * Obtiene las queries más utilizadas
     *
     * @param limite Número máximo de queries a retornar
     * @return Lista de queries populares ordenadas por uso
     */
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

    /**
     * Obtiene estadísticas generales del sistema de queries
     *
     * @return Map con estadísticas agregadas
     */
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

    /**
     * Obtiene estadísticas específicas de una query
     *
     * @param codigo Código de la query
     * @return Map con estadísticas de uso de la query
     */
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
     *
     * @param respuesta Map con los datos de respuesta del backend
     * @return MetadataRegistroQuery con la información procesada
     */
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
     * Convierte un Map a MetadataQuery con todos los campos incluyendo consolidación.
     *
     * Este método es crítico para el correcto funcionamiento del sistema, ya que
     * maneja la conversión de los campos de consolidación que pueden venir en
     * diferentes formatos desde el backend (List o String JSON).
     *
     * Flujo de conversión:
     * 1. Extrae campos básicos (id, código, nombre, etc.)
     * 2. Extrae fechas y las convierte a LocalDateTime
     * 3. Si la query es consolidable, extrae y parsea los campos de consolidación
     *
     * @param queryMap Map con los datos crudos del backend
     * @return MetadataQuery completamente poblado con todos los campos
     */
    private MetadataQuery convertirMapAMetadataQuery(Map<String, Object> queryMap) {
        if (queryMap == null) {
            return null;
        }

        // Debug: mostrar todas las claves disponibles en el map para troubleshooting
        log.debug("=== DEBUG: CLAVES EN QUERYMAP ===");
        for (String key : queryMap.keySet()) {
            Object value = queryMap.get(key);
            log.debug("Key: {} | Value: {} | Type: {}",
                    key, value, (value != null ? value.getClass().getSimpleName() : "null"));
        }

        MetadataQuery metadata = new MetadataQuery();

        // ===== SETTERS BÁSICOS =====
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

        // ===== FECHAS =====
        if (queryMap.containsKey("fechaCreacion")) {
            metadata.setFechaCreacion(convertirALocalDateTime(queryMap.get("fechaCreacion")));
        }
        if (queryMap.containsKey("fechaActualizacion")) {
            metadata.setFechaActualizacion(convertirALocalDateTime(queryMap.get("fechaActualizacion")));
        }

        // ===== ⭐ CAMPOS DE CONSOLIDACIÓN - CORRECCIÓN APLICADA ⭐ =====
        // Esta sección maneja la extracción y asignación de los campos de consolidación
        // que son necesarios para que el ZUL muestre correctamente la información
        if (Boolean.TRUE.equals(metadata.getEsConsolidable())) {
            log.debug("Procesando campos de consolidación...");

            // 🔧 CAMPOS DE AGRUPACIÓN
            // Intenta obtener desde "camposAgrupacionList" (List) o "camposAgrupacion" (JSON String)
            List<String> camposAgrupacion = procesarCampoLista(
                    queryMap,
                    "camposAgrupacionList",
                    "camposAgrupacion"
            );
            metadata.setCamposAgrupacionList(camposAgrupacion);
            log.debug("✅ Campos Agrupación asignados: {}", camposAgrupacion);

            // 🔧 CAMPOS NUMÉRICOS
            List<String> camposNumericos = procesarCampoLista(
                    queryMap,
                    "camposNumericosList",
                    "camposNumericos"
            );
            metadata.setCamposNumericosList(camposNumericos);
            log.debug("✅ Campos Numéricos asignados: {}", camposNumericos);

            // 🔧 CAMPOS DE UBICACIÓN
            List<String> camposUbicacion = procesarCampoLista(
                    queryMap,
                    "camposUbicacionList",
                    "camposUbicacion"
            );
            metadata.setCamposUbicacionList(camposUbicacion);
            log.debug("✅ Campos Ubicación asignados: {}", camposUbicacion);

            // 🔧 CAMPOS DE TIEMPO
            List<String> camposTiempo = procesarCampoLista(
                    queryMap,
                    "camposTiempoList",
                    "camposTiempo"
            );
            metadata.setCamposTiempoList(camposTiempo);
            log.debug("✅ Campos Tiempo asignados: {}", camposTiempo);
        }

        log.debug("=== FIN DEBUG ===");
        return metadata;
    }

    /**
     * Método auxiliar para procesar campos que pueden venir como List o String JSON.
     *
     * Este método maneja dos formatos posibles del backend:
     * 1. List<String> directamente (keyList)
     * 2. String JSON que debe parsearse (keyString)
     *
     * Estrategia:
     * - Primero intenta obtener la List directa
     * - Si no existe, intenta parsear el String JSON
     * - Si ambos fallan, retorna lista vacía
     *
     * @param queryMap Map con los datos del backend
     * @param keyList Nombre de la key para la List directa (ej: "camposAgrupacionList")
     * @param keyString Nombre de la key para el String JSON (ej: "camposAgrupacion")
     * @return Lista de strings procesada, nunca null
     */
    private List<String> procesarCampoLista(Map<String, Object> queryMap,
                                            String keyList,
                                            String keyString) {

        // Opción 1: Si viene como List directamente
        if (queryMap.containsKey(keyList)) {
            Object camposObj = queryMap.get(keyList);
            if (camposObj instanceof List) {
                List<String> lista = (List<String>) camposObj;
                log.debug("✅ Usando {} (List directa): {}", keyList, lista);
                return new ArrayList<>(lista); // Crear nueva lista para evitar referencias
            }
        }

        // Opción 2: Si viene como String JSON
        if (queryMap.containsKey(keyString)) {
            String camposJson = (String) queryMap.get(keyString);
            try {
                List<String> lista = parseJsonStringToList(camposJson);
                log.debug("✅ Parseado desde {} (JSON): {}", keyString, lista);
                return lista;
            } catch (Exception e) {
                log.error("❌ Error parseando {}: {}", keyString, e.getMessage());
            }
        }

        // Si no se encontró ninguna opción, devolver lista vacía
        log.debug("⚠️ No se encontró {} ni {} - devolviendo lista vacía", keyList, keyString);
        return new ArrayList<>();
    }

    /**
     * Parsea un String JSON a List<String> usando Jackson.
     *
     * Ejemplos de formatos soportados:
     * - ["campo1", "campo2", "campo3"]
     * - ["campo1","campo2","campo3"]
     * - ['campo1', 'campo2', 'campo3']
     *
     * Si Jackson falla, utiliza un parser manual como fallback.
     *
     * @param jsonString String con formato JSON array
     * @return Lista de strings parseada
     */
    private List<String> parseJsonStringToList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Intento principal con Jackson ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Error con ObjectMapper, usando parsing manual: {}", e.getMessage());
            // Fallback: parsing manual
            return parseManual(jsonString);
        }
    }

    /**
     * Parser manual de JSON array como fallback cuando Jackson falla.
     *
     * Este método es más permisivo y maneja casos edge como:
     * - Comillas simples en lugar de dobles
     * - Espacios inconsistentes
     * - Formato no estándar
     *
     * Algoritmo:
     * 1. Remover corchetes [ ]
     * 2. Dividir por comas
     * 3. Limpiar comillas y espacios de cada elemento
     *
     * @param jsonString String con formato JSON a parsear
     * @return Lista de strings parseada manualmente
     */
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
            log.error("Error en parsing manual: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Convierte un Map a MetadataRegistroQuery (versión simplificada para registro)
     *
     * @param queryMap Map con los datos básicos de la query
     * @return MetadataRegistroQuery con información de registro
     */
    private MetadataRegistroQuery convertirMapAMetadataQueryRegistro(Map<String, Object> queryMap) {
        if (queryMap == null) {
            return null;
        }

        MetadataRegistroQuery metadata = new MetadataRegistroQuery();

        // Setear solo los campos básicos necesarios para el registro
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
     * Convierte la respuesta de ejecución del backend a QueryResponse
     *
     * Procesa:
     * - Datos principales (resultados de la query)
     * - Metadata de paginación
     * - Metadata de consolidación (si aplica)
     *
     * @param respuesta Map con la respuesta del backend
     * @param codigoQuery Código de la query ejecutada
     * @return QueryResponse con todos los datos estructurados
     */
    private QueryResponse convertirRespuestaEjecucion(Map<String, Object> respuesta, String codigoQuery) {
        if (respuesta == null) {
            return null;
        }

        QueryResponse queryResponse = new QueryResponse();

        // Datos principales - pueden venir directamente o dentro de "datos"
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
            // Crear metadata básica si no viene del backend
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

    /**
     * Convierte un Map a MetadataPaginacion
     *
     * @param paginacionMap Map con datos de paginación
     * @return MetadataPaginacion estructurado
     */
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

    /**
     * Convierte un Map a MetadataConsolidacion
     *
     * @param consolidacionMap Map con datos de consolidación
     * @return MetadataConsolidacion estructurado
     */
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

    /**
     * Obtiene un valor String de forma segura desde un Map
     *
     * @param map Map fuente
     * @param key Clave a buscar
     * @return String o null si no existe
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Obtiene un valor Integer de forma segura desde un Map.
     * Maneja conversión desde diferentes tipos numéricos.
     *
     * @param map Map fuente
     * @param key Clave a buscar
     * @return Integer o null si no existe o no es número
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Obtiene un valor Long de forma segura desde un Map.
     * Maneja conversión desde diferentes tipos numéricos.
     *
     * @param map Map fuente
     * @param key Clave a buscar
     * @return Long o null si no existe o no es número
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * Obtiene un valor Boolean de forma segura desde un Map
     *
     * @param map Map fuente
     * @param key Clave a buscar
     * @return Boolean o null si no existe
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    /**
     * Convierte un objeto a LocalDateTime.
     *
     * Soporta múltiples formatos de entrada:
     * - String en formato ISO
     * - Long (timestamp en milisegundos)
     * - Date
     *
     * TODO: Implementar conversión real desde diferentes formatos
     * Actualmente retorna LocalDateTime.now() como fallback
     *
     * @param value Valor a convertir
     * @return LocalDateTime o null si no se puede convertir
     */
    private LocalDateTime convertirALocalDateTime(Object value) {
        if (value == null) {
            return null;
        }

        // TODO: Implementar conversión real según el formato que recibas del backend
        // Ejemplos de posibles implementaciones:

        /*
        // Si viene como String ISO
        if (value instanceof String) {
            return LocalDateTime.parse((String) value);
        }

        // Si viene como timestamp (Long)
        if (value instanceof Long) {
            return LocalDateTime.ofInstant(
                Instant.ofEpochMilli((Long) value),
                ZoneId.systemDefault()
            );
        }

        // Si viene como Date
        if (value instanceof Date) {
            return LocalDateTime.ofInstant(
                ((Date) value).toInstant(),
                ZoneId.systemDefault()
            );
        }
        */

        // Por ahora retorna la fecha actual como fallback
        return LocalDateTime.now();
    }
}