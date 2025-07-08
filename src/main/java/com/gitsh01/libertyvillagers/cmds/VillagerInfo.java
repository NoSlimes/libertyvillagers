package com.gitsh01.libertyvillagers.cmds;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.gitsh01.libertyvillagers.LibertyVillagersMod.CONFIG;
import static net.minecraft.server.command.CommandManager.literal;


public class VillagerInfo {

    final static String BLANK_COORDS = "                 ";

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("villagerinfo").executes(context -> {
                    processVillagerInfo(context);
                    return 1;
                })));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("vi").executes(context -> {
                    processVillagerInfo(context);
                    return 1;
                })));
    }

    public static void processVillagerInfo(CommandContext<ServerCommandSource> command) {
        ServerCommandSource source = command.getSource();
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld serverWorld = source.getWorld();

        float maxDistance = 50;
        float tickDelta = 0;

        Vec3d start = player.getCameraPosVec(tickDelta);
        Vec3d direction = player.getRotationVec(tickDelta);
        Vec3d end = start.add(direction.multiply(maxDistance));

        HitResult hit = serverWorld.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
        ));

        // If a block is hit, shorten ray to that
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getPos();
        }

        EntityHitResult closestEntityHit = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (Entity entity : serverWorld.getOtherEntities(player,
                player.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0),
                e -> e.isAlive() && !e.isSpectator() && e != player)) {

            Box entityBox = entity.getBoundingBox().expand(0.3); // Allow some leeway
            Optional<Vec3d> optionalHit = entityBox.raycast(start, end);

            if (optionalHit.isPresent()) {
                double distanceSq = start.squaredDistanceTo(optionalHit.get());
                if (distanceSq < closestDistanceSq) {
                    closestDistanceSq = distanceSq;
                    closestEntityHit = new EntityHitResult(entity, optionalHit.get());
                }
            }
        }

        if (closestEntityHit != null) {
            hit = closestEntityHit;
        }

        List<Text> lines = null;
        switch (hit.getType()) {
            case MISS:
                break;
            case BLOCK:
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = serverWorld.getBlockState(blockPos);
                if (blockState != null) {
                    lines = getBlockInfo(serverWorld, blockPos, blockState);
                }
                break;
            case ENTITY:
                Entity entity = ((EntityHitResult) hit).getEntity();
                if (entity != null) {
                    lines = getEntityInfo(serverWorld, entity);
                }
                break;
        }

        if (lines != null) {
            for (Text line : lines) {
                player.sendMessage(line);
            }
        }
    }


    public static List<Text> getEntityInfo(ServerWorld serverWorld, Entity entity) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.title"));
        if (entity == null) {
            return lines;
        }
        Text name = entity.getDisplayName();
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.name", name));

        if (!(entity instanceof VillagerEntity)) {
            return lines;
        }

        VillagerEntity villager = (VillagerEntity)entity;
        String occupation =
                VillagerStats.translatedProfession(villager.getVillagerData().profession().value());
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.occupation", occupation));

        // Client-side villagers don't have memories.
        if (serverWorld == null) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.needsServer"));
            return lines;
        }

        Optional<GlobalPos> home = villager.getBrain().getOptionalMemory(MemoryModuleType.HOME);
        String homeCoords = home.isPresent() ? home.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.home", homeCoords));

        Optional<GlobalPos> jobSite = villager.getBrain().getOptionalMemory(MemoryModuleType.JOB_SITE);
        String jobSiteCoords = jobSite.isPresent() ? jobSite.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.jobSite", jobSiteCoords));

        Optional<GlobalPos> potentialJobSite =
                villager.getBrain().getOptionalMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        String potentialJobSiteCoords =
                potentialJobSite.isPresent() ? potentialJobSite.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.potentialJobSite", potentialJobSiteCoords));

        Optional<GlobalPos> meetingPoint = villager.getBrain().getOptionalMemory(MemoryModuleType.MEETING_POINT);
        String meetingPointCoords =
                meetingPoint.isPresent() ? meetingPoint.get().pos().toShortString() : BLANK_COORDS;
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.meetingPoint", meetingPointCoords));

        Optional<WalkTarget> walkTarget = villager.getBrain().getOptionalMemory(MemoryModuleType.WALK_TARGET);
        String walkTargetCoords =
                walkTarget.isPresent() ? walkTarget.get().getLookTarget().getBlockPos().toShortString() : BLANK_COORDS;
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.walkTarget", walkTargetCoords,
                walkTarget.map(WalkTarget::getCompletionRange).orElse(0)));

        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.inventory"));

        if (villager.getInventory().isEmpty()) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.empty"));
        } else {
            for (ItemStack stack : villager.getInventory().heldStacks) {
                if (!stack.isEmpty()) {
                    lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.inventoryLine", stack.getCount(),
                            stack.getName()));
                }
            }
        }

        if (villager.getNavigation().getCurrentPath() != null && CONFIG.debugConfig.villagerInfoShowsPath) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.path"));
            Path path = villager.getNavigation().getCurrentPath();
            for (int i = path.getCurrentNodeIndex(); i < path.getLength(); i++) {
                lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.pathnode", i,
                        path.getNode(i).getBlockPos().toShortString()));
            }
        }

        return lines;
    }

    public static List<Text> getBlockInfo(ServerWorld serverWorld, BlockPos blockPos, BlockState blockState) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.title"));
        Block block = blockState.getBlock();
        if (block == null) {
            return lines;
        }
        Text name = block.getName();
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.name", name));

        if (serverWorld == null) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.needsServer"));
            return lines;
        }
        if (block instanceof BeehiveBlock) {
            BlockEntity blockEntity = serverWorld.getBlockEntity(blockPos);
            if (blockEntity instanceof BeehiveBlockEntity) {
                BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity) blockEntity;
                int numBees = beehiveBlockEntity.getBeeCount();
                lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.numBees", numBees));
            }

            int numHoney = blockState.getComparatorOutput(serverWorld, blockPos);
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.numHoney", numHoney));
        }

        Optional<RegistryEntry<PointOfInterestType>> optionalRegistryEntry =
                PointOfInterestTypes.getTypeForState(blockState);
        if (optionalRegistryEntry.isEmpty()) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.poiType",
                    Text.translatable("text.LibertyVillagers.villagerInfo.none")));
            return lines;
        }

        PointOfInterestType poiType = optionalRegistryEntry.get().value();
        Optional<RegistryKey<PointOfInterestType>> optionalRegistryKey = optionalRegistryEntry.get().getKey();
        if (optionalRegistryKey.isEmpty()) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.poiType",
                    Text.translatable("text.LibertyVillagers.villagerInfo.none")));
            return lines;
        }

        String poiTypeName = optionalRegistryKey.get().getValue().toString();

        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.poiType", poiTypeName));

        PointOfInterestStorage storage = serverWorld.getPointOfInterestStorage();
        if (!storage.hasTypeAt(optionalRegistryKey.get(), blockPos)) {
            lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.notAPOI"));
            return lines;
        }

        @SuppressWarnings("deprecation")
        int freeTickets = storage.getFreeTickets(blockPos);
        Text isOccupied =
                freeTickets < poiType.ticketCount() ? Text.translatable("text.LibertyVillagers.villagerInfo.true") :
                        Text.translatable("text" + ".LibertyVillagers.villagerInfo.false");
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.isOccupied", isOccupied));
        lines.add(Text.translatable("text.LibertyVillagers.villagerInfo.freeTickets", freeTickets,
                poiType.ticketCount()));

        return lines;

    }
}