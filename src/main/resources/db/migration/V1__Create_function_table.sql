-- Create the ltree extension if not already created
CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TABLE functions (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    parent_id INTEGER,
    path ltree, -- Using the ltree extension for path tracking

    -- Ensure parent_id is either null (for root) or references an existing node
    FOREIGN KEY (parent_id) REFERENCES functions(id) ON DELETE CASCADE,

    -- Ensure each node except root has a parent
    CONSTRAINT check_parent CHECK (id != parent_id AND (parent_id IS NOT NULL OR id = 1)), -- Assuming '1' is the root

    -- Unique constraint to ensure no duplicate names at the same level (optional, depends on your requirements)
    CONSTRAINT unique_name_per_level UNIQUE (name, parent_id)
);

-- Index for ltree path for efficient querying
CREATE INDEX path_gist_idx ON functions USING GIST (path);

CREATE OR REPLACE FUNCTION prevent_cycle_and_set_path()
RETURNS TRIGGER AS $$
DECLARE
    parent_path ltree;
BEGIN

    -- If this is not the root node
    IF NEW.parent_id IS NOT NULL THEN
        SELECT path INTO parent_path FROM functions WHERE id = NEW.parent_id;

        IF parent_path IS NULL THEN
            RAISE EXCEPTION 'Parent node does not exist';
        END IF;

        -- Check for cycle using the @> operator
        IF parent_path @> text2ltree(NEW.id::text) THEN
            RAISE EXCEPTION 'Cycle detected: Cannot set parent to a descendant';
        END IF;

        -- Set the path
        NEW.path = parent_path || NEW.id::text;
    ELSE
        -- This is the root, set path as its own id
        NEW.path = NEW.id::text;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to execute the function before insert or update
CREATE TRIGGER tree_structure_integrity
BEFORE INSERT OR UPDATE OF parent_id ON functions
FOR EACH ROW
EXECUTE FUNCTION prevent_cycle_and_set_path();

-- Ensure root node exists
INSERT INTO functions (id, name, parent_id, path) VALUES (1, 'Root', NULL, '1') ON CONFLICT (id) DO NOTHING;