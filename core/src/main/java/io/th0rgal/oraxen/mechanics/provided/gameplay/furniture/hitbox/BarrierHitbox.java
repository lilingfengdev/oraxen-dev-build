package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import org.bukkit.util.Vector;

public class BarrierHitbox extends BlockLocation {

    public BarrierHitbox from(Object hitboxObject) {
        if (hitboxObject instanceof String string) {
            return new BarrierHitbox(string);
        } else return new BarrierHitbox("0 0 0");
    }

    public BarrierHitbox(String hitboxString) {
        super(hitboxString);
    }

    public Vector vector() {
        return new Vector(this.getX(), this.getY(), this.getZ());
    }

}
