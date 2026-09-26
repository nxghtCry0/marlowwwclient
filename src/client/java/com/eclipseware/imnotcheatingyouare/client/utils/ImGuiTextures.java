package com.eclipseware.imnotcheatingyouare.client.utils;

import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.HashMap;
import java.util.Map;

public final class ImGuiTextures {
    private static final Map<Integer, Boolean> prepared = new HashMap<>();

    public record Region(int texture, float u0, float v0, float u1, float v1) {
    }

    private ImGuiTextures() {
    }

    public static int prepare(int glId, boolean linear) {
        if (glId == 0) return 0;
        Boolean state = prepared.get(glId);
        if (state != null && state == linear) return glId;
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
        int filter = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        prepared.put(glId, linear);
        return glId;
    }

    public static int glId(AbstractTexture texture) {
        if (texture != null && texture.getTexture() instanceof GlTexture gl && !gl.isClosed()) return gl.glId();
        return 0;
    }

    public static int texture(Identifier id, boolean linear) {
        try {
            return prepare(glId(Minecraft.getInstance().getTextureManager().getTexture(id)), linear);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static Region playerFace(Player player) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Identifier skin = null;
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) skin = info.getSkin().body().texturePath();
            }
            if (skin == null) return null;
            int tex = texture(skin, false);
            if (tex == 0) return null;
            return new Region(tex, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Region playerHat(Player player) {
        Region face = playerFace(player);
        if (face == null) return null;
        return new Region(face.texture(), 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f);
    }

    private static final Identifier ITEMS_ATLAS = Identifier.fromNamespaceAndPath("minecraft", "items");
    private static final Map<Identifier, Region> itemCache = new HashMap<>();

    public static Region itemSprite(Identifier sprite) {
        Region cached = itemCache.get(sprite);
        if (cached != null) return cached;
        try {
            Minecraft mc = Minecraft.getInstance();
            TextureAtlas atlas;
            try {
                atlas = mc.getAtlasManager().getAtlasOrThrow(ITEMS_ATLAS);
            } catch (Throwable t) {
                atlas = mc.getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_ITEMS);
            }
            TextureAtlasSprite found = atlas.getSprite(sprite);
            if (found == null || found == atlas.missingSprite()) return null;
            int tex = prepare(glId(atlas), false);
            if (tex == 0) return null;
            Region region = new Region(tex, found.getU0(), found.getV0(), found.getU1(), found.getV1());
            itemCache.put(sprite, region);
            return region;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Region item(String name) {
        return itemSprite(Identifier.fromNamespaceAndPath("minecraft", "item/" + name));
    }

    public static Region entityIcon(Entity entity) {
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return itemSprite(Identifier.fromNamespaceAndPath(type.getNamespace(), "item/" + type.getPath() + "_spawn_egg"));
    }
}
