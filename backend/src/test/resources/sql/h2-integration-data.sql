-- Integration test base data: categories only (users created via API for end-to-end auth testing)
-- BCrypt password for 'password123': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- Clean all data tables (reverse FK order) before each test method for method-level isolation
-- "user" and "category" are kept as they are base reference data

DELETE FROM "post_keyword";
DELETE FROM "post_similarity";
DELETE FROM "ai_qa_history";
DELETE FROM "ai_summary";
DELETE FROM "notification";
DELETE FROM "comment_recommend";
DELETE FROM "user_like";
DELETE FROM "favorite";
DELETE FROM "follow";
DELETE FROM "post_draft";
DELETE FROM "comment";
DELETE FROM "user_profile";
DELETE FROM "post";

MERGE INTO "category" ("id", "name", "description", "sort_order", "status", "create_time") KEY("id") VALUES
(1000, '技术讨论', '讨论前沿技术', 1, 1, '2026-01-01 08:00:00'),
(1001, '问答求助', '技术问答', 2, 1, '2026-01-01 08:00:00'),
(1002, '项目展示', '项目展示', 3, 1, '2026-01-01 08:00:00');
