/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.core.conditions;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.eldoria.bigdoorsopener.conditions.DoorCondition;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Resolves the {@code type} of a stored condition through the {@link ConditionRegistrar}, so conditions registered by
 * other plugins are stored and loaded like the built-in ones.
 * <p>
 * An unknown type resolves to nothing. With {@code FAIL_ON_INVALID_SUBTYPE} disabled Jackson reads such a condition as
 * {@code null}, which skips the condition instead of failing the whole file.
 */
public class ConditionTypeResolver extends TypeIdResolverBase {
    private static final Logger LOGGER = Logger.getLogger("BigDoorsOpener");

    @Override
    public String idFromValue(DatabindContext ctxt, Object value) {
        return idFromValueAndType(ctxt, value, value.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> suggestedType) {
        return ConditionRegistrar.getContainerByClass((Class<? extends DoorCondition>) suggestedType)
                .map(ConditionContainer::typeName)
                .orElseThrow(() -> new IllegalStateException("Condition " + suggestedType.getName() + " is not registered."));
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        Optional<ConditionContainer> container = ConditionRegistrar.getContainerByTypeName(id);
        if (container.isEmpty()) {
            LOGGER.warning("Skipping condition of unknown type \"" + id + "\". Is the plugin providing it missing?");
            return null;
        }
        return context.constructType(container.get().getClazz());
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
