package com.gitsh01.libertyvillagers.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.MemoryQueryResult;
import net.minecraft.entity.ai.brain.task.GoToNearbyPositionTask;
import net.minecraft.entity.ai.brain.task.TaskTriggerer;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerProfession;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GoToNearbyPositionTask.class)
public class GoToNearbyPositionTaskMixin {

    @Inject(method = "method_47156",
            at = @At("HEAD"),
            cancellable = true)
    static private void dontRunIfFishing(TaskTriggerer.TaskContext<PathAwareEntity> context, MemoryQueryResult result,
                                         int maxDistance,
            MutableLong mutableLong, MemoryQueryResult result2, float walkSpeed,
            int completionRange,
            ServerWorld serverWorld, PathAwareEntity pathAwareEntity, long time,
            CallbackInfoReturnable<Boolean> cir) {
        if (pathAwareEntity.getType() == EntityType.VILLAGER) {
            VillagerEntity villager = (VillagerEntity) pathAwareEntity;
            if (villager.getVillagerData().getProfession() == VillagerProfession.FISHERMAN &&
                    villager.getMainHandStack().isOf(Items.FISHING_ROD)) {
                    cir.setReturnValue(false);
                    cir.cancel();
            }
        }
    }
}
