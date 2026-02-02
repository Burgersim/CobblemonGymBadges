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

Path: `data/<namespace>/badge/<id>.json`

Example (badge):
```json
{
  "name": { "text": "Fire Badge" },
  "theme": "fire",
  "model_data": 2,
  "texture": "cgb:item/fire_badge",
  "badgebox": "badge"
}
```

Example (ribbon):
```json
{
  "name": { "text": "Champion Ribbon" },
  "theme": "champion",
  "model_data": 101,
  "texture": "cgb:item/blue_ribbon",
  "badgebox": "ribbon"
}
```

Field overview:
- `name` (optional): Display name component.
- `theme` (optional): Theme string. If omitted, uses the file name as the theme.
- `role` (optional): Required role name (without the `gym_leader_` prefix).
- `tags` (optional): Extra item tags to apply to the badge item.
- `model_data` (optional): Custom model data to drive model overrides.
- `model` or `texture` (optional): **Item model id** to use for the badge (see Resourcepack section).
- `badgebox` (optional): `none`, `badge`, or `ribbon`.  
  - `badge` is the default (adds the `badgebox:badges` tag).  
  - `ribbon` adds the `badgebox:ribbons` tag.  
  - `none` adds no BadgeBox tag.

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
- Models: `assets/<namespace>/models/item/<id>.json`
- Textures: `assets/<namespace>/textures/item/<id>.png`

If your badge definition uses `"texture": "cgb:item/blue_ribbon"`, you must also provide:
- `assets/cgb/models/item/blue_ribbon.json`
- `assets/cgb/textures/item/blue_ribbon.png`

Example model file:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "cgb:item/blue_ribbon"
  }
}
```

You can also drive visuals with `model_data` by adding overrides to your base badge model
(`assets/cgb/models/item/badge.json`) instead of providing a per-badge model.

### Loading Your Packs

- Datapack: put the pack in `world/datapacks/` and run `/reload`.
- Resourcepack: put the pack in `resourcepacks/` and enable it in the Resource Packs menu.

## Troubleshooting

- Missing texture in JEI/EMI/REI usually means the **model file is missing**.
  Make sure the model id in your badge definition points at a real item model JSON.
- If you use `model_data`, ensure each badge has a unique number and your base model has
  the matching overrides.

## Technical Details (Advanced)

- Recipe type: `cgb:badgemaking`
- Recipe result supports `badge` id or `theme` lookup.
- Badge definitions are loaded from the `cgb:badge` datapack registry.
- BadgeBox tags are applied via the item type chosen by `badgebox`.
