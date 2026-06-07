# BetterSleeping

BetterSleeping is a Minecraft sleep-management plugin for modern Folia servers. It lets a configurable part of the players in a world speed up or skip the night, while keeping messages, bossbars, buffs/debuffs and integrations region-aware for Folia's threaded world model.

Useful links:

- Setup/configuration guide: [project wiki](https://github.com/Nuytemans-Dieter/BetterSleeping/wiki)
- Releases: [GitHub releases](https://github.com/Nuytemans-Dieter/BetterSleeping/releases)
- Metrics: [bStats](https://bstats.org/plugin/bukkit/BetterSleeping/7414)

## Supported platform

| Component | Supported baseline |
| :-- | :-- |
| Server API | Folia API `1.21.11-R0.1-SNAPSHOT` |
| Minecraft server family | Folia/Paper 1.21.x, with Folia as the primary target |
| Java runtime | Java 21 |
| Plugin API version | `api-version: "1.21"` |

Older Spigot/Paper-only runtimes are no longer the target of this branch. The codebase uses Folia schedulers and must be compiled and tested against the Folia API.

## What this plugin does

Usually all players must sleep before a night or storm can be skipped on a multiplayer server. BetterSleeping allows this to be skipped when a configured percentage or fixed amount of players is sleeping.

Configuration includes, but is not limited to:

- Set whether night skip is accelerated or naturally reaches day.
- Configure percentage-based or absolute sleeper requirements.
- Customize messages and translations.
- Give players buffs for sleeping and debuffs for not sleeping.
- Disable phantom spawning.
- Show chat/actionbar/title messages and bossbar progress.
- Disable BetterSleeping per world without changing the user-facing config format.

## Optional hooks

| Hook | Purpose | Folia notes |
| :-- | :-- | :-- |
| PlaceholderAPI | Provides `%bettersleeping_*%` placeholders. | Placeholder calculation uses cached world/sleep snapshots and does not scan live worlds from arbitrary contexts. |
| EssentialsX | AFK/vanish checks for bypass logic. | Player-specific Essentials calls must run from the player's entity scheduler context. Incompatible Essentials runtime classes disable the hook with a warning. |
| GSit | Counts GSit sleeping/lying poses as sleepers when configured. | Pose and sleeper-state changes are scheduled through the player entity scheduler. Incompatible GSit versions are disabled with a warning. |
| bStats | Anonymous usage metrics. | Charts use config/startup snapshots and avoid Bukkit entity/world access in library-managed callbacks. |

## Known limitations

- Folia region ownership is strict: plugin code must not mutate a `World`, `Entity`, `Player`, bossbar membership, particles, or player messages from an arbitrary thread/context.
- Shutdown is special: Folia does not allow new scheduler tasks once plugin/server shutdown has started. Disable/reload cleanup must cancel tracked tasks and perform unavoidable restoration synchronously without creating new tasks.
- Public API compatibility getters that return live Bukkit objects are deprecated for Folia consumers. Use snapshot DTOs where available and schedule your own mutations in the correct Folia context.
- MockBukkit does not fully model Folia schedulers, so most tests use pure unit tests and `FakePluginScheduler` instead of a Bukkit runtime.

## Developer guide

### Scheduler ownership

Use `PluginScheduler` as the only scheduling entry point in BetterSleeping code:

- `runGlobal` / `repeatGlobal` for global server state or console command dispatch.
- `runAtLocation` / `repeatAtLocation` for world or location-owned operations.
- `runForEntity` / `runForEntityLater` / `repeatForEntity` for player/entity reads and mutations.
- `runAsync` / `repeatAsync` only for pure computation without Bukkit `World`, `Entity`, or `Player` access.

Do not add direct usages of `Bukkit.getScheduler()`, `BukkitRunnable`, `runTask*`, `entity.getScheduler()`, `server.getRegionScheduler()`, or `server.getGlobalRegionScheduler()` outside the scheduler adapter layer.

### Shutdown/reload rules

- Do not create new scheduled tasks from `onDisable()`, shutdown hooks, or code called after plugin disable starts.
- Cleanup methods called from disable/reload should cancel tracked `TaskHandle`s first.
- If state must be restored during disable, perform the minimal restoration immediately and do not enqueue it on Folia schedulers.

### Commands and tab-completion

All commands are registered through `BetterSleepingCommandFacade`:

- Every subcommand must define its permission and aliases in the facade descriptor list.
- `onTabComplete` must check subcommand permission before building any suggestions.
- Argument completers must return no suggestions when the sender lacks permission for the owning subcommand.
- Add unit tests whenever a new subcommand or contextual completer is added.

### Public API usage

`BecomeDayEvent` exposes immutable snapshot data for Folia-safe integrations. External plugins should read IDs/names/world keys from the snapshot and then schedule any Bukkit mutations through Folia's entity or region schedulers.

## Build and test

```bash
mvn test
mvn -DskipTests package
rg "BukkitRunnable|Bukkit.getScheduler\(|runTask" src/main/java
```

The static `rg` command should produce no output except for explicitly documented compatibility/adapters.
