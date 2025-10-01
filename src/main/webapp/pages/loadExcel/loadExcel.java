package loadExcel;

import ch.qos.logback.core.net.server.Client;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties;
import org.zkoss.bind.annotation.*;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Messagebox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Getter
@Setter
public class loadExcel {
    private List<String> origenList;
    private String selectedOrigen;
    private Media selectedFile;
    private boolean uploading;
    private int uploadProgress = 0;

    // Constructor para inicializar la lista
    public loadExcel() {
        origenList = Arrays.asList("Sugit", "Panamericana", "Proveedor 1", "Proveedor 2");
    }

    // ===== MÉTODOS CALCULADOS =====
    public String getProgressBarStyle() {
        return "width: " + uploadProgress + "%";
    }

    public String getProgressText() {
        return uploadProgress + "%";
    }

    public String getFileSizeFormatted() {
        if (selectedFile == null) return "";

        long bytes = selectedFile.getByteData().length;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ===== COMANDOS =====
    @Command
    @NotifyChange({"selectedFile", "uploading", "uploadProgress"})
    public void onFileUpload(@BindingParam("event") UploadEvent uploadEvent) {
        if (uploadEvent != null && uploadEvent.getMedia() != null) {
            processUploadedFile(uploadEvent.getMedia());
        }
    }

    // Método para procesar el archivo subido
    private void processUploadedFile(Media media) {
        if (media == null) return;

        // Validar que sea un archivo Excel
        if (!isValidExcelFile(media)) {
            Messagebox.show("Por favor seleccione un archivo Excel válido (.xls o .xlsx)",
                    "Formato no válido", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        this.selectedFile = media;
        this.uploading = true;
        this.uploadProgress = 0;

        // Mostrar información básica del archivo inmediatamente
        showFileInfo(media);

        // Simular progreso de upload
        simulateUploadProgress();
    }

    @Command
    @NotifyChange({"selectedFile", "uploading", "uploadProgress", "selectedOrigen"})
    public void removeFile() {
        this.selectedOrigen = null;
        this.selectedFile = null;
        this.uploading = false;
        this.uploadProgress = 0;
    }

    @Command
    @NotifyChange({"selectedFile", "uploading", "uploadProgress", "selectedOrigen"})
    public void cancel() {
        removeFile();
    }

    @Command
    public void processFile() {
        if (selectedFile == null) {
            Messagebox.show("Por favor seleccione un archivo", "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        if (selectedOrigen == null || selectedOrigen.trim().isEmpty()) {
            Messagebox.show("Por favor seleccione un origen", "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        try {
            // Procesar el archivo Excel y extraer datos
            Map<String, Object> excelData = processExcelFile(selectedFile, selectedOrigen);

            // AQUÍ PUEDES LLAMAR A TU SERVICIO
            sendDataToService(excelData);

            Messagebox.show("Archivo procesado exitosamente", "Éxito", Messagebox.OK, Messagebox.INFORMATION);

        } catch (Exception e) {
            Messagebox.show("Error al procesar el archivo: " + e.getMessage(),
                    "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Muestra información básica del archivo recién cargado
     */
    private void showFileInfo(Media media) {
        try {
            System.out.println("=== ARCHIVO CARGADO ===");
            System.out.println("Nombre: " + media.getName());
            System.out.println("Tamaño: " + getFileSizeFormatted());
            System.out.println("Tipo: " + media.getContentType());

            // Intentar extraer información básica del Excel
            Workbook workbook = createWorkbook(media);
            System.out.println("Total hojas: " + workbook.getNumberOfSheets());

            // Mostrar información de cada hoja
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Hoja " + (i+1) + ": " + sheet.getSheetName());

                // Mostrar headers si existen
                if (sheet.iterator().hasNext()) {
                    Row firstRow = sheet.iterator().next();
                    if (firstRow != null && firstRow.getLastCellNum() > 0) {
                        System.out.print("Headers: ");
                        for (int j = 0; j < firstRow.getLastCellNum(); j++) {
                            Cell cell = firstRow.getCell(j);
                            String header = getCellValueAsString(cell);
                            if (!header.trim().isEmpty()) {
                                System.out.print(header + " | ");
                            }
                        }
                        System.out.println();
                    }
                }
            }
            workbook.close();

        } catch (Exception e) {
            System.out.println("Error al leer información del archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Simula el progreso de upload - Versión completamente simplificada
     */
    private void simulateUploadProgress() {
        // Completar inmediatamente - sin animación ni hilos
        this.uploadProgress = 100;
        this.uploading = false;
    }

    /**
     * Valida que el archivo sea un Excel válido
     */
    private boolean isValidExcelFile(Media media) {
        if (media == null) return false;

        String fileName = media.getName().toLowerCase();
        return fileName.endsWith(".xls") || fileName.endsWith(".xlsx");
    }

    /**
     * Procesa el archivo Excel y extrae todos los datos
     */
    private Map<String, Object> processExcelFile(Media media, String origen) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Información básica del archivo
        result.put("fileName", media.getName());
        result.put("fileSize", media.getByteData().length);
        result.put("origen", origen);
        result.put("uploadDate", new Date());

        // Crear workbook según el tipo de archivo
        Workbook workbook = null;
        try {
            workbook = createWorkbook(media);

            // Información del workbook
            result.put("totalSheets", workbook.getNumberOfSheets());

            List<Map<String, Object>> sheetsData = new ArrayList<>();

            // Procesar cada hoja
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                Map<String, Object> sheetData = processSheet(sheet);
                sheetsData.add(sheetData);
            }

            result.put("sheets", sheetsData);

        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }

        return result;
    }

    /**
     * Crea el workbook apropiado según la extensión del archivo
     */
    private Workbook createWorkbook(Media media) throws IOException {
        String fileName = media.getName().toLowerCase();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(media.getByteData());

        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado: " + fileName);
        }
    }

    /**
     * Procesa una hoja específica del Excel
     */
    private Map<String, Object> processSheet(Sheet sheet) {
        Map<String, Object> sheetData = new HashMap<>();

        sheetData.put("sheetName", sheet.getSheetName());
        sheetData.put("sheetIndex", sheet.getWorkbook().getSheetIndex(sheet));

        List<List<String>> rows = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        Iterator<Row> rowIterator = sheet.iterator();
        boolean isFirstRow = true;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (isEmptyRow(row)) continue;

            List<String> rowData = new ArrayList<>();

            // Procesar cada celda de la fila
            int lastCellNum = row.getLastCellNum();
            for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                Cell cell = row.getCell(cellIndex);
                String cellValue = getCellValueAsString(cell);
                rowData.add(cellValue);

                // Si es la primera fila, asumir que son headers
                if (isFirstRow) {
                    headers.add(cellValue);
                }
            }

            rows.add(rowData);
            isFirstRow = false;
        }

        sheetData.put("headers", headers);
        sheetData.put("rows", rows);
        sheetData.put("totalRows", rows.size());
        sheetData.put("totalColumns", headers.size());

        return sheetData;
    }

    /**
     * Convierte el valor de una celda a String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Evitar notación científica para números
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * Verifica si una fila está vacía
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) return true;

        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Envía los datos extraídos a tu servicio
     */
    private void sendDataToService(Map<String, Object> excelData) {
        System.out.println("=== DATOS EXTRAÍDOS DEL EXCEL ===");
        System.out.println("Archivo: " + excelData.get("fileName"));
        System.out.println("Tamaño: " + excelData.get("fileSize") + " bytes");
        System.out.println("Origen: " + excelData.get("origen"));
        System.out.println("Total hojas: " + excelData.get("totalSheets"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sheets = (List<Map<String, Object>>) excelData.get("sheets");
        List<String> headers = new ArrayList<>();
        List<String> rows = new ArrayList<>();

        for (Map<String, Object> sheet : sheets) {
            System.out.println("--- Hoja: " + sheet.get("sheetName") + " ---");
            System.out.println("Filas: " + sheet.get("totalRows"));
            System.out.println("Columnas: " + sheet.get("totalColumns"));

            headers = (List<String>) sheet.get("headers");
            System.out.println("Headers: " + headers);

            // Los datos están en sheet.get("rows") - List<List<String>>
            rows = (List<String>) sheet.get("rows");
            System.out.println("Rows: " + rows);
        }

        System.out.println("Termine de obtener la metadata de Excel. Ahora enviar a servicio");
    }
}