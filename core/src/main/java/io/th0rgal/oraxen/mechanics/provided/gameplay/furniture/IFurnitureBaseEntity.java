package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IFurnitureBaseEntity {

    @NotNull
    BoundingBox getBoundingBox();

    @NotNull
    FurnitureMechanic getMechanic();

    @Nullable
    ItemStack getFurnitureItem();

    void setFurnitureItem(ItemStack itemStack);

    List<Location> getBarrierLocations();

    List<Location> getInteractionLocations();

    void setYaw(float yaw);
}
