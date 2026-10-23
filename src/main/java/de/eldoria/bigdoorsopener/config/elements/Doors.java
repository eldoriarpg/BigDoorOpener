/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.config.elements;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.eldoria.bigdoorsopener.door.ConditionalDoor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The registered doors in {@code doors.yml}, by their Big Doors id.
 */
public class Doors {
    @JsonIgnore
    private final Map<Long, ConditionalDoor> doors = new LinkedHashMap<>();

    public Doors() {
    }

    @JsonCreator
    private Doors(@JsonProperty("doors") List<ConditionalDoor> doors) {
        if (doors == null) return;
        doors.stream().filter(Objects::nonNull).forEach(door -> this.doors.put(door.doorUID(), door));
    }

    @JsonProperty("doors")
    private List<ConditionalDoor> list() {
        return new ArrayList<>(doors.values());
    }

    /**
     * @return the doors by id, backed by this file
     */
    public Map<Long, ConditionalDoor> byId() {
        return doors;
    }
}
