---
sidebar_position: 3
---

# Configuration

Big Doors Opener keeps its files in `plugins/BigDoorsOpener`:

- `config.yml` holds the settings and all door data.
- The `messages` folder holds the language files.

## config.yml

```yaml
version: 1
checkUpdates: true
refreshRate: 20
language: en_US
enableMetrics: true
jsCacheSize: 400
playerCheckRadius: 200
doors:
```

| Key | Default | What it does |
|---|---|---|
| `version` | `1` | Version of the config format. Do not change it. |
| `checkUpdates` | `true` | Checks for new versions. Players with `bdo.command.reload` are told when one is out. |
| `refreshRate` | `20` | How many ticks it takes to check every door once. 20 ticks are one second. Lower values make doors react faster but cost more. |
| `language` | `en_US` | Language of the messages. `en_US` and `de_DE` are included. |
| `enableMetrics` | `true` | Sends anonymous usage statistics to bStats. |
| `jsCacheSize` | `400` | How many results of [custom evaluators](../evaluator.md#custom) and `placeholder` conditions are remembered. |
| `playerCheckRadius` | `200` | Only players within this many blocks of a door are checked for player conditions. |
| `doors` | empty | The data of all your doors. Written by the plugin. |

### refreshRate

Big Doors Opener does not check all doors at once. It spreads the work over the ticks, so that every door is checked
once within `refreshRate` ticks. With 100 doors and the default of 20, about 5 doors are checked each tick. The default
is fine unless you have a very large number of doors.

### jsCacheSize

The same expression with the same values always gives the same result, so results are kept for reuse. A bigger cache
means less work for the server.

- With custom evaluators, use at least 200.
- With placeholder conditions, use at least 400.
- With both, use at least 600.

Below 200 you get a warning in the log. Values below 10 are raised to 10.

### playerCheckRadius

This works like a big [proximity](../conditions.md#proximity) box around every door. Players outside of it are ignored
for player conditions. It keeps the plugin fast on busy servers. Values below 100 are not recommended, and 200 works
well.

[World location](../conditions.md#world-location-conditions) conditions look at every player in the world. If a door
has only world location conditions, the radius does not matter for it.

### doors

Do not edit this section by hand. Use the [commands](./commands.md) instead. The plugin writes this section every time
a door changes, so changes you make while the server runs are lost.

## What a reload changes

`/bdo reload` reads `config.yml` again. `refreshRate`, `playerCheckRadius` and the doors take effect right away.
`language`, `jsCacheSize`, `checkUpdates` and `enableMetrics` need a server restart.

## Language files

The `messages` folder contains `messages_en_US.properties` and `messages_de_DE.properties`. You can change any text
in them. Missing entries are added again when the server starts.

To add your own language, copy a file, name it after the language, for example `messages_fr_FR.properties`, translate
it and set `language: fr_FR`.
