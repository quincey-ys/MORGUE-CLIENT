package dev.morgue.client.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Verifies the packaged JAR's entries against the embedded build-time manifest.
 *
 * This is a local tamper/corruption check, not a cryptographic signature. A
 * determined editor who changes code can also replace the embedded manifest.
 * External release checksums are provided separately by the build pipeline.
 */
public final class MorgueIntegrity {
    private static final String MANIFEST_ENTRY = "META-INF/morgue-integrity.sha256";

    private static volatile String status = "NOT CHECKED";
    private static volatile String details = "The packaged integrity check has not run.";

    private MorgueIntegrity() {
    }

    public static void verify() {
        try {
            CodeSource codeSource = MorgueIntegrity.class.getProtectionDomain() == null
                    ? null
                    : MorgueIntegrity.class.getProtectionDomain().getCodeSource();
            URL location = codeSource == null ? null : codeSource.getLocation();
            if (location == null || !"file".equalsIgnoreCase(location.getProtocol())) {
                if (isDevelopmentEnvironment()) {
                    setStatus("SKIPPED", "Fabric development environment; no packaged JAR check is required.");
                } else {
                    setStatus("FAILED", "The runtime did not expose a verifiable local JAR location.");
                }
                return;
            }

            URI locationUri = location.toURI();
            Path jarPath = Path.of(locationUri);
            if (Files.isDirectory(jarPath) && isDevelopmentEnvironment()) {
                setStatus("SKIPPED", "Fabric development run detected; packaged-JAR verification was skipped.");
                return;
            }
            if (!Files.isRegularFile(jarPath)
                    || !jarPath.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                setStatus("FAILED", "A packaged MORGUE JAR could not be located for verification.");
                return;
            }

            verifyJar(jarPath);
        } catch (Exception exception) {
            setStatus("FAILED", "Integrity verification could not complete: "
                    + exception.getClass().getSimpleName());
        }
    }

    private static boolean isDevelopmentEnvironment() {
        try {
            Class<?> loaderType = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderType.getMethod("getInstance").invoke(null);
            return (Boolean) loaderType.getMethod("isDevelopmentEnvironment").invoke(loader);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    private static void verifyJar(Path jarPath) throws IOException, java.security.NoSuchAlgorithmException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry manifestEntry = jar.getJarEntry(MANIFEST_ENTRY);
            if (manifestEntry == null) {
                setStatus("FAILED", "The embedded integrity manifest is missing.");
                return;
            }

            Map<String, String> expected = new TreeMap<>();
            try (InputStream stream = jar.getInputStream(manifestEntry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    int separator = line.indexOf("  ");
                    if (separator <= 0 || separator + 2 >= line.length()) {
                        setStatus("FAILED", "The embedded integrity manifest contains an invalid line.");
                        return;
                    }
                    String hash = line.substring(0, separator).trim().toLowerCase(java.util.Locale.ROOT);
                    String name = line.substring(separator + 2);
                    if (!hash.matches("[0-9a-f]{64}") || name.isBlank()
                            || expected.put(name, hash) != null) {
                        setStatus("FAILED", "The embedded integrity manifest contains invalid or duplicate entries.");
                        return;
                    }
                }
            }

            Map<String, String> actual = new TreeMap<>();
            int manifestCount = 0;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (MANIFEST_ENTRY.equals(entry.getName())) {
                    manifestCount++;
                    continue;
                }
                String hash = sha256(jar, entry);
                if (actual.put(entry.getName(), hash) != null) {
                    setStatus("FAILED", "The packaged JAR contains duplicate entry names.");
                    return;
                }
            }

            if (manifestCount != 1 || expected.isEmpty()) {
                setStatus("FAILED", "The embedded integrity manifest is missing, duplicated, or empty.");
                return;
            }
            if (!expected.keySet().equals(actual.keySet())) {
                setStatus("FAILED", "The packaged JAR has missing or unexpected entries.");
                return;
            }

            for (Map.Entry<String, String> entry : expected.entrySet()) {
                String actualHash = actual.get(entry.getKey());
                if (!MessageDigest.isEqual(
                        entry.getValue().getBytes(StandardCharsets.US_ASCII),
                        actualHash.getBytes(StandardCharsets.US_ASCII))) {
                    setStatus("FAILED", "A packaged file failed its SHA-256 check: " + entry.getKey());
                    return;
                }
            }

            setStatus("VERIFIED", "Verified " + actual.size() + " packaged JAR entries.");
        }
    }

    private static String sha256(JarFile jar, JarEntry entry)
            throws IOException, java.security.NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = jar.getInputStream(entry);
             DigestInputStream digestStream = new DigestInputStream(stream, digest)) {
            digestStream.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void setStatus(String nextStatus, String nextDetails) {
        status = nextStatus;
        details = nextDetails;
        System.out.println("[MORGUE][INTEGRITY] " + status + " - " + details);
    }

    public static String getStatus() {
        return status;
    }

    public static String getDetails() {
        return details;
    }

    /**
     * Only confirmed development runs may skip; packaged runs require a complete check.
     */
    public static boolean isActivationAllowed() {
        return "VERIFIED".equals(status) || "SKIPPED".equals(status);
    }
}
