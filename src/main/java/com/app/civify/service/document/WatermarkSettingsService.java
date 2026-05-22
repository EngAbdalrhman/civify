package com.app.civify.service.document;

import com.app.civify.dto.watermark.PlatformWatermarkSettingsDto;
import com.app.civify.dto.watermark.UserWatermarkSettingsDto;
import com.app.civify.dto.watermark.WatermarkSettingsUpdateRequest;
import com.app.civify.entity.watermark.PlatformWatermarkSettings;
import com.app.civify.entity.watermark.UserWatermarkSettings;
import com.app.civify.repository.watermark.PlatformWatermarkSettingsRepository;
import com.app.civify.repository.watermark.UserWatermarkSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WatermarkSettingsService {

    private final PlatformWatermarkSettingsRepository platformRepository;
    private final UserWatermarkSettingsRepository userRepository;
    private final BucketService bucketService;

    @Transactional(readOnly = true)
    public PlatformWatermarkSettingsDto getPlatformSettings() {
        PlatformWatermarkSettings settings = platformRepository.findById(1L).orElseGet(this::defaultPlatformSettings);
        return toPlatformDto(settings);
    }

    @Transactional
    public PlatformWatermarkSettingsDto updatePlatformSettings(WatermarkSettingsUpdateRequest request) {
        PlatformWatermarkSettings settings = platformRepository.findById(1L).orElseGet(this::defaultPlatformSettings);
        applyUpdate(settings, request);
        platformRepository.save(settings);
        return toPlatformDto(settings);
    }

    @Transactional
    public PlatformWatermarkSettingsDto uploadPlatformLogo(MultipartFile file) {
        String key = bucketService.storePlatformWatermark(file);
        PlatformWatermarkSettings settings = platformRepository.findById(1L).orElseGet(this::defaultPlatformSettings);
        settings.setLogoStorageKey(key);
        platformRepository.save(settings);
        return toPlatformDto(settings);
    }

    @Transactional(readOnly = true)
    public UserWatermarkSettingsDto getUserSettings(Long userId) {
        return userRepository.findByUserId(userId)
                .map(this::toUserDto)
                .orElse(UserWatermarkSettingsDto.builder()
                        .enabled(false)
                        .usePlatformWatermark(false)
                        .opacity(0.12)
                        .scale(0.35)
                        .position("center")
                        .rotation(0.0)
                        .build());
    }

    @Transactional
    public UserWatermarkSettingsDto updateUserSettings(Long userId, WatermarkSettingsUpdateRequest request) {
        UserWatermarkSettings settings = userRepository.findByUserId(userId).orElseGet(() -> newUserSettings(userId));
        applyUpdate(settings, request);
        userRepository.save(settings);
        return toUserDto(settings);
    }

    @Transactional
    public UserWatermarkSettingsDto uploadUserLogo(Long userId, MultipartFile file) {
        String key = bucketService.storeUserWatermark(userId, file);
        UserWatermarkSettings settings = userRepository.findByUserId(userId).orElseGet(() -> newUserSettings(userId));
        settings.setLogoStorageKey(key);
        settings.setEnabled(true);
        settings.setUsePlatformWatermark(false);
        userRepository.save(settings);
        return toUserDto(settings);
    }

    @Transactional
    public void deleteUserLogo(Long userId) {
        userRepository.findByUserId(userId).ifPresent(settings -> {
            if (settings.getLogoStorageKey() != null) {
                bucketService.deleteObject(settings.getLogoStorageKey());
            }
            settings.setLogoStorageKey(null);
            settings.setEnabled(false);
            userRepository.save(settings);
        });
    }

    private PlatformWatermarkSettings defaultPlatformSettings() {
        PlatformWatermarkSettings s = new PlatformWatermarkSettings();
        s.setId(1L);
        return s;
    }

    private UserWatermarkSettings newUserSettings(Long userId) {
        UserWatermarkSettings s = new UserWatermarkSettings();
        s.setUserId(userId);
        return s;
    }

    private void applyUpdate(PlatformWatermarkSettings settings, WatermarkSettingsUpdateRequest request) {
        if (request.getOpacity() != null) settings.setOpacity(request.getOpacity());
        if (request.getScale() != null) settings.setScale(request.getScale());
        if (request.getPosition() != null) settings.setPosition(request.getPosition());
        if (request.getRotation() != null) settings.setRotation(request.getRotation());
        if (request.getEnabled() != null) settings.setEnabled(request.getEnabled());
    }

    private void applyUpdate(UserWatermarkSettings settings, WatermarkSettingsUpdateRequest request) {
        if (request.getOpacity() != null) settings.setOpacity(request.getOpacity());
        if (request.getScale() != null) settings.setScale(request.getScale());
        if (request.getPosition() != null) settings.setPosition(request.getPosition());
        if (request.getRotation() != null) settings.setRotation(request.getRotation());
        if (request.getEnabled() != null) settings.setEnabled(request.getEnabled());
        if (request.getUsePlatformWatermark() != null) settings.setUsePlatformWatermark(request.getUsePlatformWatermark());
    }

    private PlatformWatermarkSettingsDto toPlatformDto(PlatformWatermarkSettings settings) {
        String logoUrl = bucketService.buildPublicLogoUrl(settings.getLogoStorageKey());
        return PlatformWatermarkSettingsDto.builder()
                .logoStorageKey(settings.getLogoStorageKey())
                .logoUrl(logoUrl)
                .opacity(settings.getOpacity())
                .scale(settings.getScale())
                .position(settings.getPosition())
                .rotation(settings.getRotation())
                .enabled(settings.getEnabled())
                .build();
    }

    private UserWatermarkSettingsDto toUserDto(UserWatermarkSettings settings) {
        String logoUrl = bucketService.buildPublicLogoUrl(settings.getLogoStorageKey());
        return UserWatermarkSettingsDto.builder()
                .enabled(settings.getEnabled())
                .logoStorageKey(settings.getLogoStorageKey())
                .logoUrl(logoUrl)
                .opacity(settings.getOpacity())
                .scale(settings.getScale())
                .position(settings.getPosition())
                .rotation(settings.getRotation())
                .usePlatformWatermark(settings.getUsePlatformWatermark())
                .build();
    }
}
