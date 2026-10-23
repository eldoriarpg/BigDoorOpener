---
sidebar_position: 5
---

# The evaluator

A door often has more than one [condition](./conditions.md). The evaluator decides how their answers are combined into
one decision: open or closed.

```text
/bdo setEvaluator <door> <and|or|custom> [expression]
```

There are three evaluators. New doors use `and`.

| Evaluator | The door opens when |
|---|---|
| `and` | every condition says yes |
| `or` | at least one condition says yes |
| `custom` | your own expression is true |

## and

All conditions must say yes. Conditions that have no opinion right now (see [Conditions](./conditions.md)) are left
out. If no condition has an opinion, the door stays as it is.

```text
/bdo setEvaluator gate and
```

Example: a door with `itemHolding` and `proximity` only opens for a player who holds the key **and** stands close.

## or

One yes is enough. If no condition says yes and at least one says no, the door closes. If no condition has an opinion,
the door stays as it is.

```text
/bdo setEvaluator gate or
```

Example: a door with `permNode` and `time` opens for players with the permission, **or** when it is daytime.

## custom

When `and` and `or` are not enough, write your own rule. You need the `bdo.customEvaluator` permission and the
[NashornJs](./installation.md#nashornjs) plugin.

```text
/bdo setEvaluator gate custom (item and location) or permission
```

The expression is a small piece of JavaScript. Only a few words and signs are allowed, so nothing in it can slow
down your server.

### Variables

Each condition group is a variable. It holds the answer of the condition in that group.

| Variable | Condition group |
|---|---|
| `item` | `itemHolding`, `itemOwning`, `itemClick`, `itemBlock` |
| `location` | `proximity`, `region`, `simpleRegion` |
| `worldLocation` | `worldProximity`, `worldRegion`, `worldSimpleRegion` |
| `permission` | `permNode`, `doorPerm` |
| `time` | `time` |
| `weather` | `weather` |
| `placeholder` | `placeholder` |
| `mythicMob` | `mythicMob` |
| `currentState` | not a condition: `true` if the door is open right now, `false` if it is closed |

A variable is `true` or `false`, or `null` when the condition has no opinion. A variable whose group has no condition
on the door is also `null`.

If a group holds more than one condition (added with `addCondition`), the variable is `true` only when all of them are
`true`, like with the `and` evaluator. Conditions without an opinion are left out; when none of them has one, the
variable is `null`.

### Words and signs

You can write the logic as words or as JavaScript signs. Words need a space on both sides.

| Word | Sign | Meaning |
|---|---|---|
| `and` | `&&` | both sides must be true |
| `or` | `\|\|` | one side must be true |
| `is` | `==` | both sides are equal |
| `is not` | `!=` | both sides are different |
| | `!` | turns true into false and false into true |

You can also use `true`, `false`, `null`, round brackets `( )`, and `if` and `else` with curly brackets `{ }`.
Everything else is rejected, for example numbers and `<` or `>`.

:::warning `not` is not "not"
The word `not` is turned into `!=` ("is different"), not into `!`. To flip a value, write `!` in front of it, for
example `!permission`.
:::

When you save the expression, the words are replaced by signs and all spaces are removed. `/bdo info` shows the
stored form, for example `(item&&location)||permission`.

### How null behaves

`null` is JavaScript's "nothing". In short:

- `null and x` gives `null`. `null or true` gives `true`.
- `!null` gives `true`.
- If the whole expression gives `null`, the door stays as it is.

### Examples

These all pass the check that runs when you save an evaluator.

**Key at the door, or a permission from anywhere near:**

```text
/bdo setEvaluator gate custom (item and location) or permission
```

**Players close by, except those with the permission** (for example a jail door that should not open for guards):

```text
/bdo setEvaluator cell custom location and !permission
```

**Open with the key. Then stay open while someone is close, even without the key:**

```text
/bdo setEvaluator gate custom item or (currentState and location)
```

**Open for everyone in the day, at night only for players with the permission who stand at the door.** Give the door
a `time` condition with `force` set to `true`, a `proximity` condition and a `permNode` condition:

```text
/bdo setEvaluator gate custom if (time) {true} else {location and permission}
```

### When saving fails

The expression is checked before it is saved. If something is wrong, you get one of these messages:

| Message | What to fix |
|---|---|
| `The parenthesis are unbalanced.` | Every `(` needs a `)`. |
| `"..." is not a valid variable.` | Only the variables above and `true`, `false`, `null`, `if`, `else` are allowed. |
| `"..." is not a valid operator.` | Use `&&`, `\|\|`, `==`, `!=` or `!`. A single `&`, `\|` or `=` is not allowed. |
| `"..." is not valid syntax.` | The expression contains a sign that is not allowed. |
| `Could not execute evaluator "...".` | The expression is not valid JavaScript, or no script engine is installed. |
| `Result is not a boolean "...".` | The expression must give `true` or `false`. |

### Hints from the plugin

When you use a custom evaluator, Big Doors Opener watches your changes:

- If you set a condition whose group is not in the expression, you get a warning. That condition has no effect until
  you add its variable.
- If you remove a condition whose variable is still in the expression, you get a warning too.
