---
sidebar_position: 2
---

# Permissions

Big Doors Opener does not give any permission to normal players by default. Operators have all of them. The console
may use every command.

## Commands

| Permission | What it allows |
|---|---|
| `bdo.command.use` | All door commands: `setCondition`, `addCondition`, `removeCondition`, `setEvaluator`, `info`, `list`, `stayOpen`, `invertOpen`, `setState`, `copyCondition`, `cloneDoor`, `unregister`. |
| `bdo.command.givekey` | `/bdo giveKey`. |
| `bdo.command.reload` | `/bdo reload` and `/bdo debug`. Also shows a message when an update is available. |
| `bdo.customEvaluator` | Setting a [custom evaluator](../evaluator.md#custom). |
| `bdo.accessAll` | Managing doors that belong to other players. Also makes `/bdo list` show all doors. |
| `de.eldoria.eldoutilitites.debug` | `/bdo debug`, as an alternative to `bdo.command.reload`. |

## Conditions

To set, add, remove or copy a condition, a player needs `bdo.command.use` and the permission for the condition's group.

| Permission | Conditions |
|---|---|
| `bdo.condition.item` | `itemHolding`, `itemOwning`, `itemClick`, `itemBlock` |
| `bdo.condition.location` | `proximity`, `region`, `simpleRegion` |
| `bdo.condition.worldLocation` | `worldProximity`, `worldRegion`, `worldSimpleRegion` |
| `bdo.condition.permission` | `permNode`, `doorPerm` |
| `bdo.condition.time` | `time` |
| `bdo.condition.weather` | `weather` |
| `bdo.condition.placeholder` | `placeholder` |
| `bdo.condition.mythicMob` | `mythicMob` |
| `bdo.condition.all` | all conditions. Also needed to copy all conditions at once and for `cloneDoor`. |

## Example

A builder who sets up doors with keys and areas, but no custom evaluators:

```text
bdo.command.use
bdo.command.givekey
bdo.condition.item
bdo.condition.location
```
