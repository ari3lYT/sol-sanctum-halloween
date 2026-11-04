**English** · [Русский](development.ru.md)

# Development

The root is a source monorepo: `mod/` is a multi-project Gradle build, and `website/` is an independent
Vite/React application plus a Python API. `mod/common/src/test` is compiled for each supported target.
Optional real-client GameTests are enabled with `-PgameTests`; they are not included in release JARs.

Run the command suites from the README. Tests cover attribution, monotonic local state, persistence,
HUD constraints, snapshot authentication/sequence handling, API validation and leaderboard sorting.
Unit tests do not establish compatibility with every server's damage/death packet behavior.

`tools/demo_site.py` seeds fictional players into a temporary local SQLite database and starts the real
website/API. The screenshots in this repository use this fixture, not a production player list.
The initial public release uses version 1.1.4 to distinguish its local-first defaults from private 1.1.3 builds.
