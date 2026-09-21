# Warper
![Modrinth Version](https://img.shields.io/modrinth/v/signwarpcreate)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/signwarpcreate)

Create warp points with signs and travel between them through an inventory-based warp GUI.
**Server-side only:** players can join with a vanilla client, and it also works in singleplayer.

## How to use
1. Place a sign on or on top of a block. That block becomes the warp's icon in the GUI.
2. Write `[warp]` on the first line and the warp's name on the second line.
3. The sign turns into a glowing warp sign and gets waxed, so it can't be edited anymore.
4. After an activation delay (20 minutes by default), right-click any warp sign to open the warp GUI and click a destination to teleport there. This works across dimensions.

## Rules
- A new warp must be at least 50 blocks away from other warps in the same dimension.
- Up to 55 warp points per world.
- Breaking a warp sign removes the warp point.
- Right-clicking a warp that isn't active yet shows the remaining time.

## Configuration
`config/warper.json` is created on first start:
```json
{ "DISTANCE": 50, "WAIT_TIME": 1200 }
```
`DISTANCE` is the minimum distance between warps, in blocks. `WAIT_TIME` is the activation delay, in seconds. Restart the server after changing it.

## Requirements
- Fabric Loader and **Fabric API**
- Supported Minecraft versions: 26.3, 26.2, 26.1.2, 1.21.11 and 1.20.4 (older release). Existing warp points are kept when you update.

## Development
Each supported Minecraft version has its own branch (`26.3`, `26.2`, `26.1`, `1.21.11`, `1.20`); the newest one is the default.
Fixes are made on one branch and copied to the others with `git cherry-pick`.

Releases are tagged `<mod version>+<minecraft version>`, e.g. `1.1.0+26.3`.
Publishing a GitHub release builds the mod, attaches the jars to the release and uploads it to [Modrinth](https://modrinth.com/mod/signwarpcreate).
