-- Start transaction
BEGIN;

-- Step 1: Insert the new key if it doesn't already exist
INSERT INTO function_metadata_keys (key)
VALUES ('rr-skjema')
ON CONFLICT DO NOTHING;

-- Step 2: Get the id of the new key
WITH new_key AS (
    SELECT id FROM function_metadata_keys WHERE key = 'rr-skjema'
)
-- Step 3: Insert new rows with updated values
INSERT INTO function_metadata (function_id, key_id, value)
SELECT
    fm.function_id,
    (SELECT id FROM new_key),
    CONCAT(
            fm.value,
            ':splitTarget:',
            SPLIT_PART(fmk.key, '-', 2),
            ':splitTarget:',
            SPLIT_PART(fmk.key, '-', 3)
    )
FROM
    function_metadata fm
        JOIN
    function_metadata_keys fmk ON fm.key_id = fmk.id
WHERE
    fmk.key LIKE 'rr-%-%';

-- Step 4: Delete old entries
DELETE FROM function_metadata
WHERE
    key_id IN (SELECT id FROM function_metadata_keys WHERE key LIKE 'rr-%-%');

-- Step 5: Optionally, clean up unused keys from function_metadata_keys
DELETE FROM function_metadata_keys
WHERE key LIKE 'rr-%-%'
  AND NOT EXISTS (
    SELECT 1 FROM function_metadata WHERE key_id = function_metadata_keys.id
);

-- Commit the transaction
COMMIT;