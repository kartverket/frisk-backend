-- Ensure the 'dependencies' key exists in function_metadata_keys
INSERT INTO function_metadata_keys (key)
VALUES ('dependencies')
ON CONFLICT (key) DO NOTHING;

-- Step 1: Retrieve all dependencies from function_dependencies
-- This step is implied as we'll use the data directly in the next step

-- Step 2: Add dependencies as metadata to the correct functions
INSERT INTO function_metadata (function_id, key_id, value)
SELECT
    fd.function_id,
    km.id,
    fd.dependency_function_id::TEXT
FROM
    function_dependencies fd
        JOIN
    function_metadata_keys km ON km.key = 'dependencies'
WHERE
    NOT EXISTS (
        SELECT 1 FROM function_metadata fm
        WHERE fm.function_id = fd.function_id
          AND fm.key_id = km.id
          AND fm.value = fd.dependency_function_id::TEXT
    );

-- Step 3: Drop the function_dependencies table after transferring the data
DROP TABLE function_dependencies;