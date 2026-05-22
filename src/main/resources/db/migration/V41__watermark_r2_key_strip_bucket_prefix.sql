-- Bucket name is already "civify"; object keys must be watermarks/... not civify/watermarks/...

UPDATE platform_watermark_settings
SET logo_storage_key = 'watermarks/platform/logo.png'
WHERE logo_storage_key = 'civify/watermarks/platform/logo.png'
   OR logo_storage_key = 'Watermark/platform/logo.png';

UPDATE platform_watermark_settings
SET logo_storage_key = REPLACE(logo_storage_key, 'civify/watermarks/', 'watermarks/')
WHERE logo_storage_key LIKE 'civify/watermarks/%';

UPDATE user_watermark_settings
SET logo_storage_key = REPLACE(logo_storage_key, 'civify/watermarks/', 'watermarks/')
WHERE logo_storage_key LIKE 'civify/watermarks/%';

UPDATE user_watermark_settings
SET logo_storage_key = REPLACE(logo_storage_key, 'Watermark/', 'watermarks/')
WHERE logo_storage_key LIKE 'Watermark/%';
