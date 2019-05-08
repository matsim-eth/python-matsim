package org.matsim.contrib.pythonmatsim.typehints;

import java.lang.reflect.Method;
import java.util.*;

class TypeHintsUtils {


    private static final Map<Class<?>, String> PRIMITIVE_TYPE_NAMES = new HashMap<>();
    static {
        PRIMITIVE_TYPE_NAMES.put(int.class, "int");
        PRIMITIVE_TYPE_NAMES.put(short.class, "int");
        PRIMITIVE_TYPE_NAMES.put(boolean.class, "int");
        PRIMITIVE_TYPE_NAMES.put(char.class, "string");
        PRIMITIVE_TYPE_NAMES.put(byte.class, "JByte");
        PRIMITIVE_TYPE_NAMES.put(long.class, "long");
        PRIMITIVE_TYPE_NAMES.put(float.class, "float");
        PRIMITIVE_TYPE_NAMES.put(double.class, "float");
        PRIMITIVE_TYPE_NAMES.put(void.class, "None");

        PRIMITIVE_TYPE_NAMES.put(int[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(short[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(boolean[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(char[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(byte[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(long[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(float[].class, "JArray");
        PRIMITIVE_TYPE_NAMES.put(double[].class, "JArray");
    }

    static final Collection<Class<?>> PRIMITIVE_TYPES = Collections.unmodifiableSet(PRIMITIVE_TYPE_NAMES.keySet());

    public static Collection<Class<?>> getImportedTypes(Class<?> classe) {
        // TODO: look at generics as well
        if (!classe.isArray()) return Collections.singleton(classe);
        return getImportedTypes(classe.getComponentType());
    }

    public static Collection<Method> getMethods(Packages.ClassInfo classe) {
        final Collection<Method> methods = new HashSet<>();

        final Queue<Packages.ClassInfo> stack = Collections.asLifoQueue(new ArrayDeque<>());
        stack.add(classe);

        while (!stack.isEmpty()) {
            final Packages.ClassInfo info = stack.remove();
            stack.addAll(info.getInnerClasses());

           methods.addAll(getMethods(info.getRootClass()));
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

    static String pythonQualifiedClassName(Class<?> classe) {
        if (PRIMITIVE_TYPES.contains(classe)) return primitivePythonClassName(classe);
        try {

            String canonicalName = classe.getCanonicalName();
            // local or anonymous classes do not have a canonical name, but we do not care about them.
            if (canonicalName == null) return "Any";

            return canonicalName;
        }
        catch (NoClassDefFoundError e) {
            return "Any";
        }
    }

    static String pythonClassName(Class<?> classe) {
        try {
            String pythonQualifiedName = pythonQualifiedClassName(classe);

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

    static String getJPypeName(Method method) {
        // TODO
        return method.getName();
    }
}
