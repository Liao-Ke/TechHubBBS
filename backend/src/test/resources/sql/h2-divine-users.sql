-- Pre-seeded users for divine comment tests (registered ≥ 30 days ago)
-- BCrypt(password123) = $2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me
-- Uses MERGE INTO for idempotency — safe to run multiple times across test classes

MERGE INTO "user" ("id", "username", "password", "email", "role", "status", "create_time", "update_time") KEY("id") VALUES
(1001, 'divine_author', '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_author@test.com', 'USER', 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00'),
(1002, 'divine_user1',  '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_user1@test.com', 'USER', 1, '2026-01-02 08:00:00', '2026-01-02 08:00:00'),
(1003, 'divine_user2',  '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_user2@test.com', 'USER', 1, '2026-01-03 08:00:00', '2026-01-03 08:00:00'),
(1004, 'divine_user3',  '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_user3@test.com', 'USER', 1, '2026-01-04 08:00:00', '2026-01-04 08:00:00'),
(1005, 'divine_user4',  '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_user4@test.com', 'USER', 1, '2026-01-05 08:00:00', '2026-01-05 08:00:00'),
(1006, 'divine_user5',  '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_user5@test.com', 'USER', 1, '2026-01-06 08:00:00', '2026-01-06 08:00:00'),
(1007, 'divine_admin',  '$2a$10$.ZpTlREkQmbYwuotCwIoSOibG2.CPcUAfaW6u03C3.D7f6V0jb4me', 'divine_admin@test.com', 'ADMIN', 1, '2026-01-01 08:00:00', '2026-01-01 08:00:00');
