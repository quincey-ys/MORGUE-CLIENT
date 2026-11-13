import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Black-box test for MORGUE's obfuscated runtime integrity verifier.
 *
 * Usage: IntegritySmokeTest <jar> <expected-status> <expected-activation-allowed>
 */
public final class IntegritySmokeTest {
    private static final String MANIFEST_MARKER = "META-INF/morgue-integrity.sha256";

    private IntegritySmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Expected: <jar> <expected-status> <expected-activation-allowed>");
        }

        Path jarPath = Path.of(args[0]).toAbsolutePath();
        String expectedStatus = args[1];
        boolean expectedAllowed = Boolean.parseBoolean(args[2]);
        String verifierClassName = locateVerifierClass(jarPath);

        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{jarPath.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> verifier = Class.forName(verifierClassName, true, loader);
            verifier.getMethod("verify").invoke(null);

            String actualStatus = (String) verifier.getMethod("getStatus").invoke(null);
            boolean actualAllowed = (Boolean) verifier.getMethod("isActivationAllowed").invoke(null);
            String details = (String) verifier.getMethod("getDetails").invoke(null);

            System.out.println("Integrity verifier " + verifierClassName + ": "
                    + actualStatus + " — " + details);
            if (!expectedStatus.equals(actualStatus)) {
                throw new IllegalStateException(
                        "Expected status " + expectedStatus + " but got " + actualStatus);
            }
            if (expectedAllowed != actualAllowed) {
                throw new IllegalStateException("Expected activation allowed=" + expectedAllowed
                        + " but got " + actualAllowed);
            }
        }
    }

    private static String locateVerifierClass(Path jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes;
                try (var stream = jar.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                String constants = new String(bytes, StandardCharsets.ISO_8859_1);
                if (constants.contains(MANIFEST_MARKER)) {
                    return entry.getName()
                            .substring(0, entry.getName().length() - ".class".length())
                            .replace('/', '.');
                }
            }
        }
        throw new IllegalStateException("Could not locate the obfuscated integrity verifier in " + jarPath);
    }
}
