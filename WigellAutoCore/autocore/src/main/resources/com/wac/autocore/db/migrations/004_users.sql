-- The users. Passwords are stored as a hash, never in clear text.
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- Demo accounts, made up for the demo and used nowhere else. The column keeps a PBKDF2 hash, never the password:
-- the login reads the salt out of the column, hashes what was typed and compares the two.
-- A hash in the repository is therefore not a password, but a weak one can still be guessed from it.

INSERT INTO users (username, password_hash, role, created_at) VALUES
    ('admin', 'pbkdf2$210000$rqrZS3wEeLjsLfnxODMimQ==$OQ40gpCv4Pven7WMDcT3I4OBenMoi45FOY5aIpAayfE=', 'ADMIN', '2026-10-20'),
    ('staff', 'pbkdf2$210000$b9V1wluKAOkaQGuOGwm5pA==$obzBNAB3n5tsSThxIzLUSlJjXUhF+3NDcHcrcLBX68Y=', 'STAFF', '2026-10-20'),
    ('readonly', 'pbkdf2$210000$RIdeI6+pnejufIaOP3GeNA==$w5zZYIwFPMbimVKCjRIHVIFetSglNdRpkkzk49jikhY=', 'READ_ONLY', '2026-10-20');
