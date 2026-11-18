# Cinematic Interactions: Penchant

Small NeoForge addon that makes [Cinematic Interactions](https://www.curseforge.com/minecraft/mc-mods/cinematic-interactions) and [Penchant Reforged](https://www.curseforge.com/minecraft/mc-mods/penchant-reforged) work together at the enchanting table.

Without it, Cinematic Interactions only knows the vanilla table with its three random offers, and Penchant's table menu falls out of the scene. With it, you walk up to the table, lay your item down by hand and pick an enchantment from the runes floating around the book, all inside the cinematic scene.

| | |
|---|---|
| Mod ID | `cinematicpenchant` |
| Version | 1.0 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.235 or newer |
| Java | 21 |

## Requirements

Both mods are required, on the client and on the server.

- [Penchant Reforged](https://www.curseforge.com/minecraft/mc-mods/penchant-reforged) `1.21.1-1.8-fix3` or newer (and Patchouli, which Penchant needs)
- [Cinematic Interactions](https://www.curseforge.com/minecraft/mc-mods/cinematic-interactions) `0.18.30` or newer. Older builds (0.18.2 and before) are missing the hooks this addon relies on.

## At the table

- **The item** goes on the table surface. It floats and turns above the book, like in the vanilla scene.
- **Lapis** goes into the blue rune to the right of the book.
- **A book** goes into the brown rune on the left. That rune only shows up when a grindstone stands near the table, same rule as Penchant's extraction.
- **Enchantments** appear as sigils around the table. Up to three sit where the vanilla offers would be; more of them shrink and spread out. Hover one to see its level cost, book requirement, and whether it is locked or incompatible.
- **Click a sigil** to enchant, or to extract onto the book. The rune flies to the item, the table lights up and the item spins. If the server refuses, nothing plays.
- A small panel shows how many bookshelves count toward the table and whether a grindstone is in range.
- Esc, E or moving steps back. Items left in the slots go back to your inventory.

Wrong items are refused on the spot: the rune turns red and shakes, and nothing is sent to the server.

## Multiplayer

Other players see what you do: the item on the table, the lapis, the book in the extraction slot, and the flash when an enchantment goes through. The book is synced by the addon itself, since Cinematic Interactions has no packet for it.

## What it doesn't touch

All the rules stay in Penchant: costs, bookshelf count, unlocked enchantments, compatibility, extraction. The addon only reads Penchant's menu and sends Penchant's own packet when you click. The camera, walking, hands and closing all come from Cinematic Interactions.

If Penchant's `table_rework` datapack is turned off, the table goes back to the regular Cinematic Interactions scene with the three vanilla offers.

## Building

```bash
./gradlew build
```

The jar lands in `build/libs/` as `cinematicpenchant-neoforge-<minecraft>-<version>.jar` (for example `cinematicpenchant-neoforge-1.21.1-1.0.jar`). Bump `mod_version`, `minecraft_version` and `mod_loader` in `gradle.properties` for each release. The Penchant and Cinematic Interactions jars it compiles against live in `libs/`.

`./gradlew runClient` starts a dev client with Patchouli and JEI.

## Credits

- [Penchant](https://modrinth.com/mod/penchant) by ThePotatoArchivist, ported to NeoForge as [Penchant Reforged](https://www.curseforge.com/minecraft/mc-mods/penchant-reforged) by me.
- [Cinematic Interactions](https://www.curseforge.com/minecraft/mc-mods/cinematic-interactions) by Southware.

Licensed under MIT. See [`LICENSE`](LICENSE).
