package com.gitsh01.libertyvillagers.mixin;

import net.minecraft.entity.ai.brain.task.UpdateJobSiteTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import static com.gitsh01.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(UpdateJobSiteTask.class)
public class UpdateJobSiteTaskMixin {

    // Inject into the lambda called by Task.trigger.
    @SuppressWarnings({"target", "descriptor"})
    @ModifyConstant(
            method = "method_46890",
            constant = @Constant(doubleValue = 2.0))
    static private double modifyDistanceInShouldRun(double distance) {
        return Math.max(distance, CONFIG.villagerPathfindingConfig.minimumPOISearchDistance + 1);
    }
}