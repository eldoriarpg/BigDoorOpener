package de.eldoria.bigdoorsopener.doors.doorkey.location;

import de.eldoria.bigdoorsopener.doors.ConditionalDoor;
import de.eldoria.bigdoorsopener.doors.doorkey.KeyParameter;
import de.eldoria.bigdoorsopener.util.TriFunction;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@KeyParameter("locationKey")
public class ProximityKey implements LocationKey {
    private Vector dimensions;
    private ProximityForm proximityForm;


    public ProximityKey(Vector dimensions, ProximityForm proximityForm) {
        this.dimensions = dimensions;
        this.proximityForm = proximityForm;
    }

    @Override
    public boolean isOpen(Player player, World world, ConditionalDoor door, boolean currentState) {
        return proximityForm.check.apply(door.getPosition(), player.getLocation().toVector(), dimensions);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return null;
    }

    public enum ProximityForm {
        CUBOID((point, target, dimensions) -> {
            if (Math.abs(point.getX() - target.getX()) > dimensions.getX()) return false;
            if (Math.abs(point.getY() - target.getY()) > dimensions.getY()) return false;
            if (Math.abs(point.getZ() - target.getZ()) > dimensions.getZ()) return false;
            return true;
        }),
        ELIPSOID((point, target, dimensions) ->
                Math.pow((target.getX() - point.getX()) / dimensions.getX(), 2)
                        + Math.pow((target.getY() - point.getY()) / dimensions.getY(), 2)
                        + Math.pow((target.getZ() - point.getZ()) / dimensions.getZ(), 2) <= 1),
        CYLINDER((point, target, dimensions) -> {
            if (Math.abs(point.getY() - target.getY()) > dimensions.getY()) return false;
            return Math.pow(target.getX() - point.getX(), 2) / Math.pow(dimensions.getX(), 2)
                    + Math.pow(target.getZ() - point.getZ(), 2) / Math.pow(dimensions.getZ(), 2) <= 1;
        });

        public TriFunction<Vector, Vector, Vector, Boolean> check;

        ProximityForm(TriFunction<Vector, Vector, Vector, Boolean> check) {
            this.check = check;
        }
    }
}
