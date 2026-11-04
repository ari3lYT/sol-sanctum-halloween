# Configuration

The mod stores `config/elysium-halloween/config.json` and `statistics.json` inside the game instance.
Legacy technical names are preserved for compatibility. `ordinaryPoints`, `uniquePoints`, `showHud`,
`showInTab`, `hudX`, `hudY`, `pollSeconds` and `syncWebsite` control local behavior. Edit while the game is closed.

`syncWebsite` defaults to **false**. To opt in, configure your own endpoint with the JVM argument
`-Dsolsanctum.endpoint=https://event.example.com/halloween/api/ingest`, then enable synchronization.
Only HTTPS is accepted for remote servers; loopback HTTP is allowed for development. Snapshots contain
the Minecraft player name/UUID, a hashed profile identifier, event counts and version information.
Make that publication clear to your players before enabling it. Upload identities and keys remain local.

The API accepts `--host`, `--port`, `--db` and `--public`. `ALLOWED_ORIGINS` is a comma-separated
allowlist; the development default is `http://127.0.0.1:5186`. Put a production service behind TLS.
The site uses `/halloween/` as its base path. External avatar requests are described in the UI source;
operators may replace `Avatar.jsx` with local placeholders if desired.

Run `python tools/prepare_downloads.py` from the repository root after building all mod versions.
This copies only the project's own JARs into the website download directory; Fabric API links point
to the official Fabric Maven repository. Do not distribute old private builds with different sync defaults.
