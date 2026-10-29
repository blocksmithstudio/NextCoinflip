# NextCoinflip

An open-source Minecraft coinflip plugin with currency wagers, item wagers, player statistics, configurable menus, and PlaceholderAPI support.

## Requirements

- Paper **26.3** and **Java 25**.
- [PlaceholderAPI 2.12.3](https://github.com/PlaceholderAPI/PlaceholderAPI/releases/tag/2.12.3).
- For currency wagers: the relevant economy plugin and PlaceholderAPI expansion, configured in `currencies.yml`.

MDLib is bundled; do not install it as a separate plugin. NextCoinflip uses Paper APIs, including Adventure item hovers. Spigot and Folia are not supported by this release. Older Minecraft/Java versions require an older NextCoinflip release.

## Build and install

With JDK 25 and Maven 3.9 or newer:

```sh
mvn --batch-mode --no-transfer-progress clean verify
```

Install `target/NextCoinflip-1.1.0.jar` and PlaceholderAPI in your server's `plugins` directory, then restart. Do not install the `original-NextCoinflip-*.jar` file. Edit the generated configuration files and restart or use `/cf reload` for configuration changes.

The default database is SQLite in the plugin data folder. MySQL is optional. Back up the plugin's configuration and database before upgrading. Preserve `data.yml`, which stores pending currency commands.

## Commands

| Command | Action |
| --- | --- |
| `/cf` | Browse and cancel your wagers; access winnings and statistics |
| `/cf <amount>` | Select a currency and create a wager |
| `/cf hand` | Offer the item stack in your main hand |
| `/cf accept <player>` | Review an item challenge |
| `/cf reload` | Reload configuration; requires `nextcoinflip.admin` |

Aliases: `/coinflip` and `/nextcoinflip`.

Wagers must be positive whole numbers, at most **1,073,741,823**, and within the selected currency's limits. Each player can create one waiting item game and one game per currency type.

## Placeholders

`%nextcoinflip_wins%`, `%nextcoinflip_losses%`, `%nextcoinflip_losestreak%`, `%nextcoinflip_winrate%`, `%nextcoinflip_total_game%`, and `%nextcoinflip_earnings_<currency>%`.

## Currency integration

Each currency uses a numeric balance placeholder and console commands to take and give funds. Check these against your installed economy plugin before enabling real wagers. A command being dispatched does not prove the economy accepted the transfer; this integration cannot provide atomic transactions across plugins.

## Validation and limitations

See [ASSESSMENT.md](ASSESSMENT.md) for the fixes, test coverage, and remaining limitations in 1.1.0. User data is loaded before use and saved synchronously to prevent stale writes from restoring claimed items. Slow or remote databases can therefore delay the server thread.

## License

NextCoinflip is licensed under **GPL-3.0-or-later**; see [LICENSE](LICENSE). Bundled dependencies retain their original licenses and notices; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
