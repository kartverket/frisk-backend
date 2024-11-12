ALTER TABLE functions ADD COLUMN order_index INTEGER;
ALTER TABLE functions_history ADD COLUMN order_index INTEGER;

WITH numbered_functions AS (
    SELECT
        id,
        parent_id,
        ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY id) - 1 AS new_order_index
    FROM
        functions
)
UPDATE functions f
SET order_index = nf.new_order_index
FROM numbered_functions nf
WHERE f.id = nf.id;

ALTER TABLE functions ALTER COLUMN order_index SET NOT NULL;

CREATE FUNCTION assign_function_order()
    RETURNS TRIGGER AS $$
BEGIN
    SELECT COALESCE(MAX(order_index), -1) + 1 INTO NEW.order_index
    FROM functions
    WHERE parent_id = NEW.parent_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_function_order_index
    BEFORE INSERT ON functions
    FOR EACH ROW
EXECUTE FUNCTION assign_function_order();


CREATE OR REPLACE FUNCTION update_functions_order_after_delete()
    RETURNS TRIGGER AS $$
BEGIN
    -- Shift down the order_index for remaining functions under the same parent
    UPDATE functions
    SET order_index = order_index - 1
    WHERE parent_id = OLD.parent_id AND order_index > OLD.order_index;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_order_index_after_delete
    AFTER DELETE ON functions
    FOR EACH ROW
EXECUTE FUNCTION update_functions_order_after_delete();