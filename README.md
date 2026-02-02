# Cobblemon Gym Badges

Cobblemon Gym Badges adds a Badge Press for crafting custom gym badges (and ribbons) with role-based access.

## Features

- Badge Press block with a dedicated crafting UI
- Gym Leader roles to gate badge crafting
- Datapack + resourcepack support for custom badges and ribbons
- JEI / EMI / REI integration (only craftable recipes are shown per player)
- Resourcepack-first design so custom art stays outside the jar

## Using the Badge Press

1. Place a Badge Press.
2. Put the core ingredient in the top slot and the base ingredient in the bottom slot.
3. If you have the required role (if any), the badge will appear in the output slot.

## Creating Custom Badges (Datapack + Resourcepack)

You need two parts:

1) A datapack for badge definitions and recipes  
2) A resourcepack for models and textures

### Datapack: Badge Definitions

Path: `data/<namespace>/cgb/badge/<id>.json`  
(`cgb` here is the registry namespace for badges.)

**Naming tip:** the `<id>` is the badge’s identity and translation key.
If you want a “Champion Ribbon”, name the file `champion_ribbon.json` and add a translation for
`badge.<namespace>.champion_ribbon`. The internal item id is always `cgb:badge`, `cgb:badge_ribbon`,
or `cgb:badge_untagged` based on `badgebox`, but players only see the badge definition name.

Example (badge):
```json
{
  "name": { "text": "Fire Badge" },
  "theme": "fire",
  "texture": "cgb:item/fire_badge",
  "badgebox": "badge"
}
```

Example (ribbon):
```json
{
  "name": { "text": "Champion Ribbon" },
  "theme": "champion",
  "texture": "cgb:item/blue_ribbon",
  "badgebox": "ribbon"
}
```

Field overview:
- `name` (optional): Display name component.
- `theme` (optional): Theme string. If omitted, uses the file name as the theme.
- `role` (optional): Required role name (without the `gym_leader_` prefix).
- `tags` (optional): Extra item tags to apply to the badge item.
- `model_data` (optional): Custom model data value. If omitted, the mod auto-assigns a unique value per badgebox type.
- `model` or `texture` (optional): Item model id to use. If you set `texture` only, the mod generates the item model.
- `badgebox` (optional): `none`, `badge`, or `ribbon`.  
  - This controls **which BadgeBox tag** the item gets.  
  - `badge` is the default (adds `badgebox:badges`).  
  - `ribbon` adds `badgebox:ribbons`.  
  - `none` adds no BadgeBox tag.  
  - The tag names come from the BadgeBox mod and are fixed; this field hides that complexity.

### Datapack: Badge Recipes

Path: `data/<namespace>/recipe/<id>.json`

Example:
```json
{
  "type": "cgb:badgemaking",
  "core": { "item": "minecraft:gold_ingot" },
  "base": { "item": "minecraft:blue_dye" },
  "result": {
    "theme": "champion"
  }
}
```

- `core` = top slot in the Badge Press  
- `base` = bottom slot in the Badge Press

### Resourcepack: Models + Textures

Paths:
- Textures: `assets/<namespace>/textures/item/<id>.png`

You only need textures for simple 2D badges/ribbons. The mod generates the base item models and
CustomModelData overrides at runtime, so you do **not** need to ship `badge.json`, `badge_ribbon.json`,
or per-badge model files.

If your badge definition uses `"texture": "cgb:item/blue_ribbon"`, you must also provide:
- `assets/cgb/textures/item/blue_ribbon.png`

If you want a custom item model (3D or layered), set `"model": "<namespace>:item/<id>"` in the badge definition
and include the model JSON yourself.

Model selection (how textures are chosen):

- This mod uses **CustomModelData** to select the right model for each badge.
- If `model_data` is omitted, the mod auto-assigns unique values per badgebox type.
- The base models and overrides are generated automatically on the client.

You can still set `model_data` manually if you need stable IDs across packs, but it is not required.

### Loading Your Packs

- Datapack: put the pack in `world/datapacks/` and run `/reload`.
- Resourcepack: put the pack in `resourcepacks/` and enable it in the Resource Packs menu.

## Troubleshooting

- Missing texture in JEI/EMI/REI usually means the **model file is missing**.
  If you use `model`, make sure the model id in your badge definition points at a real item model JSON.
- If you set `model_data` manually, ensure each badge has a unique number per badgebox type.
  Otherwise, let the mod auto-assign it.

## Technical Details (Advanced)

- Recipe type: `cgb:badgemaking`
- Recipe result supports `badge` id or `theme` lookup.
- Badge definitions are loaded from the `cgb:badge` datapack registry.
- BadgeBox tags are applied via the item type chosen by `badgebox`.
- The client injects a hidden generated resourcepack to build badge/ribbon model overrides from datapack definitions.
