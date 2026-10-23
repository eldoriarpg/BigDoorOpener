---
title: Big Doors Opener
sidebar_position: 1
---

# Big Doors Opener

Big Doors Opener is an add-on for [Big Doors](https://modrinth.com/plugin/bigdoors). Big Doors lets you build
doors, drawbridges and gates that move. Big Doors Opener makes them open and close by themselves.

You tell a door *when* it should be open. That could be when a player walks up to it, holds a key, has a permission,
or when night falls. These rules are called **conditions**. You can give a door several conditions and decide how they
work together.

## What it can do

- **Open doors with a key item.** Hold it, carry it, click with it or click a keyhole block with it. Keys can be used
  up when the door opens. See [Conditions](./conditions.md).
- **Open doors when players come close.** Use a box, ball or cylinder around the door, a region you click out, or a
  WorldGuard region.
- **Open doors by permission.** Use any permission node, or the access a player has to the door in Big Doors.
- **Open doors by world time or weather.** Open the city gate at dawn and close it at dusk.
- **Use PlaceholderAPI and MythicMobs.** Open a door based on placeholder values, or when a certain MythicMobs mob
  dies.
- **Combine conditions.** All of them must be true, one is enough, or you write your own rule. See
  [The evaluator](./evaluator.md).
- **Manage many doors.** Copy conditions or whole door settings from one door to another. Clickable chat messages let
  you edit and remove conditions. See [Managing doors](./managing-doors.md).

## Requirements

| What | Version |
|---|---|
| Server | Paper 1.21.11 or newer. Spigot is not supported. |
| Java | 25 or newer |
| Big Doors | required, the classic 0.1.8 versions. |
| NashornJs | needed for [custom evaluators](./evaluator.md) and the `placeholder` condition |
| WorldGuard | optional, version 7, for the region conditions |
| PlaceholderAPI | optional, for the `placeholder` condition |
| MythicMobs | optional, for the `mythicMob` condition |

:::info Which Big Doors?
Big Doors Opener works with the classic Big Doors plugin (the 0.1.8 versions). The plugin must be called
`BigDoors` on your server.
:::

## Download

Get the latest version from [Modrinth](https://modrinth.com/plugin/2mTWavvv) or
[lyna.eldoria.de](https://lyna.eldoria.de/products/3).

## Where to go next

- [Installation](./installation.md) puts the plugin on your server.
- [Getting started](./getting-started.md) makes your first door open by itself.
- [Conditions](./conditions.md) lists every condition and its settings.
- [The evaluator](./evaluator.md) explains how conditions are combined.
- [Commands](./reference/commands.md), [Permissions](./reference/permissions.md) and
  [Configuration](./reference/configuration.md) are the reference pages.
- [Migration](./migration.md) explains what happens when you update from an older version.
