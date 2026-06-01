-- SecurityConfigTest data
-- Uses MERGE INTO for idempotency

-- User #1 for test b (GET /users/1 → public profile)
MERGE INTO "user" ("id", "username", "password", "email", "role", "status", "create_time", "update_time") KEY("id") VALUES
(1, 'user1', 'unused', 'user1@test.com', 'USER', 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00');
