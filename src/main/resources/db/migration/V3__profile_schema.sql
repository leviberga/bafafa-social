CREATE SCHEMA IF NOT EXISTS profile;

CREATE TABLE profile.profile (
                                 account_id   UUID         PRIMARY KEY,
                                 display_name VARCHAR(50)  NOT NULL,
                                 bio          VARCHAR(160) NOT NULL DEFAULT '',
                                 created_at   TIMESTAMPTZ  NOT NULL,
                                 updated_at   TIMESTAMPTZ  NOT NULL
);