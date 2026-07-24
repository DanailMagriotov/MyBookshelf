ALTER TABLE message
    ADD COLUMN hidden_from_sender TINYINT(1) NOT NULL DEFAULT 0;
