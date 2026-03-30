# SBSShot

> 🇯🇵 [日本語はこちら](README_jp.md)

**SBSShot** is a Minecraft mod that captures **Side-by-Side (SBS) stereo screenshots** for VR/HMD devices.

Press **F12** to save a `.pns` (PNG Stereo) file containing left-eye and right-eye views side by side.

---

## Preview

You can view `.pns` files with the **SBS Stereo Viewer**:

👉 **[https://azo234.github.io/sbs-stereo-viewer/](https://azo234.github.io/sbs-stereo-viewer/)**

---

## Features

- Captures SBS stereo screenshots with a single key press (F12)
- Outputs `.pns` (PNG Side-by-Side Stereo) format
- Saved to `.minecraft/screenshots/stereo/stereo_YYYY-MM-DD_HH.mm.ss.pns`
- Configurable parallax (default: 6.5 cm) and output subdirectory
- HUD-free rendering — ideal for VR/HMD use
- Supports **Forge 1.20.1**, **NeoForge 26.1**, and **Fabric 26.1**

---

## Installation

Place the mod `.jar` in your `.minecraft/mods/` folder. No dependencies required.

---

## Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `parallax_cm` | `6.5` | Camera separation in centimeters |
| `output_sub_dir` | `stereo` | Subdirectory inside `screenshots/` |

Settings can be changed in-game via the **Mod Menu** (Fabric) or the **Config** screen (NeoForge/Forge).

---

## Key Binding

| Key | Action |
|-----|--------|
| F12 | Capture SBS stereo screenshot |

---

## Technical Notes

The mod renders the scene twice — once for the left eye and once for the right eye — with a horizontal camera offset matching the configured parallax. Rendering reads directly from the main render target FBO to ensure accurate frame capture.

### Minecraft 26.1 API Notes

- `Camera.setup()` was removed → replaced with `Camera.update(DeltaTracker)` + `Camera.setPosition(Vec3)`
- `renderLevel()` writes to `mainRenderTarget` (FBO), not the screen buffer → FBO ID obtained via `GlTexture#firstFboId`
- `ResourceLocation` → `Identifier`
- `KeyBindingHelper` → `KeyMappingHelper`

---

## License

MIT
