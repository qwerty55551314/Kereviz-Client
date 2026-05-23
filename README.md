# Kereviz Client

Kereviz Client is a Minecraft Forge 1.8.9 client.

## Build

On Windows:

```powershell
.\gradlew.bat build
```

On Linux/macOS:

```bash
./gradlew build
```

The release jar is generated under `build/libs/`.

## Install

1. Install Minecraft Forge `1.8.9`.
2. Build the project.
3. Copy the generated `KerevizClient-<version>.jar` from `build/libs/` into your `.minecraft/mods` folder.
4. Launch the Forge 1.8.9 profile.

Use only on servers or worlds where this kind of client is allowed.

## Security Notes

- The old custom `ssl.jks` truststore was removed. HTTPS now uses the normal JVM trust store.
- Account tokens are stored locally in `.minecraft/kereviz.accounts.json` if you use the account manager.
- No webhook, token exfiltration, or external command execution path is intentionally included.
