package com.gitsh01.libertyvillagers.mixin;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.CatVariant;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.gitsh01.libertyvillagers.LibertyVillagersMod.CONFIG;

@Mixin(CatEntity.class)
public abstract class CatEntityMixin extends TameableEntity {

    @Shadow
    public abstract void setVariant(RegistryEntry<CatVariant> registryEntry);

    public CatEntityMixin(EntityType<? extends CatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize",
            at = @At("RETURN"))
    void addPersistantToInitialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                   SpawnReason spawnReason, EntityData entityData,
                                   CallbackInfoReturnable<EntityData> cir) {
        if (CONFIG.catsConfig.villageCatsDontDespawn) {
            this.setPersistent();
        }

        if (CONFIG.catsConfig.allBlackCats) {
            Registry<CatVariant> catVariantRegistry = ((ServerWorld) world).getRegistryManager().getOrThrow(RegistryKeys.CAT_VARIANT);
            Identifier blackCatId = Identifier.of("minecraft", "all_black");
            catVariantRegistry.getEntry(blackCatId).ifPresent(this::setVariant);
        }
    }

}