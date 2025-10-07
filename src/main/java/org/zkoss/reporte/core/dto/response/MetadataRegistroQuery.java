package org.zkoss.reporte.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MetadataRegistroQuery{

  private MetadataQueryRegistro metadataQueryRegistro;
  private String mensaje;
  private Boolean consolidable;

}