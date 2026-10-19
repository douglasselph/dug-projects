# Leaf & Let Die — TTS Artwork Assets

This directory contains **local exports** used to build the Tabletop Simulator (TTS) version of *Leaf & Let Die*. The images are **not versioned in Git**: they can be downloaded again from their authoritative sources. This README **is versioned in Git** so the directory can be reconstructed on a new computer.

## Sources of truth

| Source | Assets to export or download |
| --- | --- |
| **Component Studio** — *Leaf & Let Die* designs | **Plant Cards, Round Cards, and Wisp Cards only** |
| **Google Drive** — `components` folder | **All other artwork**: boards, box cover, butterflies, rulebook/reference material, player components, tokens, and trays |

Do not assume a TTS saved-game file contains the artwork itself. Custom TTS objects generally store image/model **references**. For a game shared with a remote reviewer, those references must resolve to files accessible to the reviewer, not just paths on this computer.

## Expected layout

Run these instructions from the `leaf-tts/` project root. Restore the following relative paths (filenames may change as designs are revised):

```text
assets/
├── README.md                 # This document — committed to Git
├── plants/
│   ├── roots/                # Component Studio
│   ├── vines/                # Component Studio
│   └── flowers/              # Component Studio
├── rounds/                   # Component Studio
├── wisps/                    # Component Studio
├── boards/                   # Google Drive/components
├── box/                      # Google Drive/components
├── butterflies/              # Google Drive/components
├── doc/                      # Google Drive/components; rulebook PDF
├── help/                     # Google Drive/components; reference cards
├── player/                   # Google Drive/components
├── tokens/                   # Google Drive/components
└── trays/                    # Google Drive/components
```

The `models/` directory at the project root is separate: it will eventually hold the game-box and tray 3D models and related sources. Native TTS polyhedral dice do not need artwork exports.

## 1. Export cards from Component Studio

Open the current *Leaf & Let Die* project in Component Studio and export each of the following component groups:

1. **Plant Cards**: 12 Roots, 12 Vines, and 12 Flowers. Export **both face and back** for every design. Extract the images to `assets/plants/roots/`, `assets/plants/vines/`, and `assets/plants/flowers/`, respectively.
2. **Round Cards**: six Cultivation/Resource designs (two copies of each) and six Battle designs (one copy of each). Export **faces and backs** to `assets/rounds/`.
3. **Wisp Cards**: export each Wisp face and the common Wisp back to `assets/wisps/`.

Use the Component Studio **ZIP/image export** to download files locally. Choose an image format that preserves quality: JPG is appropriate for opaque photographic/illustrated artwork; PNG is preferred when transparency or lossless edges are important. If your export dialog presents different names or options, the requirement is still the same: **full-resolution fronts and backs of current designs**, including their quantities in the exported names or accompanying metadata.

Preserve the Component Studio filenames where possible. The preparation script uses patterns such as:

```text
Root_05_01[face,6].jpg
Root_05_01[back,6].jpg
Battle_Barkskin_Flow[face,1].jpg
Resource_Compost_Mulch[face,2].jpg
Wisp_Gain_Green[face,4].jpg
_back.jpg
```

The suffix `[face,6]` denotes the side and intended number of copies. The manifest generator uses these quantities; it does **not** require six separate image files for a six-card Plant stack. Confirm the real downloaded filename/extension rather than renaming `.png` to `.jpg` without converting the image.

For Wisp cards, include the common `_back` image, even if each face is a separate file.

## 2. Download the remaining artwork from Google Drive

Open the **`components`** folder for *Leaf & Let Die* in Google Drive. Download current/release-appropriate versions of the following items and put them in these local destinations:

| Destination | Artwork to restore |
| --- | --- |
| `assets/boards/` | Battle Glade board (older file may still say “Battle Grid”) |
| `assets/box/` | Box cover (`cover_main`); later add side/back/interior artwork as available |
| `assets/butterflies/` | Green, Purple, Red, Yellow butterfly faces and backs |
| `assets/doc/` | Current rulebook PDF (e.g., `Leaf_and_Let_Die_v37-01.pdf`) |
| `assets/help/` | Cultivation Round, Battle Round, Helpers & Tokens, Common Rules reference-card artwork, as currently available |
| `assets/player/` | Four Plant Core colors, Player Dice Supply, Dice Discard Bin, and Gnome images |
| `assets/tokens/` | Bee, Worm, Water, Mulch, Sunlight, Victory Point denominations, and four player-color Main Action tokens |
| `assets/trays/` | Two Graft Bed/Dice Tray strips, Token Tray, Wisp & Butterfly Tray, VP Tray |

Use **Download** in Google Drive, then extract ZIPs if Drive packages multiple selected files together. Copy the actual images into their destination directories; do not leave them nested within an extra downloaded `components/` directory unless the build scripts are updated for that layout.

If the latest physical component does not yet have finished artwork, record that fact instead of silently using an older image. Files from Google Drive may keep legacy labels (for example “Battle Grid”); check the image contents against the current rulebook before considering them ready for release.

## 3. Validate the restored assets

From the `leaf-tts/` project root:

```bash
python3 tools/prepare_tts_assets.py
```

The script scans `assets/` and generates these files under `output/preparation/`:

- `tts_asset_manifest.json` — card identities, copies, image paths, and other inventory entries
- `tts_card_inventory.csv` — reviewable card inventory
- `tts_asset_report.md` — validation summary and warnings
- `tts_hosting_template.json` — artwork paths awaiting remote URLs

For the current card set, expect **36 Plant designs × 6 = 216 cards**, **12 Cultivation + 6 Battle = 18 Round Cards**, and **32 Wisps** (a total of **266 cards**). Check for missing fronts/backs and bad filenames before continuing. Counts for dice, tokens, and physical tray contents will be managed in the component configuration/build stages rather than inferred from their PNG/JPG filenames.

To test a locally generated TTS save (once the baseline save and generator are installed):

```bash
python3 tools/build_tts_save.py --install
```

This test uses local asset paths where configured. **It is not yet suitable for remote distribution.**

## 4. Publish assets for the remote reviewer (release phase)

A remotely loaded TTS save cannot depend on `Z:/home/...`, `/home/...`, or other paths on this computer.

1. Publish finalized image assets via a stable, publicly retrievable image host usable by TTS (for example, a verified Component Studio TTS image cache for card sheets or Steam Cloud uploads for compatible assets). Upload models/textures as required once custom box and tray meshes exist.
2. Populate the hosting URL mapping produced from `tts_hosting_template.json`. The build generator must then be extended/configured to use those hosted URLs in place of local paths.
3. **Freeze a release snapshot** of the exact images, model files, manifest, and generated save used for the professional reviewer. Keep that snapshot in an appropriately backed-up location such as a versioned Google Drive folder. Component Studio and Google Drive may change later.
4. Test the exported save using a second computer or a fresh TTS cache, ideally via the reviewer distribution method. Verify every card face/back, board, token, tray texture, and model loads.

Uploading a Git repository is **not** itself the same as publishing TTS assets. The finished TTS save or Workshop item still needs valid hosted asset URLs and sharing permissions.

## 5. Git policy

The project `.gitignore` ignores PNG and JPG/JPEG artwork under `assets/`, **not this README**. Keep build scripts, manifests/configuration, and instructions in Git; keep current exported artwork available locally and recoverable from its source. The current rulebook PDF is **not excluded** by the JPG/PNG ignore rules; exclude it separately if you don't want the large PDF committed.

If some artwork has already been committed, `.gitignore` alone will not untrack it. To stop tracking existing image files **without deleting your local copies**, from the project root you can use:

```bash
git ls-files -z assets/ | grep -zEi '\.(png|jpe?g)$' | xargs -0 -r git rm --cached --
```

Review `git status` before committing. This removes the files from the *new commit's tracked set*, but **does not erase their earlier Git history**.

## 6. Rebuild checklist for a new computer

- [ ] Clone the `leaf-tts` Git project (scripts and configuration).
- [ ] Export Plant, Round, and Wisp artwork from Component Studio.
- [ ] Download all remaining current artwork from Google Drive's `components` folder.
- [ ] Place artwork into the `assets/` directories shown above.
- [ ] Run `python3 tools/prepare_tts_assets.py` and resolve validation errors.
- [ ] Generate a **local test save**, or use hosted asset URLs for a remote/reviewer release.
- [ ] Load the game in TTS, test contents and images, and verify remotely before distribution.
