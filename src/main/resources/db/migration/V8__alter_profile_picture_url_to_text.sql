-- Alter profile_picture_url column to TEXT to support base64 encoded images
ALTER TABLE public.users ALTER COLUMN profile_picture_url TYPE TEXT;
