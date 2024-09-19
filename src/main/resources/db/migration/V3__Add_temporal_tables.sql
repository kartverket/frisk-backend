ALTER TABLE functions ADD COLUMN valid_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE functions ADD COLUMN valid_to TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT 'infinity';

CREATE TABLE functions_history (
   LIKE functions
);

ALTER TABLE functions_history ADD CONSTRAINT pk_functions_history PRIMARY KEY (id, valid_from, valid_to);

-- Trigger function to handle updates and deletes
CREATE OR REPLACE FUNCTION maintain_function_history()
    RETURNS TRIGGER AS $$
BEGIN
    -- Insert the old version into history
    OLD.valid_to = CURRENT_TIMESTAMP;
    INSERT INTO functions_history SELECT (OLD).*;
    IF (TG_OP = 'UPDATE') THEN
        -- Ensure the new version starts from now
        NEW.valid_from = CURRENT_TIMESTAMP;
        NEW.valid_to = 'infinity';
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create the trigger
CREATE TRIGGER functions_temporal_trigger
    BEFORE UPDATE OR DELETE ON functions
    FOR EACH ROW EXECUTE FUNCTION maintain_function_history();

-- Trigger for INSERT to ensure temporal integrity
CREATE OR REPLACE FUNCTION check_insert_function()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.valid_from = CURRENT_TIMESTAMP;
    NEW.valid_to = 'infinity';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER functions_insert_temporal_check
    BEFORE INSERT ON functions
    FOR EACH ROW EXECUTE FUNCTION check_insert_function();