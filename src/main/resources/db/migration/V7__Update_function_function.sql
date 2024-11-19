CREATE OR REPLACE FUNCTION update_function(
    function_id INTEGER,
    new_order_index INTEGER,
    new_name TEXT,
    new_description TEXT DEFAULT NULL,
    new_parent_id INTEGER DEFAULT NULL
)
    RETURNS SETOF functions AS $$
DECLARE
    old_parent_id INTEGER DEFAULT NULL;
    old_order_index INTEGER;
BEGIN
    -- Fetch the old state from the database
    SELECT parent_id, order_index INTO old_parent_id, old_order_index
    FROM functions
    WHERE id = function_id
    FOR UPDATE;

    -- Check if parent_id or order_index has changed
    IF old_parent_id IS DISTINCT FROM new_parent_id OR old_order_index IS DISTINCT FROM new_order_index THEN
        -- If parent_id has changed
        IF old_parent_id IS DISTINCT FROM new_parent_id THEN
            -- Close the gap in the old parent
            IF old_parent_id IS NOT NULL THEN
                UPDATE functions
                SET order_index = order_index - 1
                WHERE parent_id = old_parent_id AND order_index > old_order_index;
            ELSE
                UPDATE functions
                SET order_index = order_index - 1
                WHERE parent_id IS NULL AND order_index > old_order_index;
            END IF;

            -- Make space in the new parent
            IF new_parent_id IS NOT NULL THEN
                UPDATE functions
                SET order_index = order_index + 1
                WHERE parent_id = new_parent_id AND order_index >= new_order_index;
            ELSE
                UPDATE functions
                SET order_index = order_index + 1
                WHERE parent_id IS NULL AND order_index >= new_order_index;
            END IF;
        ELSE
            -- If only order_index has changed within the same parent or root
            IF old_parent_id IS NOT NULL THEN
                -- Update within the same non-NULL parent
                IF new_order_index > old_order_index THEN
                    UPDATE functions
                    SET order_index = order_index - 1
                    WHERE parent_id = old_parent_id AND order_index > old_order_index AND order_index <= new_order_index;
                ELSE
                    UPDATE functions
                    SET order_index = order_index + 1
                    WHERE parent_id = old_parent_id AND order_index < old_order_index AND order_index >= new_order_index;
                END IF;
            ELSE
                -- Update within the root (NULL parent)
                IF new_order_index > old_order_index THEN
                    UPDATE functions
                    SET order_index = order_index - 1
                    WHERE parent_id IS NULL AND order_index > old_order_index AND order_index <= new_order_index;
                ELSE
                    UPDATE functions
                    SET order_index = order_index + 1
                    WHERE parent_id IS NULL AND order_index < old_order_index AND order_index >= new_order_index;
                END IF;
            END IF;
        END IF;
    END IF;

    -- Update the function's own order index
    RETURN QUERY
    UPDATE functions
    SET name = new_name,
        description = new_description,
        parent_id = new_parent_id,
        order_index = new_order_index
    WHERE id = function_id
    RETURNING *;
END;
$$ LANGUAGE plpgsql;