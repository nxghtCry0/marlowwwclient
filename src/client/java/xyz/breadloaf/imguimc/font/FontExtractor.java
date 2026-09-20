package xyz.breadloaf.imguimc.font;

import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FontExtractor {

    public static String getFontPath(String fontNameTtf) {
        File primary = new File(fontDir(), fontNameTtf);
        if (primary.exists() && primary.length() > 0) return primary.getAbsolutePath();
        File fallback = new File(fontDir(), "font.ttf");
        if (fallback.exists() && fallback.length() > 0) return fallback.getAbsolutePath();

        File[] systemFallbacks = new File[] {
                new File("C:/Windows/Fonts/verdana.ttf"),
                new File("C:/Windows/Fonts/segoeui.ttf"),
                new File("C:/Windows/Fonts/arial.ttf"),
                new File("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                new File("/System/Library/Fonts/SFNS.ttf")
        };

        for (File sysFont : systemFallbacks) {
            if (sysFont.exists() && sysFont.length() > 0) {
                return sysFont.getAbsolutePath();
            }
        }

        return primary.getAbsolutePath();
    }

    public static byte[] getFontBytes() {
        try (InputStream in = getFontStream()) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (IOException ignored) {}

        String fontPath = getFontPath("font.ttf");
        File file = new File(fontPath);
        if (file.exists() && file.length() > 0) {
            try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException ignored) {}
        }
        return null;
    }

    public static InputStream getFontStream() {
        InputStream in = FontExtractor.class.getResourceAsStream("/assets/imnotcheatingyouare/font/verdana.ttf");
        if (in == null)
            in = FontExtractor.class.getClassLoader().getResourceAsStream("assets/imnotcheatingyouare/font/verdana.ttf");
        if (in == null && Thread.currentThread().getContextClassLoader() != null)
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/imnotcheatingyouare/font/verdana.ttf");
        if (in == null)
            in = FontExtractor.class.getClassLoader().getResourceAsStream("assets/krs/arial.ttf");
        if (in == null)
            in = FontExtractor.class.getClassLoader().getResourceAsStream("assets/client/arial.ttf");
        return in;
    }

    public static void extractFont() throws IOException {
        File fontDir = fontDir();
        File targetFile = new File(fontDir, "font.ttf");
        if (targetFile.exists() && targetFile.length() > 0) {
            return;
        }

        InputStream in = getFontStream();
        if (in == null)
            return;

        try (InputStream stream = in) {
            Files.createDirectories(fontDir.toPath());
            Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static File fontDir() {
        Minecraft mc = Minecraft.getInstance();
        File baseDir = mc != null && mc.gameDirectory != null ? mc.gameDirectory : new File(".");
        return new File(baseDir, "imgui_fonts");
    }
}
