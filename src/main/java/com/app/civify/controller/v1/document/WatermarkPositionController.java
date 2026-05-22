package com.app.civify.controller.v1.document;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.entity.enums.WatermarkPosition;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApiV1Controller
@RequestMapping("/pdf/watermark-positions")
@RequiredArgsConstructor
@Tag(name = "PDF", description = "PDF generation helpers")
public class WatermarkPositionController {

    @Operation(summary = "List allowed watermark positions for admin/settings UI")
    @GetMapping
    public ResponseEntity<CommonResponse<List<Map<String, String>>>> listPositions() {
        List<Map<String, String>> positions = Arrays.stream(WatermarkPosition.values())
                .map(p -> Map.of("value", p.getValue(), "label", p.getLabel()))
                .toList();
        return ResponseEntity.ok(CommonResponse.success(positions));
    }
}
