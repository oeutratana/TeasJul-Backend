package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record HouseImportPreviewResponse(
        Long houseId,
        String fileName,
        int totalRows,
        int validRows,
        int invalidRows,
        List<HouseImportPreviewRowResponse> rows
) {
    public static HouseImportPreviewResponse of(Long houseId, String fileName,
            List<HouseImportPreviewRowResponse> rows) {
        int total = rows.size();
        int valid = (int) rows.stream().filter(HouseImportPreviewRowResponse::valid).count();
        return new HouseImportPreviewResponse(houseId, fileName, total, valid, total - valid, rows);
    }
}