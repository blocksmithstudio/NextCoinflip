# NextCoinflip 1.1.0 assessment

Reviewed on 2026-10-29 against Paper 26.3 build 230 and Java 25.

Compatibility follow-up on 2026-10-30 restores the owner's minimum Minecraft 1.21 baseline: `api-version: 1.21` and Java 21 bytecode, while retaining JDK 25 and the current Paper compile API. Native checks cover Paper 1.21 build 130 / Java 21 and Paper 26.3 build 231 / Java 25. All bundled base classes are Java 21 or older. Support remains Paper; plain Spigot and Folia have not been implemented.

## Dependency updates

| Component | Before | After |
| --- | --- | --- |
| Java bytecode | 21 | 21 (JDK 25 build) |
| Paper API | 1.21.9-R0.1-SNAPSHOT | 26.3.build.230-stable |
| MDLib | com.github.mdaffa48:MDLib:2.3.7 | com.muhammaddaffa:MDLib:2.4.9 |
| PlaceholderAPI | 2.11.6 | 2.12.3 |
| HikariCP | 7.0.2 | 7.1.0 |
| bStats | 3.0.2 | 3.2.1 |
| Maven compiler plugin | 3.13.0 | 3.16.0 |
| Maven shade plugin | 3.6.0 | 3.6.2 |

The minimum server API is declared as 1.21. The Paper compile API remains fixed at 26.3.build.230-stable; this does not raise the runtime minimum. Dependencies use fixed stable versions instead of an open version range that could select a prerelease or incompatible Minecraft update.

## Corrected issues

- Follow-up: use MDLib 2.4.9. Retain failed settlement steps and retry each recipient independently without repeating successful payouts, item additions or statistics changes. Preserve the selected winner throughout retries and shutdown.
- Follow-up: persist all currency credits before dispatch, retain rejected commands, and quarantine interrupted/throwing commands for reconciliation rather than losing or blindly replaying them. Migrate legacy queues without dropping duplicate credits.
- Follow-up: isolate shutdown failures, attempt every user save, always attempt resource cleanup, and preserve unfinished shutdown state in recovery snapshots. Block startup until those snapshots are reconciled.
- Follow-up: preserve earnings independently of the currently configured currencies and tolerate malformed entries without discarding valid history.

- Update MDLib to 2.4.8: Paper 26.3 command-map cleanup no longer relies on unsupported collection removal, and update-checker shutdown no longer dereferences a cleared singleton.

- Reject negative, zero, and overflowing currency wagers at the game boundary and command entry point. Negative inputs previously passed through an absolute-value check while the original signed amount reached the withdrawal command.
- Use 64-bit cumulative currency earnings so ordinary repeated wagers do not overflow statistics.
- Prevent self-challenges, repeated settlement/refunds, and stale menu actions on removed games.
- Require the confirmed item to match the current main-hand stack before consuming it. Prevent duplicate item offers from replacing an existing escrowed offer.
- Reject stale or offline item challenges before changing either selected stake. Refund the accepted challenger separately on shutdown, without refunding that stake twice.
- Load persisted user data before access, including offline refunds. Joining no longer replaces an already loaded user or allows a delayed load to overwrite new statistics.
- Serialize user saves and item claims. Require successful deletion of a stored claim before delivering it; database errors retain the in-memory claim for retry. Shutdown saves all cached users and closes the connection pool.
- Persist queued offline commands when queued and removed, rather than relying only on normal shutdown.
- Replace custom Bungee hover serialization with Paper's supported Adventure item-hover API.
- Handle PlaceholderAPI requests without a player and report the actual plugin version.
- Use the plugin data directory, modern MySQL driver name, a single SQLite connection, and a bounded connection timeout. Skip malformed currency definitions.

## Verification

Regression tests in `src/test/java/com/muhammaddaffa/nextcoinflip/SafetyTest.java` cover wager validation, payout bounds, repeated settlement, self-challenges, stale/offline item confirmations, inventory changes, persistent claims, database failure, loading existing users, and cumulative earnings.

`mvn clean verify` passed all **15 NextCoinflip tests**. The MDLib 2.4.8 fix has **108 passing library tests**.

A real Paper **26.3 build 230 / Java 25** server test passed with PlaceholderAPI 2.12.3: plugin enablement, SQLite initialization, expansion registration, `/cf reload`, null-player placeholder handling, and clean shutdown. A server-side probe saved and reloaded an item with custom persistent metadata, claimed it once, rejected a second claim, and confirmed that another save/reload did not restore it. The test server used a loopback listener and synthetic player data.

## Remaining limitations

See [RECOVERY.md](RECOVERY.md) for retry behavior, uncertain currency commands and manual reconciliation of shutdown snapshots. The follow-up fixes do not provide a full active-wager crash journal or atomic transactions across an economy plugin and the database.

- Currency commands and placeholders do not provide an atomic economy transaction or a reliable confirmation that another plugin changed the balance. Validate each configured provider on a test server.
- Synchronous database operations favor consistent claims and refunds over throughput; a slow MySQL server can stall ticks. An ordered persistence queue with immutable snapshots is the next step if latency becomes material.
- This release does not implement a crash-recovery transaction journal for in-progress wagers. A database outage or abrupt process/host failure can interrupt escrow, payout, or delivery; normal cancellation and shutdown are covered.
- Deleting an item claim and adding it to a Minecraft inventory cannot be one database transaction. A crash in between can lose delivery; claiming before deletion could instead duplicate the item.
- Live multiplayer gameplay and real economy-provider transactions require separate acceptance testing. Compilation, regression tests, and server startup alone do not prove every configuration works.

## Branch maintenance

The former `lite` branch diverged from `master` by ten commits, mainly trial restrictions and older fixes. Its tip is preserved as `archive/lite-2026-10-29` before retiring the branch. The maintained code contains the full feature set.
