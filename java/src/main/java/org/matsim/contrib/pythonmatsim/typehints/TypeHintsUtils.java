package org.matsim.contrib.pythonmatsim.typehints;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class TypeHintsUtils {
    static final Collection<Class<?>> PRIMITIVE_TYPES =
            Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(
                    int.class,
                    short.class,
                    boolean.class,
                    char.class,
                    byte.class,
                    long.class,
                    float.class,
                    double.class,
                    void.class,
                    // Those definitely need to be explicitly included
                    int[].class,
                    short[].class,
                    boolean[].class,
                    char[].class,
                    byte[].class,
                    long[].class,
                    float[].class,
                    double[].class
            )));

    public static Collection<Class<?>> getImportedTypes(Class<?> classe) {
        // TODO: look at generics as well
        if (!classe.isArray()) return Collections.singleton(classe);
        return getImportedTypes(classe.getComponentType());
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
       return classe.getCanonicalName();
    }

    static String pythonClassName(Class<?> classe) {
        try {
            return classe.getSimpleName();
        }
        catch (NoClassDefFoundError e) {
            return "Any";
        }
    }

    static String getJPypeName(Method method) {
        // TODO
        return method.getName();
    }
}
