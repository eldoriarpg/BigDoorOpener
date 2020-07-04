package de.eldoria.bigdoorsopener.doors;

import de.eldoria.bigdoorsopener.config.TimedDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyChain;
import de.eldoria.bigdoorsopener.util.CachingJSEngine;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@Getter
public abstract class ConditionalDoor {
    /**
     * UID of the door from {@link nl.pim16aap2.bigDoors.Door}.
     */
    private final long doorUID;

    /**
     * Name of the world the door is in.
     */
    private final String world;

    /**
     * Mass center of the door.
     */
    private final Vector position;

    private String evaluator;

    private EvaluationType evaluationType;

    private final KeyChain keyChain;

    /**
     * True if the door was registered in open state.
     */
    @Setter
    private boolean invertOpen = false;

    private static final CachingJSEngine JS;

    static {
        JS = new CachingJSEngine();
    }

    public ConditionalDoor(long doorUID, String world, Vector position, KeyChain keyChain) {
        this.doorUID = doorUID;
        this.world = world;
        this.position = position;
        this.keyChain = keyChain;
    }

    public ConditionalDoor(long doorUID, String world, Vector position) {
        this(doorUID, world, position, new KeyChain());
    }

    public boolean getState(Player player, World world, ConditionalDoor door, boolean currentState) {
        switch (evaluationType) {
            case CUSTOM:
                String custom = keyChain.custom(evaluator, player, world, door, currentState);
                return JS.evalBool(custom, currentState);
            case AND:
                return keyChain.and(player, world, door, currentState);
            case OR:
                return keyChain.or(player, world, door, currentState);
            default:
                throw new IllegalStateException("Unexpected value: " + evaluationType);
        }
    }

    public boolean openInverted(boolean open) {
        if (invertOpen) return !open;
        return open;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionalDoor door = (ConditionalDoor) o;
        return doorUID == door.getDoorUID();
    }

    private enum EvaluationType {
        CUSTOM, AND, OR
    }
}
