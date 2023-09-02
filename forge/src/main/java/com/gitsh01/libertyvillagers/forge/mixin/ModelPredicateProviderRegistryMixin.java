package com.gitsh01.libertyvillagers.forge.mixin;

import com.google.common.collect.Maps;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelPredicateProviderRegistry.class)
public class ModelPredicateProviderRegistryMixin {

    @Shadow
    @Mutable
    static Map<Item, Map<Identifier, ModelPredicateProvider>> ITEM_SPECIFIC;

    // Forge insists on remapping this to "modelPredicateProvider" instead of "unclampedModelPredicateProvider". :(
    @Inject(method = "register(Lnet/minecraft/item/Item;Lnet/minecraft/util/Identifier;Lnet/minecraft/client/item/ModelPredicateProvider;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void register(Item item, Identifier id, ModelPredicateProvider provider, CallbackInfo ci) {
        if (item != Items.FISHING_ROD) {
            return;
        }
        // Villagers should show the used graphic for the fishing rod.
        ModelPredicateProvider newProvider = (stack, world, entity, seed) -> {
            if (entity != null && entity.getType() == EntityType.VILLAGER) {
                return 1.0f;
            }
            return provider.call(stack, world, entity, seed);
        };
        ITEM_SPECIFIC.computeIfAbsent(item, key -> Maps.newHashMap()).put(id, newProvider);
        ci.cancel();
    }
}
