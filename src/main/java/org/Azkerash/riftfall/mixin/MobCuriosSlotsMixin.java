package org.Azkerash.riftfall.mixin;
import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

/** Reuse the configured player slot types, preserving explicit per-entity Curios overrides. */
@Mixin(value=CuriosImplMixinHooks.class,remap=false)
public abstract class MobCuriosSlotsMixin {
    @Inject(method="getEntitySlots",at=@At("RETURN"),cancellable=true)
    private static void riftfall$mobSlots(EntityType<?> type,boolean client,CallbackInfoReturnable<Map<String,ISlotType>> cir){
        if(type==EntityType.PLAYER || type==EntityType.ARMOR_STAND || !DefaultAttributes.hasSupplier(type))return;
        var slots=new LinkedHashMap<>(CuriosApi.getPlayerSlots(client));slots.putAll(cir.getReturnValue());
        cir.setReturnValue(Collections.unmodifiableMap(slots));
    }
}
