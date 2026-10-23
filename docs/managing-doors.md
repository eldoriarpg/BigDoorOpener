---
sidebar_position: 6
---

# Managing doors

This page covers everything around a door besides its conditions. All commands are listed in
[Commands](./reference/commands.md).

## Which doors you can manage

You can manage the doors you own in Big Doors, by name or by id. Players with `bdo.accessAll` can manage every door.
They use the id for doors that belong to someone else. The console can always manage every door by id.

A door becomes a Big Doors Opener door when you give it its first condition. From then on, Big Doors Opener opens and
closes it, and Big Doors' own auto close timer is ignored for it. Commands like `info`, `stayOpen` or `setEvaluator`
only work on doors that are registered this way. Otherwise you get `This door is not registered.`

## See what a door does

```text
/bdo info <door>
```

Shows the door's name and id, whether it is enabled, its world, its [evaluator](./evaluator.md), how long it stays open
and all its conditions. Click **[edit]** to put the matching command into your chat, ready to change. Click
**[remove]** to remove a condition.

```text
/bdo list
```

Lists your registered doors with id, name and world. With `bdo.accessAll` it lists all registered doors.

## Keep a door open

```text
/bdo stayOpen <door> <seconds>
```

After the door has fully opened, it stays open for this many seconds. The conditions are not checked during that
time. The default is `0`. This is useful for doors that open on a short event, like `itemClick` or `mythicMob`.

## Doors built in the open position

```text
/bdo invertOpen <door>
```

Big Doors Opener needs to know which position counts as open. If you created the door in its open position, run this
command once. Running it again switches it back.

## Pause a door

```text
/bdo setState <door> <enabled|disabled>
```

`disabled` pauses the door. Big Doors Opener no longer opens or closes it, but keeps all its settings. `enabled`
turns it back on.

## Copy settings to other doors

```text
/bdo copyCondition <source door> <target door> [group] [index]
```

Without a group, all conditions of the source door replace the conditions of the target door. This needs
`bdo.condition.all`. With a group, one condition of that group replaces the target's conditions of the group: the first
one, or the one with the index shown in `/bdo info`.

```text
/bdo cloneDoor <source door> <target door>
```

Copies everything: conditions, evaluator, stay open time and the open position setting. This needs
`bdo.condition.all`.

Both commands register the target door if it is not registered yet.

## Hand out keys

```text
/bdo giveKey <door> [amount] [player] [index]
```

Gives copies of the key item of the door's item condition. You need `bdo.command.givekey`.

| Argument | Required | What it means |
|---|---|---|
| `door` | yes | The door with the item condition. |
| `amount` | no | How many items. Default `64`. |
| `player` | no | Who gets them. An online player. Default: you. |
| `index` | no | Which item condition, if the door has several. Default `0`. |

From the console, `amount` and `player` are required.

## Stop managing a door

```text
/bdo unregister <door>
```

Removes all Big Doors Opener settings of the door. The door itself stays in Big Doors and works as before.

Doors that you delete in Big Doors are removed from Big Doors Opener on their own.

:::warning Unloaded worlds
Big Doors Opener also removes a door when the world of the door is not loaded while it checks the door. If you unload
worlds while the server runs, the doors in them lose their Big Doors Opener settings.
:::
