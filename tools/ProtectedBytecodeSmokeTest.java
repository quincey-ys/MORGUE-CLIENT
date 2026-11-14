import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Eagerly asks the target JVM to verify every class in an obfuscated JAR.
 *
 * Usage: ProtectedBytecodeSmokeTest <jar> <compile-classpath>
 */
public final class ProtectedBytecodeSmokeTest {
    private ProtectedBytecodeSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Expected: <protected-jar> <compile-classpath>");
        }

        Path jarPath = Path.of(args[0]).toAbsolutePath();
        List<URL> urls = new ArrayList<>();
        urls.add(jarPath.toUri().toURL());

        String classpath = args[1];
        if (!classpath.isBlank()) {
            for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!entry.isBlank()) {
                    urls.add(Path.of(entry).toAbsolutePath().toUri().toURL());
                }
            }
        }

        int verified = 0;
        try (JarFile jar = new JarFile(jarPath.toFile());
             URLClassLoader loader = new URLClassLoader(
                     urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || !entryName.endsWith(".class")
                        || entryName.equals("module-info.class")
                        || entryName.endsWith("/module-info.class")) {
                    continue;
                }

                String className = entryName.substring(0, entryName.length() - ".class".length())
                        .replace('/', '.');
                try {
                    Class<?> type = Class.forName(className, false, loader);

                    // Resolve descriptors too, so missing dependency types and
                    // malformed method/field signatures are caught in CI.
                    type.getDeclaredConstructors();
                    type.getDeclaredMethods();
                    type.getDeclaredFields();
                    verified++;
                } catch (ReflectiveOperationException | LinkageError | SecurityException failure) {
                    throw new IllegalStateException(
                            "JVM bytecode verification failed for " + className, failure);
                }
            }
        }

        if (verified == 0) {
            throw new IllegalStateException("The protected JAR did not contain any verifiable classes.");
        }
        System.out.println("BYTECODE VERIFIED: " + verified
                + " protected classes loaded with -Xverify:all.");
    }
}
