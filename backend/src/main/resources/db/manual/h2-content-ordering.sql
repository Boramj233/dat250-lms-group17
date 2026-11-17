-- Manual H2 upgrade. Back up and stop the application first; H2 DDL is not atomic.
-- Normalize each module using old position, then ID to break ties. No IDs/history are changed.
ALTER TABLE contents DROP CONSTRAINT IF EXISTS uq_content_module_position;
CREATE LOCAL TEMPORARY TABLE content_order_migration AS
    SELECT id, CAST(ROW_NUMBER() OVER (PARTITION BY module_id ORDER BY content_position, id) - 1 AS INTEGER) AS new_position
    FROM contents;
UPDATE contents SET content_position = (
    SELECT new_position FROM content_order_migration WHERE content_order_migration.id = contents.id
);
DROP TABLE content_order_migration;
ALTER TABLE contents ADD CONSTRAINT IF NOT EXISTS ck_content_position_nonnegative CHECK (content_position >= 0);
ALTER TABLE contents ADD CONSTRAINT uq_content_module_position UNIQUE (module_id, content_position);
