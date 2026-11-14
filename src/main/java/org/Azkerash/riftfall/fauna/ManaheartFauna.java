package org.Azkerash.riftfall.fauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import org.Azkerash.riftfall.flora.ManaheartFlora;
public final class ManaheartFauna {
 private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,"riftfall");
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("riftfall");
 public static final DeferredHolder<EntityType<?>,EntityType<RyEntity>> RY=TYPES.register("ry",()->EntityType.Builder.of(RyEntity::new,MobCategory.CREATURE).sized(.65f,.9f).clientTrackingRange(10).build("riftfall:ry"));
 public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> EGG=ITEMS.register("ry_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(RY,0x8fa6bb,0x92dfd4,new Item.Properties()));
 public static void register(IEventBus bus){
  yesman.epicfight.world.capabilities.entitypatch.Faction.ENUM_MANAGER.registerEnumCls("riftfall",MagicBeastFaction.class);
  yesman.epicfight.api.event.EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(e->{
   yesman.epicfight.gameasset.Armatures.registerEntityTypeArmature(RY.get(),RyAnimations.RIG);
   e.registerEntityPatchUnsafe(RY.get(),RyPatch::new);
  });TYPES.register(bus);ITEMS.register(bus);
  bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->e.put(RY.get(),Wolf.createAttributes().add(yesman.epicfight.registry.entries.EpicFightAttributes.IMPACT,1.4).add(yesman.epicfight.registry.entries.EpicFightAttributes.STUN_ARMOR,3).add(Attributes.FOLLOW_RANGE,32).add(Attributes.SCALE,1.35).add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.34).add(Attributes.ATTACK_DAMAGE,6).build()));
  bus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e)->e.register(RY.get(),SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->{
   var ground=level.getBlockState(pos.below());return (ground.is(net.minecraft.tags.BlockTags.DIRT)||ground.is(ManaheartFlora.MOSS.get())||ground.is(ManaheartFlora.CARPET.get()))&&level.getFluidState(pos).isEmpty();
  },net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE));
  bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG.get());});
 }
 private ManaheartFauna(){}
}
