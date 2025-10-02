CREATE TABLE IF NOT EXISTS nextcoinflip_users (
    id INT unsigned NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL
    PRIMARY KEY (id, uuid)
);

CREATE TABLE IF NOT EXISTS nextcoinflip_user_statistic (
    user_id INT unsigned NOT NULL,
    wins INT unsigned NOT NULL,
    losses INT unsigned NOT NULL,
    lose_streak unsigned INT NOT NULL,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES nextcoinflip_users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS nextcoinflip_users_items (
    user_id INT unsigned NOT NULL,
    item_won TEXT,
    timestamp BIGINT unsigned NOT NULL,
    PRIMARY KEY (user_id, timestamp),
    FOREIGN KEY (user_id) REFERENCES nextcoinflip_users(id) ON DELETE CASCADE
);