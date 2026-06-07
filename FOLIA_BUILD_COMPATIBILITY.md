# Folia 1.21.11 build compatibility notes

This file records the build/test incompatibilities found while moving the first migration slice to Folia API `1.21.11-R0.1-SNAPSHOT`.

## Resolved in this slice

- `org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT` was replaced by `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` with `provided` scope.
- Java 8 compiler source/target was replaced by Maven `release` 21.
- `maven-shade-plugin` `3.3.0` failed on Java 21 class files with `Unsupported class file major version 65`; it was upgraded to `3.6.1`.
- `me.clip:placeholderapi:2.11.2` was not available from the current PlaceholderAPI repository URL during resolution; the dependency was updated to `2.11.6` and the repository to `https://repo.helpch.at/releases/`.
- The old `mockito-inline:4.6.1` setup could not reliably mock the widened Folia/Paper `Player` interface graph on Java 21; tests now use `mockito-core:5.12.0`.
- `MockBukkit-v1.16:1.5.2` is incompatible with the Folia/Paper API surface. A trial upgrade to `MockBukkit-v1.21:3.120.2` still failed at runtime because its embedded 1.21 tag data did not match the 1.21.11 API (`minecraft:chain`), so the affected tests were converted to pure Mockito/fake-service unit tests instead of depending on MockBukkit runtime boot.
- JUnit 4 tests were not executed under the auto-detected JUnit Platform provider until `junit-vintage-engine:5.11.0` was added.

## Remaining warnings

- `mvn clean -DskipTests package` compiles with deprecation warnings around Bukkit `GameRule` constants marked for future removal. Those belong to a later world-state lifecycle migration task.
- A local Maven warning may mention a cached legacy HTTP PlaceholderAPI repository URL from previous runs. The checked-in repository URL now uses `https://repo.helpch.at/releases/`.
