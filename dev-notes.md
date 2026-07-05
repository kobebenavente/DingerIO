# DingerIO Dev Notes

## Concepts Applied

### Thread Safety & Race Conditions
Two threads can interleave between a "check" and an "act" if they're separate operations — this is a race condition.

**Bad (not atomic):**
```java
if (!set.contains(x)) { // thread A and B both pass here
    set.add(x);         // both add and both proceed
}
```

**Good (atomic):**
```java
if (!set.add(x)) { // add returns false if already present — check+insert in one operation
    return;
}
```

Applied in `GamePollingService` — `gamesBeingProcessed` prevents two threads from processing the same game concurrently when a poll fires before the previous thread finishes.

---

### Boolean Operator Precedence
`&&` binds tighter than `||`, so mixed expressions evaluate in non-obvious order.

```java
A || B && C || D
// parsed as:
A || (B && C) || D  // NOT (A || B) && (C || D)
```

Always use parentheses when mixing `&&` and `||` to make intent explicit.

Applied in `PostGameService` — standings update check groups each team's wins/losses with `||`, then requires both teams updated with `&&`:
```java
(homeWinsChanged || homeLossesChanged) && (awayWinsChanged || awayLossesChanged)
```

---

### Per-Game Locking vs Fixed Delay
When polling multiple games concurrently, two strategies exist to prevent double-processing:

- **Fixed delay** — next poll waits for all threads to finish. Simple but one slow game blocks all others.
- **Per-game lock** — only blocks a second thread for the *same* game. Other games still run in parallel.

Per-game lock is better here because each game's MLB API calls are independent I/O operations.

---

### Atomic Operations & `finally`
When using a flag/set as a lock, always release it in a `finally` block so it's removed even if an exception is thrown mid-processing.

```java
if (!gamesBeingProcessed.add(gamePk)) return;
try {
    // processing
} finally {
    gamesBeingProcessed.remove(gamePk);
}
```
