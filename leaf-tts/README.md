# Leaf & Let Die — Tabletop Simulator build system

> **Project handoff / context for ChatGPT and human maintainers.** This repository creates and updates a **Tabletop Simulator (TTS) simulation of the physical retail game**, including cards, dice, trays, four player boxes, a Game Box placard and a readable PDF rulebook and a prototype-photo PDF. The current purpose is a **blind-playtest setup**, not an automated implementation of game rules. TTS users manipulate the pieces themselves.
>
> **Read this file before changing the scripts.** It describes the workflow as it exists in the October 20, 2026 project pack **plus the v27 CSV-driven card patch**. Some older builder docstrings still describe an earlier, pre-tray version of the simulation.

## Quick start: most common tasks

Run these commands **from the `leaf-tts/` project root**, with your Python environment activated if required.

| Goal | Commands |
| --- | --- |
| Install a patch from ChatGPT | `./scripts/unpack.sh ~/Downloads/PATCH.zip` |
| Edit card names/effects in the four CSVs | `./scripts/update_card_data.sh` then `./scripts/publish.sh` |
| Restore the approved Game Box layout / descriptions | `./scripts/rebuild_layout.sh` then `./scripts/publish.sh` |
| Rebuild the Game Box placard and its contents | `./scripts/rebuild_box.sh` then `./scripts/publish.sh` |
| Rebuild all four player boxes | `./scripts/rebuild_player.sh` then `./scripts/publish.sh` |
| Rebuild both dice trays | `./scripts/rebuild_dice.sh` then `./scripts/publish.sh` |
| Rebuild the Critter/nutrient token tray | `./scripts/rebuild_token.sh` then `./scripts/publish.sh` |
| Rebuild the Wisp/Butterfly tray | `./scripts/rebuild_wisp.sh` then `./scripts/publish.sh` |
| Rebuild the Victory Point tray | `./scripts/rebuild_vp.sh` then `./scripts/publish.sh` |
| Share current save and scripts for troubleshooting | `./scripts/pack.sh` (see **Pack limitations** below) |

A typical card-text-only edit is:

```bash
# Edit assets/card_data/*.csv, then:
./scripts/update_card_data.sh
./scripts/publish.sh
```

A typical physical-component edit is:

```bash
# Edit a tray/box builder or its artwork, then:
./scripts/rebuild_player.sh   # replace with the relevant rebuild_*.sh
./scripts/rebuild_layout.sh   # optional: reapply approved saved layout
./scripts/publish.sh
```

**Do not run `publish.sh` between every rebuild** if you are making several changes. Each rebuild normally picks up the staged save and updates it; publish once after reviewing the complete result. Running a rebuild does **not** itself publish to TTS.

## Mental model: three layers

1. **Editable source data and images:** `assets/` contains card artwork, box/tray textures, and **the four canonical card CSVs**. the newest `docs/Leaf_and_Let_Die_vNN-NN.pdf` is the readable rulebook; `docs/Box Illustrations.pdf` is a visual photo guide to the physical prototype, especially the Player Supply and Discard boxes. Both are kept inside the Game Box Sack.
2. **Generated definitions and geometry:** `config/card_catalog_effects.json` is generated from the CSVs. The `scripts/build_*.py` programs create OBJ meshes, MTL files, texture atlases, and updated TTS JSON saves under `generated/`.
3. **Staged and published save:** The working candidate is `output/TS_Save_latest.json`. `publish.sh` copies it to the TTS save directory specified in `config/tts_workflow.conf`. The game is then reloaded in TTS.

The current project has **two distinct build paths**:

- **Initial/fresh game creation:** `tools/prepare_tts_assets.py` inventories card artwork and writes a manifest; `tools/build_tts_save.py` creates a brand-new component-oriented save, assigning readable card names and effects at creation. This is a lower-level starting point and **does not itself rebuild all the later 3D trays or the final hand-arranged Game Box**.
- **Day-to-day incremental maintenance (recommended):** `scripts/rebuild_*.sh` updates an existing published or staged save. `scripts/rebuild_layout.sh` restores the approved hand-arranged positions and descriptions. `scripts/update_card_data.sh` refreshes CSV-based text without recreating pieces.

The initial builder's input is `source/TS_Save_8.json` and its manifest is `output/preparation/tts_asset_manifest.json`. Those are **not** the same as `source/TS_Save_latest.json` or `output/TS_Save_latest.json`. Be careful not to confuse source, staged, and published save files.

## Directory guide

```text
leaf-tts/
├── assets/
│   ├── card_data/                # EDIT THESE four CSVs
│   │   ├── Cultivation_Cards - Root.csv
│   │   ├── Cultivation_Cards - VF.csv
│   │   ├── Round_Cards.csv
│   │   └── Wisp_Cards.csv
│   ├── plants/                   # Root/Vine/Flower card face/back images
│   ├── rounds/                   # Round card art
│   ├── wisps/                    # Wisp card art
│   ├── player/                   # Supply tray and Discard Bin art
│   ├── trays/                    # Tray artwork, including grassy_field
│   └── box/                      # cover_main.png / .jpg / .jpeg
├── config/
│   ├── tts_workflow.conf         # Published save directory and filename
│   ├── card_catalog_effects.json # GENERATED from card CSVs; do not hand-edit
│   ├── game_box_layout.json      # Approved positions, rotations and scales
│   └── plant_card_names.json     # Older naming artifact; prefer CSV-driven data
├── docs/
│   ├── Leaf_and_Let_Die_vNN-NN.pdf # Highest numeric revision used by game box
│   └── card_csv_workflow.md      # Additional CSV notes
├── tools/                      # First-build / asset tools
├── scripts/                    # Incremental builders and shell entry points
├── generated/                  # Derived meshes, textures and test saves
├── source/                     # Source TTS JSON used by some builder defaults
└── output/
    ├── TS_Save_latest.json      # STAGED save to review/publish
    ├── backups/                 # Pre-publish backups
    ├── unpack-backups/          # Overwritten files backed up during patch install
    └── preparation/             # Asset manifest and audit outputs, when generated
```

Some of these directories/files, particularly complete artwork and the PDF, may be absent from a **packed handoff archive** even when present in the actual working project.

## Script reference

### Workflow scripts (`scripts/*.sh`)

| Script | Function / important behavior |
| --- | --- |
| `unpack.sh ARCHIVE` | Installs a `.zip` or `.tgz` patch into the project through `safe_unpack.py`; saves backups of replaced files. Does **not** publish a TTS save. |
| `pack.sh` | Creates `output/leaf-tts-pack-YYYYMMDD-HHMMSS.tgz`, copies it to `~/Downloads`, and backs up the current TTS save. The **published** save takes precedence over the staged save in this pack process. |
| `publish.sh [SAVE.json]` | Publishes staged save (or an explicitly supplied JSON file) to the configured TTS save directory; backs up the previous published save. Reload it inside TTS. |
| `update_card_data.sh` | Runs `generate_card_config.py`, then `rebuild_layout.sh` to update card names, effect text, and permanent IDs in an existing save. The latter runs the generator again; this is harmless. |
| `rebuild_layout.sh` | Uses staged save, or copies the published save into staging if needed; regenerates card config; runs `apply_game_box_layout.py`. |
| `rebuild_box.sh` | Rebuilds the placard/rulebook/Game Box contents, stages the result, regenerates card config, then applies approved layout. |
| `rebuild_player.sh` | Rebuilds all four player box assemblies from the latest staged or published save. |
| `rebuild_dice.sh` | Rebuilds both dice trays; the underlying Python builder also accepts `--tray low` / `--tray high`. |
| `rebuild_token.sh` | Rebuilds the Critter/nutrient token tray. |
| `rebuild_wisp.sh` | Rebuilds Wisp/Butterfly tray. |
| `rebuild_vp.sh` | Rebuilds the Victory Point tray. |
| `tts_workflow.sh` | Shared implementation for `pack`, `unpack`, and `publish`; generally invoke its wrappers above. |

### Python scripts (`scripts/`)

| Script | Responsibility |
| --- | --- |
| `generate_card_config.py` | Validates the four CSVs and emits `config/card_catalog_effects.json`. Supports `--csv-dir` and `--output`. |
| `apply_game_box_layout.py` | Applies approved TTS object transforms and friendly descriptions, adjusts Catalog placement, and updates Plant/Wisp/Round card titles and effects from the generated config. Supports `--save`, `--layout`, `--card-effects`. |
| `build_game_box.py` | Creates a flat Game Box display board using `assets/box/cover_main`; adds the TTS Custom PDF rulebook and prototype-photo guide; adjusts bag/catalog arrangement. Supports `--save`, `--assets-box`, `--rules-pdf`, `--output`, `--tts-root`. |
| `build_player_box.py` | Creates Blue, Green, Red, Yellow player supply trays and Discard Bin pieces with the components laid out. Reads artwork from `assets/player/`; uses `assets/trays/grassy_field.png` for supply walls and `Discard Bin Cover` art for Discard Bin walls. |
| `build_dice_trays.py` | Generates the low/high dice tray meshes, textures and staged-save objects. |
| `build_token_tray.py` | Generates the shared Critter/nutrient tray and token placements/stacks. |
| `build_wisp_butterfly_tray.py` | Generates the Wisp/Butterfly tray. |
| `build_vp_tray.py` | Generates the VP tray. |
| `safe_unpack.py` | Validates patch archive paths before extraction and backs up existing target files. |

Each `build_*.py` normally writes its own `generated/<component>/...Test_Save.json` file. The corresponding `rebuild_*.sh` copies that result to **the staged** `output/TS_Save_latest.json`. Prefer shell wrappers for routine maintenance rather than invoking each Python builder with the wrong `--save`.

### Initial build and asset tools (`tools/`)

| Tool | Function |
| --- | --- |
| `prepare_tts_assets.py` | Audits artwork, produces `output/preparation/tts_asset_manifest.json`, card CSV inventory, reports and URL template. Does not upload files. Typical invocation: `python3 tools/prepare_tts_assets.py --root .` |
| `build_tts_save.py` | Builds an initial full component TTS save using the prepared manifest and source save; regenerates card config automatically. Example: `python3 tools/build_tts_save.py --mode local`. `--install` creates a new numbered TTS save. |
| `make_token_models.py` | Produces supplemental custom token model assets; not a regular incremental rebuild command. |
| `make_token_models_vp_butterfly_fix.py` | Older specialized generator for VP/Butterfly models; not part of the everyday flow. |

**Fresh-build warning:** Before running `build_tts_save.py`, verify `source/TS_Save_8.json`, the prepared asset manifest, and all referenced art exist. Its output is a *new* JSON under `output/`, not necessarily the staged `TS_Save_latest.json`; do not publish it accidentally in place of the approved layout.

## Card-data architecture (v27)

The **four CSV files** in `assets/card_data/` are authoritative. Do not manually maintain names/effects in JSON or embed them in layout code.

```text
assets/card_data/*.csv  (edit)
          │
          ▼
scripts/generate_card_config.py
          │
          ▼
config/card_catalog_effects.json  (generated)
          │
          ├── tools/build_tts_save.py       -> new cards, correctly labeled at creation
          └── scripts/apply_game_box_layout.py -> update labels/effects in existing saves
```

**Identity is not display text.** The CSV `name` field is the stable design ID (`Root_05_04`, `Vine_07_01`, `zFlower_17_04`, etc.) and must continue matching the art manifest. The CSV `title` and `effect` fields may change. A TTS object's `GMNotes` stores its persistent design ID; `Nickname` is its human-readable title and `Description` is the **effect text only**.

- **Plants:** `[cost] Name`, such as `[5] Root Well`; the tooltip description is the printed effect. This applies to every Plant stack and to its constituent cards (36 designs, six of each).
- **Round cards:** Two effects per card; the visible title combines the effect names, and the description contains both labeled effects.
- **Wisp cards:** Actual Wisp name and effect. A quantity-zero design such as `Wisp_Quake` stays in the config but does not become a physical card.
- **Butterflies:** Their description describes rerolling one die and flipping the Butterfly face down; these are not Wisp cards.

Existing saves created before permanent IDs were introduced may need the compatibility migration in `apply_game_box_layout.py`. The script must **fail loudly** if a required design ID is missing, inconsistent, or unmapped; do not silently skip unknown cards. When modifying this code, validate the **Plant stack and all six child cards**, including all Vines and Flowers, and the mixed Round and Wisp decks.

### Where to change card text

1. Edit the correct CSV in `assets/card_data/`.
2. Run `./scripts/update_card_data.sh`.
3. Inspect the staged save or reload TTS after `./scripts/publish.sh`.
4. If a card is misidentified, check its `GMNotes` ID, its art filename/manifest ID, and the generated mapping. **Fix the identity mismatch; do not special-case its display name.**

## Layout and dimensions

`config/game_box_layout.json` captures the **approved user-arranged table geometry**. The current arrangement places components on the Game Box placard; the **Plant Card Catalog bag sits next to the Game Box Sack**, outside it; and the four player boxes and shared trays retain the user's chosen positions.

**Do not replace this with a fresh arbitrary layout.** When a user supplies a new TTS save containing manually adjusted positions, use that save as the geometry source, preserve stable GUIDs, and update the layout config/builders accordingly.

Dimensions are converted from inches using TTS calibration constants in builders. For example, most tray scripts use `UPI = 3.06 / 3.5 * 1.67`, while **the Game Box builder currently uses a deliberately larger `UPI = 3.06 / 3.5 * 2.67`**. Do not normalize these automatically; the Game Box enlargement was intentional.

In `scripts/build_game_box.py`, the editable size controls include `BOARD_W`, `BOARD_H`, `BOARD_THICK`, and `PDF_SCALE`; `BOX_X/Y/Z` are the builder's baseline position settings. **The layout application may override generated placement** with saved transforms: if a dimension/position change appears not to take effect, check `config/game_box_layout.json` and `scripts/apply_game_box_layout.py` before assuming the builder is broken.

In `scripts/build_player_box.py`, the layout of Blue is the master and the other colors are built from it; the Discard Bin has different wall artwork from the Supply tray. Keep the actual user-approved transforms and don't revert to the earlier single-row approximation.

## Game Box and blind-playtest presentation

The Game Box placard is a **flat simulation of the retail box**, not a true container. Its description explains the deliberate TTS approximation (especially the player component boxes) and points a blind playtester toward the rulebook. Components displayed on the placard stand for what a buyer takes out and will use on the table.

The **Game Box Sack** is a standard TTS Bag. Its description should tell the user it holds the remaining boxed material and is a convenient place to put **off-table or unwanted components**. The current intended workflow is: take out the readable PDF rulebook; use the setup instructions to arrange components; use the Sack for storage. Do not expose development-only labels such as `Physical Test` or raw filenames to playtesters.

The PDF rules are selected from the highest numeric `docs/Leaf_and_Let_Die_vNN-NN.pdf` revision. The generated Custom PDF currently points to a **local Proton/Windows `Z:/...` file path** unless hosted differently; that works on the author's machine but not necessarily for remote playtesters. For remote distribution, assets and the PDF need accessible hosted URLs. Check this before sending a TTS save to a blind tester.

## Safe workflow and troubleshooting

- **Stage first, publish second.** Rebuild wrappers use the existing `output/TS_Save_latest.json` when present; otherwise they start from the configured published save. `publish.sh` backs up the previous live save before replacing it.
- **Back up hand placements.** TTS edits made after the last publish can be overwritten by a subsequent `publish.sh`. If you hand-arrange pieces in TTS, save them and capture the latest JSON before letting a script regenerate the layout.
- **Check error output.** A strict missing card-ID error means a CSV, manifest, GMNotes, artwork ID, or save object no longer agrees; resolve the mismatch instead of making the renamer ignore the card.
- **Check TTS image loading.** Python success establishes that JSON/assets were generated, not that TTS can load every local or hosted image. Reload the save in TTS to confirm.
- **Avoid accidentally resetting geometry.** For visual changes, use the specific builder and then `rebuild_layout.sh` where appropriate. Validate the finished staged JSON for duplicate GUIDs and lost objects before publishing.
- **Inspect the active config.** `config/tts_workflow.conf` determines both the TTS save directory and filename, and is tailored to one Ubuntu/Steam Proton installation. Modify if the installation moves.
- **Dependencies.** The scripts require Python 3; image builders import Pillow (`PIL`). The workflow also uses standard shell tools (`bash`, `tar`, `zip`/archive handling) on Linux. If needed, install Pillow in the project's Python environment.

### Pack limitations — important for ChatGPT handoffs

`./scripts/pack.sh` currently copies `scripts/`, `tools/`, `config/`, selected `generated/` directories, and **`assets/trays/`**. It also includes `assets/files.txt` (a **list of asset filenames**, not their contents) and copies the **published TTS save** if available, falling back to the staged save. It does **not** currently include the actual `assets/card_data/` CSV contents, the full card/box/player image library, or `docs/`.

**Therefore the pack is a diagnostic snapshot, not a complete reproducible-source backup.** For a ChatGPT conversation involving card changes, also attach the four CSV files or explicitly extend `pack.sh` to include `assets/card_data/`. For rulebook/cover changes, attach the PDF or cover art as needed. Always state whether the source save or the published save should be treated as authoritative for the latest placements.

## Instructions for a future ChatGPT conversation

When given this README and a current project archive:

1. **Inspect the actual files first.** The archive may be older than recent patches; do not assume a command or function exists without checking it.
2. **Honor the source of truth:** card CSVs for card text, image assets for artwork, permanent design IDs for identity, and the latest user-approved TTS save/layout for transforms.
3. **Preserve user decisions:** do not change established box dimensions, object locations, card effect wording, player-box arrangements, art orientation, or bag contents unless explicitly asked.
4. **Make patches small.** Prefer a ZIP containing only changed files with paths relative to the `leaf-tts/` root so it can be installed with `./scripts/unpack.sh ~/Downloads/PATCH.zip`.
5. **Do not modify the live TTS save directly.** Rebuild into staging (`output/TS_Save_latest.json`), verify, then have the user run `./scripts/publish.sh`.
6. **Test both fresh and repeat builds** whenever changing object generation, GUID handling or renaming. Ensure no duplicated/lost components and no silently unidentified cards; intentionally test a failure case.
7. **Report tests accurately.** Static validation and JSON inspection are not a live TTS test. Say explicitly when visual/runtime testing remains for the user.
8. **Provide a short Git commit message** when delivering source changes; do not include unrelated generated files unless needed for the workflow.
9. **Use the existing terminology:** Game Box placard, Game Box Sack, Plant Card Catalog, Player Supply tray, Discard Bin, Wisp/Butterfly tray, Battle Glade and Grove.

### Suggested prompt when starting a new chat

> I'm continuing development of my Leaf & Let Die Tabletop Simulator retail-box simulation. Read `README.md` first, then inspect the attached `leaf-tts-pack-*.tgz` and any updated CSVs, files or TTS saves. Preserve my approved TTS layout and existing script workflows. Use the four CSVs under `assets/card_data/` as the source of truth for card names and effect descriptions; card IDs remain stable in `GMNotes`. Give me minimal, tested ZIP patches installable with `./scripts/unpack.sh`, the appropriate rebuild/publish commands, and a Git commit message. Do not silently skip missing card mappings or invent undocumented rules.

---

*Documented from `leaf-tts-pack-20261020-165204.tgz` with `Leaf_TTS_CSV_Driven_Cards_v27.zip` overlaid. Update this README when script names, staging behavior, data sources, or pack contents change.*

### Round card stacks on the Game Box placard

The 18 Round cards are unpacked as two separate stacks: **12 Cultivation** and **6 Battle**. Their exact hand-arranged TTS positions are saved in `config/round_stack_layout.json` (from `TS_Save_8(4).json`). The Game Box Sack and Plant Card Catalog positions are in `config/game_box_layout.json`. Both `scripts/build_game_box.py` and `scripts/apply_game_box_layout.py` move Round cards out of the Game Box Sack, splitting legacy 18-card decks automatically. The scripts reuse each card's original GUID, artwork, permanent `GMNotes` ID, and CSV-generated name/effect. Run `./scripts/rebuild_layout.sh` or `./scripts/rebuild_box.sh` then `./scripts/publish.sh`. The operation is repeatable, and an unexpected card count or unidentified Round card halts the rebuild.

### Updating the rulebook without rebuilding the game

Put the new versioned PDF under `docs/`, for example `docs/Leaf_and_Let_Die_v37-03.pdf`, then run:

```bash
./scripts/rebuild_rulebook.sh
./scripts/publish.sh
```

`rebuild_rulebook.sh` chooses the **highest numeric version** matching `Leaf_and_Let_Die_vNN-NN.pdf` (so v37-10 sorts after v37-09), first using the existing staged save, otherwise the published save. It changes **only** the rulebook inside the Game Box Sack: its URL, displayed version, and short tooltip. The existing PDF GUID (`f0d002`), transform, page state, other sack contents, every other game object and approved layout remain untouched. The script stages the save and does not publish automatically. It fails without altering staging if the PDF, Sack, or unique rulebook object is missing. To select a specific revision, run `./scripts/rebuild_rulebook.sh --rules-pdf docs/Leaf_and_Let_Die_v37-03.pdf`.

Both `rebuild_layout.sh` and `rebuild_box.sh` now select the latest versioned PDF as well; they do *more* work, so prefer `rebuild_rulebook.sh` for rules-only updates. The rulebook currently uses a local `Z:/...` path, suitable for this host's Proton setup, **not a portable URL**. Before sharing a save with remote playtesters, upload and configure an accessible HTTPS PDF URL through the cloud-asset workflow. Reload the generated save in TTS to check that the PDF opens.

### Prototype photos inside Game Box Sack

Keep `docs/Box Illustrations.pdf` in the local project. The scripts `scripts/box_illustrations.py`, `scripts/apply_game_box_layout.py`, and `scripts/build_game_box.py` ensure one `Leaf & Let Die — Prototype Box Photos` Custom PDF (permanent GUID `f0d003`) resides inside the Game Box Sack. Run `./scripts/rebuild_layout.sh` and `./scripts/publish.sh` to add it to an existing save, or `./scripts/rebuild_box.sh` when regenerating the box. Both fail if the PDF is absent and never duplicate the object. It supplements rather than replaces the Rulebook and explains real physical packaging details that TTS cannot simulate exactly. The PDF uses a local `Z:/...` path and requires an HTTPS-hosted URL for remote players. Diagnostic packs may omit the actual PDF.


## Cloud release publishing (Steam Workshop candidate)

Keep local development and cloud release saves separate:

- `./scripts/publish.sh` copies the staged `output/TS_Save_latest.json` to **Leaf and Let Die - Generated**, using local `Z:/...` URLs. Use this for editing and rebuilding.
- `./scripts/cloud_publish.sh` validates the staged local save, substitutes the 163 verified hosted URLs from `config/cloud_asset_urls.json`, and creates **both** `output/cloud/Leaf_and_Let_Die_Blind_Playtest_Cloud.json` and the separate **Save & Load → Active** copy. It does **not** modify `output/TS_Save_latest.json` or Generated.
- To convert a different local source: `./scripts/cloud_publish.sh /path/to/local-save.json`.
- It **fails closed** if any asset URL is not known or is not secure HTTPS. New images/PDFs/models must first be uploaded to Steam Cloud, and their paths/URLs added to `config/cloud_asset_urls.json`; this script does not upload assets. Do not run the normal local `publish.sh` on the cloud release.
- Backups of previous Active cloud files are made under `output/backups/`. The release is a **local TTS save**, not a Steam Workshop upload; use TTS Workshop Upload separately.
- Card validation occurs **before** rewriting URLs because the validator currently uses original artwork filenames.

## Versioned local and cloud saves (v37.03 workflow)

The desired Save & Load folders are **Active**, **Generated**, and **Legacy**. Keep manually approved blind-playtest snapshots in Active and older snapshots in Legacy. Neither folder is migrated or deleted by the scripts.

**One-time folder migration:** Run `./scripts/migrate_generated_folder.sh` from the project root. It safely moves `Leaf and Let Die - Generated` to `Generated`, preserving everything in that folder. If `Generated` already exists, it stops without changing either folder. The configuration now targets `Generated` for future local publishes. In TTS, use Save & Load to manage the Active and Legacy folders.

**Local candidate:** Run `./scripts/rebuild_rulebook.sh` followed by `./scripts/publish.sh`. `publish.sh` retains the stable TTS JSON filename (`TS_Save_latest.json`) so existing scripts still work, but sets the **in-game SaveName** to `LOCAL v37.03`, derived from the PDF in the Game Box Sack. A file named `TS_Save_...json` can be long; TTS displays its `SaveName` field. Nothing else about the saved objects is changed by this naming step.

**Cloud candidate:** First upload each new asset to Steam Cloud. In particular, upload `docs/Leaf_and_Let_Die_v37-03.pdf` using **Upload → Cloud Manager**, then click the uploaded item to copy its HTTPS URL. Add this mapping in `config/cloud_asset_urls.json` under `assets`, alongside the existing v37.01 entry. Preserve the exact local PDF URL found in your staged JSON as the JSON key (it normally begins `Z:/home/.../docs/Leaf_and_Let_Die_v37-03.pdf`). The cloud conversion fails safely if any referenced asset is not HTTPS-hosted or mapped.

Run `./scripts/cloud_publish.sh` after the mapping is in place. This creates a separately versioned cloud candidate in **Active** with display name `Leaf & Let Die — Blind Playtest v37.03 (Cloud)`, and an archive copy in `output/cloud/`. It does **not** overwrite the pre-existing unversioned cloud save, manually approved v37.01, or manual v37.03 snapshot. A repeated conversion of the same version backs up the prior versioned cloud candidate first. A completed conversion is only a **cloud-asset-backed local TTS save**, not a Steam Workshop publication and not proof that a laptop can see the local JSON file.

**Laptop verification:** On the machine that built the cloud candidate, load the versioned cloud save in TTS and inspect the Rulebook inside the Game Box Sack. To test on a separate laptop, either install that JSON into its local TTS Saves folder or use Steam Workshop. Simply hosting assets in Steam Cloud does not sync this generated Save & Load entry to other computers automatically. Confirm the PDF opens and other components load on the second machine before sharing it.

**Blind-playtest release:** With the cloud-backed save *loaded* in TTS, use **Upload → Workshop Upload** to create the Workshop item or **Update Workshop** for an existing item. Supply its required thumbnail and choose the visibility appropriate for the testers. For an invite-only test, use unlisted/friends-only Workshop visibility as supported by Steam; send testers the Workshop item URL and ask them to subscribe/open through TTS. A hidden item is only visible to its owner. Keep the Workshop item ID for subsequent updates; do not create a fresh item for every revision. The published Workshop listing title should also state `Leaf & Let Die — Blind Playtest v37.03` so testers can identify the version. This final Workshop action is done inside TTS and is **not** performed by the scripts.

Documentation: https://kb.tabletopsimulator.com/custom-content/cloud-manager/ and https://kb.tabletopsimulator.com/custom-content/steam-workshop/ .
