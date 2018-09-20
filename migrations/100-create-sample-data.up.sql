-- SAMPLE USER DATA
INSERT INTO kaizen.user_detail (id, user_name, user_type_id, active, email)
    VALUES (2, 'tech1', 2, true, 'tech1@test.org');
INSERT INTO kaizen.user_detail (id, user_name, user_type_id, active, email)
    VALUES (3, 'tech2', 2, true, 'tech2@test.org');
INSERT INTO kaizen.user_detail (id, user_name, user_type_id, active, email)
    VALUES (4, 'tech3', 2, true, 'tech3@test.org');
--;;
-- default tech1 password is tech1
INSERT INTO kaizen.user_auth (user_id, password) 
    VALUES (2, '$argon2id$v=19$m=65536,t=8,p=1$M/LQBZfwo4M8xD68qbnGIw$g7nOn3n01Kn1hkgEqDVpHwlwaMA0bLipAwRf7/ZeIGc');
-- default tech2 password is tech2
INSERT INTO kaizen.user_auth (user_id, password) 
    VALUES (3, '$argon2id$v=19$m=65536,t=8,p=1$AAksxp1yiwQl6eHhAyCgMw$fZ4SSkV1tZ6PqJSyEO5cg5z8274byJQNUZvZKLIuJ/0');
-- default tech3 password is tech3
INSERT INTO kaizen.user_auth (user_id, password) 
    VALUES (4, '$argon2id$v=19$m=65536,t=8,p=1$hLTVZhymJn69QxZH5O80/Q$EQ9d6PtkYvZlLAGzAqMr4xhTAqRRV5PsSpAr0YwZ1lM');
--;;
INSERT INTO kaizen.user_group (id, user_group_name) VALUES (1, 'Tech Group');
INSERT INTO kaizen.user_group (id, user_group_name) VALUES (2, 'Manager Group');
--;;
INSERT INTO kaizen.user_group_membership (user_id, user_group_id)
	   VALUES(1,2);
INSERT INTO kaizen.user_group_membership (user_id, user_group_id)
	   VALUES(2,2);
INSERT INTO kaizen.user_group_membership (user_id, user_group_id)
	   VALUES(3,1);
INSERT INTO kaizen.user_group_membership (user_id, user_group_id)
	   VALUES(4,1);
--;;
-- SAMPLE TICKETS
INSERT INTO kaizen.ticket (subject, detail, status_id, priority_id, created_by_id)
	   VALUES('Test', 'Test Ticket Detail', 1, 1, 1);
INSERT INTO kaizen.ticket (subject, detail, status_id, priority_id, created_by_id)
	   VALUES('Test 2', 'Test Ticket Detail Two', 2, 2, 1);
INSERT INTO kaizen.ticket (subject, detail, status_id, priority_id, created_by_id)
	   VALUES('Test 3', 'Test Ticket Detail Three', 3, 3, 1);	   
--;;
-- Permissions
INSERT INTO kaizen.permission_rule (id, permission_name, entity, qualification, rule_order, created_by_id, description)
VALUES (1, 'test1', 'ticket', '(api-op = "read")', 999, 1, 'Test Rule allowing all ticket Read');
INSERT INTO kaizen.permission_assignment (permission_rule_id, assignment_type, user_id)
		 VALUES (1, 'USR', 2);
