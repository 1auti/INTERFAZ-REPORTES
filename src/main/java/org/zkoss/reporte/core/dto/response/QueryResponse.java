package org.zkoss.reporte.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    private List<Map<String, Object>> datos;
    private List<String> columnas;
    private MetadataPaginacion paginacion;
    private MetadataQuery query;
    private MetadataConsolidacion consolidacion;
}