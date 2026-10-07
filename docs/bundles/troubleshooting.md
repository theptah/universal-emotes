# Universal Emotes - Troubleshooting

## 6. Issues & Solutions

Bundle problems are written to the game or server log in this format:
​
```
[ERROR] Bundle [<source>] <message>
[WARN]  Bundle [<source>] <message>
```

- **`<source>`** is the file or ZIP the problem was found in (e.g. `emotes/wave/info.json`, `my-bundle.zip`, `collection myname:megapack`).
- **ERROR** – the bundle (or the whole collection) is **not loaded**. A single error in any file rejects the entire bundle.
- **WARN** – the bundle loads; the mod skips or replaces the problematic value.
- You don't need to close game after fix just use `/universal-emotes reload` command.

### Fast Travel
- [6.1 ZIP Files](#61-zip-files)
- [6.2 JSON Files](#62-json-files)
- [6.3 Common field errors](#63-common-field-errors)
- [6.4 pack.json](#64-packjson)
- [6.5 Bundles](#65-bundles)
- [6.6 info.json](#66-infojson)
- [6.7 animation.json](#67-animationjson)
- [6.8 events.json](#68-eventsjson)
- [6.9 Assets](#69-assets)
- [6.10 Collections](#610-collections)
- [6.11 Server sync](#611-server-sync)
- [6.12 Support And Feedback](#612-support--feedback)

### 6.1 ZIP Files
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `ZIP missing or larger than limit` | The ZIP is larger than 100 MB or can't be read | Reduce the size or split the bundle into a [collection](collections.md) |
| ERROR | `Expanded ZIP exceeds limit` | Uncompressed contents exceed 250 MB | Reduce asset sizes or use a [collection](collections.md) |
| ERROR | `Unsafe ZIP entry: <name>` | An entry uses an absolute path, `..`, `\` or other unsafe characters | Re-create the ZIP with normal relative paths |
| ERROR | `Duplicate ZIP entry: <name>` | The same path appears twice in the ZIP | Re-create the ZIP |
| ERROR | `ZIP entry has unknown size: <name>` | The ZIP tool didn't store file sizes (streamed ZIP) | Re-create the ZIP with a standard tool (7-Zip, Windows, `zip`) |
| ERROR | `Suspicious compression ratio: <name>` | A file over 1 MB compresses more than 200:1 (ZIP bomb protection) | Check the file; remove padding or empty data |
| ERROR | `pack.json must be at ZIP root` | `pack.json` is inside a folder | ZIP the files themselves, not the folder that contains them |
| WARN | `icon.png is missing` | No `icon.png` at the ZIP root | Add a square `icon.png` |
​
### 6.2 JSON Files
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Required JSON missing` | `pack.json`, `info.json` or `animation.json` doesn't exist | Add the file at the correct path |
| ERROR | `Invalid JSON: <details>` | Syntax error (missing comma, quote, bracket…) or the root isn't an object `{ }` | Validate the file with a JSON validator |
| ERROR | `Invalid JSON: File exceeds limit` | The JSON file is larger than 4 MB | Reduce keyframes or split the animation |
​
### 6.3 Common field errors
​
These messages can appear in any file. `<key>` tells you which field is wrong.
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Missing or invalid string: <key>` | A required text field is missing, empty, whitespace-only or not a string | Fill in the field as text |
| ERROR | `Missing required integer: <key>` | A required number is missing | Add the number |
| ERROR | `Invalid integer <key> (expected <min>..<max>)` | The number is outside the allowed range or not a number | Use a value in the shown range |
| ERROR | `Invalid resource id: <namespace>:<path>` | An id contains invalid characters (uppercase, spaces, special characters) | Use lowercase `a-z 0-9 _ . -` (and `/` in paths) |
| ERROR | `<key> must be semantic version x.y.z` | A version isn't in `x.y.z` format | Use versions like `1.0.0` |
​
### 6.4 pack.json
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Missing required integer: schema_version` | `schema_version` is missing | Add `"schema_version": 1` |
| ERROR | `Invalid integer schema_version (expected 1..1)` | Unsupported schema version | Use `1` |
| ERROR | `Missing or invalid string: min_mod_version` | `min_mod_version` is missing | Add e.g. `"min_mod_version": "1.0.0"` |
| ERROR | `Missing or invalid string: namespace` / `id` / `title` / `author` / `version` | A required `bundle` field is missing or `bundle` itself is missing | Add the field inside `"bundle": { }` |
| ERROR | `Requires mod <x>, current is <y>` | The installed mod is older than `min_mod_version` | Update the mod, or lower `min_mod_version` |
| WARN | `Unknown rig_type '<value>', defaulting to r6` | `rig_type` isn't `r6` or `r15` | Use `r6` or `r15` |
| WARN | `Invalid rig_type, defaulting to r6` | `rig_type` isn't text | Use `"r6"` or `"r15"` |
​
### 6.5 Bundles
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Duplicate bundle id: <namespace>:<id>` | Another installed bundle already uses this `namespace:id` | Change `id`, remove the duplicate ZIP, or turn the bundles into a [collection](collections.md) |
| ERROR | `Too many emotes` | More than 256 emote folders in one ZIP | Split the bundle into a [collection](collections.md) |
| ERROR | `Bundle contains no valid emotes` | Every emote failed to load, or there are none | Fix the earlier errors; make sure emotes are at `emotes/<folder>/info.json` |
​
### 6.6 info.json
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Missing or invalid string: id` / `animation` / `name` | A required field is missing or empty | Add the field |
| ERROR | `Missing or invalid string: rarity` + `Invalid rarity` | `rarity` is missing | Add `rarity` |
| ERROR | `Invalid rarity` | `rarity` isn't a known value | Use `legendary`, `rare`, `uncommon`, `common` or `complementary` |
| ERROR | `Invalid integer op (expected 0..4)` | `op` is outside `0`–`4` | Use `0`–`4` |
| ERROR | `Duplicate emote id: <namespace>:<id>` | Two emotes share the same `id`, or another bundle already registered it | Give every emote a unique `id` |
​
> Emote folders must be directly inside `emotes/`. Nested folders like `emotes/dance/wave/info.json` are ignored without an error.
​
### 6.7 animation.json
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Missing format_version` | `format_version` is missing | Add `"format_version": "1.8.0"` |
| WARN | `Unrecognized format_version '<v>' (supported: [1.8.0]); parsing anyway` | A different format version is used | Re-export from Blockbench as Bedrock `1.8.0` |
| ERROR | `Missing object: animations` | `animations` is missing or not an object | Export as a Bedrock animation |
| ERROR | `Animation key not found: <key>` | `info.json → animation` doesn't match a key in `animations` | Make the names identical (case-sensitive) |
| ERROR | `animation_length must be > 0 and <= 1800.0` | `animation_length` is missing, zero, negative or over 30 minutes | Set a positive length in seconds |
| WARN | `loop_start ignored (needs a looping clip and 0 <= loop_start < animation_length)` | `loop_start` is used without `loop: true` or is out of range | Set `"loop": true` and keep `loop_start` below `animation_length` |
| WARN | `Unsupported bone skipped: <bone>` | Unknown bone name | Use a [supported bone](emotes.md#bones); names are case-sensitive |
| ERROR | `Bone must be an object: <bone>` | A bone's value isn't `{ }` | Write the bone as an object with channels |
| ERROR | `rotation must be a keyframe object` / `position must be a keyframe object` | A channel is a plain array (static value) | Use keyframes: `"rotation": { "0.0": [0, 0, 0] }` |
| ERROR | `Invalid rotation keyframe at <time>` / `Invalid position keyframe at <time>` | Time isn't a number, is negative or over 30 minutes; value isn't 3 numbers; or uses Molang | Use numeric times and `[x, y, z]` numbers |
| ERROR | `Too many keyframes in rotation` / `position` | More than 20,000 keyframes in one channel | Reduce keyframes (bake at a lower FPS) |
| ERROR | `bend must be a keyframe object` | `bend` isn't an object | Use keyframes: `"bend": { "0.0": [0, 0] }` |
| ERROR | `Invalid bend keyframe at <time>` | The value isn't 2 numbers | Use `[amount, axis]` |
| ERROR | `Too many keyframes in bend` | More than 20,000 bend keyframes | Reduce keyframes |
​
### 6.8 events.json
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `events must be an array` | The root doesn't contain an `events` array | Use `{ "events": [ ... ] }` |
| ERROR | `Too many events: <n>` | More than 10,000 events | Reduce events |
| ERROR | `Event must be an object` | An item in `events` isn't `{ }` | Write every event as an object |
| ERROR | `Invalid event` | `time` or `type` is missing | Add both fields |
| ERROR | `Invalid time` | `time` is negative or larger than `animation_length` | Keep `time` between `0` and `animation_length` |
| ERROR | `For input string: "<value>"` | `time` or `volume` isn't a number | Use numbers, not text |
| ERROR | `Unknown event type: <type>` | `type` isn't supported | Use `play_music`, `play_sound`, `spawn_particle`, `set_skin`, `reset_skin` or `render_model` |
| ERROR | `Invalid volume` | `volume` is outside `0`–`4` | Use `0`–`4` |
| ERROR | `Invalid integer amount (expected 1..1024)` | `spawn_particle` `amount` is out of range | Use `1`–`1024` |
| ERROR | `Missing or invalid string: asset_id` | `asset_id` is missing | Add `asset_id` |
| ERROR | `asset_id must be namespace:path` | `asset_id` has no `:` or an empty side | Use `myname:file` |
| WARN | `keyframes must be an array; model stays static` | `render_model` `keyframes` isn't an array | Use `"keyframes": [ { ... } ]` |
| WARN | `Too many model keyframes (<n>); model stays static` | More than 1,024 model keyframes | Reduce keyframes |
| WARN | `Keyframe must be an object; skipped` | A model keyframe isn't `{ }` | Write it as an object |
| WARN | `Invalid keyframe time; skipped` | A model keyframe's `time` is missing or outside `0`–`animation_length` | Fix the `time` |
​
### 6.9 Assets
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `Referenced asset is missing: <path>` | The file for an `asset_id` (or DMCA alternative) doesn't exist | Put it at the [expected path](assets.md#asset-ids) |
| ERROR | `Cross-bundle asset reference is not allowed: <id>` | `asset_id` uses another bundle's namespace | Use your own namespace or `minecraft` |
​
These problems are **not** reported while loading. They only show up in game:
​
| Symptom | Cause | Solution |
| --- | --- | --- |
| Custom particle doesn't appear | `assets/textures/<path>.png` is missing | Add the texture |
| Model has no texture | `assets/textures/<name>.png` is missing, or it's in a subfolder | Put the texture directly in `assets/textures/` |
| `render_model: model file not found: <file>` in the log | Model file couldn't be loaded on the client | Check the model path |
| `render_model: <file> has no renderable geometry (cubes / poly_mesh / elements)` | The model is empty or in an unsupported format | Export as Bedrock geometry or a Java block model |
| `render_model: could not read <file>` | The model JSON is broken | Re-export the model |
| `Duplicate bundle audio id <id>; first file wins` | Two bundles provide the same audio id | Use unique namespaces |
| Copyrighted audio is silent | Player uses DMCA mode and there's no alternative | Add a DMCA alternative: `"dmca": "myname:safe_song"` |
​
### 6.10 Collections
​
When any of these occur, the log ends with `Collection rejected` and **no part** of the collection is loaded.
​
| Level | Message | Cause | Solution |
| --- | --- | --- | --- |
| ERROR | `collection must be an object` | `collection` isn't `{ }` | Use `"collection": { "total": 2, "order": 1 }` |
| ERROR | `Missing required integer: total` / `order` | A collection field is missing | Add both `total` and `order` |
| ERROR | `Invalid integer total (expected 1..64)` | `total` is out of range | Use `1`–`64` |
| ERROR | `Invalid integer order (expected 1..<total>)` | `order` is larger than `total` or below `1` | Keep `order` between `1` and `total` |
| WARN | `Unknown collection key: <key>` | Extra field inside `collection` | Only use `total` and `order` |
| ERROR | `Collection is incomplete, missing part(s): [..] of <total>` | Not every part is installed | Install every part from `1` to `total` |
| ERROR | `Duplicate collection part <n> (also <file>)` | Two ZIPs have the same `order` | Give every part a unique `order` |
| ERROR | `pack.json of collection part <n> differs from part <m>` | `pack.json` differs between parts (other than `order`) | Make every `pack.json` identical except `order` |
| ERROR | `Duplicate emote id <id> (also in collection part <n>)` | The same emote exists in two parts | Keep each emote in one part |
| ERROR | `Asset <path> is already provided by collection part <n>` | The same asset file is in two parts | Keep each asset in one part |
| ERROR | `Referenced asset is missing in collection: <path>` | The asset is in none of the parts | Add the file to one of the parts |
| ERROR | `Collection contains no valid emotes` | No part has a valid emote | Add at least one emote |
| ERROR | `Duplicate bundle id: <namespace>:<id>` | A normal bundle and a collection share the same id | Change the id or remove one of them |
​
### 6.11 Server sync
​
These appear in the server or client log with the `[Emote Sync]` prefix.
​
| Side | Message | Cause | Solution |
| --- | --- | --- | --- |
| Server | `Client <name> has no bundle sync receiver` | The player doesn't have the mod (or has an incompatible version) | Install the same mod version on the client |
| Client | `Protocol mismatch server=<x> client=<y>` | Server and client mod versions are incompatible | Use the same mod version |
| Server | `Delta failed for <name>` + `Sync delta exceeds session limit` | More than 250 MB had to be sent at once | The player should reconnect; the rest is downloaded next time |
| Server | `Delta failed for <name>` + `Asset exceeds sync limit` | A single asset is larger than 100 MB | Make the file smaller |
| Server | `Delta request from <name> without a pending manifest` | The client sent a request out of order | Usually harmless; reconnect if emotes are missing |
| Server | `Failed to index assets` | A bundle ZIP couldn't be read while preparing sync | Check that the ZIP files still exist and aren't locked or corrupted |
| Client | `Invalid definition` | A received emote failed its integrity check | Reconnect; clear `config/universal-emotes/cache/` if it repeats |
| Client | `Asset cache failed: <id>` | A received file couldn't be verified or saved | Check disk space and permissions; clear the cache |
| Client | `Resource reload failed` | Synced assets couldn't be applied | Check earlier errors in the log; clear the cache and reconnect |
​
### 6.12 Support & Feedback

[Create Issue](https://github.com/theptah/universal-emotes/issues/new) here. If the problem persists, reach out for support. To help us troubleshoot faster, make sure you attach your full log:
- If the game crashed: Provide the newest file from crash-reports/.
- If the game did not crash: Provide logs/latest.log. 