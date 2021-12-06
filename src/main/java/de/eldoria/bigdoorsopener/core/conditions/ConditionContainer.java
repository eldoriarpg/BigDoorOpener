/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2021 EldoriaRPG Team and Contributor
 */

package de.eldoria.bigdoorsopener.core.conditions;

import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.eldoutilities.consumer.QuadConsumer;
import de.eldoria.eldoutilities.functions.TriFunction;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.localization.Replacement;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConditionContainer {
    private final Class<? extends DoorCondition> clazz;
    private final Scope scope;
    private final String name;
    private final String group;
    private final int cost;
    private final QuadConsumer<Player, MessageSender, Consumer<DoorCondition>, String[]> create;
    private final TriFunction<CommandSender, ILocalizer, String[], List<String>> onTabComplete;

    private ConditionContainer(Class<? extends DoorCondition> clazz, Scope scope, String name, String group, int cost,
                               QuadConsumer<Player, MessageSender, Consumer<DoorCondition>, String[]> create,
                               TriFunction<CommandSender, ILocalizer, String[], List<String>> onTabComplete) {
        this.clazz = clazz;
        this.scope = scope;
        this.name = name;
        this.group = group;
        this.cost = cost;
        this.create = create;
        this.onTabComplete = onTabComplete;
    }

    public static Builder ofClass(Class<? extends DoorCondition> clazz, Scope scope) {
        return new Builder(clazz, scope);
    }

    public Class<? extends DoorCondition> getClazz() {
        return clazz;
    }

    public Scope getScope() {
        return scope;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public int getCost() {
        return cost;
    }

    /**
     * Creates a new condition and adds it to the condition bag if the creation was successful.
     *
     * @param player        player which wants to create this condition.
     * @param messageSender message sender to send messages
     * @param conditionBag  condition bag where the condition should be set
     * @param arguments     arguments for condition creation
     */
    public void create(Player player, MessageSender messageSender, Consumer<DoorCondition> conditionBag, String[] arguments) {
        create.accept(player, messageSender, conditionBag, arguments);
    }

    /**
     * Handles the tab completion event for the wrapped condition.
     *
     * @param sender    Source of the command.  For players tab-completing a command inside of a command block, this
     *                  will be the player, not the command block.
     * @param localizer localizer for argument localization
     * @param args      The arguments passed to the command, including final partial argument to be completed and
     *                  command label
     * @return A List of possible completions for the final argument, or null to default to the command executor
     */
    public List<String> onTabComplete(CommandSender sender, ILocalizer localizer, String[] args) {
        return onTabComplete.apply(sender, localizer, args);
    }

    public static final class Builder {
        private final Class<? extends DoorCondition> clazz;
        private final Scope scope;
        private String name = null;
        private String group = null;
        private int cost = 50;
        private QuadConsumer<Player, MessageSender, Consumer<DoorCondition>, String[]> create = null;
        private TriFunction<CommandSender, ILocalizer, String[], List<String>> onTabComplete = (sender, localizer, strings) -> Collections.emptyList();

        /**
         * Creates a new builder instance for a condition of type {@code clazz}.
         *
         * @param clazz the class where the condition is for.
         * @param scope scope of the condition
         */
        private Builder(Class<? extends DoorCondition> clazz, Scope scope) {
            this.clazz = clazz;
            this.scope = scope;
        }

        /**
         * This consumer must satisfy these rules:
         * <p>- If a condition was added to the condition bag, a message must be send to the player via {@link
         * MessageSender#sendLocalizedMessage(CommandSender, String, Replacement...)}
         * <p>- If the condition can't be set, a message must be send to the player via {@link
         * MessageSender#sendLocalizedError(CommandSender, String, Replacement...)}
         * <p>- Do not remove any condition from the bag here.
         * <p>- Do not try to save the config.
         * <p>- The String[] input is only input for the condition itself. It does not contain anything else and may be
         * empty.
         * <p>- Use the message sender to send messages.
         *
         * @param create a consumer, which fullfills the rules above.
         * @return this builder instance
         */
        public Builder withFactory(QuadConsumer<Player, MessageSender, Consumer<DoorCondition>, String[]> create) {
            this.create = create;
            return this;
        }

        /**
         * This quad function should handle the tab completion event like defined in {@link
         * TabCompleter#onTabComplete(CommandSender, Command, String, String[])}
         *
         * @param onTabComplete a quad function which i
         * @return this builder instance
         */
        public Builder onTabComplete(TriFunction<CommandSender, ILocalizer, String[], List<String>> onTabComplete) {
            this.onTabComplete = onTabComplete;
            return this;
        }

        /**
         * Set the meta for this condition
         *
         * @param name name and group of the condition. case sensitive.
         * @param cost cost of the condition. describes the costs to calculate the condition in the given scope.
         * @return this builder instance
         */
        public Builder withMeta(String name, int cost) {
            return withMeta(name, name, cost);
        }

        /**
         * Set the meta for the condition.
         *
         * @param name  name of the condition. case sensitive.
         * @param group group of the condition. case sensitive.
         * @param cost  cost of the condition. describes the costs to calculate the condition in the given scope.
         * @return this builder instance
         */
        public Builder withMeta(String name, String group, int cost) {
            this.name = name;
            this.group = group;
            this.cost = cost;
            return this;
        }

        public ConditionContainer build() {
            return new ConditionContainer(clazz, scope, Objects.requireNonNull(name), group, cost, create, onTabComplete);
        }

        /**
         * Provides estimated cost for conditions.
         */
        public enum Cost {
            PLAYER_HIGH(100),
            PLAYER_MEDIUM(50),
            PLAYER_LOW(10),
            WORLD_HIGH(100),
            WORLD_MEDIUM(50),
            WORLD_LOW(10);
            public final int cost;

            Cost(int cost) {

                this.cost = cost;
            }
        }
    }

}
