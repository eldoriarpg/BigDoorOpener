---
sidebar_position: 1
---

# Commands

The base command is `/bigdoorsopener`. The short form is `/bdo`. `/bdo` on its own shows the help. Sub-command names
are not case sensitive.

`<door>` is the name of a door you own, or the id of any door. See
[Which doors you can manage](../managing-doors.md#which-doors-you-can-manage).

| Command | Permission | What it does |
|---|---|---|
| `/bdo setCondition <door> <condition> [settings]` | `bdo.command.use` and a condition permission | Set a condition. Replaces the other conditions of its group. |
| `/bdo addCondition <door> <condition> [settings]` | `bdo.command.use` and a condition permission | Add a condition. Keeps the other conditions of its group. |
| `/bdo removeCondition <door> <group> [index]` | `bdo.command.use` and a condition permission | Remove a condition. |
| `/bdo setEvaluator <door> <and\|or\|custom> [expression]` | `bdo.command.use`, for `custom` also `bdo.customEvaluator` | Choose how conditions are combined. |
| `/bdo info <door>` | `bdo.command.use` | Show the settings of a door with clickable edit and remove buttons. |
| `/bdo list` | `bdo.command.use` | List registered doors. |
| `/bdo stayOpen <door> <seconds>` | `bdo.command.use` | Keep the door open for some seconds after it opened. |
| `/bdo invertOpen <door>` | `bdo.command.use` | Swap open and closed, for doors built in the open position. |
| `/bdo setState <door> <enabled\|disabled>` | `bdo.command.use` | Pause or resume a door. |
| `/bdo copyCondition <source> <target> [group]` | `bdo.command.use` and a condition permission | Copy conditions to another door. |
| `/bdo cloneDoor <source> <target>` | `bdo.command.use` and `bdo.condition.all` | Copy all settings to another door. |
| `/bdo giveKey <door> [amount] [player] [index]` | `bdo.command.givekey` | Give copies of a door's key item. |
| `/bdo unregister <door>` | `bdo.command.use` | Remove all Big Doors Opener settings of a door. |
| `/bdo reload` | `bdo.command.reload` | Reload the config and the doors. |
| `/bdo debug` | `bdo.command.reload` | Upload debug data for support. |
| `/bdo about` | none | Show the version and support links. |
| `/bdo help` | none | Show the command list. |

"A condition permission" means the permission of the condition's group, for example `bdo.condition.item`, or
`bdo.condition.all`. See [Permissions](./permissions.md).

## Conditions

### setCondition and addCondition

```text
/bdo setCondition <door> <condition> [settings]
/bdo addCondition <door> <condition> [settings]
```

| Argument | Required | What it means |
|---|---|---|
| `door` | yes | The door. It is registered with Big Doors Opener if it was not yet. |
| `condition` | yes | The condition name, for example `proximity`. Type it exactly, tab completion helps. |
| `settings` | depends | The settings of the condition. |

All conditions and their settings are on the [Conditions](../conditions.md) page.

### removeCondition

```text
/bdo removeCondition <door> <group> [index]
```

| Argument | Required | What it means |
|---|---|---|
| `door` | yes | The door. |
| `group` | yes | The condition group, for example `item` or `location`. |
| `index` | no | Which condition of the group. Default `0`, the first one. `/bdo info` shows the index. |

### setEvaluator

```text
/bdo setEvaluator <door> <and|or|custom> [expression]
```

| Argument | Required | What it means |
|---|---|---|
| `door` | yes | The door. |
| `and`, `or` or `custom` | yes | The evaluator. |
| `expression` | for `custom` | Your rule, for example `(item and location) or permission`. |

See [The evaluator](../evaluator.md).

## Door settings

### stayOpen

```text
/bdo stayOpen <door> <seconds>
```

After the door has fully opened, it stays open this many seconds without checking its conditions. `0` turns it off.

### invertOpen

```text
/bdo invertOpen <door>
```

Swaps which position counts as open. Run it again to swap back.

### setState

```text
/bdo setState <door> <enabled|disabled>
```

A disabled door keeps its settings but is not opened or closed by Big Doors Opener.

### unregister

```text
/bdo unregister <door>
```

Removes the door and all its settings from Big Doors Opener. The door stays in Big Doors.

## Copying

### copyCondition

```text
/bdo copyCondition <source> <target> [group] [index]
```

| Argument | Required | What it means |
|---|---|---|
| `source` | yes | The door to copy from. |
| `target` | yes | The door to copy to. |
| `group` | no | Only copy a condition of this group. Without it, all conditions are copied, which needs `bdo.condition.all`. |
| `index` | no | Which condition of the group to copy, as numbered in `/bdo info`. Default `0`. |

### cloneDoor

```text
/bdo cloneDoor <source> <target>
```

Copies conditions, evaluator, stay open time and the `invertOpen` setting from `source` to `target`.

## Keys

### giveKey

```text
/bdo giveKey <door> [amount] [player] [index]
```

| Argument | Required | What it means |
|---|---|---|
| `door` | yes | A registered door with an item condition. |
| `amount` | no, yes from the console | Number of items. Default `64`. |
| `player` | no, yes from the console | An online player who gets the items. Default: you. |
| `index` | no | Which item condition, if there are several. Default `0`. |

## Plugin

### reload

```text
/bdo reload
```

Reads `config.yml` again, including the doors, and restarts the door checks. Players with this permission also get a
message when an update is available.

### debug

```text
/bdo debug
```

Sends debug data to the developers' server, so they can help you. It includes your installed plugins, the latest log,
server information and the plugin's config. The first time, you see what is sent
and have to run the command again to agree. You then get a link to view the data and a link to delete it. The data
is deleted after 14 days. Besides `bdo.command.reload`, the permission `de.eldoria.eldoutilitites.debug` also allows
this command.

### about and help

`/bdo about` shows the plugin version, the author and the support links. `/bdo help` lists all commands.
