package io.th0rgal.oraxen.nms.v1_20_R3;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureHelpers;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureSubEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.hitbox.InteractionHitbox;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.seats.FurnitureSeat;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class FurniturePacketManager implements IFurniturePacketManager {

    public FurniturePacketManager() {
        if (VersionUtil.isPaperServer())
            MechanicsManager.registerListeners(OraxenPlugin.get(), "furniture", new FurniturePacketListener());
        else {
            Logs.logWarning("Seems that your server is a Spigot-server");
            Logs.logWarning("FurnitureHitboxes will not work due to it relying on Paper-only events");
            Logs.logWarning("It is heavily recommended to make the upgrade to Paper");
        }
    }

    private final int INTERACTION_WIDTH_ID = 8;
    private final int INTERACTION_HEIGHT_ID = 9;
    private final Map<UUID, Set<FurnitureSubEntityPacket>> interactionHitboxPacketMap = new HashMap<>();
    private final Map<UUID, Set<FurnitureSubEntityPacket>> furnitureSeatPacketMap = new HashMap<>();

    @Override
    public void sendInteractionEntityPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        List<InteractionHitbox> interactionHitboxes = mechanic.hitbox().interactionHitboxes();
        if (interactionHitboxes.isEmpty()) return;
        if (mechanic.isModelEngine()) {
            ModelBlueprint blueprint = ModelEngineAPI.getBlueprint(mechanic.getModelEngineID());
            if (blueprint != null && blueprint.getMainHitbox() != null) return;
        }

        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        interactionHitboxPacketMap.computeIfAbsent(baseEntity.getUniqueId(), baseUuid -> {
            List<Integer> entityIds = interactionHitboxIdMap.stream()
                    .filter(ids -> ids.baseUUID().equals(baseUuid))
                    .findFirst()
                    .map(FurnitureSubEntity::entityIds)
                    .orElseGet(() -> {
                        List<Integer> newEntityIds = new ArrayList<>(interactionHitboxes.size());
                        while (newEntityIds.size() < interactionHitboxes.size())
                            newEntityIds.add(net.minecraft.world.entity.Entity.nextEntityId());

                        FurnitureSubEntity id = new FurnitureSubEntity(baseUuid, newEntityIds);
                        interactionHitboxIdMap.add(id);
                        return id.entityIds();
                    });

            Set<FurnitureSubEntityPacket> packets = new HashSet<>();
            for (int i = 0; i < interactionHitboxes.size(); i++) {
                InteractionHitbox hitbox = interactionHitboxes.get(i);
                int entityId = entityIds.get(i);

                Location loc = baseLoc.clone().add(hitbox.offset(baseEntity.getYaw()));
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                        entityId, UUID.randomUUID(),
                        loc.x(), loc.y(), loc.z(), loc.getPitch(), loc.getYaw(),
                        EntityType.INTERACTION, 0, Vec3.ZERO, 0.0
                );

                ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(
                        entityId, Arrays.asList(
                        new SynchedEntityData.DataValue<>(INTERACTION_WIDTH_ID, EntityDataSerializers.FLOAT, hitbox.width()),
                        new SynchedEntityData.DataValue<>(INTERACTION_HEIGHT_ID, EntityDataSerializers.FLOAT, hitbox.height())
                ));

                packets.add(new FurnitureSubEntityPacket(entityId, addEntityPacket, metadataPacket));
            }
            return packets;
        }).forEach(packets -> {
            ((CraftPlayer) player).getHandle().connection.send(packets.addEntity);
            ((CraftPlayer) player).getHandle().connection.send(packets.metadata);
        });

    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : baseEntity.getWorld().getPlayers()) {
            removeInteractionHitboxPacket(baseEntity, mechanic, player);
        }
        interactionHitboxIdMap.removeIf(id -> id.baseUUID().equals(baseEntity.getUniqueId()));
        interactionHitboxPacketMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeInteractionHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        interactionHitboxIdMap.stream().filter(subEntity -> subEntity.baseUUID().equals(baseEntity.getUniqueId()))
                .findFirst().ifPresent(subEntity ->
                {
                    ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(subEntity.entityIds().toIntArray()));
                }
                );
    }

    @Override
    public void sendBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Position, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation()))
                .collect(Collectors.toMap(Position::block, l -> BARRIER_DATA));
        player.sendMultiBlockChange(positions);

        for (BlockPosition position : positions.keySet().stream().map(Position::toBlock).toList()) {
            barrierHitboxPositionMap.compute(baseEntity.getUniqueId(), (d, blockPos) -> {
                Set<com.comphenix.protocol.wrappers.BlockPosition> newBlockPos = new HashSet<>();
                com.comphenix.protocol.wrappers.BlockPosition newPos = new com.comphenix.protocol.wrappers.BlockPosition(position.blockX(), position.blockY(), position.blockZ());
                newBlockPos.add(newPos);
                if (blockPos != null) newBlockPos.addAll(blockPos);
                return newBlockPos;
            });
        }
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {
        for (Player player : baseEntity.getWorld().getPlayers()) {
            removeBarrierHitboxPacket(baseEntity, mechanic, player);
        }
        barrierHitboxPositionMap.remove(baseEntity.getUniqueId());
    }

    @Override
    public void removeBarrierHitboxPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        Map<Position, BlockData> positions = mechanic.hitbox().barrierHitboxes().stream()
                .map(c -> c.groundRotate(baseEntity.getYaw()).add(baseEntity.getLocation()))
                .collect(Collectors.toMap(Position::block, l -> AIR_DATA));
        player.sendMultiBlockChange(positions);
    }

    @Override
    public void sendSeatPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {
        List<FurnitureSeat> seats = mechanic.seats();
        if (seats.isEmpty()) return;

        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        furnitureSeatPacketMap.computeIfAbsent(baseEntity.getUniqueId(), baseUuid -> {
            List<Integer> entityIds = furnitureSeatIdMap.stream()
                    .filter(subEntities -> subEntities.baseUUID().equals(baseUuid))
                    .findFirst()
                    .map(FurnitureSubEntity::entityIds)
                    .orElseGet(() -> {
                        List<Integer> newEntityIds = new ArrayList<>(seats.size());
                        while (newEntityIds.size() < seats.size())
                            newEntityIds.add(net.minecraft.world.entity.Entity.nextEntityId());

                        FurnitureSubEntity id = new FurnitureSubEntity(baseUuid, newEntityIds);
                        interactionHitboxIdMap.add(id);
                        return id.entityIds();
                    });

            Set<FurnitureSubEntityPacket> packets = new HashSet<>();
            for (int i = 0; i < seats.size(); i++) {
                FurnitureSeat seat = seats.get(i);
                int entityId = entityIds.get(i);

                Location loc = baseLoc.clone().add(seat.offset(baseEntity.getYaw()));
                ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(
                        entityId, UUID.randomUUID(),
                        loc.x(), loc.y(), loc.z(), loc.getPitch(), loc.getYaw(),
                        EntityType.ARMOR_STAND, 0, Vec3.ZERO, 0.0
                );

                ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(
                        entityId, Arrays.asList(
                        new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte) 0x20) // Invisible
                ));

                packets.add(new FurnitureSubEntityPacket(entityId, addEntityPacket, metadataPacket));
            }
            return packets;
        });
    }
    @Override
    public void mountSeatPacket(@NotNull Entity baseEntity, @NotNull Location interactionPoint, @NotNull FurnitureMechanic mechanic, @NotNull Player passenger) {
        List<FurnitureSeat> seats = mechanic.seats();
        seats.sort(Comparator.comparingDouble(s -> baseEntity.getLocation().add(s.offset(FurnitureHelpers.furnitureYaw(baseEntity))).distanceSquared(interactionPoint)));
        seats.stream().findFirst().ifPresent(seat -> {
            Location seatLoc = baseEntity.getLocation().add(seat.offset(FurnitureHelpers.furnitureYaw(baseEntity)));
            for (FurnitureSubEntityPacket packet : furnitureSeatPacketMap.getOrDefault(baseEntity.getUniqueId(), new HashSet<>())) {
                if (!locationFromPacket(baseEntity.getWorld(), packet.addEntity).equals(seatLoc)) continue;
            }
        });
    }
    private Location locationFromPacket(World world, ClientboundAddEntityPacket packet) {
        return new Location(world, packet.getX(), packet.getY(), packet.getZ());
    }
    @Override
    public void dismountSeatPacket(@NotNull Entity baseEntity, int seatId, @NotNull FurnitureMechanic mechanic, @NotNull Player passenger) {

    }
    @Override
    public void removeSeatPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic) {

    }
    @Override
    public void removeSeatPacket(@NotNull Entity baseEntity, @NotNull FurnitureMechanic mechanic, @NotNull Player player) {

    }
}
