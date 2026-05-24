# Kereviz Client

Kereviz Client is a private Minecraft Forge 1.8.9 client focused on a cleaner Kereviz-branded experience, practical quality-of-life modules, configurable visuals, and safer local runtime handling.

The project is built as a customized continuation inspired by [OpenMyau-Plus](https://github.com/IamNespola/OpenMyau-Plus). The original client foundation and core skeleton come from the Myau+/OpenMyau-Plus codebase; Kereviz-specific work includes the rebrand, UI polish, configuration improvements, module additions, account manager updates, Discord Rich Presence integration, HUD changes, MLG utilities, and security cleanup.

## Highlights

- Minecraft Forge 1.8.9 client with Kereviz branding and green accent theme.
- Normal and Rise-style ClickGUI support.
- Expanded config system with named configs and in-game config commands.
- Local account manager support, including Microsoft and offline/cracked entries.
- Discord Rich Presence module with customizable display options.
- Utility and visual modules such as HUD, DynamicIsland, FreeLook, MLG, BedTracker, and AnticheatDetector.
- Local runtime data stored under `.minecraft/Kereviz Client/`.

## Build

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

The release jar is generated under `build/libs/`.

## Install

1. Install Minecraft Forge `1.8.9`.
2. Build the project.
3. Copy the generated `KerevizClient-<version>.jar` from `build/libs/` into your `.minecraft/mods` folder.
4. Launch the Forge 1.8.9 profile.

Use only on servers, private games, or test environments where this kind of client is allowed.

## Configs

Runtime files are stored under:

```text
.minecraft/Kereviz Client/
```

Configs are stored under:

```text
.minecraft/Kereviz Client/configs/
```

In-game config commands:

```text
.config list
.config save <name>
.config load <name>
.config rename <old> to <new>
.config delete <name>
.config folder
```

## Security Notes

- The old custom `ssl.jks` truststore was removed. HTTPS now uses the normal JVM trust store.
- Account tokens are stored locally in `.minecraft/Kereviz Client/accounts/kereviz.accounts.json` if you use the account manager.
- No webhook, token exfiltration, or external command execution path is intentionally included.
- Network-facing features should stay transparent, optional, and easy to audit.

## Attribution

Kereviz Client is heavily inspired by and structurally based on [OpenMyau-Plus](https://github.com/IamNespola/OpenMyau-Plus). Credit for the original client skeleton and foundation belongs to the OpenMyau-Plus/Myau+ project and its contributors.

Selected feature ideas and implementation references may also come from open-source Minecraft client projects such as [LiquidBounce](https://github.com/CCBlueX/LiquidBounce), with adaptations made for this Forge 1.8.9 codebase.

## License

This project is distributed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
