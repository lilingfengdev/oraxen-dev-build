package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.InteractionHitbox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class FurnitureBaseEntity implements IFurnitureBaseEntity {

    @NotNull
    private final UUID entityUuid;
    @NotNull
    private final FurnitureMechanic mechanic;

    public FurnitureBaseEntity(@NotNull Entity baseEntity) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) throw new NullPointerException();
        this.entityUuid = baseEntity.getUniqueId();
        this.mechanic = mechanic;
    }

    @NotNull
    public BoundingBox getBoundingBox() {
        List<Vector> hitboxVectors = mechanic.hitbox().interactionHitboxes().stream().map(InteractionHitbox::vector).toList();
        Vector min = new Vector();
        Vector max = new Vector();

        for (Vector vector : hitboxVectors) {
            min = Vector.getMinimum(min, vector);
            max = Vector.getMaximum(max, vector);
        }

        Location corner1 = getBukkitEntity().getLocation().clone().add(min);
        Location corner2 = getBukkitEntity().getLocation().clone().add(max);
        return BoundingBox.of(corner1, corner2);
    }

    @NotNull
    public Entity getBukkitEntity() {
        return Bukkit.getEntity(entityUuid);
    }

    @NotNull
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    public ItemStack getFurnitureItem() {
        if (this instanceof ItemDisplay itemDisplay) {
            return itemDisplay.getItemStack();
        } else if (this instanceof ItemFrame itemFrame) {
            return itemFrame.getItem();
        } else return null;

        //return OraxenItems.getItemById(mechanic.getItemID()).getReferenceClone();
    }

    public void setFurnitureItem(ItemStack itemStack) {
        if (this instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(itemStack);
        } else if (this instanceof ItemFrame itemFrame) {
            itemFrame.setItem(itemStack, false);
        }
    }

    public List<Location> getBarrierLocations() {
        return mechanic.hitbox().barrierHitboxLocations(getBukkitEntity().getLocation(), this.getYaw());
    }

    public List<Location> getInteractionLocations() {
        return mechanic.hitbox().interactionHitboxLocations(getBukkitEntity().getLocation(), this.getYaw());
    }

    public float getYaw() {
        return getBukkitEntity().getLocation().getYaw();
    }

    public void setYaw(float yaw) {
        Entity entity = getBukkitEntity();
        if (entity instanceof ItemFrame itemFrame) {
            itemFrame.setRotation(FurnitureHelpers.yawToRotation(yaw));
        } else entity.setRotation(yaw, entity.getPitch());
    }
}
