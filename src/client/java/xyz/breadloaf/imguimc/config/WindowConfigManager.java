package xyz.breadloaf.imguimc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class WindowConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WindowConfig config = new WindowConfig();

    public static void loadAll() {
        File configFile = configFile();
        if (configFile.exists()) {
            try (var reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
                config = gson().fromJson(reader, WindowConfig.class);
                normalizeConfig();
            } catch (Exception e) {
                config = new WindowConfig();
                System.err.println("Failed to load ImGui window config: " + e.getMessage());
            }
        }
    }

    public static void saveAll() {
        Path tempFile = null;
        try {
            File baseDir = baseDir();
            File configFile = configFile();
            Files.createDirectories(baseDir.toPath());

            tempFile = Files.createTempFile(baseDir.toPath(), configFile.getName() + ".", ".tmp");
            byte[] jsonBytes = gson().toJson(config).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(jsonBytes);
                while (buffer.hasRemaining())
                    channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(tempFile, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile = null;
        } catch (Exception e) {
            System.err.println("Failed to save ImGui window config: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static WindowState getWindowState(String name) {
        normalizeConfig();
        return config.windows.computeIfAbsent(name, k -> new WindowState());
    }

    private static void normalizeConfig() {
        if (config == null || config.windows == null)
            config = new WindowConfig();
    }

    private static File baseDir() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = mc != null && mc.gameDirectory != null ? mc.gameDirectory : new File(".");
        return new File(gameDir, "imgui_config");
    }

    private static File configFile() {
        return new File(baseDir(), "imgui_windows.json");
    }

    private static Gson gson() {
        return GSON;
    }
}
