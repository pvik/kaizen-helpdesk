CREATE TABLE IF NOT EXISTS kaizen.company (
	   id serial PRIMARY KEY,
	   company_name VARCHAR(512)
		 -- manager_id
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.location (
	   id serial PRIMARY KEY,
	   location_name VARCHAR(512),
		 address VARCHAR(512),
		 address2 VARCHAR(512),
		 city VARCHAR(128),
		 zip VARCHAR(12),
		 country VARCHAR(12),
		 phone VARCHAR(12),
		 fax VARCHAR(12),
		 company_id INT REFERENCES kaizen.company(id)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.location_group (
	   id serial PRIMARY KEY,
	   location_group_name VARCHAR(512)
		 -- manager_id
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.location_group_member (
	   id serial PRIMARY KEY,
		 location_group_id INT REFERENCES kaizen.location_group(id),
		 location_id INT REFERENCES kaizen.location(id)
		 -- manager_id
);
