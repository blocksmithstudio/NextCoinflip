PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS nextcoinflip_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS nextcoinflip_user_statistic (
    user_id INTEGER NOT NULL,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    lose_streak INTEGER NOT NULL DEFAULT 0,
    earnings INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES nextcoinflip_users(id)
);

CREATE TABLE IF NOT EXISTS nextcoinflip_users_items (
    user_id INTEGER NOT NULL,
    item_won TEXT,
    timestamp INTEGER NOT NULL,
    PRIMARY KEY (user_id, timestamp),
    FOREIGN KEY (user_id) REFERENCES nextcoinflip_users(id)
)