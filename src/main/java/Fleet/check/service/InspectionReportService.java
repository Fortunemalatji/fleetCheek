package Fleet.check.service;

import Fleet.check.dto.ChecklistItemAnalyticsDTO;
import Fleet.check.dto.InspectionDTO;
import Fleet.check.entity.ChecklistItem;
import Fleet.check.exception.ResourceNotFoundException;
import Fleet.check.repository.ChecklistItemRepository;
import Fleet.check.repository.InspectionRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InspectionReportService {
    private static final DateTimeFormatter STANDARD_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

    private final InspectionRepository inspectionRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final InspectionService inspectionService;

    public byte[] exportInspectionReport(UUID inspectionId, String format) {
        InspectionDTO inspection = inspectionRepository.findById(inspectionId)
                .map(inspectionService::toInspectionDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found: " + inspectionId));

        return switch (normalizeFormat(format)) {
            case "pdf" -> exportInspectionPdf(inspection);
            case "xlsx" -> exportInspectionXlsx(inspection);
            case "csv" -> exportInspectionCsv(inspection);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    public byte[] exportChecklistAnalyticsReport(String format) {
        List<ChecklistItemAnalyticsDTO> analytics = getChecklistAnalytics();
        return switch (normalizeFormat(format)) {
            case "pdf" -> exportChecklistAnalyticsPdf(analytics);
            case "xlsx" -> exportChecklistAnalyticsXlsx(analytics);
            case "csv" -> exportChecklistAnalyticsCsv(analytics);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    public List<ChecklistItemAnalyticsDTO> getChecklistAnalytics() {
        return checklistItemRepository.findAllBy().stream()
                .filter(item -> item.getStartTime() != null && item.getEndTime() != null)
                .collect(Collectors.groupingBy(item -> item.getTemplate().getItemCode()))
                .values().stream()
                .map(this::toAnalytics)
                .sorted(Comparator.comparingLong(ChecklistItemAnalyticsDTO::getTotalDurationMs).reversed())
                .toList();
    }

    private ChecklistItemAnalyticsDTO toAnalytics(List<ChecklistItem> items) {
        ChecklistItem first = items.getFirst();
        LongSummary summary = items.stream()
                .mapToLong(item -> Duration.between(item.getStartTime(), item.getEndTime()).toMillis())
                .collect(LongSummary::new, LongSummary::accept, LongSummary::combine);

        return new ChecklistItemAnalyticsDTO(
                first.getTemplate().getItemCode(),
                first.getTemplate().getDisplayName(),
                first.getTemplate().getZoneName(),
                summary.total,
                summary.count == 0 ? 0 : summary.total / summary.count,
                summary.max,
                summary.count
        );
    }

    private byte[] exportInspectionPdf(InspectionDTO inspection) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Inspection Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Inspection ID: " + inspection.getId()));
            document.add(new Paragraph("Shipment ID: " + safe(inspection.getShipmentId())));
            document.add(new Paragraph("Driver: " + safe(inspection.getDriverName(), inspection.getDriverId())));
            document.add(new Paragraph("Status: " + safe(inspection.getOverallStatus())));
            document.add(new Paragraph("Start Time: " + formatDateTime(inspection.getStartTime())));
            document.add(new Paragraph("End Time: " + formatDateTime(inspection.getEndTime())));
            document.add(new Paragraph("Total Inspection Time: " + formatDuration(inspection.getTotalDurationMs())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            addHeader(table, "Item");
            addHeader(table, "Zone");
            addHeader(table, "Response");
            addHeader(table, "Fixed");
            addHeader(table, "Status");
            addHeader(table, "Duration");
            addHeader(table, "Remarks");

            for (var item : inspection.getChecklistItems()) {
                table.addCell(safe(item.getItemName(), item.getItemCode()));
                table.addCell(safe(item.getZoneName()));
                table.addCell(safe(item.getResponse()));
                table.addCell(Boolean.toString(item.isFixed()));
                table.addCell(safe(item.getStatus()));
                table.addCell(formatDuration(item.getDurationMs()));
                table.addCell(safe(item.getRemarks()));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Failed to generate PDF report.", e);
        }
    }

    private byte[] exportInspectionXlsx(InspectionDTO inspection) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet summary = workbook.createSheet("Inspection");
            writeSummaryRow(summary, 0, "Inspection ID", String.valueOf(inspection.getId()));
            writeSummaryRow(summary, 1, "Shipment ID", inspection.getShipmentId());
            writeSummaryRow(summary, 2, "Driver", safe(inspection.getDriverName(), inspection.getDriverId()));
            writeSummaryRow(summary, 3, "Status", inspection.getOverallStatus());
            writeSummaryRow(summary, 4, "Start Time", formatDateTime(inspection.getStartTime()));
            writeSummaryRow(summary, 5, "End Time", formatDateTime(inspection.getEndTime()));
            writeSummaryRow(summary, 6, "Total Inspection Time", formatDuration(inspection.getTotalDurationMs()));

            Sheet checklist = workbook.createSheet("Checklist");
            Row header = checklist.createRow(0);
            header.createCell(0).setCellValue("Item Code");
            header.createCell(1).setCellValue("Item Name");
            header.createCell(2).setCellValue("Zone");
            header.createCell(3).setCellValue("Response");
            header.createCell(4).setCellValue("Fixed");
            header.createCell(5).setCellValue("Status");
            header.createCell(6).setCellValue("Duration");
            header.createCell(7).setCellValue("Remarks");

            int rowIndex = 1;
            for (var item : inspection.getChecklistItems()) {
                Row row = checklist.createRow(rowIndex++);
                row.createCell(0).setCellValue(safe(item.getItemCode()));
                row.createCell(1).setCellValue(safe(item.getItemName()));
                row.createCell(2).setCellValue(safe(item.getZoneName()));
                row.createCell(3).setCellValue(safe(item.getResponse()));
                row.createCell(4).setCellValue(item.isFixed());
                row.createCell(5).setCellValue(safe(item.getStatus()));
                row.createCell(6).setCellValue(formatDuration(item.getDurationMs()));
                row.createCell(7).setCellValue(safe(item.getRemarks()));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate XLSX report.", e);
        }
    }

    private byte[] exportInspectionCsv(InspectionDTO inspection) {
        StringBuilder csv = new StringBuilder();
        csv.append("Inspection ID,").append(quote(inspection.getId())).append('\n');
        csv.append("Shipment ID,").append(quote(inspection.getShipmentId())).append('\n');
        csv.append("Driver,").append(quote(safe(inspection.getDriverName(), inspection.getDriverId()))).append('\n');
        csv.append("Status,").append(quote(inspection.getOverallStatus())).append('\n');
        csv.append("Start Time,").append(quote(formatDateTime(inspection.getStartTime()))).append('\n');
        csv.append("End Time,").append(quote(formatDateTime(inspection.getEndTime()))).append('\n');
        csv.append("Total Inspection Time,").append(quote(formatDuration(inspection.getTotalDurationMs()))).append('\n');
        csv.append('\n');
        csv.append("Item Code,Item Name,Zone,Response,Fixed,Status,Duration,Remarks\n");

        for (var item : inspection.getChecklistItems()) {
            csv.append(quote(item.getItemCode())).append(',')
                    .append(quote(item.getItemName())).append(',')
                    .append(quote(item.getZoneName())).append(',')
                    .append(quote(item.getResponse())).append(',')
                    .append(item.isFixed()).append(',')
                    .append(quote(item.getStatus())).append(',')
                    .append(quote(formatDuration(item.getDurationMs()))).append(',')
                    .append(quote(item.getRemarks())).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportChecklistAnalyticsPdf(List<ChecklistItemAnalyticsDTO> analytics) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Checklist Analytics Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Items are sorted by total time spent."));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            addHeader(table, "Item Code");
            addHeader(table, "Item Name");
            addHeader(table, "Zone");
            addHeader(table, "Submissions");
            addHeader(table, "Avg (ms)");
            addHeader(table, "Max (ms)");
            addHeader(table, "Total (ms)");

            for (ChecklistItemAnalyticsDTO item : analytics) {
                table.addCell(safe(item.getItemCode()));
                table.addCell(safe(item.getItemName()));
                table.addCell(safe(item.getZoneName()));
                table.addCell(String.valueOf(item.getSubmissions()));
                table.addCell(String.valueOf(item.getAverageDurationMs()));
                table.addCell(String.valueOf(item.getMaxDurationMs()));
                table.addCell(String.valueOf(item.getTotalDurationMs()));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Failed to generate checklist analytics PDF.", e);
        }
    }

    private byte[] exportChecklistAnalyticsXlsx(List<ChecklistItemAnalyticsDTO> analytics) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Checklist Analytics");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Item Code");
            header.createCell(1).setCellValue("Item Name");
            header.createCell(2).setCellValue("Zone");
            header.createCell(3).setCellValue("Submissions");
            header.createCell(4).setCellValue("Average Duration (ms)");
            header.createCell(5).setCellValue("Max Duration (ms)");
            header.createCell(6).setCellValue("Total Duration (ms)");

            int rowIndex = 1;
            for (ChecklistItemAnalyticsDTO item : analytics) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safe(item.getItemCode()));
                row.createCell(1).setCellValue(safe(item.getItemName()));
                row.createCell(2).setCellValue(safe(item.getZoneName()));
                row.createCell(3).setCellValue(item.getSubmissions());
                row.createCell(4).setCellValue(item.getAverageDurationMs());
                row.createCell(5).setCellValue(item.getMaxDurationMs());
                row.createCell(6).setCellValue(item.getTotalDurationMs());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate checklist analytics XLSX.", e);
        }
    }

    private byte[] exportChecklistAnalyticsCsv(List<ChecklistItemAnalyticsDTO> analytics) {
        StringBuilder csv = new StringBuilder("Item Code,Item Name,Zone,Submissions,Average Duration (ms),Max Duration (ms),Total Duration (ms)\n");
        for (ChecklistItemAnalyticsDTO item : analytics) {
            csv.append(quote(item.getItemCode())).append(',')
                    .append(quote(item.getItemName())).append(',')
                    .append(quote(item.getZoneName())).append(',')
                    .append(item.getSubmissions()).append(',')
                    .append(item.getAverageDurationMs()).append(',')
                    .append(item.getMaxDurationMs()).append(',')
                    .append(item.getTotalDurationMs()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addHeader(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value));
        table.addCell(cell);
    }

    private void writeSummaryRow(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(safe(value));
    }

    private String normalizeFormat(String format) {
        return format == null ? "pdf" : format.toLowerCase(Locale.ROOT);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safe(String firstChoice, String fallback) {
        return firstChoice != null && !firstChoice.isBlank() ? firstChoice : safe(fallback);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(STANDARD_TIME_FORMAT);
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return "0 sec";
        }

        long totalSeconds = Duration.ofMillis(durationMs).getSeconds();
        if (totalSeconds < 60) {
            return totalSeconds + " sec";
        }

        long totalMinutes = Duration.ofMillis(durationMs).toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            return minutes + " min";
        }
        if (minutes == 0) {
            return hours + " hr";
        }
        return hours + " hr " + minutes + " min";
    }

    private String quote(Object value) {
        return "\"" + safe(value).replace("\"", "\"\"") + "\"";
    }

    private static class LongSummary {
        private long total;
        private long max;
        private long count;

        void accept(long value) {
            total += value;
            max = Math.max(max, value);
            count++;
        }

        void combine(LongSummary other) {
            total += other.total;
            max = Math.max(max, other.max);
            count += other.count;
        }
    }
}
