---
sidebar_position: 3
---

# Getting started

This page walks you through your first automatic door. You need a door made with Big Doors and the
`bdo.command.use` permission (operators have it).

The main command is `/bigdoorsopener`. The short form is `/bdo`. Every command has tab completion, so pressing Tab
always shows you what comes next.

## 1. Build a door

Create a door with Big Doors as usual and leave it **closed**. In this guide the door is called `gate`.

You can point at a door in two ways:

- By its **name**, for doors you own. If you own several doors with the same name, use the id instead.
- By its **id**, the number Big Doors gives every door. You need the id for doors you do not own.

## 2. Add a condition

Let the door open when a player comes close:

```text
/bdo setCondition gate proximity 5
```

`proximity` is the condition. `5` means up to 5 blocks away from the middle of the door, in every direction. Walk up to
the door and it opens. Walk away and it closes again.

That is it. The first condition you add turns the door into a door that Big Doors Opener controls. From now on,
Big Doors' own auto close timer is not used for this door.

## 3. Add a second condition

Maybe only guards should get in. Add a permission condition:

```text
/bdo setCondition gate permNode castle.guard
```

Now the door has two conditions. By default **all** conditions must be true, so the door only opens for a player who is
close *and* has `castle.guard`.

If one condition should be enough, change the [evaluator](./evaluator.md):

```text
/bdo setEvaluator gate or
```

## 4. Check your door

```text
/bdo info gate
```

This shows the door's state, its evaluator, how long it stays open and every condition. Each condition has a
**[remove]** and an **[edit]** button you can click.

## 5. Fine tune

- **Keep it open a while.** `/bdo stayOpen gate 10` keeps the door open for 10 seconds after it opened.
- **Built it open?** If you created the door in its open position, run `/bdo invertOpen gate`. Otherwise
  Big Doors Opener sees open and closed the wrong way round.
- **Pause it.** `/bdo setState gate disabled` stops the automatic opening. `enabled` turns it back on.

More on this in [Managing doors](./managing-doors.md).

## Good to know

- Doors are checked in turns, not all at once. A door can take up to about a second to react. See `refreshRate` in
  [Configuration](./reference/configuration.md).
- Conditions about players only look at players near the door, 200 blocks by default. See `playerCheckRadius`.
- Doors in unloaded chunks, locked doors and doors that are moving are skipped.
- If you delete a door in Big Doors, Big Doors Opener forgets it on its own.

## Where to go next

- [Conditions](./conditions.md) lists everything a door can react to.
- [The evaluator](./evaluator.md) shows how to combine conditions, including your own rules.
- [Commands](./reference/commands.md) has every command in one place.
