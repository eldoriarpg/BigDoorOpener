---
sidebar_position: 8
---

# Migration

## From version 2 to version 3

Version 3 runs on Paper 1.21.11 or newer with Java 25. Spigot is no longer supported. Your doors and settings are
taken over on the first start, you do not need to change anything yourself.

:::danger Make a backup first
Copy `plugins/BigDoorsOpener` before you update. Update from 2.x straight to 3.x and do not edit the files while the
server starts.
:::

What happens on the first start:

- The doors move from `config.yml` into the new file `doors.yml`. The old `config.yml` is kept as
  `config.yml.2.x.bak`.
- `config.yml` keeps your settings. `checkUpdates` is replaced by the `updates` section, see
  [Configuration](./reference/configuration.md).
- The message files use a new format. Your old files are kept in `messages.2.x.bak` and new ones are created. If you
  changed texts, copy them into the new files and write placeholders like `<door_name>` instead of `%DOOR_NAME%`.
- A condition from a plugin that is not installed anymore is skipped with a warning in the log.

The server log says how many doors were moved, for example
`Moved 12 door(s) from config.yml to doors.yml. The old file is kept as config.yml.2.x.bak.`

### What changed

- Players with `bdo.updates` hear about new versions. `bdo.command.reload` no longer does.
- `time` and `weather` are world conditions now. A door with only these conditions opens and closes even when nobody
  is near it.
- Custom evaluators count every condition of a group, see [The evaluator](./evaluator.md#variables).
- Players who are only users of a door in Big Doors cannot manage it anymore.
- Doors in worlds that are not loaded are kept instead of removed.

## From version 1 to version 2

Version 3 does not read the files of version 1. Update to 2.5.3 first, start the server once so the doors are
converted, then update to version 3.

Version 2 was nearly a complete rewrite. Doors in version 1 had a fixed set of settings: a permission, open and close
times and an open range. Version 2 uses [conditions](./conditions.md) instead.

You do not need to convert anything yourself. When version 2 starts with a version 1 `config.yml` (one without a
`version` key), it converts the file on its own.

:::danger Do not edit the config yourself
Let the plugin do the conversion. Make a backup of `plugins/BigDoorsOpener` before you update, then start the server
and leave `config.yml` alone.
:::

### What happens to your doors

Every version 1 door becomes a version 2 door with these conditions:

| Version 1 setting | Becomes |
|---|---|
| permission (if set) | a `permNode` condition with the same permission |
| open and close time (if they differ) | a `time` condition with the same ticks and `force` set to `false` |
| open range (if above 0) | a `proximity` condition with that range in every direction and the form `ellipsoid` |

The converted doors use the `and` [evaluator](./evaluator.md). A door with a permission and a range opens for a
player who has the permission **and** is close enough. Because `force` is off, the `time` condition only acts at the
open and close times. In between, the other conditions decide. If you want another combination, change the evaluator,
for example to `or` or a custom expression.

The server log lists each door and each condition that was added, ending with `Config migration to version 1
completed.`

### Check after the update

- **Doors built in the open position.** The `invertOpen` setting of version 1 is not carried over. Run
  `/bdo invertOpen <door>` again for these doors.
- **Time doors.** The converted `time` conditions do not use `force`. If a door should stay in the right state all
  the time, set the condition again with `force` set to `true`, for example
  `/bdo setCondition <door> time 0 12000 true`.
- **Evaluator.** Check with `/bdo info <door>` that each door behaves as you expect.

### Settings that changed

The old keys `approachRefreshRate` and `timedRefreshRate` are removed. They are replaced by `refreshRate`, set to `20`.
`checkUpdates` is added and set to `true`. See [Configuration](./reference/configuration.md).

### Message files

Version 2 added and changed many messages. New entries are added to your message files on their own, but changed texts
are not replaced. To get all current texts, delete the files in `plugins/BigDoorsOpener/messages` and restart the
server. They are created again.
