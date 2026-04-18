package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Screenshare / Staff-mode bypass utilities.
 *
 * Covers the main vectors staff use during a live screenshare:
 *   1. Process list  — client JAR name should not be suspicious
 *   2. %appdata% scan — no obvious cheat folders / config files
 *   3. Recent files   — clean up MRU entries that reference cheat paths
 *   4. Temp files     — wipe any temp artefacts we dropped
 *   5. Fabric mod list — our mod.json uses a clean, innocent mod ID
 *
 * NOTE: Config is stored in a dot-prefixed hidden directory with an
 * innocuous name so it doesn't show up in a casual folder browse.
 */
public class ScreenshareBypass {

    private static final String CONFIG_DIR_NAME = ".minecraft_perf_cache";

    /**
     * Returns the safe config directory path, creating it if needed.
     * Use this instead of writing to .minecraft/config/clientname/
     */
    public static Path getSafeConfigDir() {
        Path base = Paths.get(System.getProperty("user.home"), CONFIG_DIR_NAME);
        try {
            Files.createDirectories(base);
            File f = base.toFile();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("attrib +H " + f.getAbsolutePath());
            }
        } catch (IOException ignored) {}
        return base;
    }

    /**
     * Wipe any temp files we may have created during this session.
     * Call on shutdown / before a screenshare is expected.
     */
    public static void cleanTempArtefacts() {
        try {
            Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
            Files.list(tmp)
                 .filter(p -> p.getFileName().toString().startsWith("mc_ec_"))
                 .forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {}
    }

    /**
     * Write a file with an innocuous name (looks like a JVM perf log).
     * Wraps FileWriter so callers don't need to think about paths.
     */
    public static FileWriter getSafeConfigWriter(String filename) throws IOException {
        return new FileWriter(getSafeConfigDir().resolve(filename).toFile());
    }
}
