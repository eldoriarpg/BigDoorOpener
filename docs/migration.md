---
sidebar_position: 8
---

# Migrating from version 1

Version 2 was nearly a complete rewrite. Doors in version 1 had a fixed set of settings: a permission, open and close
times and an open range. Version 2 uses [conditions](./conditions.md) instead.

You do not need to convert anything yourself. When version 2 starts with a version 1 `config.yml` (one without a
`version` key), it converts the file on its own.

:::danger Do not edit the config yourself
Let the plugin do the conversion. Make a backup of `plugins/BigDoorsOpener` before you update, then start the server
and leave `config.yml` alone.
:::

## What happens to your doors

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

## Check after the update

- **Doors built in the open position.** The `invertOpen` setting of version 1 is not carried over. Run
  `/bdo invertOpen <door>` again for these doors.
- **Time doors.** The converted `time` conditions do not use `force`. If a door should stay in the right state all
  the time, set the condition again with `force` set to `true`, for example
  `/bdo setCondition <door> time 0 12000 true`.
- **Evaluator.** Check with `/bdo info <door>` that each door behaves as you expect.

## Settings that changed

The old keys `approachRefreshRate` and `timedRefreshRate` are removed. They are replaced by `refreshRate`, set to `20`.
`checkUpdates` is added and set to `true`. See [Configuration](./reference/configuration.md).

## Message files

Version 2 added and changed many messages. New entries are added to your message files on their own, but changed texts
are not replaced. To get all current texts, delete the files in `plugins/BigDoorsOpener/messages` and restart the
server. They are created again.
