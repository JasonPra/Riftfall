package org.Azkerash.riftfall.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.Azkerash.riftfall.GoblinEntity;

/** Static materials only. Eyes and expressions are articulated model parts, never texture frames. */
public final class GoblinFaceTextures {
    private static final Map<String,ResourceLocation> TEXTURES=new HashMap<>();
    public static ResourceLocation base(String role) { return ResourceLocation.fromNamespaceAndPath("riftfall","textures/entity/"+role+".png"); }
    public static ResourceLocation texture(GoblinEntity mob) {
        String role=mob.isShaman()?"goblin_shaman":mob.isArcher()?"goblin_archer":"goblin";
        return TEXTURES.computeIfAbsent(role,key->{
            try (var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(base(key)).open();var source=NativeImage.read(stream)) {
                var image=new NativeImage(source.getWidth(),source.getHeight(),false);image.copyFrom(source);
                for(int y=10;y<16;y++) for(int x=8;x<16;x++) image.setPixelRGBA(x,y,source.getPixelRGBA(x,15));
                return register(key,image);
            } catch(java.io.IOException ex) { org.slf4j.LoggerFactory.getLogger(GoblinFaceTextures.class).warn("Cannot load facial skin",ex);return base(key); }
        });
    }
    public static ResourceLocation material() {
        return TEXTURES.computeIfAbsent("materials",key->{
            var image=new NativeImage(48,8,false);
            int[] colors={0xE6CA68,0x17210F,0x528138,0x20391A,0xF0E4B7,0x3F1521};
            for(int y=0;y<8;y++) for(int x=0;x<48;x++) image.setPixelRGBA(x,y,0xff000000|Integer.reverseBytes(colors[x/8])>>>8);
            return register(key,image);
        });
    }
    private static ResourceLocation register(String name,NativeImage image) {
        var texture=new DynamicTexture(image);texture.setFilter(false,false);
        var location=ResourceLocation.fromNamespaceAndPath("riftfall","dynamic/face/"+name);
        Minecraft.getInstance().getTextureManager().register(location,texture);return location;
    }
    public static void clear() {
        var manager=Minecraft.getInstance().getTextureManager();TEXTURES.values().forEach(manager::release);TEXTURES.clear();
    }
    private GoblinFaceTextures() {}
}
