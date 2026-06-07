# План полного перехода BetterSleeping на Folia API 1.21.11

Цель: перевести плагин на нативную модель Folia, где любые обращения к миру, сущностям и игрокам выполняются в правильном region/entity/global контексте, а общие вычисления и I/O остаются асинхронными. Целевая зависимость API: `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` с областью `compileOnly`/`provided`.

## Использованные источники API

- Folia Javadocs 1.21.11: <https://jd.papermc.io/folia/1.21.11/>
- Paper/Folia: supporting Paper and Folia: <https://docs.papermc.io/paper/dev/folia-support>
- Paper scheduler docs: <https://docs.papermc.io/paper/dev/scheduler/>
- Folia overview: <https://docs.papermc.io/folia/reference/overview/>
- Bukkit Javadocs: <https://hub.spigotmc.org/javadocs/bukkit/>

## Краткий аудит текущего состояния

- Проект сейчас Maven-плагин с Java 8 и `org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT` в `provided`, поэтому сборка не видит Folia scheduler API и современный Paper/Folia surface.
- Основной tick-цикл сна реализован через `BukkitRunnable` и запускается `runTaskTimer(plugin, 1L, 1L)` отдельно для каждого мира.
- BossBar обновляется через `BukkitRunnable` и глобальный обход `Bukkit.getOnlinePlayers()`.
- Анимация `ZZZAnimation` вызывает `World#spawnParticle(...)` из `runTaskAsynchronously(...)` и дополнительно управляет циклом через `Thread.sleep(...)`; это главный Folia-риск, потому что доступ к миру выполняется вне region/entity scheduler.
- `SleepWorld` хранит `World` и напрямую вызывает `World#getPlayers`, `World#getTime`, `World#setTime`, `World#setStorm`, `World#setThundering`.
- `BuffsHandler` применяет эффекты к игроку и dispatch консольных команд в одном обработчике `BecomeDayEvent`; для Folia это нужно разнести по entity/global scheduler.
- В `SleepWorldManager#removeSleeper(Player)` обнаружена логическая ошибка: вместо `runnable.removeSleeper(player)` вызывается `runnable.addSleeper(player)`. Исправлять лучше отдельной задачей перед/вместе с миграцией событий сна.
- Команды наследуются от BetterCore `BPCommandHandler`; в текущем коде нет явной регистрации tab-completer и локальной фильтрации подсказок по permissions, поэтому перед полной Folia-миграцией нужно решить, расширяем ли BetterCore или добавляем собственный command facade.

## Архитектурные принципы миграции

- Не оставлять прямых `Bukkit.getScheduler()`, `BukkitRunnable#runTask*`, `Bukkit.getOnlinePlayers()` и обращений к `World`/`Player` из произвольного потока в бизнес-логике.
- Ввести единый слой планирования задач, чтобы не размазывать Folia API по listeners/services/commands:
  - `PluginScheduler` / `FoliaTaskScheduler` как сервис-обертка над `GlobalRegionScheduler`, `RegionScheduler`, `EntityScheduler`, `AsyncScheduler`.
  - `ScheduledTaskHandle` для отмены задач без зависимости ядра от конкретных классов scheduler API.
  - Методы уровня намерений: `runGlobal`, `runAtWorldSpawn`, `runAtLocation`, `runForEntity`, `runAsync`, `repeatGlobal`, `repeatAtLocation`, `repeatForEntity`.
- Все данные состояния хранить по стабильным идентификаторам (`UUID`, имя мира/`NamespacedKey`, immutable snapshots), а не по `Player`/`World` как ключам долгоживущих коллекций.
- Любой код, работающий сразу с несколькими регионами/игроками, должен сначала собрать snapshot в безопасном scheduler-контексте, затем раздать per-entity/per-region задачи.

## План задач

Каждый пункт ниже рассчитан как отдельная задача на одно сообщение пользователя. Задачи расположены в рекомендуемом порядке, чтобы не ломать текущий функционал и сохранять проверяемое состояние после каждого шага.

### 1. Обновить build-конфигурацию под Folia API 1.21.11

**Задача:** заменить Spigot API на Folia API и поднять baseline сборки.

**Что сделать:**
- Для Gradle-варианта добавить:
  ```kotlin
  compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
  ```
- Для текущего Maven-проекта добавить PaperMC repository и зависимость Maven-эквивалентом со scope `provided`:
  ```xml
  <dependency>
      <groupId>dev.folia</groupId>
      <artifactId>folia-api</artifactId>
      <version>1.21.11-R0.1-SNAPSHOT</version>
      <scope>provided</scope>
  </dependency>
  ```
- Обновить compiler target/source или release до Java 21, так как целевые серверы 1.21.x требуют современную Java-среду.
- Обновить `api-version` в `plugin.yml` до актуальной 1.21-ветки.
- Проверить, какие тестовые зависимости перестали компилироваться после смены API, и зафиксировать список несовместимостей.

**Критерии приемки:** `mvn -DskipTests package` хотя бы доходит до ошибок исходного кода, связанных с API-миграцией, а не падает на резолвинге Folia dependency.

### 2. Ввести модуль scheduler-abstraction

**Задача:** добавить `services/scheduler` слой для Folia-планирования.

**Что сделать:**
- Создать интерфейсы `PluginScheduler`, `TaskHandle`, при необходимости `EntityTaskHandle`.
- Реализовать `FoliaPluginScheduler` через API сервера:
  - `server.getGlobalRegionScheduler()` для глобального состояния сервера и консольных команд.
  - `server.getRegionScheduler()` для действий по миру/локации.
  - `entity.getScheduler()` для действий с игроками/сущностями.
  - `server.getAsyncScheduler()` для вычислений без Bukkit world/entity доступа.
- Зарегистрировать scheduler в Guice `BetterSleepingModule` или отдельном модуле.
- Запретить новым классам напрямую импортировать `BukkitRunnable`/`BukkitScheduler`, кроме временного compatibility-кода на время миграции.

**Критерии приемки:** в проекте появляется единая точка использования Folia scheduler API, а новые сервисы получают ее через DI.

### 3. Разделить доменную модель сна и Bukkit/Folia state access

**Задача:** убрать прямую зависимость ядра сна от `World` как mutable runtime object.

**Что сделать:**
- Ввести `SleepWorldId`/`ManagedWorldKey` и `SleepWorldState` для хранения имени мира, internal time, counters и config-derived параметров.
- Перенести прямые вызовы `World#getTime`, `World#setTime`, `World#getPlayers`, `World#setStorm`, `World#setThundering` в отдельный `WorldAccessService`.
- Методы `SleepWorld` оставить как доменную оболочку или заменить на `SleepWorldState`, чтобы расчеты sleepers-needed и time-speedup не зависели от потока Folia.
- Долгоживущие maps перевести с ключа `World` на имя мира/UUID мира, чтобы не хранить region-sensitive объекты.

**Критерии приемки:** расчет `SleepStatus`, sleep-speedup и sleeper counters можно unit-тестировать без Bukkit `World`.

### 4. Перевести `SleepRunnable` в Folia region task по миру

**Задача:** заменить `BukkitRunnable` основного цикла сна на управляемый Folia task.

**Что сделать:**
- Переименовать `SleepRunnable` в `SleepTickService` или `SleepWorldTicker`, не наследоваться от `BukkitRunnable`.
- Запускать per-world тик через `PluginScheduler#repeatAtLocation` в стабильной локации мира, например spawn location, с периодом 1 tick.
- Все операции `World#getTime`, `World#setTime`, `World#setStorm`, `World#setThundering`, `World#getPlayers` выполнять внутри этого region task или через `WorldAccessService`.
- Создание и вызов `BecomeDayEvent` выполнять из безопасного scheduler-контекста; если событие содержит игроков из разных регионов, передавать UUID/snapshot или гарантировать дальнейшую обработку через entity scheduler.
- Сохранить semantics: ускорение дня/ночи, natural skip, sleep skip, сообщения `sleep_possible_*`, `enough_sleeping`, `morning_message`.

**Критерии приемки:** в основном цикле сна не остается `BukkitRunnable`, а `rg "runTaskTimer|BukkitRunnable" src/main/java/be/betterplugins/bettersleeping/runnables` не находит старый запуск сна.

### 5. Перевести управление списком спящих на entity scheduler

**Задача:** сделать `addSleeper/removeSleeper` безопасными для игрока в Folia.

**Что сделать:**
- В `BedEventListener`, `SleepCommand`, `GSitListener` и обработчиках quit/leave выполнять операции с игроком через `PluginScheduler#runForEntity(player, ...)`, если вызов может прийти не из entity-owning контекста.
- Исправить ошибку `SleepWorldManager#removeSleeper(Player)`, где сейчас вызывается `addSleeper` вместо `removeSleeper`.
- Хранить sleepers как `Set<UUID>`, а валидацию игрока делать через entity scheduler или snapshot, не через прямой `Bukkit.getPlayer(UUID)` из region tick другого мира.
- Разделить broadcast-рассылку: собрать список UUID игроков мира, затем отправлять каждому через entity scheduler.

**Критерии приемки:** `SleepWorldManager` больше не обращается к `player.getWorld()` вне контролируемого scheduler-контекста, а regression-тест покрывает `removeSleeper`.

### 6. Перевести сообщения и ScreenMessenger на per-player delivery

**Задача:** обезопасить отправку chat/actionbar/title сообщений игрокам.

**Что сделать:**
- Создать `MessageDeliveryService`, который принимает `CommandSender`/UUID/snapshot и отправляет игрокам через entity scheduler.
- `ScreenMessenger` перевести с `Map<Player, ScreenMessageSender>` на `Map<UUID, ScreenMessageSender>` или stateless sender, чтобы не хранить `Player` как ключ.
- Создание/removal screen sender выполнять в `PlayerJoinEvent`/`PlayerQuitEvent`, но сами delayed/queued отправки выполнять через entity scheduler.
- Проверить, не использует ли BetterCore `Messenger` синхронные Bukkit-вызовы; при необходимости обернуть его, а не дублировать логику.

**Критерии приемки:** массовая отправка сообщений из sleep tick, bossbar, buffs не требует прямого доступа к `Player` из чужого региона.

### 7. Перевести BossBar на Folia entity/global-safe модель

**Задача:** заменить `BossBarRunnable` на сервис, безопасно обновляющий bossbar для игроков.

**Что сделать:**
- Убрать наследование `BukkitRunnable`.
- Не использовать глобальный `Bukkit.getOnlinePlayers()` внутри region task.
- Держать bossbar state по имени мира, а добавление/удаление игроков выполнять через player entity scheduler.
- Обновление title/progress делать из snapshot `SleepStatus`; если `BossBar` API требует main/entity context, выполнять операции per-player/per-region через scheduler abstraction.
- Корректно cancel/removeAll при reload/disable через tracked task handles.

**Критерии приемки:** bossbar сохраняет текущую функциональность, но не использует legacy scheduler и не обходит online players напрямую.

### 8. Переписать ZZZ animation без async Bukkit world access и `Thread.sleep`

**Задача:** сделать анимацию частиц полностью Folia-safe.

**Что сделать:**
- Оставить precompute в `AsyncScheduler`, так как там только math/vector calculations.
- Спавн частиц выполнять через `EntityScheduler` игрока или `RegionScheduler` по актуальной sleep location.
- Заменить while-loop + `Thread.sleep(delay)` на repeating scheduled task с периодом в ticks.
- `PlayerSleepLocation#getLocation()` вызывать только в entity-owning контексте игрока.
- Хранить task handle в `AnimationHandler`, отменять при `PlayerBedLeaveEvent`, `PlayerQuitEvent`, `BecomeDayEvent`, reload/disable.

**Критерии приемки:** `ZZZAnimation` больше не вызывает `World#spawnParticle` из async task, а `rg "Thread.sleep|runTaskAsynchronously" src/main/java/be/betterplugins/bettersleeping/animation` не находит опасный цикл.

### 9. Разнести buffs/debuffs/commands по scheduler-контекстам

**Задача:** сделать `BecomeDayEvent` обработку безопасной для игроков и глобальных команд.

**Что сделать:**
- Для каждого игрока применять `Player#addPotionEffects` через entity scheduler.
- `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)` выполнять через global region scheduler, если команда не привязана к конкретному игроку как entity action.
- Передавать в handler immutable snapshots (`UUID`, player name, world name, slept/non-slept flag), а не живые списки `Player`.
- Сохранить bypass-проверки, но привести `PermissionsCache`/`BypassChecker` к thread-safe access или выполнять проверки в player context.

**Критерии приемки:** buff/debuff логика не обращается к `Player` и server command dispatcher из произвольного event context.

### 10. Перевести world-state lifecycle: gamerule, weather, time restore

**Задача:** безопасно управлять состоянием миров при enable/reload/disable.

**Что сделать:**
- `WorldStateHandler#setWorldStates(...)` выполнять через region/global scheduler в зависимости от конкретного API-вызова.
- Сохранение исходного состояния миров делать snapshot-операцией при старте после загрузки миров.
- Восстановление daylight cycle/time/weather на disable/reload выполнять через tracked tasks и ожидать/логировать невозможность выполнения при shutdown, если API не гарантирует завершение.
- Проверить обработку worlds, добавленных/удаленных во время работы сервера.

**Критерии приемки:** enable/reload/disable не делают прямых world mutations из plugin lifecycle thread без scheduler abstraction.

### 11. Обновить listeners под Folia event assumptions

**Задача:** привести все обработчики Bukkit events к правилу “event thread is not a universal main thread”.

**Что сделать:**
- `BedEventListener`: оставить быстрые проверки события, а изменения состояния отправлять в entity/region services.
- `PhantomHandler`: отмена spawn event может оставаться в event context, но любые дополнительные действия с entity/location — только region/entity scheduled.
- `TimeSetToDayCounter`: сделать счетчик потокобезопасным (`AtomicInteger`) или обеспечить вызов только из одного scheduler context.
- `AnimationHandler`: хранить UUID/task handles, а не runtime player/world references.

**Критерии приемки:** в listeners нет долгих операций, cross-region обходов и mutable shared state без синхронизации/ownership.

### 12. Добавить tab-completion с permission-first фильтрацией

**Задача:** выполнить требование по командам и не зависеть от неизвестного поведения BetterCore.

**Что сделать:**
- Создать `BetterSleepingCommandFacade`, который реализует `CommandExecutor` и `TabCompleter` или расширяет BetterCore только там, где это безопасно.
- Для каждого subcommand описать permission, aliases, argument completers.
- В `onTabComplete` сначала проверить permission sender-а на subcommand, затем формировать список подсказок.
- Не показывать аргументы, если у игрока нет permission на саму команду/подкоманду.
- Покрыть unit-тестами: user видит только `help/status/buffs/shout`, op/admin видит admin-команды, no-permission не видит скрытые аргументы.

**Критерии приемки:** `plugin.yml` команда имеет tab completer, а подсказки контекстны и permission-aware.

### 13. Проверить hooks на Folia-совместимость

**Задача:** отдельно мигрировать integrations: Essentials, PlaceholderAPI, GSit.

**Что сделать:**
- `PapiExpansion`: убедиться, что placeholder calculation не трогает чужие region objects; тяжелые операции заменить snapshots/cache.
- `EssentialsHook`: проверить API-вызовы EssentialsX на thread-safety; при сомнениях выполнять через player entity scheduler или документировать ограничение.
- `GSitListener`: все действия с player pose/sleep state выполнять через entity scheduler.
- Обновить `hooks.yml`/логирование, чтобы несовместимые hook версии явно отключались без падения плагина.

**Критерии приемки:** каждый hook либо Folia-safe, либо отключается с понятным логом и не ломает старт BetterSleeping.

### 14. Обновить публичный API BetterSleeping

**Задача:** сделать API пригодным для Folia без передачи живых объектов между регионами.

**Что сделать:**
- Добавить snapshot DTO для `BecomeDayEvent`: world name/key, cause, slept UUIDs/names, non-slept UUIDs/names.
- Сохранить старые getters с `Player`/`World`, если нужна обратная совместимость, но отметить ограничения/депрекацию и возвращать только безопасно собранные snapshots.
- Рассмотреть `AsyncEvent`/custom scheduling contract только после проверки Folia/Paper docs; не делать event async без необходимости.
- Документировать, что внешние плагины должны выполнять entity/world mutations через Folia scheduler.

**Критерии приемки:** внешние потребители API могут получить данные события без cross-thread доступа к `Player`/`World`.

### 15. Мигрировать тесты и добавить scheduler contract tests

**Задача:** закрепить миграцию автоматическими проверками.

**Что сделать:**
- Обновить MockBukkit или заменить/дополнить тесты чистыми unit-тестами сервисов, где MockBukkit не поддерживает Folia API.
- Для `PluginScheduler` сделать fake implementation для unit-тестов.
- Покрыть:
  - расчет количества спящих;
  - переходы времени;
  - `removeSleeper` regression;
  - permission-aware tab-completion;
  - message fan-out by UUID;
  - animation task cancellation.
- Добавить `mvn test`, `mvn -DskipTests package`, и статический grep/check на legacy scheduler imports в checklist CI.

**Критерии приемки:** основные доменные тесты не зависят от Bukkit runtime, а Folia-specific слой проверяется контрактными тестами/fakes.

### 16. Провести ручную проверку на Folia server

**Задача:** проверить реальное поведение на Folia 1.21.11.

**Что сделать:**
- Поднять чистый Folia test server 1.21.11 с BetterSleeping.
- Проверить сценарии:
  - старт/остановка/reload;
  - вход в кровать одним и несколькими игроками в одном мире;
  - игроки в разных регионах одного мира;
  - игроки в разных мирах;
  - bossbar;
  - particles animation;
  - buffs/debuffs/commands;
  - PlaceholderAPI/Essentials/GSit при наличии;
  - permissions/tab-completion.
- Собрать логи region-thread violations, если Folia их выводит, и исправить отдельными маленькими задачами.

**Критерии приемки:** в логах нет scheduler/thread violation, а все текущие пользовательские сценарии BetterSleeping работают.

### 17. Финальная зачистка legacy Bukkit scheduler и документации

**Задача:** удалить временные переходные куски и обновить документацию для пользователей.

**Что сделать:**
- Удалить все оставшиеся импорты `org.bukkit.scheduler.BukkitRunnable` и прямые `Bukkit.getScheduler()` из production-кода.
- Обновить README: поддерживаемая платформа Folia 1.21.11, Java 21, список hooks и known limitations.
- Обновить config templates только при необходимости; не менять пользовательский формат без migration step.
- Добавить раздел для разработчиков: как использовать `PluginScheduler`, как добавлять команды с tab-completion, как не нарушать Folia region ownership.

**Критерии приемки:** `rg "BukkitRunnable|Bukkit.getScheduler\(|runTask" src/main/java` не находит legacy scheduler usage, кроме явно разрешенных compatibility/adapters.

## Рекомендуемые границы модулей после миграции

```text
be.betterplugins.bettersleeping
├── api                  # public events/snapshots/interfaces
├── commands             # command facade + subcommands + tab completion
├── hooks                # integrations, isolated adapters
├── listeners            # thin event adapters only
├── messaging            # message formatting + per-player delivery
├── model                # immutable/domain state, status DTOs
├── services
│   ├── scheduler        # Folia scheduler abstraction
│   ├── sleeping         # SleepTickService, SleepWorldRegistry
│   ├── world            # WorldAccessService, WorldStateService
│   ├── bossbar          # BossBarService
│   └── animation        # AnimationService, particle renderers
└── utils                # pure helpers only
```

## Definition of Done для полного перехода

- Сборка использует Folia API `1.21.11-R0.1-SNAPSHOT` как compile-only/provided dependency.
- Production-код не использует legacy Bukkit scheduler напрямую.
- Нет Bukkit world/entity/player mutations вне `PluginScheduler` abstraction.
- Долгоживущие коллекции не используют `Player`/`World` как ключи.
- Команды имеют permission-aware tab-completion.
- Unit/contract tests проходят, ручной тест на Folia 1.21.11 не показывает thread/region violations.
