# Bundle Packages - Animation Events

This section covers the **optional** `events.json` file, which allows you to render custom models, trigger visual effects, and execute scripted instructions.

## 4. Events Structure

Below is an example of a properly structured events.json file.

```json
{
  "events": [
    {
      "time": 1.5,
      "type": "<event_type>"
    }
  ]
}
```

- The root object of [JSON file](https://en.wikipedia.org/wiki/JSON) contains an array named `events`, where each keyframe event is defined as an object:
	- `time`: The keyframe timestamp in seconds. It must be between `0` and `animation_length`.
	- `type`: Specifies the instruction or event to trigger at the given time."
- **It supports up to 10,000 events per emote, and the file size must not exceed 4 MB.**

### 4.1 Event Types

| Type | Description |
| --- | --- |
| [`play_music`](#4121-play_music) | Plays background music that follows the emote |
| [`play_sound`](#4122-play_sound) | Plays a one-shot sound effect |
| [`spawn_particle`](#413-spawn_particle) | Spawns vanilla or custom particles |
| [`set_skin`](#414-set_skin) | Changes the player's skin during emote |
| [`reset_skin`](#415-reset_skin) | Restores the player's own skin |
| [`render_model`](#416-render_model) | Shows a 3D model |

#### 4.1.2 Sound Events

##### 4.1.2.1 play_music

Plays background music for the emote. 

- Only one music track plays at a time. If music is already playing, the next `play_music` is queued and starts when the current one ends.

Example Usage:
```json
{
  "time": 0.0,
  "type": "play_music",
  "asset_id": "<your_namespace>:<your_assetname>",
  "volume": 1.0,
  "dmca": false
}
```

Extra Fields:

| Field | Required | Default | Description |
| --- | --- | --- | --- |
| `asset_id` | ✔ | | Audio file → `assets/audio/<path>.ogg` |
| `volume` |  | `1.0` | Between `0` and `4` |
| `dmca` | | `false` | Copyright flag, see about [DMCA field](#423-dmca-field) |

##### 4.1.2.2 play_sound

Plays a sound effect. 

- Multiple sounds can play at the same time.

Example Usage:
```json
{
  "time": 0.5,
  "type": "play_sound",
  "asset_id": "minecraft:entity.player.levelup",
  "volume": 1.0,
  "pitch": 1.0,
  "dmca": false
}
```

Extra Fields:

| Field | Required | Default | Description |
| --- | --- | --- | --- |
| `asset_id` | ✔ | | Your own audio (`assets/audio/<path>.ogg`) or a vanilla sound |
| `volume` | | `1.0` | Between `0` and `4` |
| `pitch` | | `1.0` | Greater than `0`, up to `4`. Invalid values fall back to `1.0` |
| `dmca` | | `false` | Copyright flag, see about [DMCA field](#423-dmca-field) |


##### 4.1.2.3 DMCA Field

| Value | Normal mode | DMCA mode |
| --- | --- | --- |
| `false` or not set | Plays | Plays |
| `true` | Plays | Silent |
| `"<namespace>:<alternative_audio>"` | Plays the original | Plays `<alternative_audio>` instead |

#### 4.1.3 spawn_particle

Spawns vanilla or custom particles

- Particles spawn at chest height and follow the direction the player is facing.
- Without `pos`, particles spread over a wider area. Without `rot`, they slowly drift upward.
- Only simple vanilla particles are supported (particles that need extra options, like `minecraft:dust`, don't work).
- Custom texture particles are flat billboards that last 1 second.
- Players can hide particles with the **Hide particle effects** setting.

Example Usage:

```json
{
  "time": 1.0,
  "type": "spawn_particle",
  "asset_id": "minecraft:heart",
  "amount": 5,
  "pos": [0, 4, 8],
  "rot": [-30, 0, 0]
}
```

Extra Fields:

| Field | Required | Default | Description |
| --- | --- | --- | --- |
| `asset_id` | ✔ | | Vanilla particle (`minecraft:heart`) or a custom texture → `assets/textures/<path>.png` |
| `amount` | | `1` | Number of particles, `1`–`1024` |
| `pos` | | | `[x, y, z]` offset in pixels (1/16 block). `x` = right, `y` = up, `z` = forward |
| `rot` | | | `[pitch, yaw, roll]` in degrees. Particles move in this direction |


#### 4.1.4 set_skin

It changes the player's skin during emote.

- The original skin is restored automatically when the emote ends.

Example Usage:

```json
{
  "time": 0.0,
  "type": "set_skin",
  "asset_id": "myname:outfit",
  "model": "slim"
}
```

Extra Fields:

| Field | Required | Default | Description |
| --- | --- | --- | --- |
| `asset_id` | ✔ | | Skin texture → `assets/textures/<path>.png` |
| `model` | | | Arm model: `slim` (`thin`, `alex`) or `default` (`wide`, `classic`, `steve`). If not set, the arm model doesn't change |

#### 4.1.5 reset_skin

Restores the player's own skin during emote.

```json
{
  "time": 3.0,
  "type": "reset_skin"
}
```

#### 4.1.6 render_model

Displays a 3D model during the emote.

```json
{
  "time": 0.0,
  "type": "render_model",
  "asset_id": "myname:guitar",
  "texture": "guitar_red",
  "attach_mode": "model_root",
  "pos": [0, 12, 4],
  "rot": [0, 90, 0],
  "scale": 1.0,
  "end": 4.0
}
```

| Field | Required | Default | Description |
| --- | --- | --- | --- |
| `asset_id` | ✔ | | Model file → `assets/models/<path>.json` |
| `texture` | | model file name | Texture name → `assets/textures/<name>.png` |
| `attach_mode` | | `bone` | `bone`, `model_root` or `world` |
| `bone` | | | Bone to place the model at, used with `attach_mode: bone` |
| `pos` | | | `[x, y, z]` offset in pixels |
| `rot` | | | `[x, y, z]` rotation in degrees |
| `scale` | | `1.0` | Model scale |
| `end` | | | Emote time in seconds when the model disappears |
| `keyframes` | | | Animates the model, see below |

##### 4.1.6.1 Attach modes

| Mode | Behavior |
| --- | --- |
| `world` | Stays where the player was when the event fired |
| `model_root` | Follows the player |
| `bone` | Follows the player and is placed at the given bone's position |

> With `bone`, the model is placed at the bone's position but does not follow the bone's animation.

##### 4.1.6.2 Keyframes

```json
"keyframes": [
  { "time": 0.0, "pos": [0, 0, 0], "rot": [0, 0, 0], "scale": 1.0 },
  { "time": 2.0, "pos": [0, 16, 0], "rot": [0, 360, 0], "scale": 2.0, "lerp_mode": "catmullrom" }
]
```

| Field | Description |
| --- | --- |
| `time` | Emote time in seconds (not relative to the event) |
| `pos` | `[x, y, z]` offset in pixels |
| `rot` | `[x, y, z]` rotation in degrees |
| `scale` | Model scale |
| `lerp_mode` | `linear` (default), `catmullrom` / `smooth`, `step` |

- At least two keyframes are needed to animate; up to 1024 are allowed.
- Model textures must be directly in `assets/textures/` (no subfolders).
- Supported model formats: Blockbench Bedrock geometry and Java block models.
- Without `end`, the model stays until the emote ends.

<br/>
<br/>
<br/>
<br/>

\>>> That's it, now go to next step by [clicking here](limits.md).