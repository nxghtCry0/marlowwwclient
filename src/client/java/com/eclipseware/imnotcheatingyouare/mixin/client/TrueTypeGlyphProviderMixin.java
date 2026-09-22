package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TrueTypeGlyphProvider.class)
public class TrueTypeGlyphProviderMixin {

    @WrapMethod(method = "loadGlyph")
    private UnbakedGlyph imnotcheatingyouare$synchronizeLoadGlyph(int codePoint, FT_Face face, int glyphIndex, Operation<UnbakedGlyph> original) {
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            return original.call(codePoint, face, glyphIndex);
        }
    }
}
