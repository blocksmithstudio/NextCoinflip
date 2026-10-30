# Settlement and currency recovery

Back up the plugin data folder and configured database before upgrading or
reconciling a failed delivery. Keep these files private: they contain player
balances, item metadata and server currency commands.

## Normal retries

Incomplete settlements remain registered and retry once per second. Each
recipient has separate progress. A successful refund, item addition or statistics
update is not repeated when a later save fails. A selected winner remains fixed
even if shutdown requests cancellation while settlement is pending.

Pending item-offer refunds also retain their progress until saving succeeds.
Users referenced by unfinished games stay cached, preventing a later retry from
saving an obsolete user object after reconnect.

## Currency commands

All currency credits are saved in `data.yml` before being dispatched. Older
`commands.<player UUID>.commands` lists migrate automatically to entries with
unique IDs. Identical queued commands remain separate credits.

Entries have these states:

| State | Behavior |
| --- | --- |
| `PENDING` | Retry when the player is online. A missing command retains this state. |
| `DISPATCHING` | Saved immediately before calling the provider. On restart it becomes `UNCERTAIN`. |
| `COMPLETE` | Dispatch returned successfully. Remove without dispatching again. |
| `UNCERTAIN` | Execution threw or was interrupted. Retain for manual reconciliation. |

Console dispatch cannot prove that an economy provider changed a balance. A
provider may have applied a transfer before throwing. Automatically replaying
that command could pay twice, so uncertain entries are never replayed silently.

To reconcile an uncertain entry, stop the server and compare the provider's
actual transaction/balance records with the intended credit. Set the entry to
`COMPLETE` if it was applied, or to `PENDING` only after establishing that it was
not applied. Restart to load the change. `/cf reload` intentionally does not
reload the delivery queue. Do not delete an uncertain entry before reconciling it.

Queue writes replace the file atomically after flushing its contents. A failed
write prevents a new credit from being accepted or dispatched. An invalid queue
blocks loading and is preserved rather than overwritten during shutdown.

## Shutdown recovery snapshots

Shutdown attempts every game and user independently, then always attempts to
close the database and library resources. If work is still unfinished, a new
`recovery-<UUID>.yml` file records the game outcome, recipient progress, original
items and cached user statistics/items. Existing snapshots are preserved.

Snapshots are recovery evidence, not an automatic replay queue. Startup refuses
to enable the plugin while an unresolved snapshot is present. Stop the server,
reconcile its entries against the database and `data.yml`, and preserve the
snapshot under a name ending in `.resolved` only after reconciliation is complete.

The `deliveries` list identifies each recipient and its remaining steps:

- `currency`: one remaining step means the credit has not been accepted into the
  durable queue; zero means it has. Check `data.yml` and provider records before
  crediting anything manually.
- `user`: three means loading remains, two means mutation remains, one means the
  cached mutation is complete but saving remains, and zero means saving returned
  successfully. Use the matching `users` snapshot and stable item IDs to reconcile
  pending saves without adding items or statistics twice.
- `offer-refund`: the matching `pending-offers` entry gives the inner user-update
  progress. A completed offer refund is removed from that list.

These counts describe where execution stopped; they do not prove that a remote
database rejected a write when its connection failed. Reconcile ambiguous
outcomes rather than applying the complete game again.

## Limits

Active wagers are still held in memory before settlement; abrupt process or host
failure is not covered by a full escrow journal. If storage itself cannot be
written, recovery snapshots cannot be guaranteed. Database deletion and Minecraft
inventory delivery cannot form one transaction. Economy-provider multiplayer and
MySQL acceptance testing remain necessary for each server configuration.
