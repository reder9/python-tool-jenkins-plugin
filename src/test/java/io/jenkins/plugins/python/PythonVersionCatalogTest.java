package io.jenkins.plugins.python;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonVersionCatalogTest {

    @Test
    void testCatalogLoadsVersions() {
        PythonVersionCatalog catalog = PythonVersionCatalog.getInstance();
        List<String> versions = catalog.getAvailableVersions();

        assertNotNull(versions);
        assertFalse(versions.isEmpty(), "Catalog should contain Python versions");
        assertTrue(versions.size() >= 5, "Catalog should have at least 5 versions");
    }

    @Test
    void testResolveVersion() {
        PythonVersionCatalog catalog = PythonVersionCatalog.getInstance();

        String latest = catalog.resolveVersion("latest");
        assertNotNull(latest);
        assertFalse(latest.isEmpty());

        String py312 = catalog.resolveVersion("3.12");
        assertNotNull(py312);
        assertTrue(py312.startsWith("3.12."), "3.12 prefix should resolve to 3.12.x");

        String py313 = catalog.resolveVersion("3.13");
        assertNotNull(py313);
        assertTrue(py313.startsWith("3.13."), "3.13 prefix should resolve to 3.13.x");
    }

    @Test
    void testDownloadsContainMajorPlatforms() {
        PythonVersionCatalog catalog = PythonVersionCatalog.getInstance();
        String latest = catalog.getLatestVersion();

        PythonVersionCatalog.PlatformDownload linuxX64 = catalog.getDownload(latest, "linux-x86_64");
        assertNotNull(linuxX64, "Missing linux-x86_64 for " + latest);
        assertTrue(linuxX64.getUrl().contains("linux-gnu"), "URL should target linux-gnu");

        PythonVersionCatalog.PlatformDownload macArm64 = catalog.getDownload(latest, "macos-aarch64");
        assertNotNull(macArm64, "Missing macos-aarch64 for " + latest);
        assertTrue(macArm64.getUrl().contains("apple-darwin"), "URL should target apple-darwin");

        PythonVersionCatalog.PlatformDownload winX64 = catalog.getDownload(latest, "windows-x86_64");
        assertNotNull(winX64, "Missing windows-x86_64 for " + latest);
        assertTrue(winX64.getUrl().contains("windows-msvc"), "URL should target windows-msvc");
    }
}
