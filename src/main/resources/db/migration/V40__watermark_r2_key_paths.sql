-- Align stored keys with R2 layout: civify/watermarks/{userId}/logo.png (same bucket as CVs)

UPDATE platform_watermark_settings
SET logo_storage_key = 'civify/watermarks/platform/logo.png'
WHERE logo_storage_key = 'Watermark/platform/logo.png';

UPDATE platform_watermark_settings
SET logo_storage_key = REPLACE(logo_storage_key, 'Watermark/platform/', 'civify/watermarks/platform/')
WHERE logo_storage_key LIKE 'Watermark/platform/%';

UPDATE user_watermark_settings
SET logo_storage_key = REPLACE(logo_storage_key, 'Watermark/', 'civify/watermarks/')
WHERE logo_storage_key LIKE 'Watermark/%';
