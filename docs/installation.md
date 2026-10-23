---
sidebar_position: 2
---

# Installation

## Install the plugin

1. Make sure you run Paper 1.21.11 or newer on Java 25, and that [Big Doors](https://modrinth.com/plugin/bigdoors) is
   installed and works.
2. Download Big Doors Opener from [Modrinth](https://modrinth.com/plugin/2mTWavvv) or
   [lyna.eldoria.de](https://lyna.eldoria.de/products/3).
3. Put the jar into the `plugins` folder of your server.
4. Start the server.

Big Doors Opener creates the folder `plugins/BigDoorsOpener`. It holds `config.yml` with the settings, `doors.yml`
with your doors and a `messages` folder with the language files. See [Configuration](./reference/configuration.md).

:::note Libraries are downloaded on the first start
Big Doors Opener is a Paper plugin. On the first start Paper downloads the libraries it needs from Maven Central and
`eldonexus.de` and keeps them in the `libraries` folder of your server. The server needs internet access for that once.
:::

:::warning Big Doors must be running
If Big Doors is missing or did not start, Big Doors Opener turns itself off. Look for `Hooked into Big Doors
successfully.` in your server log.
:::

## Optional plugins

Big Doors Opener finds these plugins on its own when they are installed. You do not need to set anything up.

| Plugin | What it adds |
|---|---|
| NashornJs | The script engine for [custom evaluators](./evaluator.md) and the `placeholder` condition. |
| WorldGuard 7 | The `region` and `worldRegion` conditions. Older WorldGuard versions are not used. |
| PlaceholderAPI | The `placeholder` condition. |
| MythicMobs | The `mythicMob` condition. |

The server log tells you what was found, for example `World Guard found. Trying to get a hook.` or
`Placeholder API not found. Placeholder usage is disabled.`

### NashornJs

Custom evaluators and the `placeholder` condition are small JavaScript expressions. Java 15 and newer no longer come
with a JavaScript engine, and Big Doors Opener needs Java 25. So you need the
[NashornJs](https://www.spigotmc.org/resources/91204/) plugin if you want to use either of them.

If no engine is found, the log shows `No script engine found.` The rest of the plugin works fine. Only custom
evaluators and placeholder conditions do not.

## Give permissions

Operators can use everything. For other players, see [Permissions](./reference/permissions.md). A typical builder
needs `bdo.command.use` and the condition permissions for the conditions they should use.

## Next step

Head over to [Getting started](./getting-started.md) and make your first door open by itself.
