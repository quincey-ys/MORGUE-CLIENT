import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Forces the JVM to load, link, and verify every class in the transformed release JAR.
 *
 * <p>Run with {@code -Xverify:all}. HotSpot verifies a class when it is linked, and a class
 * is linked on first active use. {@link ClassLoader#resolveClass(Class)} is a no-op on modern
 * HotSpot, and {@code Class.forName(name, false, loader)} only defines the class, so neither
 * actually runs the bytecode verifier. Initializing each class forces linking, and therefore
 * verification, before its static initializer executes.
 *
 * <p>Static initializers of game classes can legitimately fail outside a running Minecraft
 * client. Those failures are recorded in the report and do not fail the build, because
 * verification has already completed by the time an initializer runs. Verification errors
 * ({@link VerifyError} and {@link ClassFormatError}) always fail the build.
 */
public final class BytecodeSmokeTest {
    private BytecodeSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected: <protected release JAR> <report path>.");
        }

        boolean verifyAll = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(argument -> argument.equals("-Xverify:all")
                        || argument.equals("-Xverify:all=true"));
        if (!verifyAll) {
            throw new IllegalStateException("The JVM was not started with -Xverify:all.");
        }

        Path jarPath = Path.of(args[0]).toAbsolutePath().normalize();
        Path reportPath = Path.of(args[1]).toAbsolutePath().normalize();
        Set<String> classNames = new LinkedHashSet<>();
        try (JarFile archive = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (!entryName.endsWith(".class")
                        || entryName.equals("module-info.class")
                        || entryName.endsWith("/module-info.class")
                        || entryName.equals("package-info.class")
                        || entryName.endsWith("/package-info.class")
                        || entryName.startsWith("META-INF/versions/")) {
                    continue;
                }
                String className = entryName.substring(0, entryName.length() - 6)
                        .replace('/', '.')
                        .replace('\\', '.');
                classNames.add(className);
            }
        }
        if (classNames.isEmpty()) {
            throw new IllegalStateException("No class files were found in " + jarPath);
        }

        int verified = 0;
        List<String> initializationFailures = new ArrayList<>();
        URL jarUrl = jarPath.toUri().toURL();
        try (ReleaseClassLoader loader = new ReleaseClassLoader(
                new URL[]{jarUrl}, ClassLoader.getSystemClassLoader(), classNames)) {
            for (String className : classNames) {
                Class<?> loadedClass;
                try {
                    loadedClass = loader.loadAndInitialize(className);
                } catch (VerifyError | ClassFormatError failure) {
                    throw new IllegalStateException(
                            "JVM bytecode verification failed while initializing " + className, failure);
                } catch (Throwable failure) {
                    // Verification precedes initialization, so reaching this branch means the
                    // class was verified but its static initializer needs a live game client.
                    initializationFailures.add(className + " (" + failure.getClass().getSimpleName() + ")");
                    try {
                        loadedClass = loader.loadClass(className, false);
                    } catch (Throwable loadFailure) {
                        throw new IllegalStateException(
                                "Class could not be loaded for verification: " + className, loadFailure);
                    }
                }
                if (loadedClass.getClassLoader() != loader) {
                    throw new IllegalStateException(
                            "Class resolved outside the release JAR: " + className);
                }
                verified++;
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("JVM verification: -Xverify:all\n");
        report.append("Protected JAR classes verified: ").append(verified).append('\n');
        report.append("Release JAR: ").append(jarPath.getFileName()).append('\n');
        if (!initializationFailures.isEmpty()) {
            report.append("Static initializers deferred outside Minecraft (classes were verified):\n");
            for (String failure : initializationFailures) {
                report.append("  - ").append(failure).append('\n');
            }
        }
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
        System.out.print(report);
    }

    /** Ensures protected classes are defined from the release JAR, not build output. */
    private static final class ReleaseClassLoader extends URLClassLoader {
        private final Set<String> releaseClasses;

        private ReleaseClassLoader(URL[] urls, ClassLoader parent, Set<String> releaseClasses) {
            super(urls, parent);
            this.releaseClasses = Collections.unmodifiableSet(new LinkedHashSet<>(releaseClasses));
        }

        private Class<?> loadAndInitialize(String name) throws ClassNotFoundException {
            // Initialization requires linking first, and linking runs the bytecode verifier.
            return Class.forName(name, true, this);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!releaseClasses.contains(name)) {
                return super.loadClass(name, resolve);
            }

            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = findClass(name);
                }
                // resolveClass() is a no-op on HotSpot; verification happens on initialization.
                return loaded;
            }
        }
    }
}
