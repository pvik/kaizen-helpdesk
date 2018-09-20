CREATE TABLE IF NOT EXISTS kaizen.action_rule (
	   id 		 					 serial PRIMARY KEY,
	   action_rule_name  VARCHAR(128) NOT NULL UNIQUE CHECK (action_rule_name <> ''),
	   entity            VARCHAR(32) NOT NULL,
		 api_operation     VARCHAR(6) NOT NULL
		 									 CHECK (api_operation IN ('create', 'read', 'update', 'delete')),
	   qualification     VARCHAR(1024) NOT NULL,
	   rule_order 			 INT2 NOT NULL CHECK (rule_order >= 0 AND rule_order < 1000),
		 after_api_op			 BOOL NOT NULL,
	   enabled           BOOL DEFAULT true,
		 action_detail     JSON NOT NULL,
	   -- user details
	   created_by_id     INT NOT NULL REFERENCES kaizen.user_detail(id),
     created_on        TIMESTAMPTZ DEFAULT now(),
	   updated_by_id     INT REFERENCES kaizen.user_detail(id),
     updated_on        TIMESTAMPTZ DEFAULT now(),
	   
	   description VARCHAR(2048)
);
--;;
