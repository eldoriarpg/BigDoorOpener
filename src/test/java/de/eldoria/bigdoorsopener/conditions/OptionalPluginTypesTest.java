/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions;

import de.eldoria.bigdoorsopener.conditions.item.ItemHolding;
import de.eldoria.bigdoorsopener.conditions.item.ItemOwning;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemBlock;
import de.eldoria.bigdoorsopener.conditions.item.interacting.ItemClick;
import de.eldoria.bigdoorsopener.conditions.location.Proximity;
import de.eldoria.bigdoorsopener.conditions.location.Region;
import de.eldoria.bigdoorsopener.conditions.location.SimpleRegion;
import de.eldoria.bigdoorsopener.conditions.permission.DoorPermission;
import de.eldoria.bigdoorsopener.conditions.permission.PermissionNode;
import de.eldoria.bigdoorsopener.conditions.standalone.Placeholder;
import de.eldoria.bigdoorsopener.conditions.standalone.Time;
import de.eldoria.bigdoorsopener.conditions.standalone.mythicmobs.MythicMob;
import de.eldoria.bigdoorsopener.conditions.standalone.weather.Weather;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldProximity;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldRegion;
import de.eldoria.bigdoorsopener.conditions.worldlocation.WorldSimpleRegion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Jackson inspects the fields, constructors and methods of every condition. A type of an optional plugin there fails
 * loading the doors on servers without that plugin.
 */
class OptionalPluginTypesTest {
    private static final List<String> OPTIONAL_PACKAGES = List.of("com.sk89q.", "io.lumine.", "me.clip.");

    @ParameterizedTest
    @ValueSource(
            classes = {
                ItemBlock.class,
                ItemClick.class,
                ItemHolding.class,
                ItemOwning.class,
                Proximity.class,
                Region.class,
                SimpleRegion.class,
                WorldProximity.class,
                WorldRegion.class,
                WorldSimpleRegion.class,
                PermissionNode.class,
                DoorPermission.class,
                Time.class,
                Weather.class,
                Placeholder.class,
                MythicMob.class
            })
    void signaturesUseNoOptionalPluginTypes(Class<?> condition) {
        List<String> found = new ArrayList<>();
        for (Class<?> clazz = condition; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) check(found, field.toString(), field.getType());
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                check(found, constructor.toString(), constructor.getParameterTypes());
            }
            for (Method method : clazz.getDeclaredMethods()) {
                check(
                        found,
                        method.toString(),
                        Stream.concat(Stream.of(method.getReturnType()), Arrays.stream(method.getParameterTypes()))
                                .toArray(Class[]::new));
            }
        }
        assertEquals(List.of(), found);
    }

    private static void check(List<String> found, String member, Class<?>... types) {
        for (Class<?> type : types) {
            if (OPTIONAL_PACKAGES.stream().anyMatch(type.getName()::startsWith)) found.add(member);
        }
    }
}
