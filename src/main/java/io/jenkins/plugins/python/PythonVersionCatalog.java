package io.jenkins.plugins.python;

import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and caches the standalone Python version catalog from embedded versions.json.
 */
public final class PythonVersionCatalog implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PythonVersionCatalog.class.getName());

    private static volatile PythonVersionCatalog instance;

    private final Map<String, PythonRelease> releases = new LinkedHashMap<>();

    public static class PlatformDownload implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String url;
        private final long size;
        private final String filename;

        public PlatformDownload(String url, long size, String filename) {
            this.url = url;
            this.size = size;
            this.filename = filename;
        }

        public String getUrl() {
            return url;
        }

        public long getSize() {
            return size;
        }

        public String getFilename() {
            return filename;
        }
    }

    public static class PythonRelease implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String version;
        private final String releaseTag;
        private final Map<String, PlatformDownload> platforms = new LinkedHashMap<>();

        public PythonRelease(String version, String releaseTag) {
            this.version = version;
            this.releaseTag = releaseTag;
        }

        public String getVersion() {
            return version;
        }

        public String getReleaseTag() {
            return releaseTag;
        }

        public Map<String, PlatformDownload> getPlatforms() {
            return Collections.unmodifiableMap(platforms);
        }

        public PlatformDownload getPlatform(String platformKey) {
            return platforms.get(platformKey);
        }

        public void addPlatform(String platformKey, PlatformDownload download) {
            this.platforms.put(platformKey, download);
        }
    }

    public static PythonVersionCatalog getInstance() {
        if (instance == null) {
            synchronized (PythonVersionCatalog.class) {
                if (instance == null) {
                    instance = new PythonVersionCatalog();
                }
            }
        }
        return instance;
    }

    private PythonVersionCatalog() {
        loadCatalog();
    }

    private void loadCatalog() {
        try (InputStream in = PythonVersionCatalog.class.getResourceAsStream("versions.json")) {
            if (in == null) {
                LOGGER.log(Level.WARNING, "Embedded versions.json resource not found on classpath!");
                return;
            }
            String content = IOUtils.toString(in, StandardCharsets.UTF_8);
            JSONObject root = JSONObject.fromObject(content);
            if (root.has("versions")) {
                JSONObject vObj = root.getJSONObject("versions");
                for (Object keyObj : vObj.keySet()) {
                    String ver = String.valueOf(keyObj);
                    JSONObject vData = vObj.getJSONObject(ver);
                    String releaseTag = vData.optString("releaseTag", "");
                    PythonRelease release = new PythonRelease(ver, releaseTag);

                    if (vData.has("platforms")) {
                        JSONObject pObj = vData.getJSONObject("platforms");
                        for (Object pKeyObj : pObj.keySet()) {
                            String pKey = String.valueOf(pKeyObj);
                            JSONObject pData = pObj.getJSONObject(pKey);
                            String url = pData.getString("url");
                            long size = pData.optLong("size", 0);
                            String filename = pData.optString("filename", "");
                            release.addPlatform(pKey, new PlatformDownload(url, size, filename));
                        }
                    }
                    releases.put(ver, release);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse embedded Python versions catalog", e);
        }
    }

    public List<String> getAvailableVersions() {
        return new ArrayList<>(releases.keySet());
    }

    public PythonRelease getRelease(String version) {
        return releases.get(version);
    }

    public String getLatestVersion() {
        List<String> list = getAvailableVersions();
        return list.isEmpty() ? "3.12.15" : list.get(0);
    }

    /**
     * Resolves exact version or prefix (e.g. "3.12" -> "3.12.15", "latest" -> latest stable).
     */
    public String resolveVersion(String versionSpec) {
        if (versionSpec == null || versionSpec.trim().isEmpty() || "latest".equalsIgnoreCase(versionSpec.trim())) {
            return getLatestVersion();
        }
        String clean = versionSpec.trim();
        if (releases.containsKey(clean)) {
            return clean;
        }
        // Prefix matching: find first matching major.minor
        for (String v : releases.keySet()) {
            if (v.startsWith(clean + ".") || v.equals(clean)) {
                return v;
            }
        }
        return clean;
    }

    public PlatformDownload getDownload(String version, String platformKey) {
        String resolved = resolveVersion(version);
        PythonRelease release = releases.get(resolved);
        if (release != null) {
            return release.getPlatform(platformKey);
        }
        return null;
    }
}
