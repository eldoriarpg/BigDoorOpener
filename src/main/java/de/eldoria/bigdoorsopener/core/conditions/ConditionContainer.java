/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.conditions;

import com.fasterxml.jackson.annotation.JsonTypeName;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import de.eldoria.eldoutilities.commands.command.util.Arguments;
import de.eldoria.eldoutilities.commands.exceptions.CommandException;
import de.eldoria.eldoutilities.localization.ILocalizer;
import de.eldoria.eldoutilities.messages.MessageSender;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Describes a registered condition: its class, scope, name, group, how it is created from a command and how its
 * arguments are completed.
 * <p>
 * The condition class must carry {@link JsonTypeName}. That name identifies the condition in {@code doors.yml}.
 */
public final class ConditionContainer {
    private final Class<? extends DoorCondition> clazz;
    private final Scope scope;
    private final String name;
    private final String group;
    private final String typeName;
    private final int cost;
    private final ConditionFactory create;
    private final ConditionCompleter onTabComplete;

    private ConditionContainer(
            Class<? extends DoorCondition> clazz,
            Scope scope,
            String name,
            String group,
            String typeName,
            int cost,
            ConditionFactory create,
            ConditionCompleter onTabComplete) {
        this.clazz = clazz;
        this.scope = scope;
        this.name = name;
        this.group = group;
        this.typeName = typeName;
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

    /**
     * The name which identifies the condition in the stored doors.
     *
     * @return type name of the condition
     */
    public String typeName() {
        return typeName;
    }

    public int getCost() {
        return cost;
    }

    /**
     * Creates a new condition and hands it to the condition bag if the creation was successful.
     *
     * @param sender        sender which wants to create this condition
     * @param messageSender message sender to send messages
     * @param args          arguments for condition creation
     * @param conditionBag  receives the created condition
     * @throws CommandException when the condition can not be created
     */
    public void create(
            CommandSender sender, MessageSender messageSender, Arguments args, Consumer<DoorCondition> conditionBag)
            throws CommandException {
        create.create(sender, messageSender, args, conditionBag);
    }

    /**
     * Completes the arguments of this condition.
     *
     * @param sender    sender of the command
     * @param localizer localizer for argument hints
     * @param args      arguments of the condition, including the partial argument to complete
     * @return possible completions of the last argument
     */
    public List<String> onTabComplete(CommandSender sender, ILocalizer localizer, Arguments args) {
        return onTabComplete.complete(sender, localizer, args);
    }

    public static final class Builder {
        private final Class<? extends DoorCondition> clazz;
        private final Scope scope;
        private String name = null;
        private String group = null;
        private int cost = 50;
        private ConditionFactory create = null;
        private ConditionCompleter onTabComplete = (sender, localizer, args) -> Collections.emptyList();

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
         * Sets the factory which creates the condition from a command. See {@link ConditionFactory} for its rules.
         *
         * @param create factory of the condition
         * @return this builder instance
         */
        public Builder withFactory(ConditionFactory create) {
            this.create = create;
            return this;
        }

        /**
         * Sets the completion of the condition arguments.
         *
         * @param onTabComplete completer of the condition arguments
         * @return this builder instance
         */
        public Builder onTabComplete(ConditionCompleter onTabComplete) {
            this.onTabComplete = onTabComplete;
            return this;
        }

        /**
         * Set the meta for this condition
         *
         * @param name name and group of the condition.
         * @param cost cost of the condition. describes the costs to calculate the condition in the given scope.
         * @return this builder instance
         */
        public Builder withMeta(String name, int cost) {
            return withMeta(name, name, cost);
        }

        /**
         * Set the meta for the condition.
         *
         * @param name  name of the condition.
         * @param group group of the condition.
         * @param cost  cost of the condition. describes the costs to calculate the condition in the given scope.
         * @return this builder instance
         */
        public Builder withMeta(String name, String group, int cost) {
            this.name = name;
            this.group = group;
            this.cost = cost;
            return this;
        }

        /**
         * Builds the container.
         *
         * @return new container
         * @throws IllegalStateException when the condition class has no {@link JsonTypeName}
         */
        public ConditionContainer build() {
            JsonTypeName typeName = clazz.getAnnotation(JsonTypeName.class);
            if (typeName == null) {
                throw new IllegalStateException("Missing @JsonTypeName on condition " + clazz.getName());
            }
            return new ConditionContainer(
                    clazz,
                    scope,
                    Objects.requireNonNull(name),
                    group,
                    typeName.value(),
                    cost,
                    Objects.requireNonNull(create),
                    onTabComplete);
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
