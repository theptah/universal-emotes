# Bundle Packages - Animation Data

This topic dives into the `animation.json` file, as promised in the previous section.

## 3. Animation Data

```json
{
  "format_version": "1.8.0",
  "animations": {
    "<animation_name>": {
      "animation_length": 1.5,
      "loop": false,
      "loop_start": 0.0,
      "bones": {
        "<bone_name>": {
          "rotation": {
            "0.0": [0, 0, 0]
          },
          "position": {
            "0.0": [0, 0, 0]
          }
        }
      }
    }
  }
}
```

- `format_version`: Generated automatically by the exporter (current supported version: 1.8.0). Omitting it triggers an error, while a mismatched version raises a warning.

- `animations` -> `<animation_name>`: This key must match the animation field defined in your `info.json`. All keyframe data, poses, and timing values are contained within this object.

- `animation_length`: The total animation duration in seconds. Must be greater than 0 and no longer than 30 minutes (1800 seconds).

- `bones`: Contains transformation data for individual model bones throughout the timeline. Within this object, specify `<bone_name>` for each target bone to animate ([See Bone List](#4-bones)), followed by its corresponding transformation channels ([details in next title](#5-transformation-channels)).

- `loop`: Determines whether the animation loops. The value can be either true or false.

- `loop_start`: When loop is set to true, this defines the timestamp (in seconds) where the animation restarts after its initial playback. **This also applies to events**, any events scheduled before this timestamp will not be triggered during subsequent loops.

### 3.1 Bones

Animations in the Universal Emotes mod are divided into two formats: **R6** and **R15**. As implied by their names, **R6 utilizes the standard 6-bone player model**, whereas **R15 takes advantage of a custom 15-bone rig rendered by the mod** to enable richer, more better animations.

- Here is Bone list for every Rig Type supports:

| Bone Name | R15 | R6 |
| --- | --- | --- |
| Root | ✔ | ✔ |
| Head | ✔ | ✔ |
| Torso | ❌ | ✔ |
| RightArm | ❌ | ✔ |
| LeftArm | ❌ | ✔ |
| RightLeg | ❌ | ✔ |
| LeftLeg | ❌ | ✔ |
| LowerTorso | ✔ | ❌ |
| UpperTorso | ✔ | ❌ |
| RightUpperArm | ✔ | ❌ |
| LeftUpperArm | ✔ | ❌ |
| RightLowerArm | ✔ | ❌ |
| LeftLowerArm | ✔ | ❌ |
| LeftHand | ✔ | ❌ |
| RightHand | ✔ | ❌ |
| RightUpperLeg | ✔ | ❌ |
| LeftUpperLeg | ✔ | ❌ |
| RightLowerLeg | ✔ | ❌ |
| LeftLowerLeg | ✔ | ❌ |
| LeftFoot | ✔ | ❌ |
| RightFoot | ✔ | ❌ |

- `Root` moves and rotates the whole body.
- `RightItem` & `LeftItem` are reserved for held-item animations. **They are accepted but currently have no visible effect.**
- Only use bones of your bundle's rig. Bones from the other rig are accepted but ignored.
- Unknown bone names are skipped with a warning.
- In `r15`, bones follow a hierarchy (e.g. `RightHand` → `RightLowerArm` → `RightUpperArm`), so moving a parent also moves its children.

### 3.2 Transformation Channels

- Here is list of all transformation channels which currently supported:

| Channel Name | Value Format | Description | Rig Support |
| --- | --- | --- | --- |
| `rotation` | `[X, Y, Z]` | Specifies bone rotation in degrees across each axis. | Both |
| `position` | `[X, Y, Z]` | Defines translation offsets measured in pixels (1/16th of a block) along each axis. | Both |
| `bend` | `[angle, axis]` | Adds smooth joint bending for more natural movement. Only supported on `Torso`, `Arms`, and `Legs` bones. | R6 |

### 3.3 Keyframe Structure

Each transformation channel holds a timeline dictionary where:

- **Key (Timestamp):** The time in seconds, written as a string (e.g., `"0.0"`, `"0.5"`). It must be between `0` and `animation_length`, **supporting up to 20,000 keyframes per channel.**
- **Value:** The channel's value at that timestamp. 

For example:

```json
"RightArm": {
  "rotation": {
    "0.0": [0.0, 0.0, 0.0],
    "0.5": [0.0, 0.0, 90.0]
  },
  "position": {
    "0.0": [0.0, 0.0, 0.0],
    "0.5": [0.0, 2.0, 0.0]
  }
}
```

#### 3.3.1 Object Form

A keyframe can also be written as an object. This is required when you want to set an interpolation mode:

```json
"RightArm": {
  "rotation": {
    "0.5": {
      "vector": [0, 0, 90],
      "lerp_mode": "catmullrom"
    }
  }
}
```

- **`vector`:** The keyframe value. `post` or `pre` are also accepted for Blockbench compatibility (priority: `vector` → `post` → `pre`).
- **`lerp_mode`:** How the value moves to the next keyframe. Defaults to `linear`.

| `lerp_mode` | Behavior |
| --- | --- |
| `linear` | Straight transition (default) |
| `catmullrom` / `smooth` | Smooth, curved transition |
| `step` | No transition; the value holds, then jumps at the next keyframe |

> **Note:** Only one value is read per keyframe. Keyframes with separate `pre` and `post` values (instant jumps) are not supported; `pre` is ignored. Unknown `lerp_mode` values, including `bezier`, fall back to `linear`.

<br/>
<br/>
<br/>
<br/>

\>>> That's it, now go to next step by [clicking here](animation-events.md).