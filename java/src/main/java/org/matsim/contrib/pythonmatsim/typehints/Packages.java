package org.matsim.contrib.pythonmatsim.typehints;

import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aim of this class is to group classes per package, to allow easier generation of python files
 */
class Packages {
    private static final Logger log = Logger.getLogger(Packages.class);
    private final Map<String, PackageInfo> packages = new HashMap<>();

    private static final Collection<Class<?>> PRIMITIVE_TYPES =
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
            ));

    public ClassTypeInfo addClass(Class<?> classe) {
        if (PRIMITIVE_TYPES.contains(classe)) {
            // TODO return information pertinent to JPype
            return null;
        }
        log.debug(classe);
        if (classe.isArray()) {
            Class<?> component = classe.getComponentType();
            return createPackage(component).addClass(classe).finalizeMethods();
        }
        // generics?
        return createPackage(classe).addClass(classe).finalizeMethods();
    }

    private PackageInfo createPackage(Class<?> classe) {
        if (classe == null) throw new IllegalArgumentException();
        return packages.computeIfAbsent(
                classe.getPackage().getName(),
                PackageInfo::new);
    }

    public Iterable<PackageInfo> getPackages() {
        return packages.values();
    }

    public class PackageInfo {
        private final String packageName;
        private final Map<Class<?>, ClassTypeInfo> classes = new LinkedHashMap<>();

        public PackageInfo(String packageName) {
            this.packageName = packageName;
        }

        private ClassTypeInfo addClass(Class<?> classe) {
            return classes.computeIfAbsent(classe, ClassTypeInfo::new);
        }

        public String getPackageName() {
            return packageName;
        }

        public Iterable<ClassTypeInfo> getClasses() {
            return classes.values();
        }

        public Iterable<String> getImportedPackages() {
            return classes.values().stream()
                    .flatMap(c -> c.getMethods().stream())
                    .map(MethodTypeInfo::getReturnType)
                    .filter(Objects::nonNull)
                    .map(ClassTypeInfo::getPackage)
                    .map(PackageInfo::getPackageName)
                    .map(Packages::pythonPackage)
                    .collect(Collectors.toSet());
        }
    }

    private static String pythonPackage(String module) {
        final int lastPoint = module.lastIndexOf('.');
        if (lastPoint < 0) return "";
        return module.substring(0, lastPoint);
    }

    private static String className(String fullName) {
        final int lastPoint = fullName.lastIndexOf('.');
        if (lastPoint < 0) return fullName;
        return fullName.substring(lastPoint + 1);
    }

    public class ClassTypeInfo {
        private final PackageInfo packageInfo;
        private final String className;

        private final Set<MethodTypeInfo> methods = new LinkedHashSet<>();
        private boolean finalized = false;

        private ClassTypeInfo(final Class<?> classe) {
            if (classe.isArray()) {
                packageInfo = createPackage(classe.getComponentType());
            }
            else {
                packageInfo = createPackage(classe);
            }
            // this does not use "Class::getSimpleName", because it fails for classes not on the classpath.
            className = className(classe.getName());

            try {
                for (Method method : classe.getMethods()) {
                    methods.add(new MethodTypeInfo(method));
                }
            }
            catch (NoClassDefFoundError e) {
                // not sure what the best level is to catch this...
                log.info("Could not find class "+classe.getName()+". Might not be a problem.");
            }
        }

        private ClassTypeInfo finalizeMethods() {
            if (finalized) return this;

            finalized = true;
            for (MethodTypeInfo method : methods) {
               method.setReturnType();
            }
            return this;
        }

        public PackageInfo getPackage() {
            return packageInfo;
        }

        public String getClassName() {
            return className;
        }

        public Set<MethodTypeInfo> getMethods() {
            return methods;
        }
    }

    public class MethodTypeInfo {
        private final String methodName;
        private Class<?> returnClass;
        private ClassTypeInfo returnType;


        private MethodTypeInfo(Method method) {
            methodName = method.getName();
            returnClass = method.getReturnType();
        }

        public String getMethodName() {
            return methodName;
        }

        private void setReturnType() {
            // test on class rather than type, because primitive types allow null types.
            if (returnClass == null) return;
            returnType = addClass(returnClass);
            returnClass = null;

            if (returnType != null) {
                returnType.finalizeMethods();
            }
        }

        public ClassTypeInfo getReturnType() {
            return returnType;
        }
    }
}
