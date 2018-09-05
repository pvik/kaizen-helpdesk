CREATE TABLE IF NOT EXISTS kaizen.ticket_status (
	   ticket_status_id serial PRIMARY KEY,
	   status_name VARCHAR(512)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.ticket_priority (
	   ticket_priority_id serial PRIMARY KEY,
	   priority_name VARCHAR(512)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.ticket_detail (
        ticket_id serial  PRIMARY KEY,
        subject           VARCHAR (1024) NOT NULL CHECK (subject <> ''),
		detail            TEXT,
		assigned_to_id    INT REFERENCES kaizen.user_detail(user_id),
		assigned_group_id INT REFERENCES kaizen.user_group(user_group_id),
		status_id         INT REFERENCES kaizen.ticket_status(ticket_status_id),
		priority_id       INT REFERENCES kaizen.ticket_priority(ticket_priority_id),
		--location_id    INT REFERENCES kaizen.location(location_id),
        created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(user_id),
        created_on        TIMESTAMPTZ DEFAULT now(),
		updated_by_id     INT REFERENCES kaizen.user_detail(user_id),
        updated_on        TIMESTAMPTZ DEFAULT now()
        );
--;;
CREATE VIEW kaizen.ticket AS
	   SELECT *,
	   		  (SELECT status_name FROM kaizen.ticket_status s WHERE s.ticket_status_id = t.status_id) as status,
	   		  (SELECT priority_name FROM kaizen.ticket_priority p WHERE p.ticket_priority_id = t.priority_id) as priority,
			  (SELECT user_name FROM kaizen.user_detail u WHERE u.user_id = t.assigned_to_id) as assigned_to,
			  (SELECT user_group_name FROM kaizen.user_group g WHERE g.user_group_id = t.assigned_group_id) as assigned_group,
			  (SELECT user_name FROM kaizen.user_detail u WHERE u.user_id = t.updated_by_id) as updated_by,
			  (SELECT user_name FROM kaizen.user_detail u WHERE u.user_id = t.created_by_id) as created_by
	   FROM kaizen.ticket_detail t
--;;
CREATE TABLE IF NOT EXISTS kaizen.ticket_custom_field (
        custom_field_id serial  PRIMARY KEY,
        field_name        VARCHAR (512) NOT NULL UNIQUE
		                      CHECK (field_name ~ '^[_a-z]+$'),
		field_type        VARCHAR (128) NOT NULL,
        created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(user_id),
        created_on        TIMESTAMPTZ DEFAULT now()
        );
--;;
CREATE OR REPLACE FUNCTION kaizen.add_custom_field_to_ticket_detail()
  	   RETURNS trigger AS
$BODY$
BEGIN
	EXECUTE 'ALTER TABLE kaizen.ticket_detail ADD COLUMN ' || NEW.field_name || ' ' || NEW.field_type;

	RETURN NEW;
END;
$BODY$ LANGUAGE plpgsql;
--;;
CREATE OR REPLACE FUNCTION kaizen.delete_custom_field_to_ticket_detail()
  	   RETURNS trigger AS
$BODY$
BEGIN
	EXECUTE 'ALTER TABLE kaizen.ticket_detail DROP COLUMN ' || OLD.field_name;

	RETURN OLD;
END;
$BODY$ LANGUAGE plpgsql;
--;;
CREATE TRIGGER add_custom_field_to_ticket_detail_trig
	   BEFORE INSERT ON kaizen.ticket_custom_field
	   FOR EACH ROW
	   EXECUTE PROCEDURE kaizen.add_custom_field_to_ticket_detail();
--;;
CREATE TRIGGER delete_custom_field_to_ticket_detail_trig
	   BEFORE DELETE ON kaizen.ticket_custom_field
	   FOR EACH ROW
	   EXECUTE PROCEDURE kaizen.delete_custom_field_to_ticket_detail();
--;;
-- DATA INSERTS
INSERT INTO kaizen.ticket_status (status_name)
	   VALUES ('New');
INSERT INTO kaizen.ticket_status (status_name)
	   VALUES ('Assigned');
INSERT INTO kaizen.ticket_status (status_name)
	   VALUES ('In Progress');
INSERT INTO kaizen.ticket_status (status_name)
	   VALUES ('Resolved');
INSERT INTO kaizen.ticket_status (status_name)
	   VALUES ('Closed');
--;;
INSERT INTO kaizen.ticket_priority (priority_name)
	   VALUES ('Low');
INSERT INTO kaizen.ticket_priority (priority_name)
	   VALUES ('Medium');
INSERT INTO kaizen.ticket_priority (priority_name)
	   VALUES ('High');
--;;
