-- ============================================================================
-- SecondHand – Sample Database Script (SQLite)
-- ============================================================================
-- Creates all tables and inserts sample data that exactly matches the
-- development database used during the project.
--
-- ⚠️  BEFORE YOU RUN THIS SCRIPT
-- ───────────────────────────────
-- 1. Make sure you have the sample photos in place:
--      docs/setup-sample-data.sh   (Linux/macOS/Git Bash)
--      docs/setup-sample-data.bat  (Windows CMD)
--    Running either script copies the photos from docs/sample-photos/
--    to uploads/advertisements/ so they are found by the app at runtime.
--
-- 2. SQLite does NOT enforce foreign keys by default. Enable them if
--    you want referential integrity checks during the INSERTs:
--        PRAGMA foreign_keys = ON;
--
-- Usage:
--   sqlite3 secondhand.db < docs/sample-data.sql
--
-- Or from the sqlite3 prompt:
--   .read docs/sample-data.sql
-- ============================================================================

-- ============================================================================
-- TABLES
-- ============================================================================
-- Hibernate's ddl-auto=update creates these automatically on first run.
-- The CREATE TABLE statements are included here so the script is
-- self-contained and can create a fresh database from scratch.

CREATE TABLE IF NOT EXISTS users (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at   TEXT,
    updated_at   TEXT,
    email        VARCHAR(100),
    full_name    VARCHAR(255) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20)  NOT NULL UNIQUE,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER'
                              CHECK (role IN ('USER','ADMIN')),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                              CHECK (status IN ('ACTIVE','BLOCKED')),
    username     VARCHAR(50)  NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT,
    updated_at TEXT,
    name       VARCHAR(100) NOT NULL UNIQUE,
    parent_id  BIGINT REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS cities (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT,
    updated_at TEXT,
    name       VARCHAR(100) NOT NULL,
    province   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS advertisements (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at  TEXT,
    updated_at  TEXT,
    admin_note  TEXT,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'
                CHECK (status IN ('PENDING_REVIEW','ACTIVE','REJECTED','DELETED','SOLD')),
    title       VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    city_id     BIGINT NOT NULL REFERENCES cities(id),
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    buyer_id    BIGINT REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS advertisement_images (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at        TEXT,
    updated_at        TEXT,
    display_order     INTEGER DEFAULT 0,
    image_path        VARCHAR(255) NOT NULL,
    advertisement_id  BIGINT NOT NULL REFERENCES advertisements(id)
);

CREATE TABLE IF NOT EXISTS conversations (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at        TEXT,
    updated_at        TEXT,
    advertisement_id  BIGINT NOT NULL REFERENCES advertisements(id),
    buyer_id          BIGINT NOT NULL REFERENCES users(id),
    seller_id         BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at      TEXT,
    updated_at      TEXT,
    content         TEXT NOT NULL,
    is_read         INTEGER NOT NULL DEFAULT 0,
    sent_at         TEXT NOT NULL,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id),
    sender_id       BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS favorites (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at        TEXT,
    updated_at        TEXT,
    advertisement_id  BIGINT NOT NULL REFERENCES advertisements(id),
    user_id           BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ratings (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at        TEXT,
    updated_at        TEXT,
    comment           TEXT,
    score             INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    advertisement_id  BIGINT NOT NULL REFERENCES advertisements(id),
    buyer_id          BIGINT NOT NULL REFERENCES users(id),
    seller_id         BIGINT NOT NULL REFERENCES users(id)
);

-- JOINED inheritance child tables
CREATE TABLE IF NOT EXISTS electronics_advertisements (
    brand           VARCHAR(50),
    warranty_months INTEGER,
    id              BIGINT NOT NULL PRIMARY KEY REFERENCES advertisements(id)
);

CREATE TABLE IF NOT EXISTS real_estate_advertisements (
    area_sqm REAL,
    rooms    INTEGER,
    id       BIGINT NOT NULL PRIMARY KEY REFERENCES advertisements(id)
);

CREATE TABLE IF NOT EXISTS vehicle_advertisements (
    brand            VARCHAR(50),
    manufacture_year INTEGER,
    mileage_km       INTEGER,
    id               BIGINT NOT NULL PRIMARY KEY REFERENCES advertisements(id)
);

-- ============================================================================
-- SAMPLE DATA
-- ============================================================================

-- ── Users ───────────────────────────────────────────────────────────────────
-- Passwords are real BCrypt hashes generated by the app at registration.
INSERT INTO users (id, created_at, updated_at, email, full_name, password,
                   phone_number, role, status, username)
VALUES
    (1, '2026-07-12T10:33:19', '2026-07-23T10:32:44',
     'arad.a1385@gmail.com', 'arad allahdini',
     '$2a$10$AmKGa3RpOFSRp1HDvGBQ5ukymd3GLyspTLlZvQf/qPhdKW1.5MzOC',
     '09131798522', 'USER', 'ACTIVE', 'arad'),

    (2, '2026-07-18T10:33:18', '2026-07-18T10:33:18',
     'admin@gmail.com', 'admin',
     '$2a$10$TILRhaEiw3QZ2s9PKN/MYeHiw8JME2.vKBVN6jzK7McwhTWdc1Nyu',
     '1234', 'ADMIN', 'ACTIVE', 'admin'),

    (3, '2026-07-21T12:15:45', '2026-07-21T12:17:30',
     'amirmohammad@gmail.com', 'amirmohammad sherafat',
     '$2a$10$PaZFH1k1.Yn1VVaYCSRT0OcdWMa.q12E9CjU7jBGxAXJucH4jeuKC',
     '09121111111', 'USER', 'ACTIVE', 'amirmohammad');

-- ── Categories (hierarchical) ──────────────────────────────────────────────
INSERT INTO categories (id, created_at, updated_at, name, parent_id)
VALUES
    (1,  '2026-07-12T10:33:19', '2026-07-12T10:33:19', 'Electronics',          NULL),
    (2,  '2026-07-21T18:24:00', '2026-07-21T18:24:00', 'Cellphone',            1),
    (3,  '2026-07-21T18:26:58', '2026-07-21T18:26:58', 'real estate',          NULL),
    (4,  '2026-07-22T11:33:44', '2026-07-22T11:33:44', 'Laptop',               1),
    (5,  '2026-07-22T11:37:01', '2026-07-22T11:37:01', 'Iphone',               2),
    (6,  '2026-07-22T11:37:09', '2026-07-22T11:37:09', 'Personal Computer',    1),
    (7,  '2026-07-23T03:17:24', '2026-07-23T03:17:24', 'Registered Iphone',    5),
    (8,  '2026-07-23T03:17:35', '2026-07-23T03:17:35', 'non-registered Iphone',5),
    (10, '2026-07-23T09:02:16', '2026-07-23T09:02:16', 'Game Console',         1),
    (11, '2026-07-23T09:04:59', '2026-07-23T09:04:59', 'Residental Sales',     3),
    (12, '2026-07-23T09:05:15', '2026-07-23T09:05:15', 'Residental Rent',      3),
    (13, '2026-07-23T09:05:32', '2026-07-23T09:05:32', 'Commercial & Office',  3),
    (14, '2026-07-23T09:05:48', '2026-07-23T09:05:48', 'viechles',             NULL),
    (15, '2026-07-23T09:06:00', '2026-07-23T09:06:00', 'Cars',                 14),
    (16, '2026-07-23T09:06:18', '2026-07-23T09:06:18', 'Motorcycles',          14),
    (17, '2026-07-23T09:06:47', '2026-07-23T09:06:47', 'Personal Items',       NULL),
    (18, '2026-07-23T09:07:09', '2026-07-23T09:07:09', 'Watches & Jewelry',    17),
    (19, '2026-07-23T09:07:22', '2026-07-23T09:07:22', 'Clothing',             17);

-- ── Cities ─────────────────────────────────────────────────────────────────
INSERT INTO cities (id, created_at, updated_at, name, province)
VALUES
    (1, '2026-07-12T10:33:19', '2026-07-12T10:33:19', 'Tehran',  'Tehran'),
    (2, '2026-07-21T18:24:14', '2026-07-21T18:24:14', 'sirjan',  'kerman'),
    (3, '2026-07-23T10:20:07', '2026-07-23T10:20:07', 'Shiraz',  'Fars'),
    (4, '2026-07-23T10:20:18', '2026-07-23T10:20:18', 'Isfahan', 'Isfahan');

-- ── Advertisements ─────────────────────────────────────────────────────────
INSERT INTO advertisements (id, created_at, updated_at, admin_note, description,
                            price, status, title, category_id, city_id, owner_id, buyer_id)
VALUES
    (1,  '2026-07-17T18:18:25', '2026-07-19T05:02:40', NULL,
     'used for 1 year', 1000000,   'SOLD',   'used laptop',      1,  1,  1,  3),
    (2,  '2026-07-21T18:25:13', '2026-07-19T05:02:55', NULL,
     'old nokia from 2- years ago', 100000, 'SOLD',  'old nokia',  2,  1,  1,  3),
    (3,  '2026-07-21T18:30:38', '2026-07-21T18:31:23', 'not related to the category',
     'meow', 12,       'REJECTED', 'apartment for rent', 1,  2,  3,  NULL),
    (4,  '2026-07-21T18:37:39', '2026-07-21T18:43:15', NULL,
     'totally new', 10000,        'DELETED', 'logitech mouse',    1,  2,  2,  NULL),
    (5,  '2026-07-21T18:45:45', '2026-07-23T00:38:37', NULL,
     'asf', 1234,                'SOLD',    'adasfd',            2,  1,  3,  2),
    (6,  '2026-07-21T21:46:48', '2026-07-22T18:43:39', NULL,
     'very expensive mansion locataed in fereshteh', 9000000, 'SOLD', 'mansion', 3, 1, 2, 1),
    (7,  '2026-07-21T21:52:22', '2026-07-21T21:52:34', 'what the hell is this',
     'asdf', 123,                'REJECTED','asdf',              1,  1,  2,  NULL),
    (8,  '2026-07-21T21:45:00', '2026-07-21T22:10:56', NULL,
     'asdf', 1234,               'DELETED', 'asdf',              1,  2,  3,  NULL),
    (9,  '2026-07-23T00:33:55', '2026-07-23T00:41:19', NULL,
     '100 meters, 2 rooms', 999999999, 'SOLD', 'apartment for sale', 3, 2, 3, 1),
    (10, '2026-07-23T00:33:59', '2026-07-23T00:40:34', 'repetitive',
     '100 meters, 2 rooms', 999999999, 'REJECTED', 'apartment for sale', 3, 2, 3, NULL),
    (11, '2026-07-23T00:40:59', '2026-07-23T02:02:11', NULL,
     'totally new, only used for 1 week', 1321487, 'SOLD', 'samsung s26 ultra', 2, 1, 1, 3),
    (12, '2026-07-23T00:45:46', '2026-07-23T05:05:54', NULL,
     'used for 6 months', 200, 'SOLD', 'PS5', 1, 2, 3, 1),
    (13, '2026-07-23T00:46:28', '2026-07-23T21:54:46', NULL,
     'beach house located in sirjan', 20000000, 'DELETED', 'beach house', 3, 2, 2, NULL),
    (14, '2026-07-23T00:47:44', '2026-07-23T14:56:40', NULL,
     'Selling a classic PlayStation 2 console in great overall shape. The system has been properly cared for, thoroughly tested, and functions exactly as it should.

Functional Condition:

Disc Drive: The laser is strong and reads both CD-ROM (blue bottom) and DVD-ROM (silver) formats quickly. The disc tray opens and closes smoothly without jamming, and there are no grinding or clicking noises during gameplay.

Ports & Inputs: Both controller ports and memory card slots are fully operational and clean. The AV and power inputs at the rear are snug with no loose connections.

Thermals: The internal fan is quiet and unobstructed. The console runs at normal temperatures and does not overheat during extended play sessions.

Cosmetic Condition:

The outer casing is intact with no cracks, dents, or broken plastic.

There is no heavy discoloration or grime in the vents.

It shows only minor, superficial surface wear (light scuffs) consistent with normal use and its age.',
     40, 'ACTIVE', 'PS2', 10, 1, 1, NULL),
    (15, '2026-07-23T12:46:32', '2026-07-23T12:47:25', NULL,
     'model 84', 2000, 'ACTIVE', '405 shooti', 15, 2, 2, NULL),
    (16, '2026-07-23T18:26:31', '2026-07-23T23:29:11', 'spam',
     'asdf', 123, 'REJECTED', 'asdfasf', 1, 1, 3, NULL),
    (17, '2026-07-23T18:31:59', '2026-07-23T18:31:59', NULL,
     '', 123, 'PENDING_REVIEW', 'asdf', 1, 1, 3, NULL),
    (18, '2026-07-23T22:48:23', '2026-07-23T22:48:36', 'no',
     'wre', 123, 'REJECTED', 'merow', 2, 1, 2, NULL),
    (19, '2026-07-23T14:08:02', '2026-07-23T14:15:50', NULL,
     'It works perfectly, looks great, and has been very well taken care of.

Condition Details:

Screen & Body: Clean and free of any cracks or heavy scratches. It’s been kept in a protective case, so the body is in excellent shape with only very light signs of normal use.

Cameras: The lenses are completely scratch-free and take beautiful, sharp photos and videos.

Battery: Holds a great charge and easily gets through a full day of everyday use. (Battery health is at 90%).',
     900, 'ACTIVE', 'Iphone 17', 7, 1, 3, NULL),
    (20, '2026-07-23T14:45:06', '2026-07-23T14:57:49', NULL,
     'my own laptop, used it for 3 years. totally healthy and in a good shape and great battery health', 600, 'ACTIVE', 'Asus gaming laptop', 4, 3, 3, NULL),
    (21, '2026-07-23T15:38:09', '2026-07-23T15:41:56', NULL,
     'mansion located in velenjak, tehran
built 5 years ago', 5000000, 'ACTIVE', 'mansion for sale', 11, 1, 1, NULL),
    (22, '2026-07-23T16:59:11', '2026-07-23T18:21:02', NULL,
     'Omega Constellation Megaquartz f2.4 Cal 1510', 4800, 'ACTIVE', 'omega vintage watch', 18, 4, 1, NULL);

-- ── Advertisement Images ───────────────────────────────────────────────────
INSERT INTO advertisement_images (id, created_at, updated_at, display_order,
                                   image_path, advertisement_id)
VALUES
    (19, '2026-07-23T19:56:17', '2026-07-23T19:56:17', 0, '6ee8902b-aa29-48a0-9114-d773400e1daf.png', 22),
    (20, '2026-07-23T19:56:17', '2026-07-23T19:56:17', 1, '23430055-bd92-46be-9f5e-d200cf7766a5.png', 22),
    (21, '2026-07-23T20:00:55', '2026-07-23T20:00:55', 0, '62a15ac8-ac07-46c5-8ce3-655e49ed754f.jpg', 21),
    (22, '2026-07-23T20:00:55', '2026-07-23T20:00:55', 1, '3d155543-23b4-459f-b618-68733a1766b2.jpg', 21),
    (23, '2026-07-23T20:00:55', '2026-07-23T20:00:55', 2, 'c9ad0249-4e9e-473f-8fee-01bd5f0ee65f.jpg', 21),
    (24, '2026-07-23T20:08:47', '2026-07-23T20:08:47', 0, '1f62f533-0ab0-401d-b93d-198b98ad6fa9.jpg', 14),
    (25, '2026-07-23T20:08:47', '2026-07-23T20:08:47', 1, 'b54fc266-8dae-4cda-8b45-5d841549b208.jpg', 14),
    (26, '2026-07-23T20:08:47', '2026-07-23T20:08:47', 2, '99448089-d1d0-4a9e-9cb0-46c091ce8a01.jpg', 14),
    (27, '2026-07-23T20:28:14', '2026-07-23T20:28:14', 0, '0a7bbe1e-5ef5-43ec-9a3e-a68a6593fc36.jpg', 20),
    (28, '2026-07-23T20:48:54', '2026-07-23T20:48:54', 0, '41217ee3-43b4-47ee-8e72-466875e3e530.jpg', 19),
    (29, '2026-07-23T20:48:54', '2026-07-23T20:48:54', 1, 'eeeaade4-e0ef-4133-87ca-aa8fcde83bc9.jpg', 19),
    (30, '2026-07-23T20:48:54', '2026-07-23T20:48:54', 2, '4674253f-1666-43f2-a204-e88528b9bf6d.jpg', 19),
    (31, '2026-07-23T20:49:12', '2026-07-23T20:49:12', 0, '73834333-14a3-4253-bd40-51ec14b6b0c7.jpg', 15);

-- ── Favorites ──────────────────────────────────────────────────────────────
INSERT INTO favorites (id, created_at, updated_at, advertisement_id, user_id)
VALUES
    (1, '2026-07-21T18:29:27', '2026-07-21T18:29:27', 2,  3),
    (2, '2026-07-21T18:33:28', '2026-07-21T18:33:28', 3,  3),
    (3, '2026-07-21T18:43:05', '2026-07-21T18:43:05', 4,  1),
    (4, '2026-07-22T18:37:55', '2026-07-22T18:37:55', 5,  1),
    (5, '2026-07-23T02:01:57', '2026-07-23T02:01:57', 11, 2),
    (6, '2026-07-23T22:08:25', '2026-07-23T22:08:25', 13, 1),
    (7, '2026-07-23T15:45:04', '2026-07-23T15:45:04', 21, 1),
    (8, '2026-07-23T17:24:43', '2026-07-23T17:24:43', 20, 1),
    (9, '2026-07-23T17:25:09', '2026-07-23T17:25:09', 19, 1);

-- ── Ratings ────────────────────────────────────────────────────────────────
INSERT INTO ratings (id, created_at, updated_at, comment, score,
                     advertisement_id, buyer_id, seller_id)
VALUES
    (1, '2026-07-22T19:11:07', '2026-07-22T19:11:07',
     'the hosue has a great and modern architecture', 5, 6,  1, 2),
    (2, '2026-07-23T00:38:50', '2026-07-23T00:38:50',
     'great!', 5, 5,  2, 3),
    (3, '2026-07-23T00:42:02', '2026-07-23T00:42:02',
     'it was scam! the house was too small', 1, 9,  1, 3),
    (4, '2026-07-23T02:02:51', '2026-07-23T02:02:51',
     'very good overall, the phone has few scratches', 4, 11, 3, 1),
    (5, '2026-07-23T02:04:08', '2026-07-23T02:04:08',
     'good!', 4, 1,  3, 1),
    (6, '2026-07-23T02:04:47', '2026-07-23T02:04:47',
     'did not expect it to work!', 5, 2,  3, 1),
    (7, '2026-07-23T05:06:45', '2026-07-23T05:06:45',
     'great experience! he sent the console on time', 5, 12, 1, 3);

-- ── Conversations ──────────────────────────────────────────────────────────
INSERT INTO conversations (id, created_at, updated_at, advertisement_id, buyer_id, seller_id)
VALUES
    (1, '2026-07-23T04:42:44', '2026-07-23T04:42:44', 12, 1, 3),
    (2, '2026-07-23T12:56:45', '2026-07-23T12:56:45', 15, 3, 2),
    (3, '2026-07-23T18:52:52', '2026-07-23T18:52:52', 14, 2, 1),
    (4, '2026-07-23T20:41:04', '2026-07-23T20:41:04', 15, 1, 2);

-- ── Chat Messages ──────────────────────────────────────────────────────────
INSERT INTO chat_messages (id, created_at, updated_at, content, is_read, sent_at,
                           conversation_id, sender_id)
VALUES
    (1,  '2026-07-23T04:42:44', '2026-07-23T04:42:44', 'salam aya daste ha salem hastan?',         1, '2026-07-23T04:42:44', 1, 1),
    (2,  '2026-07-23T04:43:43', '2026-07-23T04:43:43', 'bale kamelan noo hastan',                 1, '2026-07-23T04:43:43', 1, 3),
    (3,  '2026-07-23T04:44:08', '2026-07-23T04:44:08', 'pas ye takhfif riz be ma bedid moshtari shim', 1, '2026-07-23T04:44:08', 1, 1),
    (4,  '2026-07-23T04:44:16', '2026-07-23T04:44:16', 'NO',                                      1, '2026-07-23T04:44:16', 1, 3),
    (5,  '2026-07-23T04:44:21', '2026-07-23T04:44:21', ':(',                                      1, '2026-07-23T04:44:21', 1, 1),
    (6,  '2026-07-23T04:44:40', '2026-07-23T04:44:40', 'bedoone takhfif mikharam',               1, '2026-07-23T04:44:40', 1, 1),
    (7,  '2026-07-23T04:44:46', '2026-07-23T04:44:46', 'OK',                                      1, '2026-07-23T04:44:46', 1, 3),
    (8,  '2026-07-23T04:44:54', '2026-07-23T04:44:54', 'merci',                                   1, '2026-07-23T04:44:54', 1, 1),
    (9,  '2026-07-23T12:56:45', '2026-07-23T12:56:45', 'salam. mashin bedoone range?',            1, '2026-07-23T12:56:45', 2, 3),
    (10, '2026-07-23T18:52:52', '2026-07-23T18:52:52', 'asdff',                                   0, '2026-07-23T18:52:52', 3, 2),
    (11, '2026-07-23T18:52:54', '2026-07-23T18:52:54', 'asdfsaf',                                 0, '2026-07-23T18:52:54', 3, 2),
    (12, '2026-07-23T18:53:02', '2026-07-23T18:53:02', 'asdf',                                    0, '2026-07-23T18:53:02', 3, 2),
    (13, '2026-07-23T20:41:04', '2026-07-23T20:41:04', 'hello',                                   1, '2026-07-23T20:41:04', 4, 1);
