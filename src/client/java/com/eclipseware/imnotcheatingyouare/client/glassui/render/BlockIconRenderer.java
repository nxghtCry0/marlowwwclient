package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3x2fStack;

public final class BlockIconRenderer {
    private BlockIconRenderer() {
    }

    public static void drawBlockIcon(
        GuiGraphicsExtractor graphics,
        String blockId,
        int fallbackColor,
        float x,
        float y,
        float size,
        float alpha
    ) {
        float a = Math.max(0f, Math.min(1f, alpha));
        if (a <= 0.01f) {
            return;
        }
        String trimmed = blockId.trim().toLowerCase(Locale.ROOT);
        String normalized = trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
        Identifier parsed = Identifier.tryParse(normalized);
        Identifier id = parsed != null ? parsed : Identifier.withDefaultNamespace("stone");
        Optional<Block> opt = BuiltInRegistries.BLOCK.getOptional(id);
        Block block = opt.orElse(Blocks.AIR);
        drawBlockIcon(graphics, block, id, fallbackColor, x, y, size, a);
    }

    public static void drawBlockIcon(
        GuiGraphicsExtractor graphics,
        Block block,
        Identifier blockId,
        int fallbackColor,
        float x,
        float y,
        float size,
        float alpha
    ) {
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawBlockIcon(blockId, fallbackColor, x, y, size, alpha);
            return;
        }
        Item item = block != Blocks.AIR ? block.asItem() : Items.AIR;
        if (item != Items.AIR && alpha > 0.25f) {
            ItemStack stack = new ItemStack(item);
            float scale = Math.max(0.1f, size / 16f);
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(x, y);
            pose.scale(scale, scale);
            graphics.item(stack, 0, 0);
            pose.popMatrix();
            return;
        }

        int tintFill = UiRenderPrimitives.multiplyAlpha(fallbackColor | 0xFF000000, 0.28f * alpha);
        int tintRim = UiRenderPrimitives.multiplyAlpha(fallbackColor | 0xFF000000, 0.85f * alpha);
        float r = Math.max(2.5f, size * 0.26f);
        UiRenderPrimitives.drawRoundedRect(graphics, x, y, size, size, r, tintFill);
        UiRenderPrimitives.drawRoundedBorder(graphics, x, y, size, size, r, 0.9f, tintRim, tintRim);
        float inner = size * 0.68f;
        float pad = (size - inner) * 0.5f;
        UiRenderPrimitives.drawIcon(
            graphics,
            LucideIcon.CUBOID,
            x + pad,
            y + pad,
            inner,
            UiRenderPrimitives.argb(Math.round(235f * alpha), 255, 255, 255)
        );
    }
}
