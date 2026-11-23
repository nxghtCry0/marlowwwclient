package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class KineticTextRenderer {
    private static final char[] CIPHER_GLYPHS = new char[]{':', '/', '+', '-', '*', '=', '~', '.'};

    private KineticTextRenderer() {
    }

    public static void drawKineticDecode(
        GuiGraphicsExtractor graphics,
        String fullText,
        float x,
        float y,
        float maxWidth,
        float sizePx,
        TextGradient gradient,
        SatoshiWeight weight,
        float ageSec,
        int accentColor,
        int secondaryColor,
        float alpha,
        float targetDurationSec,
        boolean showTrail,
        boolean typingEnabled,
        UiTheme theme
    ) {
        if (fullText == null || fullText.isEmpty() || alpha <= 0.02f) {
            return;
        }
        String clipped = UiRenderPrimitives.ellipsize(fullText, maxWidth, sizePx, weight);
        if (clipped.isEmpty()) {
            return;
        }
        if (!typingEnabled || ageSec >= targetDurationSec + 0.16f) {
            SatoshiFont.draw(graphics, clipped, x, y, sizePx, gradient, weight);
            return;
        }

        int len = clipped.length();
        float waveWidth = Math.min(5.5f, Math.max(2.2f, len * 0.22f));
        float effectiveDur = clamp(targetDurationSec, 0.14f, 0.55f);
        float speed = (len + waveWidth) / effectiveDur;
        float safeAge = Math.max(0f, ageSec);
        float headProgress = safeAge * speed;

        int settledCount = clampInt((int) Math.floor(headProgress - waveWidth), 0, len);
        int headCount = clampInt((int) Math.ceil(headProgress), 0, len);

        if (settledCount > 0) {
            SatoshiFont.draw(graphics, clipped.substring(0, settledCount), x, y, sizePx, gradient, weight);
        }

        int frameStep = (int) Math.floor(safeAge * 26f);
        for (int i = settledCount; i < headCount; i++) {
            char realChar = clipped.charAt(i);
            if (realChar == ' ') {
                continue;
            }
            float t = clamp((headProgress - i) / waveWidth, 0f, 1f);
            if (t <= 0.08f) {
                continue;
            }
            float gx = x + SatoshiFont.width(clipped.substring(0, i), sizePx, weight);
            float lift = (1f - UiRenderPrimitives.easeOutCubic(t)) * 1.9f;
            String charStr;
            TextGradient glyphGrad;
            if (t < 0.56f) {
                int cIdx = Math.abs(frameStep * 3 + i * 7 + realChar) % CIPHER_GLYPHS.length;
                charStr = String.valueOf(CIPHER_GLYPHS[cIdx]);
                float glowA = (0.35f + 0.65f * (t / 0.56f)) * alpha;
                glyphGrad = TextGradient.lerp(
                    TextGradient.accent(glowA, secondaryColor, theme),
                    TextGradient.accent(glowA, accentColor, theme),
                    t / 0.56f
                );
            } else {
                charStr = String.valueOf(realChar);
                float morph = clamp((t - 0.56f) / 0.44f, 0f, 1f);
                glyphGrad = TextGradient.lerp(
                    TextGradient.accent(alpha, accentColor, theme),
                    gradient,
                    UiRenderPrimitives.easeOutCubic(morph)
                );
            }
            SatoshiFont.draw(graphics, charStr, gx, y - lift, sizePx, glyphGrad, weight);
        }

        if (showTrail) {
            float headW = SatoshiFont.width(clipped.substring(0, headCount), sizePx, weight);
            float tailSpan = Math.min(headW, 18f);
            float doneAge = safeAge - effectiveDur;
            float rawFade;
            if (doneAge < 0f) {
                rawFade = 0.85f + 0.15f * (float) Math.sin(safeAge * 22f);
            } else if (doneAge < 0.14f) {
                rawFade = 1f - UiRenderPrimitives.easeOutCubic(doneAge / 0.14f);
            } else {
                rawFade = 0f;
            }
            float headFade = clamp(rawFade, 0f, 1f);
            if (headFade > 0.02f) {
                float tipX = x + headW + 0.8f;
                if (tailSpan > 2f) {
                    UiRenderPrimitives.drawRoundedGradient(
                        graphics, tipX - tailSpan, y + sizePx + 0.8f, tailSpan, 1.1f, 0.55f,
                        UiRenderPrimitives.multiplyAlpha(accentColor, 0.38f * headFade * alpha),
                        UiRenderPrimitives.multiplyAlpha(secondaryColor, 0.12f * headFade * alpha)
                    );
                }
                float needleH = (sizePx + 1.2f) * headFade;
                float needleY = y + (sizePx + 1.2f - needleH) * 0.5f;
                UiRenderPrimitives.drawRoundedRect(
                    graphics, tipX, needleY, 1.15f, needleH, 0.57f,
                    UiRenderPrimitives.multiplyAlpha(accentColor, 0.92f * headFade * alpha)
                );
            }
        }
    }

    public static void drawSearchPlaceholder(
        GuiGraphicsExtractor graphics,
        List<String> prompts,
        float x,
        float y,
        float maxWidth,
        float sizePx,
        float elapsedSeconds,
        int accentColor,
        int secondaryColor,
        float panelAlpha,
        UiTheme theme
    ) {
        if (prompts == null || prompts.isEmpty()) {
            return;
        }
        if (!theme.typingAnimationEnabled() || prompts.size() == 1) {
            String first = UiRenderPrimitives.ellipsize(prompts.get(0), maxWidth, sizePx, SatoshiWeight.MEDIUM);
            SatoshiFont.draw(graphics, first, x, y, sizePx, TextGradient.mist(0.56f * panelAlpha, theme), SatoshiWeight.MEDIUM);
            return;
        }

        float cycleDur = 3.8f;
        int idx = ((int) (elapsedSeconds / cycleDur)) % prompts.size();
        int nextIdx = (idx + 1) % prompts.size();
        String prompt = prompts.get(idx);
        String nextPrompt = prompts.get(nextIdx);
        float phase = elapsedSeconds % cycleDur;

        if (phase < 0.40f) {
            drawKineticDecode(
                graphics, prompt, x, y, maxWidth, sizePx,
                TextGradient.mist(0.62f * panelAlpha, theme),
                SatoshiWeight.MEDIUM, phase, accentColor, secondaryColor, panelAlpha,
                0.34f, true, true, theme
            );
            return;
        }

        if (phase < 3.45f) {
            SatoshiFont.draw(graphics, prompt, x, y, sizePx, TextGradient.mist(0.56f * panelAlpha, theme), SatoshiWeight.MEDIUM);
            float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 5.8f);
            float cx = x + SatoshiFont.width(prompt, sizePx, SatoshiWeight.MEDIUM) + 1.4f;
            UiRenderPrimitives.drawRoundedRect(
                graphics, cx, y - 0.3f, 1.0f, sizePx + 0.8f, 0.5f,
                UiRenderPrimitives.multiplyAlpha(accentColor, (0.28f + 0.38f * pulse) * panelAlpha)
            );
            return;
        }

        float morphT = clamp((phase - 3.45f) / (cycleDur - 3.45f), 0f, 1f);
        int len = Math.max(prompt.length(), nextPrompt.length());
        float sweepPos = morphT * (len + 3f);
        int step = (int) Math.floor(elapsedSeconds * 24f);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            float d = i - sweepPos;
            if (d > 0.6f && i < prompt.length()) {
                sb.append(prompt.charAt(i));
            } else if (d < -1.8f && i < nextPrompt.length()) {
                sb.append(nextPrompt.charAt(i));
            } else if (d >= -1.8f && d <= 0.6f) {
                int cIdx = Math.abs(step * 5 + i * 3) % CIPHER_GLYPHS.length;
                sb.append(CIPHER_GLYPHS[cIdx]);
            }
        }
        String morphed = UiRenderPrimitives.ellipsize(sb.toString(), maxWidth, sizePx, SatoshiWeight.MEDIUM);
        TextGradient grad = TextGradient.lerp(
            TextGradient.mist(0.58f * panelAlpha, theme),
            TextGradient.pearl(0.68f * panelAlpha, accentColor, 0.25f, theme),
            0.35f + 0.45f * (float) Math.sin(morphT * 3.14159f)
        );
        SatoshiFont.draw(graphics, morphed, x, y, sizePx, grad, SatoshiWeight.MEDIUM);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
