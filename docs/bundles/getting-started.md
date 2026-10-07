# Bundle Packages - Getting Started

In this topic, you'll discover the contents of Emote Bundles including supported file types and formats and how to effortlessly build your own.

## 1. Structure

The bundle contents must match the structure below, **do not** place the files inside an inner folder within the package!

```
My First Bundle.zip
├── pack.json
├── icon.png					(optional)
├── emotes/
│   └── wave/
│       ├── info.json
│       ├── animation.json
│       └── events.json        	(optional)
└── assets/                    	(optional)
    ├── audio/				   	(optional)
    ├── textures/				(optional)
    └── models/					(optional)
```

- Each emote must have its own folder inside the `emotes/` directory representing that emote and containing its data. **A single bundle supports up to 256 emotes.**
- The `icon.png` file is the in-game square thumbnail for your bundle. While optional, **it must be a 1:1 square with a maximum resolution of 512x512 pixels and cannot exceed 512 KB**.

### `pack.json`

This defines your bundle's metadata and core configuration, allowing the mod to correctly identify and load your pack:

```json
{
  "schema_version": 1,
  "min_mod_version": "1.0.0",
  "bundle": {
    "namespace": "myname",
    "id": "starter",
    "title": "Starter Emotes",
    "author": "MyName",
    "version": "1.0.0",
    "description": "My first emotes",
	"rig_type": "r6"
  }
}
```

- `schema_version`: The format version of this configuration file, just set it to `1`.
- `min_mod_version`: Specifies the minimum mod version supported by the bundle. Shortly, it prevents features introduced in newer mod versions from causing crashes or errors on older, unsupported versions.
- `namespace`: A **unique** identifier for you or your project to prevent ID collisions. You will access assets by using the name that you set for this key.
- `id`: The unique technical identifier of this bundle.
- `title`: The title of your emote bundle.
- `author`: Your name or handle.
- `version`: The version of your emote bundle; make sure to update this with every release.
- `description`: The in-game description text displayed for your emote bundle.
- `rig_type`: Specifies the rig format for emotes within the bundle. Defaults to R6.  **The following step gives full details about this.**

### `emotes/wave/animation.json`
> **NOTE:** Below is an example JSON structure representing how animation data is stored and you to test. **The following step details how this file created.**

```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.wave": {
      "animation_length": 1.5,
      "bones": {
        "RightArm": {
          "rotation": {
            "0.0": [0, 0, 0],
            "0.5": [0, 0, 150],
            "1.0": [0, 0, 120],
            "1.5": [0, 0, 0]
          }
        }
      }
    }
  }
}
```

### `emotes/wave/info.json`

```json
{
  "id": "wave",
  "animation": "animation.wave",
  "name": "Wave",
  "rarity": "common"
}
```

- `id`: The unique technical identifier for your emote, used in-game to recognize it. It must be **unique within this bundle's namespace**, contain **no spaces**, and **only use lowercase alphanumeric** and allowed path characters (`a-z`, `0-9`, `_`, `.`, `-`, `/`).
- `animation`: The value of this field must exactly match the key specified inside the `animations: {}` object within the `animation.json` file.
- `name`: Sets the display name of the emote shown in-game.
- `rarity`: This key must be set to one of 5 possible rarity values **(case-insensitive)**. It defines your emote's category in the Emote Wheel menu and sets the **required in-game experience level needed to unlock it**. The rarities and their default XP thresholds are shown below.

| Rarity 			| Level Required |
| ---    			| --- 			 |
| `legendary` 		| 50 			 |
| `rare` 			| 25			 |
| `uncommon`		| 12 			 |
| `common` 		  	| 5  			 |
| `complementary` 	| 0  			 |

## 2. Lets Test

1. ZIP the files **(the contents, not the parent folder)**.
2. Put it in `config/universal-emotes/bundles/`.
3. Start the game, or if games already running `/universal-emotes reload` in a world.
4. Now check Emote Wheel menu and play it. Check the log for lines starting with `Bundle [` if the emote doesn't appear.

<br/>
<br/>
<br/>
<br/>

\>>> That's it, now go to next step by [clicking here](animation-data.md).