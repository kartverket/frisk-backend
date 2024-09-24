CREATE TABLE function_metadata_keys (
    id SERIAL PRIMARY KEY,
    key TEXT UNIQUE NOT NULL

    CONSTRAINT key_lowercase CHECK (key = LOWER(key))
);

CREATE TABLE function_metadata (
    function_id INTEGER NOT NULL,
    key_id INTEGER NOT NULL,
    value TEXT NOT NULL,
    PRIMARY KEY (function_id, key_id),
    FOREIGN KEY (function_id) REFERENCES functions(id),
    FOREIGN KEY (key_id) REFERENCES function_metadata_keys(id)
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
