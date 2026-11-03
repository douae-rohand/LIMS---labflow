package com.backend.export.controller;

import com.backend.export.service.ExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export de données pour les administrateurs (M12)")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/{jeu}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<byte[]> exporter(
            @PathVariable String jeu,
            @RequestParam(defaultValue = "csv") String format) {
        byte[] donnees = exportService.exporter(jeu, format);

        String contentType;
        String extension;
        String filename;

        switch (format.toLowerCase()) {
            case "xlsx":
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = ".xlsx";
                break;
            case "pdf":
                contentType = "application/pdf";
                extension = ".pdf";
                break;
            default:
                contentType = "text/csv; charset=UTF-8";
                extension = ".csv";
                break;
        }

        filename = "export_" + jeu + extension;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(donnees);
    }
}
