CREATE TABLE IF NOT EXISTS kaizen.user_type (
			 id INT PRIMARY KEY,
			 user_type VARCHAR (50) UNIQUE NOT NULL
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.user_detail (
			 id serial PRIMARY KEY,
			 user_name VARCHAR (128) UNIQUE NOT NULL CHECK (user_name <> ''),
			 user_type_id INT REFERENCES kaizen.user_type(id) ON DELETE CASCADE,
			 first_name VARCHAR (512),
			 last_name VARCHAR (512),
			 active BOOLEAN DEFAULT true,
			 api_key VARCHAR (512),
			 email VARCHAR (512) UNIQUE NOT NULL CHECK (email ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+$'),
			 secondary_email VARCHAR (512) CHECK (email ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+$'),
			 phone_number VARCHAR (15),
			 created_on TIMESTAMPTZ DEFAULT now(),
			 last_login TIMESTAMPTZ,
			 linked_client VARCHAR[512] -- for LDAP
);
--;;
CREATE VIEW kaizen.user AS
	   SELECT id, user_name, user_type_id, first_name, last_name, email,
		 				secondary_email, phone_number,
  		  (SELECT user_type FROM kaizen.user_type t WHERE t.id = u.user_type_id) as user_type
	   FROM kaizen.user_detail u
		 WHERE u.active = true
--;;
CREATE TABLE IF NOT EXISTS kaizen.user_auth (
			 id serial PRIMARY KEY,
			 user_id INT NOT NULL REFERENCES kaizen.user_detail(id) ON DELETE CASCADE,
			 password VARCHAR (512) NOT NULL CHECK (password <> '')
);
--;;
--CREATE INDEX ON user_auth_tokens (id, created_at DESC);
--;;
CREATE TABLE IF NOT EXISTS kaizen.user_group (
			 id INT PRIMARY KEY,
			 user_group_name VARCHAR (100) UNIQUE NOT NULL
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.user_group_membership (
			 id serial PRIMARY KEY,
			 user_id INT NOT NULL REFERENCES kaizen.user_detail(id),
			 user_group_id INT NOT NULL REFERENCES kaizen.user_group(id) ON DELETE CASCADE,
			 added_on TIMESTAMPTZ DEFAULT now(),
			 active BOOLEAN DEFAULT true
);
--;;
-- DATA INSERTS
INSERT INTO kaizen.user_type (id, user_type) VALUES (1, 'admin');
INSERT INTO kaizen.user_type (id, user_type) VALUES (2, 'tech');
INSERT INTO kaizen.user_type (id, user_type) VALUES (3, 'client');
--;;
INSERT INTO kaizen.user_detail (id, user_name, user_type_id, active, email)
VALUES (1, 'admin', 1, true, 'admin1@test.org');
--;;	
-- Default admin password is khd#admin
INSERT INTO kaizen.user_auth (user_id, password) 
VALUES (1, '$argon2id$v=19$m=65536,t=8,p=1$3X1n5ZCGrDYIbCYghBfEAw$aUyr8jhSZj1ODJda938F9avMTOo0S4r0o22Ge+oKw9s');
--;;
