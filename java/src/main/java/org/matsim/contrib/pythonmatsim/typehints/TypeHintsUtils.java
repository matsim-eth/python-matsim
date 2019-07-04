package org.matsim.contrib.pythonmatsim.typehints;

import java.lang.reflect.Method;
import java.util.*;

class TypeHintsUtils {


    private static final Map<Class<?>, String> PRIMITIVE_TYPE_NAMES = new HashMap<>();
    static {
        PRIMITIVE_TYPE_NAMES.put(int.class, "Union[int, JInt]");
        PRIMITIVE_TYPE_NAMES.put(short.class, "Union[int, JShort]");
        PRIMITIVE_TYPE_NAMES.put(boolean.class, "Union[int, JBoolean]");
        PRIMITIVE_TYPE_NAMES.put(char.class, "Union[string, JString]");
        PRIMITIVE_TYPE_NAMES.put(byte.class, "JByte");
        PRIMITIVE_TYPE_NAMES.put(long.class, "Union[long, JLong]");
        PRIMITIVE_TYPE_NAMES.put(float.class, "Union[float, JFloat]");
        PRIMITIVE_TYPE_NAMES.put(double.class, "Union[float, JDouble]");
        PRIMITIVE_TYPE_NAMES.put(void.class, "None");

        PRIMITIVE_TYPE_NAMES.put(int[].class, "JArray(JInt, 1)");
        PRIMITIVE_TYPE_NAMES.put(short[].class, "JArray(JShort, 1)");
        PRIMITIVE_TYPE_NAMES.put(boolean[].class, "JArray(JBoolean, 1)");
        PRIMITIVE_TYPE_NAMES.put(char[].class, "JArray(JChar, 1)");
        PRIMITIVE_TYPE_NAMES.put(byte[].class, "JArray(JByte, 1)");
        PRIMITIVE_TYPE_NAMES.put(long[].class, "JArray(JLong, 1)");
        PRIMITIVE_TYPE_NAMES.put(float[].class, "JArray(JFloat, 1)");
        PRIMITIVE_TYPE_NAMES.put(double[].class, "JArray(JDouble, 1)");
    }

    static final Collection<Class<?>> PRIMITIVE_TYPES = Collections.unmodifiableSet(PRIMITIVE_TYPE_NAMES.keySet());

    // This is the list defined in JPype for renaming methods to avoid clashes.
    // It should only be modified if bumping to another version of JPype that uses another list
    private static final Set<String> PY_KEYWORDS =
            new HashSet<>( Arrays.asList(
                    "del", "for", "is", "raise",
                    "assert", "elif", "from", "lambda", "return",
                    "break", "else", "global", "not", "try",
                    "class", "except", "if", "or", "while",
                    "continue", "exec", "import", "pass", "yield",
                    "def", "finally", "in", "print", "as", "None"
            ));


    public static Collection<Class<?>> getImportedTypes(Class<?> classe) {
        // TODO: look at generics as well
        // TODO: handle java.xxx packages specially (apparently not found by Reflections, but provided by JPype)
        if (!classe.isArray()) return Collections.singleton(classe);
        return getImportedTypes(classe.getComponentType());
    }

    public static Map<String, Collection<Method>> getMethods(Packages.ClassInfo classe) {
        final Map<String, Collection<Method>> methods = new HashMap<>();

        final Queue<Packages.ClassInfo> stack = Collections.asLifoQueue(new ArrayDeque<>());
        stack.add(classe);

        while (!stack.isEmpty()) {
            final Packages.ClassInfo info = stack.remove();
            stack.addAll(info.getInnerClasses());

            for (Method method : getMethods(info.getRootClass())) {
                methods.computeIfAbsent(
                        method.getName(),
                        k -> new HashSet<>()
                ).add(method);
            }
        }

        return methods;
    }

    public static Collection<Method> getMethods(Class<?> classe) {
        try {
            return Arrays.asList(classe.getMethods());
        }
        catch (NoClassDefFoundError e) {
            return Collections.emptyList();
        }
    }

    public static String pythonPackage(String module) {
        final int lastPoint = module.lastIndexOf('.');
        if (lastPoint < 0) return "";
        return module.substring(0, lastPoint);
    }

    public static String className(String fullName) {
        final int lastPoint = fullName.lastIndexOf('.');
        if (lastPoint < 0) return fullName;
        return fullName.substring(lastPoint + 1);
    }

    static String pythonQualifiedClassName(String rootPackage, Class<?> classe) {
        if (PRIMITIVE_TYPES.contains(classe)) return primitivePythonClassName(classe);
        try {

            String canonicalName = classe.getCanonicalName();
            // local or anonymous classes do not have a canonical name, but we do not care about them.
            if (canonicalName == null) return "Any";

            if (rootPackage == null || rootPackage.length() == 0 || canonicalName.startsWith("java.")) return canonicalName;
            return rootPackage+"."+canonicalName;
        }
        catch (NoClassDefFoundError e) {
            return "Any";
        }
    }

    static String pythonClassName(Class<?> classe) {
        // TODO handle case of a java class that would have a Python reserved keyword.
        // rather unlikely

        // TODO handle case of java arrays. Use a Union type allowing whatever JPype accepts for it
        try {
            String pythonQualifiedName = pythonQualifiedClassName(null, classe);

            String packageName = classe.getPackage().getName();

            return pythonQualifiedName.startsWith(packageName) ?
                    pythonQualifiedName.substring(packageName.length() + 1) :
                    // For case where "Any" or a JPype wrapper type
                    pythonQualifiedName;
        }
        catch (NoClassDefFoundError e) {
            return "Any";
        }
    }

    private static String primitivePythonClassName(Class<?> classe) {
        return PRIMITIVE_TYPE_NAMES.get(classe);
    }

    static String getJPypeName(String rawName) {
        return PY_KEYWORDS.contains(rawName) ? rawName+"_" : rawName;
    }
}
