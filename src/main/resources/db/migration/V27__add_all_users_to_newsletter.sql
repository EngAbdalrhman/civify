CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO newsletter_subscribers (id, email, active, subscribed_at)
SELECT gen_random_uuid(), u.email, true, CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM newsletter_subscribers ns WHERE ns.email = u.email
);
