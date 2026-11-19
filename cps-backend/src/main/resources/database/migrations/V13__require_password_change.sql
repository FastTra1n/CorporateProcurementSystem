ALTER TABLE user_credentials
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_credentials
    ALTER COLUMN must_change_password SET DEFAULT TRUE;
