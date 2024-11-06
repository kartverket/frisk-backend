CREATE OR REPLACE FUNCTION update_child_paths()
    RETURNS TRIGGER AS $$
BEGIN
    -- If this is not the root node
    IF NEW.parent_id IS NOT NULL THEN
        WITH RECURSIVE path_update(id, path, parent_id) AS (
            SELECT id, path, parent_id
            FROM functions
            WHERE id = NEW.id
            UNION ALL
            SELECT f.id, pu.path || f.id::text, f.parent_id
            FROM functions f
                     JOIN path_update pu ON f.parent_id = pu.id
        )
        UPDATE functions f
        SET path = pu.path
        FROM path_update pu
        WHERE f.id = pu.id;

    ELSE
        IF OLD.parent_id IS NOT NULL THEN
            WITH RECURSIVE path_reset(id, path) AS (
                SELECT id, id::text
                FROM functions
                WHERE id = NEW.id
                UNION ALL
                SELECT f.id, pr.path || f.id::text
                FROM functions f
                         JOIN path_reset pr ON f.parent_id = pr.id
            )
            UPDATE functions f
            SET path = pr.path
            FROM path_reset pr
            WHERE f.id = pr.id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to execute the function before insert or update
CREATE TRIGGER tree_structure_integrity_recursive_children
    AFTER UPDATE OF parent_id ON functions
    FOR EACH ROW
EXECUTE FUNCTION update_child_paths();