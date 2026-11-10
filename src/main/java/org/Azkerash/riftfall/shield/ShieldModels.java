package org.Azkerash.riftfall.shield;

import com.google.gson.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

/** Server-owned model dimensions. JSON shields are derived automatically; custom renderers may supply a data profile. */
public final class ShieldModels {
    public record Model(AABB bounds,JsonObject display,JsonObject skin,boolean forearm) {
        public OpenMatrix4f display(InteractionHand hand) {
            String key=hand==InteractionHand.MAIN_HAND?"thirdperson_righthand":"thirdperson_lefthand";
            JsonObject transform=display.has(key)?display.getAsJsonObject(key):display.has("thirdperson_righthand")?display.getAsJsonObject("thirdperson_righthand"):new JsonObject();
            return transform(transform,hand==InteractionHand.OFF_HAND,true).translate(-.5F,-.5F,-.5F);
        }
        public OpenMatrix4f correction(InteractionHand hand,String joint,boolean onGoblinForearm) {
            if(onGoblinForearm)return forearmCorrection(hand);
            String key=hand==InteractionHand.MAIN_HAND?"mainhand":"offhand";
            if(skin.has("transforms") && skin.getAsJsonObject("transforms").has(key)) {
                var transforms=skin.getAsJsonObject("transforms").getAsJsonObject(key);
                if(transforms.has(joint))return transform(transforms.getAsJsonObject(joint),false,false);
            }
            return new OpenMatrix4f().translate(0,0,-.13F).rotateDeg(-90,Vec3f.X_AXIS);
        }
        public boolean alwaysInHand(){return forearm || skin.has("alwaysInHand") && skin.get("alwaysInHand").getAsBoolean();}
    }
    private static final Map<ResourceLocation,Model> CACHE=new ConcurrentHashMap<>();
    public static OpenMatrix4f forearmCorrection(InteractionHand hand){return new OpenMatrix4f().translate(hand==InteractionHand.OFF_HAND?-.13F:.13F,.14F,0);}
    private static final Map<ResourceLocation,Model> REGISTERED=new ConcurrentHashMap<>();
    public static void clear(){CACHE.clear();ShieldCollisions.clearPoseCache();}
    public static void register(ResourceLocation item,Model model){REGISTERED.put(item,model);}
    public static Model get(ItemStack stack){var id=BuiltInRegistries.ITEM.getKey(stack.getItem());return REGISTERED.containsKey(id)?REGISTERED.get(id):CACHE.computeIfAbsent(id,ShieldModels::load);}
    public static JsonObject resource(String path) {
        try {
            var stream=ShieldModels.class.getClassLoader().getResourceAsStream(path);
            if(stream==null && ModList.get()!=null)for(var mod:ModList.get().getModFiles()) {
                var file=mod.getFile().findResource(path.split("/"));
                if(Files.isRegularFile(file)){stream=Files.newInputStream(file);break;}
            }
            if(stream!=null)try(var reader=new InputStreamReader(stream)){return JsonParser.parseReader(reader).getAsJsonObject();}
        }catch(IOException|RuntimeException ignored){}
        return new JsonObject();
    }
    private static Model load(ResourceLocation item) {
        var skin=resource("assets/"+item.getNamespace()+"/item_skins/"+item.getPath()+".json");
        var profile=resource("data/"+item.getNamespace()+"/shield_colliders/"+item.getPath()+".json");
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server!=null) {
            var location=ResourceLocation.fromNamespaceAndPath(item.getNamespace(),"shield_colliders/"+item.getPath()+".json");
            try {var data=server.getResourceManager().getResource(location);if(data.isPresent())try(var reader=data.get().openAsReader()){profile=JsonParser.parseReader(reader).getAsJsonObject();}}catch(IOException ignored){}
        }
        if(profile.has("from") && profile.has("to"))return new Model(bounds(profile).inflate(1D/16),profile.has("display")?profile.getAsJsonObject("display"):new JsonObject(),skin,skin.has("renderer")&&skin.get("renderer").getAsString().equals("riftfall:forearm_buckler"));
        var model=inherit(ResourceLocation.fromNamespaceAndPath(item.getNamespace(),"item/"+item.getPath()),ShieldModels::modelJson,new HashSet<>());
        AABB box=elements(model);
        if(box==null) {
            // Opaque Java renderers have no JSON geometry. A server data profile can replace this conservative vanilla fallback.
            System.getLogger("riftfall").log(System.Logger.Level.WARNING,"No JSON geometry for shield "+item+"; supply shield_colliders/"+item.getPath()+".json for custom renderer dimensions");
            if(item.equals(ResourceLocation.withDefaultNamespace("shield")))throw new IllegalStateException("Missing built-in shield collider profile");
            var vanilla=load(ResourceLocation.withDefaultNamespace("shield"));
            return new Model(vanilla.bounds(),model.has("display")?model.getAsJsonObject("display"):vanilla.display(),skin,false);
        }
        return new Model(box.inflate(1D/16),model.has("display")?model.getAsJsonObject("display"):new JsonObject(),skin,skin.has("renderer")&&skin.get("renderer").getAsString().equals("riftfall:forearm_buckler"));
    }
    private static JsonObject modelJson(ResourceLocation id){return resource("assets/"+id.getNamespace()+"/models/"+id.getPath()+".json");}
    public static JsonObject inherit(ResourceLocation id,Function<ResourceLocation,JsonObject> reader,Set<ResourceLocation> visiting) {
        if(!visiting.add(id))return new JsonObject();
        var own=reader.apply(id);JsonObject result=new JsonObject();
        if(own.has("parent"))result=inherit(ResourceLocation.parse(own.get("parent").getAsString()),reader,visiting).deepCopy();
        for(var entry:own.entrySet()) {
            if(entry.getKey().equals("display") && result.has("display")) {
                var merged=result.getAsJsonObject("display").deepCopy();entry.getValue().getAsJsonObject().entrySet().forEach(e->merged.add(e.getKey(),e.getValue()));result.add("display",merged);
            }else result.add(entry.getKey(),entry.getValue());
        }
        visiting.remove(id);return result;
    }
    public static AABB elements(JsonObject model) {
        if(!model.has("elements"))return null;AABB result=null;
        for(var value:model.getAsJsonArray("elements")) {
            var element=value.getAsJsonObject();var box=bounds(element);var matrix=new OpenMatrix4f();
            if(element.has("rotation")) {
                var rotation=element.getAsJsonObject("rotation");var origin=vector(rotation,"origin",new Vec3(8,8,8)).scale(1D/16);float angle=rotation.get("angle").getAsFloat();
                String axis=rotation.get("axis").getAsString();var direction=axis.equals("x")?Vec3f.X_AXIS:axis.equals("y")?Vec3f.Y_AXIS:Vec3f.Z_AXIS;
                matrix.translate((float)origin.x,(float)origin.y,(float)origin.z).rotateDeg(angle,direction);
                if(rotation.has("rescale") && rotation.get("rescale").getAsBoolean()) {float scale=(float)(1/Math.cos(Math.toRadians(angle)));matrix.scale(axis.equals("x")?1:scale,axis.equals("y")?1:scale,axis.equals("z")?1:scale);}
                matrix.translate((float)-origin.x,(float)-origin.y,(float)-origin.z);
            }
            for(int i=0;i<8;i++) {
                var point=OpenMatrix4f.transform(matrix,new Vec3((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ));
                var one=new AABB(point,point);result=result==null?one:result.minmax(one);
            }
        }
        return result;
    }
    private static AABB bounds(JsonObject json){return new AABB(vector(json,"from",Vec3.ZERO).scale(1D/16),vector(json,"to",new Vec3(16,16,16)).scale(1D/16));}
    private static Vec3 vector(JsonObject json,String name,Vec3 fallback){if(!json.has(name))return fallback;var a=json.getAsJsonArray(name);return new Vec3(a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble());}
    private static OpenMatrix4f transform(JsonObject json,boolean left,boolean pixels) {
        var t=vector(json,"translation",Vec3.ZERO).scale(pixels?1D/16:1);var r=vector(json,"rotation",Vec3.ZERO);var s=vector(json,"scale",new Vec3(1,1,1));
        var matrix=new OpenMatrix4f().translate((float)(left?-t.x:t.x),(float)t.y,(float)t.z);
        matrix.rotateDeg((float)r.x,Vec3f.X_AXIS).rotateDeg((float)(left?-r.y:r.y),Vec3f.Y_AXIS).rotateDeg((float)(left?-r.z:r.z),Vec3f.Z_AXIS);
        return matrix.scale((float)s.x,(float)s.y,(float)s.z);
    }
    private ShieldModels(){}
}
