/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.conditions.cooldown;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownTest {

    @Test
    void globalCooldownBlocksOnlyWhileRunning() {
        GlobalCooldown cooldown = new GlobalCooldown(60);
        assertEquals(true, cooldown.isOpen(null, null, null, false));

        cooldown.opened(null);

        assertEquals(false, cooldown.isOpen(null, null, null, false), "closed door stays closed");
        assertNull(cooldown.isOpen(null, null, null, true), "open door is not closed by the cooldown");
    }

    @Test
    void playerCooldownOnlyAffectsThePlayerWhoOpened() {
        PlayerCooldown cooldown = new PlayerCooldown(60);
        Player opener = player();
        Player other = player();

        cooldown.opened(opener);

        assertEquals(false, cooldown.isOpen(opener, null, null, false));
        assertEquals(true, cooldown.isOpen(other, null, null, false));
    }

    @Test
    void cooldownEnds() {
        GlobalCooldown cooldown = new GlobalCooldown(5);

        assertTrue(cooldown.state(Instant.now().minusSeconds(6), false));
        assertFalse(cooldown.state(Instant.now().minusSeconds(4), false));
    }

    private static Player player() {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
                CooldownTest.class.getClassLoader(),
                new Class[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "hashCode" -> id.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
