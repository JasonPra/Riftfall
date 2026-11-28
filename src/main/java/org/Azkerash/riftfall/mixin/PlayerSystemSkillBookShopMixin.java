package org.Azkerash.riftfall.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Shop views carry a translation key, not stack components; distinguish each offered book. */
@Pseudo
@Mixin(targets = "dev.kubas.playersystem.shop.ShopEntry", remap = false)
public abstract class PlayerSystemSkillBookShopMixin {
    @Shadow
    public abstract ResourceLocation id();

    @ModifyReturnValue(method = "nameKey", at = @At("RETURN"))
    private String riftfall$skillBookName(String original) {
        var id = id();
        if (id.getNamespace().equals("riftfall") && id.getPath().startsWith("epic_fight_")) {
            return "shop.riftfall." + id.getPath();
        }
        return original;
    }
}
