import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads every class from the transformed release JAR without initializing it.
 * Run with -Xverify:all so the JVM verifies bytecode as each class is defined.
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

        int loaded = 0;
        URL jarUrl = jarPath.toUri().toURL();
        try (URLClassLoader loader = new ReleaseClassLoader(
                new URL[]{jarUrl}, ClassLoader.getSystemClassLoader(), classNames)) {
            for (String className : classNames) {
                try {
                    Class<?> loadedClass = Class.forName(className, false, loader);
                    if (loadedClass.getClassLoader() != loader) {
                        throw new IllegalStateException(
                                "Class resolved outside the release JAR: " + className);
                    }
                    loaded++;
                } catch (Throwable failure) {
                    throw new IllegalStateException(
                            "JVM bytecode verification failed while loading " + className, failure);
                }
            }
        }

        String report = "JVM verification: -Xverify:all\\n"
                + "Protected JAR class definitions verified: " + loaded + "\\n"
                + "Release JAR: " + jarPath.getFileName() + "\\n";
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
        System.out.print(report);
    }

    /** Ensures protected classes are defined from the release JAR, not build output. */
    private static final class ReleaseClassLoader extends URLClassLoader {
        private final Set<String> releaseClasses;

        private ReleaseClassLoader(URL[] urls, ClassLoader parent, Set<String> releaseClasses) {
            super(urls, parent);
            this.releaseClasses = Collections.unmodifiableSet(new LinkedHashSet<>(releaseClasses));
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
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }
    }
}
