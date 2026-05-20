-- Initial Schema for ListItUp (PostgreSQL Dialect)

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    auth_provider VARCHAR(50) NOT NULL, -- e.g., 'GOOGLE', 'GITHUB'
    role VARCHAR(50) NOT NULL DEFAULT 'STANDARD', -- 'STANDARD', 'VERIFIED', 'ADMIN'
    has_badge BOOLEAN DEFAULT FALSE,
    can_pin_lists BOOLEAN DEFAULT FALSE,
    has_analytics_access BOOLEAN DEFAULT FALSE,
    can_moderate_content BOOLEAN DEFAULT FALSE,
    can_delete_any BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    category_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    icon VARCHAR(255)
);

CREATE TABLE lists (
    list_id UUID PRIMARY KEY,
    creator_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(category_id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_photo VARCHAR(2048),
    visibility VARCHAR(50) NOT NULL DEFAULT 'PUBLIC', -- 'PUBLIC', 'PRIVATE'
    view_count INTEGER NOT NULL DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE items (
    item_id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES lists(list_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    external_url VARCHAR(2048),
    photo VARCHAR(2048),
    position_index INTEGER NOT NULL
);

CREATE TABLE comments (
    comment_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    list_id UUID NOT NULL REFERENCES lists(list_id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE likes (
    like_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    list_id UUID NOT NULL REFERENCES lists(list_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_list_like UNIQUE (user_id, list_id)
);

CREATE TABLE follows (
    follow_id UUID PRIMARY KEY,
    follower_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    followee_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_follower_followee UNIQUE (follower_id, followee_id),
    CONSTRAINT chk_no_self_follow CHECK (follower_id != followee_id)
);

CREATE TABLE saved_lists (
    saved_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    list_id UUID NOT NULL REFERENCES lists(list_id) ON DELETE CASCADE,
    saved_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_list_save UNIQUE (user_id, list_id)
);

CREATE TABLE category_proposals (
    proposal_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
    proposed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_category_proposal UNIQUE (user_id, category_id)
);

CREATE TABLE reports (
    report_id UUID PRIMARY KEY,
    submitted_by_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reviewed_by_admin_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    target_list_id UUID REFERENCES lists(list_id) ON DELETE SET NULL,
    target_item_id UUID REFERENCES items(item_id) ON DELETE SET NULL,
    target_comment_id UUID REFERENCES comments(comment_id) ON DELETE SET NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'REVIEWED', 'RESOLVED'
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_disjoint_targeting CHECK (
        (CASE WHEN target_list_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN target_item_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN target_comment_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY,
    receiver_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    trigger_user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    related_list_id UUID REFERENCES lists(list_id) ON DELETE CASCADE,
    notif_type VARCHAR(50) NOT NULL, -- 'LIKE', 'COMMENT', 'NEW_POST', 'SAVED_LIST_UPDATE'
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE list_analytics (
    analytics_id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES lists(list_id) ON DELETE CASCADE UNIQUE,
    views INTEGER NOT NULL DEFAULT 0,
    saves INTEGER NOT NULL DEFAULT 0,
    link_clicks INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Initial Categories Seed
INSERT INTO categories (category_id, name, icon) VALUES 
(gen_random_uuid(), 'Movies', '🎬'),
(gen_random_uuid(), 'Books', '📚'),
(gen_random_uuid(), 'Tech', '💻'),
(gen_random_uuid(), 'Travel', '✈️'),
(gen_random_uuid(), 'Food', '🍔');
