-- SHA-256 hex digest for file deduplication (nullable for pre-existing rows)
ALTER TABLE stored_files ADD COLUMN file_hash VARCHAR(64);
-- Unique index: prevents race-condition duplicates; NULLs are excluded by most DBs
CREATE UNIQUE INDEX idx_stored_files_hash ON stored_files(file_hash);
