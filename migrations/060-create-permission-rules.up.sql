CREATE TABLE IF NOT EXISTS kaizen.permission_rule (
	   id 		 					 serial PRIMARY KEY,
	   permission_name   VARCHAR(128) NOT NULL UNIQUE CHECK (permission_name <> ''),
	   entity            VARCHAR(32) NOT NULL,
	   qualification     VARCHAR(1024) NOT NULL,
	   rule_order INT2   NOT NULL CHECK (rule_order >= 0 AND rule_order < 1000),
	   default_rule      BOOL DEFAULT false,
	   enabled           BOOL DEFAULT true,
	   -- user details
	   created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(id),
     created_on        TIMESTAMPTZ DEFAULT now(),
	   updated_by_id     INT REFERENCES kaizen.user_detail(id),
     updated_on        TIMESTAMPTZ DEFAULT now(),
	   
	   description VARCHAR(2048)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_group (
	   id serial PRIMARY KEY,
	   permission_group_name VARCHAR(128) NOT NULL UNIQUE CHECK (permission_group_name <> '')
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_group_member (
	   id serial PRIMARY KEY,
	   permission_rule_id   INT NOT NULL REFERENCES kaizen.permission_rule (id),
	   permission_group_id  INT NOT NULL REFERENCES kaizen.permission_group (id)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_assignment (
	   id serial PRIMARY KEY,
	   permission_rule_id   INT NOT NULL REFERENCES kaizen.permission_rule (id),
	   assignment_type      CHAR(3) NOT NULL CHECK (assignment_type = 'USR' OR assignment_type = 'GRP'),
	   user_id              INT REFERENCES kaizen.user_detail(id),
	   user_group_id        INT REFERENCES kaizen.user_group(id)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_group_assignment (
	   id serial PRIMARY KEY,
	   permission_group_id  INT NOT NULL REFERENCES kaizen.permission_rule (id),
	   assignment_type      CHAR(3) NOT NULL CHECK (assignment_type = 'USR' OR assignment_type = 'GRP'),
	   user_id              INT REFERENCES kaizen.user_detail(id),
	   user_group_id        INT REFERENCES kaizen.user_group(id)
);
