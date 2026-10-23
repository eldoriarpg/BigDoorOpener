/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.cooldown;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.bigdoorsopener.core.conditions.ConditionInput;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;
import de.eldoria.bigdoorsopener.util.C;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.Replacement;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Base of the cooldown conditions. A cooldown starts when Big Doors Opener opens the door and blocks opening it again
 * until the cooldown is over.
 * <p>
 * While the cooldown runs the condition is {@code false} as long as the door is closed, and has no opinion while the
 * door is still open, so the door is not closed the moment its cooldown starts. After the cooldown it is {@code true}.
 * Cooldowns are not stored and start over with a restart.
 */
public abstract class Cooldown implements DoorCondition {
    private static final String SYNTAX = "<$syntax.seconds$>";
    private final int seconds;

    protected Cooldown(int seconds) {
        this.seconds = seconds;
    }

    /**
     * Reads the length of a new cooldown.
     *
     * @param args arguments of the condition
     * @return seconds of the cooldown
     * @throws CommandException when the seconds are missing or not positive
     */
    protected static int parseSeconds(Arguments args) throws CommandException {
        ConditionInput.require(args, 1, DoorCondition.localizer(), SYNTAX);
        int seconds = args.asInt(0);
        if (seconds < 1) throw CommandException.message("error.tooSmall", Replacement.create("MIN", 1));
        return seconds;
    }

    protected static List<String> complete(CommandSender sender, ILocalizer localizer, Arguments args) {
        if (args.sizeIs(1)) {
            return Collections.singletonList("<" + localizer.getMessage("syntax.seconds") + ">");
        }
        return Collections.emptyList();
    }

    /**
     * Decides the state for a cooldown that started at a point in time.
     *
     * @param started      start of the cooldown, null if it never started
     * @param currentState whether the door is open
     * @return true after the cooldown, false while it runs and the door is closed, null while it runs and the door is
     *     open
     */
    protected @Nullable Boolean state(@Nullable Instant started, boolean currentState) {
        if (started == null || !Instant.now().isBefore(started.plus(Duration.ofSeconds(seconds)))) return true;
        return currentState ? null : false;
    }

    public int seconds() {
        return seconds;
    }

    protected Component describe(ILocalizer localizer, String typeKey) {
        return DoorCondition.headline(localizer, typeKey, getClass())
                .append(Component.newline())
                .append(Component.text(localizer.getMessage("conditionDesc.cooldown") + " ", C.baseColor))
                .append(Component.text(seconds + "s", C.highlightColor));
    }

    @Override
    public String getRemoveCommand(ConditionalDoor door) {
        return REMOVE_COMMAND + door.doorUID() + " cooldown";
    }

    @Override
    public abstract Cooldown clone();
}
