CREATE TABLE function_dependencies (
    function_id INTEGER NOT NULL,
    dependency_function_id INTEGER NOT NULL,

    PRIMARY KEY (function_id, dependency_function_id),

    FOREIGN KEY (function_id) REFERENCES functions(id) ON DELETE CASCADE,
    FOREIGN KEY (dependency_function_id) REFERENCES functions(id) ON DELETE CASCADE,

    -- check for no self-dependencies
    CONSTRAINT check_no_self_dependency CHECK (function_id != dependency_function_id)
);

CREATE OR REPLACE FUNCTION check_function_dependency_insert()
RETURNS TRIGGER AS $$
DECLARE
    ancestor_path ltree;
    descendant_path ltree;
BEGIN
    -- Fetch the path of the function being depended upon
    SELECT path INTO ancestor_path FROM functions WHERE id = NEW.dependency_function_id;

    -- Fetch the path of the function that is supposed to depend on another
    SELECT path INTO descendant_path FROM functions WHERE id = NEW.function_id;

    -- Check if there's a direct descendant relationship
    IF ancestor_path @> descendant_path THEN
        RAISE EXCEPTION 'Cannot insert: The dependency would create a direct descendant relationship.';
    END IF;

    -- Check if there's a direct ancestor relationship
    IF descendant_path @> ancestor_path THEN
        RAISE EXCEPTION 'Cannot insert: The dependency would create a direct ancestor relationship.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for insert
CREATE TRIGGER prevent_invalid_dependency_insert
BEFORE INSERT OR UPDATE ON function_dependencies
FOR EACH ROW
EXECUTE FUNCTION check_function_dependency_insert();