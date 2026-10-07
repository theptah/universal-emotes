# Bundle Packages - Limits
This section summarizes all mod constraints and limits in one place.

## 5. Limit Tables

### 5.1 Bundle files

| Limit | Value |
| --- | --- |
| ZIP file size | 100 MB |
| Total uncompressed size per ZIP | 250 MB |
| Single JSON file | 4 MB |
| Emotes per ZIP | 256 |
| Parts per collection | 64 |
| Compression ratio (files over 1 MB) | 200:1 |
| `schema_version` | `1` |

### 5.2 Emotes

| Limit | Value |
| --- | --- |
| `op` | `0` – `4` |
| `animation_length` | Greater than `0`, up to 30 minutes |
| `loop_start` | `0` ≤ value < `animation_length` |
| Keyframes per bone channel | 20,000 |

### 5.3 Events

| Limit | Value |
| --- | --- |
| Events per emote | 10,000 |
| `time` | `0` – `animation_length` |
| `volume` | `0` – `4` |
| `pitch`  | Greater than `0`, up to `4` |
| `amount`  | `1` – `1024` |
| `keyframes` per `render_model` | 1,024 |

### 5.4 Runtime

| Limit | Value |
| --- | --- |
| Custom particles on screen | 4,096 |
| Custom particle lifetime | 1 second |
| Models on screen | 256 |
| Audio streaming threshold | Files over 512 KB are streamed |

<br/>
<br/>
<br/>
<br/>

\>>> That's it, now go to next step by [clicking here](troubleshooting.md).