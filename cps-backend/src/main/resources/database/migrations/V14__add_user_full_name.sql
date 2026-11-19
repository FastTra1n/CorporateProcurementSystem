ALTER TABLE users ADD COLUMN full_name VARCHAR(255);

UPDATE users SET full_name = username;

ALTER TABLE users ALTER COLUMN full_name SET NOT NULL;
