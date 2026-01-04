package com.app.civify.controller.v1.geo;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.service.geo.GeoLocationResult;
import com.app.civify.service.geo.GeoLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public endpoint for geolocation detection
 * Used to determine user's country for pricing purposes
 */
@ApiV1Controller
@RequestMapping("/geo")
@RequiredArgsConstructor
@Tag(name = "Geolocation", description = "IP-based location detection")
public class GeoLocationController {

    private final GeoLocationService geoLocationService;

    @GetMapping("/location")
    @Operation(summary = "Get location from IP", description = "Detects user's country based on their IP address")
    public ResponseEntity<GeoLocationResult> getLocation(HttpServletRequest request) {
        GeoLocationResult result = geoLocationService.getLocationFromRequest(request);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/country")
    @Operation(summary = "Get country code", description = "Returns just the ISO country code for the user's IP")
    public ResponseEntity<CountryResponse> getCountryCode(HttpServletRequest request) {
        String clientIp = geoLocationService.extractClientIp(request);
        String countryCode = geoLocationService.getCountryCode(clientIp);
        return ResponseEntity.ok(new CountryResponse(countryCode, clientIp));
    }
    
    public record CountryResponse(String countryCode, String ip) {}
}
