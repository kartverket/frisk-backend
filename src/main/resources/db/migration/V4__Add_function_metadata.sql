CREATE TABLE function_metadata_keys (
    id SERIAL PRIMARY KEY,
    key TEXT UNIQUE NOT NULL

    CONSTRAINT key_lowercase CHECK (key = LOWER(key))
);

CREATE TABLE function_metadata (
    id SERIAL PRIMARY KEY,
    function_id INTEGER NOT NULL,
    key_id INTEGER NOT NULL,
    value TEXT NOT NULL,
    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE CASCADE,
    FOREIGN KEY (key_id) REFERENCES function_metadata_keys(id) ON DELETE CASCADE,
    CONSTRAINT unique_function_key_value UNIQUE(function_id, key_id, value)
);

CREATE OR REPLACE FUNCTION lowercase_key()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.key = LOWER(NEW.key);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ensure_lowercase_key
    BEFORE INSERT OR UPDATE ON function_metadata_keys
    FOR EACH ROW
EXECUTE FUNCTION lowercase_key();
