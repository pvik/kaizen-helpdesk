CREATE TABLE IF NOT EXISTS kaizen.status (
	   id serial PRIMARY KEY,
	   status_name VARCHAR(512)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.priority (
	   id serial PRIMARY KEY,
	   priority_name VARCHAR(512)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.ticket (
    id serial  PRIMARY KEY,
    subject           VARCHAR (1024) NOT NULL CHECK (subject <> ''),
		detail            TEXT,
		assigned_to_id    INT REFERENCES kaizen.user_detail(id),
		assigned_group_id INT REFERENCES kaizen.user_group(id),
		status_id         INT REFERENCES kaizen.status(id),
		priority_id       INT REFERENCES kaizen.priority(id),
		--location_id    INT REFERENCES kaizen.location(location_id),
    created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(id),
    created_on        TIMESTAMPTZ DEFAULT now(),
		updated_by_id     INT REFERENCES kaizen.user_detail(id),
    updated_on        TIMESTAMPTZ DEFAULT now()
);
--;;
CREATE VIEW kaizen.ticket_view AS
	   SELECT *,
  		  (SELECT status_name FROM kaizen.status s WHERE s.id = t.status_id) as status,
   		  (SELECT priority_name FROM kaizen.priority p WHERE p.id = t.priority_id) as priority,
			  (SELECT user_name FROM kaizen.user_detail u WHERE u.id = t.assigned_to_id) as assigned_to,
			  (SELECT user_group_name FROM kaizen.user_group g WHERE g.id = t.assigned_group_id) as assigned_group,
			  (SELECT user_name FROM kaizen.user_detail u WHERE u.id = t.updated_by_id) as updated_by,
			  (SELECT user_name FROM kaizen.user_detail u WHERE u.id = t.created_by_id) as created_by
	   FROM kaizen.ticket t
--;;
CREATE TABLE IF NOT EXISTS kaizen.custom_field (
    id serial  PRIMARY KEY,
		entity            VARCHAR (512) NOT NULL UNIQUE,
    field_name        VARCHAR (512) NOT NULL UNIQUE
		                      CHECK (field_name ~ '^[_a-z]+$'),
		field_type        VARCHAR (128) NOT NULL,
    created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(id),
    created_on        TIMESTAMPTZ DEFAULT now()
);
--;;
CREATE OR REPLACE FUNCTION kaizen.add_custom_field_to_entity()
  	   RETURNS trigger AS
$BODY$
BEGIN
	EXECUTE 'ALTER TABLE kaizen.' || NEW.entity || ' ADD COLUMN ' || NEW.field_name || ' ' || NEW.field_type;

	RETURN NEW;
END;
$BODY$ LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION kaizen.delete_custom_field_to_entity()
  	   RETURNS trigger AS
$BODY$
BEGIN
	EXECUTE 'ALTER TABLE kaizen.' || OLD.entity || ' DROP COLUMN ' || OLD.field_name;

	RETURN OLD;
END;
$BODY$ LANGUAGE plpgsql;
--;;
CREATE TRIGGER add_custom_field_to_entity_trig
	   BEFORE INSERT ON kaizen.custom_field
	   FOR EACH ROW
	   EXECUTE PROCEDURE kaizen.add_custom_field_to_entity();
--;;
CREATE TRIGGER delete_custom_field_to_entity_trig
	   BEFORE DELETE ON kaizen.custom_field
	   FOR EACH ROW
	   EXECUTE PROCEDURE kaizen.delete_custom_field_to_entity();
--;;
-- DATA INSERTS
INSERT INTO kaizen.status (status_name)
	   VALUES ('New');
INSERT INTO kaizen.status (status_name)
	   VALUES ('Assigned');
INSERT INTO kaizen.status (status_name)
	   VALUES ('In Progress');
INSERT INTO kaizen.status (status_name)
	   VALUES ('Resolved');
INSERT INTO kaizen.status (status_name)
	   VALUES ('Closed');
--;;
INSERT INTO kaizen.priority (priority_name)
	   VALUES ('Low');
INSERT INTO kaizen.priority (priority_name)
	   VALUES ('Medium');
INSERT INTO kaizen.priority (priority_name)
	   VALUES ('High');
--;;
