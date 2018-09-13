CREATE TABLE IF NOT EXISTS kaizen.permission_rule (
	   permission_rule_id serial PRIMARY KEY,
	   permission_name   VARCHAR(128) NOT NULL UNIQUE CHECK (permission_name <> ''),
	   entity            VARCHAR(32) NOT NULL,
	   qualification     VARCHAR(1024) NOT NULL,
	   rule_order INT2   NOT NULL CHECK (rule_order >= 0 AND rule_order < 1000),
	   default_rule      BOOL DEFAULT false,
	   enabled           BOOL DEFAULT true,
	   -- user details
	   created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(user_id),
     created_on        TIMESTAMPTZ DEFAULT now(),
	   updated_by_id     INT REFERENCES kaizen.user_detail(user_id),
     updated_on        TIMESTAMPTZ DEFAULT now(),
	   
	   description VARCHAR(2048)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_group (
	   permission_group_id serial PRIMARY KEY,
	   permission_group_name VARCHAR(128) NOT NULL UNIQUE CHECK (permission_group_name <> '')
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_group_member (
	   permission_group_member_id serial PRIMARY KEY,
	   permission_rule_id   INT NOT NULL REFERENCES kaizen.permission_rule (permission_rule_id),
	   permission_group_id  INT NOT NULL REFERENCES kaizen.permission_group (permission_group_id)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_assignment (
	   permission_assignment_id serial PRIMARY KEY,
	   permission_rule_id   INT NOT NULL REFERENCES kaizen.permission_rule (permission_rule_id),
	   assignment_type      CHAR(3) NOT NULL CHECK (assignment_type = 'USR' OR assignment_type = 'GRP'),
	   user_id              INT REFERENCES kaizen.user_detail(user_id),
	   user_group_id        INT REFERENCES kaizen.user_group(user_group_id)
);
--;;
CREATE TABLE IF NOT EXISTS kaizen.permission_group_assignment (
	   permission_group_assignment_id serial PRIMARY KEY,
	   permission_group_id  INT NOT NULL REFERENCES kaizen.permission_rule (permission_rule_id),
	   assignment_type      CHAR(3) NOT NULL CHECK (assignment_type = 'USR' OR assignment_type = 'GRP'),
	   user_id              INT REFERENCES kaizen.user_detail(user_id),
	   user_group_id        INT REFERENCES kaizen.user_group(user_group_id)
);
