# Elemental Katanas (Fabric, Minecraft 1.21.1)

Five elemental katanas with custom recipes, particles, tiers and right-click abilities.

## Directory tree

```
elemental-katanas/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/main
    ├── java/com/example/katanas
    │   ├── ElementalKatanas.java          main initializer
    │   ├── ability/GravityWellManager.java timed pull -> lift -> slam logic
    │   ├── item/
    │   │   ├── KatanaMaterial.java        Tier 1 / Tier 2 stats
    │   │   ├── KatanaItem.java            base class (cooldown, hooks, particles, tooltips)
    │   │   ├── FrostKatana.java
    │   │   ├── FlameKatana.java
    │   │   ├── LightningKatana.java
    │   │   ├── GravityKatana.java
    │   │   └── SpaceKatana.java
    │   ├── registry/ModItems.java         item + creative tab registration
    │   └── util/
    │       ├── KatanaFX.java              particle + sound helpers
    │       └── AbilityUtil.java           targeting + damage helpers
    └── resources
        ├── fabric.mod.json
        ├── assets/elementalkatanas/{lang/en_us.json, models/item/*.json, textures/item/*.png}
        └── data/elementalkatanas/recipe/*.json
```

## Build

No Gradle wrapper is included. Either:
- copy `src/` into a fresh project from https://fabricmc.net/develop (1.21.1, same mod id/package), or
- run `gradle wrapper --gradle-version 8.10` here, then `./gradlew build` / `./gradlew runClient`.

Versions in `gradle.properties` should be checked against fabricmc.net/develop.

## Stats

| Katana | Tier | Damage | Attack speed | Durability | Cooldown |
|---|---|---|---|---|---|
| Frost | 1 | 6 | 2.0 | 650 | 10s |
| Flame | 1 | 6 | 2.0 | 650 | 6s |
| Lightning | 1 | 6 | 2.0 | 650 | 12s |
| Gravity | 2 | 9 | 2.2 | 2800 | 15s |
| Dimensional | 2 | 9 | 2.2 | 2800 | 8s |

Iron sword for comparison: 6 damage, 1.6 speed. Tweak in `KatanaMaterial` and each katana's `super(...)` call.

## Recipes (all shaped, same diagonal layout)

```
. . B
. B C      B = blade metal, C = core, H = handle
H . .
```

- Frost: iron + prismarine crystals/shard, stick
- Flame: iron + blaze rod/powder, stick
- Lightning: iron + trident, stick
- Gravity: netherite + nether star, blaze rod
- Dimensional: netherite + dragon egg (consumed), blaze rod

## Animations (first person, client side)

`client/KatanaPoses.java` holds every number. Per katana: an idle grip and its own right-click animation. Shared: a 4-hit attack combo that cycles on each swing and resets after about 1.2s idle.

| Hit | Technique |
|---|---|
| 1 | kesa-giri, diagonal cut upper right to lower left |
| 2 | yoko-giri, horizontal cut |
| 3 | gyaku-kesa, rising diagonal cut |
| 4 | tsuki, straight thrust |

Right-click animations: Frost overhead slam, Flame iai draw plus wide cut, Lightning blade to the sky then stab, Gravity full spin then slam, Dimensional lunge thrust then sweeping slash. Equipping plays a draw motion. The slash particles (server side) follow the same technique as the arm animation.

If a motion looks mirrored or too strong in game, edit the numbers in `KatanaPoses.java` (positions in blocks, rotations in degrees). The animation is a mixin on `HeldItemRenderer`. If a Minecraft update breaks it, the mod still loads and katanas fall back to the vanilla swing.

## Textures

Textures are 64x64 (curved blade with an element pattern, guard, wrapped handle). `models/item/*.json` still use `handheld` plus an enlarged `display` block.

## Older note: 32x32 textures and longer blades

The generated PNGs are placeholders (a diagonal katana in each element's colors). To make your own:

1. Canvas 32x32, transparent background. Draw the katana on the diagonal: handle bottom-left, blade toward the top-right corner, so it reads like the vanilla sword orientation.
2. Keep a 1px dark outline, a bright spine highlight along the blade and a gold guard. Use 3-4 shades per blade color.
3. Models use `minecraft:item/handheld`. A 32x32 texture maps onto the same 16-unit model square, so it renders at the same size but with 4x the detail. To get the **longer blade look**, each model overrides `display` with a larger scale (`1.3` third person, `0.95` first person) and shifted translation.
4. Fine-tune in Blockbench: open any `models/item/*_katana.json`, go to Display, and adjust Rotation/Translation/Scale per slot until the handle sits in the hand and the blade extends forward. Paste the numbers back into the JSON.
5. For a truly 3D blade (thin elements extending past the 16x16 box), build the model in Blockbench with an extended element list instead of `handheld`; keep the `display` block.

## Known limits

- Particles are vanilla ones, spawned server-side so other players see them. Custom particles need a `ParticleType`, a client factory and `assets/.../particles/*.json`.
- The flame wave is instant (a line scan), not a moving projectile entity.
- Gravity wells are not saved: they vanish if the server stops mid-ability.
