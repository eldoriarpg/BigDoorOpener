---
sidebar_position: 4
---

# Conditions

A condition is one rule that decides whether a door should be open. A condition can say **yes** (open), **no**
(closed) or, for some conditions, **nothing**. "Nothing" means the condition has no opinion right now, and the door
keeps its current state as far as this condition is concerned. [The evaluator](./evaluator.md) combines the answers of
all conditions into one decision.

## Adding and removing conditions

```text
/bdo setCondition <door> <condition> <settings>
/bdo addCondition <door> <condition> <settings>
/bdo removeCondition <door> <group> [index]
```

Every condition belongs to a **group**. `setCondition` replaces all conditions of the same group on that door.
`addCondition` adds one more condition to the group and keeps the others. For example, a door can have two `item`
conditions for two different keys.

`removeCondition` takes the group name, not the condition name. When a group holds more than one condition, the index
picks which one. The first one is `0`, which is also the default. `/bdo info <door>` shows the index in front of each
condition.

You need the permission for the group, for example `bdo.condition.item`, or `bdo.condition.all`. See
[Permissions](./reference/permissions.md).

:::tip Use tab completion
Condition names must be typed exactly as shown below, including capital letters. `itemBlock` works, `itemblock` does
not. Tab completion fills them in for you.
:::

## Player conditions and world conditions

Most conditions are **player conditions**. Big Doors Opener checks them for every player near the door, 200 blocks by
default (see `playerCheckRadius` in [Configuration](./reference/configuration.md)). The door opens when **one player**
meets the conditions on their own. If nobody is near, player conditions count as "no".

This is also true for `time`, `weather`, `placeholder` and `mythicMob`. They only take effect while a player is near
the door.

The `worldLocation` conditions are **world conditions**. They are checked once for the whole world. If a door has only
world conditions, nobody needs to be near the door. As soon as it also has a player condition, the door only opens
while a player is near.

## Overview

| Condition | Group | Opens when | Needs |
|---|---|---|---|
| `itemHolding` | `item` | a player holds the key item | |
| `itemOwning` | `item` | a player has the key item in the inventory | |
| `itemClick` | `item` | a player clicks with the key item | |
| `itemBlock` | `item` | a player clicks a keyhole block with the key item | |
| `proximity` | `location` | a player is close to the door | |
| `region` | `location` | a player is inside a WorldGuard region | WorldGuard 7 |
| `simpleRegion` | `location` | a player is inside a box you clicked out | |
| `worldProximity` | `worldLocation` | any player is close to the door | |
| `worldRegion` | `worldLocation` | any player is inside a WorldGuard region | WorldGuard 7 |
| `worldSimpleRegion` | `worldLocation` | any player is inside a box you clicked out | |
| `permNode` | `permission` | a player has a permission | |
| `doorPerm` | `permission` | a player has access to the door in Big Doors | |
| `time` | `time` | it is the right time of day | |
| `weather` | `weather` | the weather matches | |
| `placeholder` | `placeholder` | a PlaceholderAPI expression is true | PlaceholderAPI, NashornJs |
| `mythicMob` | `mythicMob` | a MythicMobs mob of a certain type died | MythicMobs |

## Item conditions

Item conditions need a key item. To set one up, **hold the key item in your main hand** and run the command. The
condition remembers that item. A player's item counts as the key when its type and its meta match: name, lore,
enchantments and so on. These conditions must be set by a player, not from the console.

All four item conditions take the same settings:

```text
/bdo setCondition <door> <itemCondition> <amount> [consumed]
```

| Setting | Required | What it means |
|---|---|---|
| `amount` | yes | How many key items the player needs. 1 to 64. |
| `consumed` | no | `true` takes the items from the player when the door opens. Default `false`. |

Items are only taken when the door really opens, and only from the player who opened it. A player who holds the key but
is not close enough, for example, keeps the items.

Use `/bdo giveKey <door>` to get copies of the key. See [Managing doors](./managing-doors.md).

:::tip Combine with a location
`itemHolding`, `itemOwning` and `itemClick` work from anywhere within the check radius. Add a `location` condition
and keep the [evaluator](./evaluator.md) on `and`, so players must also be at the door.
:::

### itemHolding

Opens when a player holds the key in the main hand or the off hand.

```text
/bdo setCondition gate itemHolding 1
```

### itemOwning

Opens when a player has the key anywhere in the inventory.

```text
/bdo setCondition gate itemOwning 1
```

### itemClick

Opens when a player clicks with the key in hand. Any click counts, on a block or into the air. The player must still
hold the key when the door is checked.

```text
/bdo setCondition gate itemClick 1 true
```

### itemBlock

Opens when a player clicks a certain block, the keyhole, with the key in hand. Left and right clicks both count.

```text
/bdo setCondition gate itemBlock 1
```

After the command, **right-click the block** that should be the keyhole. The condition is only saved after that click.

## Location conditions

Location conditions need a player at a certain place.

### proximity

Opens when a player is within a certain distance of the door. The distance is measured from the middle of the door,
as it was when Big Doors Opener first registered the door.

```text
/bdo setCondition <door> proximity <size> [form]
```

| Setting | Required | What it means |
|---|---|---|
| `size` | yes | One number for the same distance in every direction, or `x,y,z` for different distances. Each value 1 to 100. |
| `form` | no | `cuboid` (a box), `ellipsoid` (a ball) or `cylinder`. Default `cuboid`. |

The sizes work like a radius: `10` means up to 10 blocks in each direction. With `cylinder`, `x` and `z` shape the round
part and `y` is how far up and down it reaches.

```text
/bdo setCondition gate proximity 20,5,20 cylinder
```

This opens the door for players up to 20 blocks away on the ground and up to 5 blocks above or below the middle of the
door.

### region

Opens when a player is inside a WorldGuard region. Needs WorldGuard 7.

```text
/bdo setCondition <door> region <region name>
```

The region is looked up in the world you are standing in. It must be the world of the door.

### simpleRegion

Opens when a player is inside a box. You do not need WorldGuard for this.

```text
/bdo setCondition <door> simpleRegion
```

After the command, **left-click two blocks**. They are two opposite corners of the box.

## World location conditions

These work like the location conditions, with one difference. They are true when **any** player in the world is in
the area. That player does not need to meet the other conditions of the door. Use them for things like "open the gate
for everyone while someone stands on the bridge".

| Condition | Settings | Works like |
|---|---|---|
| `worldProximity` | `<size> [form]` | [proximity](#proximity) |
| `worldRegion` | `<region name>` | [region](#region) |
| `worldSimpleRegion` | none, then left-click two blocks | [simpleRegion](#simpleregion) |

:::warning worldSimpleRegion does not work
In version 2.5.3 a door with `worldSimpleRegion` fails while it is checked and shows errors in the server log. Use
`worldProximity` or `worldRegion` instead.
:::

## Permission conditions

### permNode

Opens for a player who has a certain permission. Give the permission with your permission plugin.

```text
/bdo setCondition <door> permNode <permission>
```

```text
/bdo setCondition gate permNode castle.guard
```

### doorPerm

Opens for a player who has a certain access level for the door in Big Doors.

```text
/bdo setCondition <door> doorPerm <owner|editor|user>
```

| Level | Who gets in |
|---|---|
| `owner` | only the owner of the door |
| `editor` | the owner and editors |
| `user` | the owner, editors and users |

:::tip
Permission conditions alone open the door for a matching player anywhere near it. Add a `location` condition so they
must also stand at the door.
:::

## time

Opens and closes the door at certain times of the Minecraft day.

```text
/bdo setCondition <door> time <open> <close> [force]
```

| Setting | Required | What it means |
|---|---|---|
| `open` | yes | When the door opens, in ticks from 0 to 24000. |
| `close` | yes | When the door closes, in ticks from 0 to 24000. |
| `force` | no | `true` keeps the door in the right state all the time. Default `false`. |

A Minecraft day has 24000 ticks. Tick 0 is 6:00 in the morning, 6000 is noon, 12000 is 18:00 and 18000 is midnight.

```text
/bdo setCondition gate time 0 12000 true
```

This opens the gate at 6:00 and closes it at 18:00.

Without `force`, the door only opens once at the open time and closes once at the close time. In between, the
condition has no opinion, so players can still open or close the door by hand. With `force`, the condition says
"open" for the whole open period and "closed" for the rest.

:::warning Enter ticks, not clock times
Tab completion offers clock times like `18:00`, but version 2.5.3 rejects them with `Invalid open time.` Use ticks.
:::

:::tip Use force when players come and go
Like all player conditions, `time` only works while a player is near the door. When nobody is near, the door closes.
Without `force`, it may not open again until the next open time. Set `force` to `true` if that is not what you want.
:::

## weather

Opens the door depending on the weather.

```text
/bdo setCondition <door> weather <clear|downfall>
```

| Setting | Required | What it means |
|---|---|---|
| `clear` or `downfall` | yes | `clear` opens the door when it is not raining or snowing. `downfall` opens it when it is. |

Like `time` without force, the condition only changes the door when the weather changes. In between it has no opinion.

The temperature at the door counts. Where it can never rain, for example in a desert, the weather always counts as
clear.

:::note
The command accepts a second setting, `force`, but version 2.5.3 does not use it.
:::

## placeholder

Opens the door when an expression with [PlaceholderAPI](./installation.md#optional-plugins) placeholders is true. Needs
PlaceholderAPI and [NashornJs](./installation.md#nashornjs). Must be set by a player.

```text
/bdo setCondition <door> placeholder <expression>
```

The expression is a short piece of JavaScript. Big Doors Opener fills in the placeholders for each player near the
door and checks if the result is `true`. You can use the same words as in [custom evaluators](./evaluator.md): `and`,
`or`, `is`, `is not`. You can also compare numbers with `<`, `>`, `<=` and `>=`.

```text
/bdo setCondition vault placeholder %player_level% >= 30
```

```text
/bdo setCondition portal placeholder "%player_world%" is "world_nether"
```

When you enter the condition, Big Doors Opener tests it with your own values. If the test fails, you get an error and
the condition is not saved.

:::note Spaces are removed
All spaces are removed from the expression. Text values that contain spaces will not compare as you expect.
:::

## mythicMob

Opens the door when a MythicMobs mob of a certain type dies. Needs MythicMobs.

```text
/bdo setCondition <door> mythicMob <mob type>
```

The mob type is the internal name you also use to spawn the mob. Tab completion lists them.

The condition is true for one check after the mob died, anywhere on the server. After that check it is false again, so
the door closes on the next check. Use `/bdo stayOpen` to keep it open longer. See
[Managing doors](./managing-doors.md).
