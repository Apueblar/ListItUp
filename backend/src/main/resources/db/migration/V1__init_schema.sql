-- =============================================================
-- ListItUp — Consolidated Final Schema
-- =============================================================

-- -- Users ----------------------------------------------------
CREATE TABLE users (
    user_id                 UUID PRIMARY KEY,
    username                VARCHAR(255)    NOT NULL,
    email                   VARCHAR(255)    NOT NULL UNIQUE,
    auth_provider           VARCHAR(50)     NOT NULL,
    role                    VARCHAR(50)     NOT NULL DEFAULT 'STANDARD',
    has_badge               BOOLEAN         DEFAULT FALSE,
    is_blocked              BOOLEAN         DEFAULT FALSE,
    can_pin_lists           BOOLEAN         DEFAULT FALSE,
    has_analytics_access    BOOLEAN         DEFAULT FALSE,
    can_moderate_content    BOOLEAN         DEFAULT FALSE,
    can_delete_any          BOOLEAN         DEFAULT FALSE,
    has_completed_setup     BOOLEAN         DEFAULT FALSE,
    profile_picture         VARCHAR(2048),
    bio                     TEXT,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- -- Categories -----------------------------------------------
CREATE TABLE categories (
    category_id UUID        PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    icon        VARCHAR(255)
);

-- -- Lists ----------------------------------------------------
CREATE TABLE lists (
    list_id     UUID         PRIMARY KEY,
    creator_id  UUID         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID         NOT NULL REFERENCES categories(category_id) ON DELETE RESTRICT,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    cover_photo VARCHAR(2048),
    visibility  VARCHAR(50)  NOT NULL DEFAULT 'PUBLIC',
    view_count  INTEGER      NOT NULL DEFAULT 0,
    is_pinned   BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- -- Items ----------------------------------------------------
CREATE TABLE items (
    item_id         UUID         PRIMARY KEY,
    list_id         UUID         NOT NULL REFERENCES lists(list_id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    external_url    VARCHAR(2048),
    photo           VARCHAR(2048),
    position_index  INTEGER      NOT NULL,
    click_count     INTEGER      NOT NULL DEFAULT 0
);

-- -- Comments -------------------------------------------------
CREATE TABLE comments (
    comment_id  UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    list_id     UUID NOT NULL REFERENCES lists(list_id)  ON DELETE CASCADE,
    text        TEXT NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- -- Likes ----------------------------------------------------
CREATE TABLE likes (
    like_id     UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    list_id     UUID NOT NULL REFERENCES lists(list_id)  ON DELETE CASCADE,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_list_like UNIQUE (user_id, list_id)
);

-- -- Follows --------------------------------------------------
CREATE TABLE follows (
    follow_id   UUID PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    followee_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_follower_followee UNIQUE (follower_id, followee_id),
    CONSTRAINT chk_no_self_follow   CHECK (follower_id != followee_id)
);

-- -- Saved Lists ----------------------------------------------
CREATE TABLE saved_lists (
    saved_id    UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    list_id     UUID NOT NULL REFERENCES lists(list_id)  ON DELETE CASCADE,
    saved_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_list_save UNIQUE (user_id, list_id)
);

-- -- Reports --------------------------------------------------
-- Matches Report.java: list_id, reporter_id, reason, details, status
CREATE TABLE reports (
    report_id   UUID         PRIMARY KEY,
    list_id     UUID         NOT NULL REFERENCES lists(list_id)  ON DELETE CASCADE,
    reporter_id UUID         NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
    reason      VARCHAR(255) NOT NULL,
    details     VARCHAR(1000),
    status      VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- -- Notifications --------------------------------------------
-- Matches Notification.java: user_id, message, link_url, is_read
CREATE TABLE notifications (
    notification_id UUID    PRIMARY KEY,
    user_id         UUID    NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message         TEXT    NOT NULL,
    link_url        VARCHAR(2048),
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- -- Analytics Snapshots --------------------------------------
-- Matches AnalyticsSnapshot.java: platform-wide daily snapshot
CREATE TABLE analytics_snapshots (
    snapshot_id     UUID    PRIMARY KEY,
    snapshot_date   DATE    NOT NULL,
    total_users     BIGINT  NOT NULL DEFAULT 0,
    total_lists     BIGINT  NOT NULL DEFAULT 0,
    total_views     BIGINT  NOT NULL DEFAULT 0,
    total_likes     BIGINT  NOT NULL DEFAULT 0,
    total_saves     BIGINT  NOT NULL DEFAULT 0
);

-- -- Seed: Categories -----------------------------------------
INSERT INTO categories (category_id, name, icon) VALUES
(gen_random_uuid(), 'Movies',  '🎬'),
(gen_random_uuid(), 'Books',   '📚'),
(gen_random_uuid(), 'Tech',    '💻'),
(gen_random_uuid(), 'Travel',  '✈️'),
(gen_random_uuid(), 'Food',    '🍔'),
(gen_random_uuid(), 'Music',   '🎵'),
(gen_random_uuid(), 'Games',   '🎮'),
(gen_random_uuid(), 'Sports',  '⚽');
