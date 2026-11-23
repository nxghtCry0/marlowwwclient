package com.eclipseware.imnotcheatingyouare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LicenseMigrationTest {

    @Test
    void licenseFileExistsAndContainsFullNpslText() throws IOException {
        Path licensePath = Path.of("LICENSE.md");
        Assertions.assertTrue(Files.exists(licensePath));
        String content = Files.readString(licensePath, StandardCharsets.UTF_8);
        Assertions.assertTrue(content.contains("Night Public Software License (NPSL)"));
        Assertions.assertTrue(content.contains("Version 1.0"));
        Assertions.assertTrue(content.contains("Copyright (c) 2026 Nxght (nxghtCry0)"));
        Assertions.assertTrue(content.contains("All Rights Reserved."));
        Assertions.assertTrue(content.contains("1. DEFINITIONS"));
        Assertions.assertTrue(content.contains("9. SEPARATE PERMISSIONS"));
    }

    @Test
    void legacyGplLicenseFilesAreRemoved() {
        Path gnuLicense = Path.of("GNU-LICENSE.md");
        Path licenseTxt = Path.of("LICENSE.txt");
        Assertions.assertFalse(Files.exists(gnuLicense));
        Assertions.assertFalse(Files.exists(licenseTxt));
    }

    @Test
    void fabricModMetadataUsesNpsl() throws IOException {
        Path modJsonPath = Path.of("src/main/resources/fabric.mod.json");
        String modJson = Files.readString(modJsonPath, StandardCharsets.UTF_8);
        Assertions.assertTrue(modJson.contains("\"license\": \"NPSL-1.0\""));
        Assertions.assertFalse(modJson.contains("GPL"));
        Assertions.assertFalse(modJson.contains("GNU"));
        Assertions.assertFalse(modJson.contains("Open Source"));
    }

    @Test
    void buildAndReadmeReferenceNpslLicenseMd() throws IOException {
        Path buildGradlePath = Path.of("build.gradle");
        String buildGradle = Files.readString(buildGradlePath, StandardCharsets.UTF_8);
        Assertions.assertTrue(buildGradle.contains("from(\"LICENSE.md\")"));
        Assertions.assertFalse(buildGradle.contains("from(\"LICENSE\")"));

        Path readmePath = Path.of("README.md");
        String readme = Files.readString(readmePath, StandardCharsets.UTF_8);
        Assertions.assertTrue(readme.contains("Night Public Software License (NPSL)"));
        Assertions.assertTrue(readme.contains("LICENSE.md"));
        Assertions.assertFalse(readme.contains("GPL-3.0"));
        Assertions.assertFalse(readme.contains("GNU-LICENSE.md"));
        Assertions.assertFalse(readme.contains("LICENSE.txt"));
    }
}

