import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Black-box check that every enum in the protected client JAR remains a JVM enum
 * and that its constants can be resolved through Enum.valueOf after obfuscation.
 *
 * Usage: EnumSmokeTest <protected-jar>
 */
public final class EnumSmokeTest {
    private static final int ACC_ENUM = 0x4000;

    private EnumSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected: <protected-jar>");
        }

        Path jarPath = Path.of(args[0]).toAbsolutePath();
        List<String> enumClasses = findEnumClasses(jarPath);
        if (enumClasses.isEmpty()) {
            throw new IllegalStateException("No enum classes were found in the protected JAR.");
        }

        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{jarPath.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            int testedConstants = 0;
            for (String className : enumClasses) {
                Class<?> type = Class.forName(className, true, loader);
                if (!type.isEnum()) {
                    throw new IllegalStateException(
                            className + " has enum bytecode but Class.isEnum() returned false.");
                }

                Object[] constants = type.getEnumConstants();
                if (constants == null || constants.length == 0) {
                    throw new IllegalStateException(className + " has no accessible enum constants.");
                }

                for (Object value : constants) {
                    Enum<?> constant = (Enum<?>) value;
                    Object resolved = Enum.valueOf(asEnumClass(type), constant.name());
                    if (resolved != value) {
                        throw new IllegalStateException(
                                "Enum.valueOf returned a different instance for "
                                        + className + "." + constant.name());
                    }
                    testedConstants++;
                }
                System.out.println("ENUM OK: " + className + " (" + constants.length + " constants)");
            }

            System.out.println("Verified " + enumClasses.size() + " enum classes and "
                    + testedConstants + " enum constants.");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Class asEnumClass(Class<?> type) {
        return type;
    }

    private static List<String> findEnumClasses(Path jarPath) throws Exception {
        List<String> result = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes;
                try (var stream = jar.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                String className = enumClassName(bytes);
                if (className != null) {
                    result.add(className.replace('/', '.'));
                }
            }
        }
        return result;
    }

    private static String enumClassName(byte[] bytes) throws Exception {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != 0xCAFEBABE) {
                return null;
            }
            input.readUnsignedShort(); // minor
            input.readUnsignedShort(); // major
            int count = input.readUnsignedShort();
            Object[] pool = new Object[count];

            for (int index = 1; index < count; index++) {
                int tag = input.readUnsignedByte();
                switch (tag) {
                    case 1 -> pool[index] = input.readUTF();
                    case 3, 4 -> input.skipNBytes(4);
                    case 5, 6 -> {
                        input.skipNBytes(8);
                        index++;
                    }
                    case 7 -> pool[index] = input.readUnsignedShort();
                    case 8, 16, 19, 20 -> input.skipNBytes(2);
                    case 9, 10, 11, 12, 17, 18 -> input.skipNBytes(4);
                    case 15 -> input.skipNBytes(3);
                    default -> throw new IllegalStateException("Unknown class-file constant-pool tag: " + tag);
                }
            }

            int accessFlags = input.readUnsignedShort();
            int thisClass = input.readUnsignedShort();
            int superClass = input.readUnsignedShort();
            if ((accessFlags & ACC_ENUM) == 0 || superClass == 0) {
                return null;
            }

            String superName = className(pool, superClass);
            if (!"java/lang/Enum".equals(superName)) {
                return null;
            }
            return className(pool, thisClass);
        }
    }

    private static String className(Object[] pool, int classIndex) {
        Object nameIndex = pool[classIndex];
        if (!(nameIndex instanceof Integer)) {
            throw new IllegalStateException("Malformed class constant at index " + classIndex);
        }
        Object value = pool[(Integer) nameIndex];
        if (!(value instanceof String)) {
            throw new IllegalStateException("Malformed UTF-8 class name at index " + nameIndex);
        }
        return (String) value;
    }
}
