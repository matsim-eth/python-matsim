package org.matsim.contrib.pythonmatsim.typehints;

import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aim of this class is to group classes per package, to allow easier generation of python files
 */
class Packages {
    private static final Logger log = Logger.getLogger(Packages.class);
    private final Map<String, PackageInfo> packages = new HashMap<>();

    public void addClass(Class<?> classe) {
        if (TypeHintsUtils.PRIMITIVE_TYPES.contains(classe)) {
            return;
        }

        log.debug(classe);
        if (classe.isArray()) {
            Class<?> component = classe.getComponentType();
            createPackage(component).addClass(classe);
        }
        else {
            // generics?
           createPackage(classe).addClass(classe);
        }
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
        private final Map<Class<?>, ClassInfo> rootClasses = new LinkedHashMap<>();

        public PackageInfo(String packageName) {
            this.packageName = packageName;
        }

        private void addClass(Class<?> classe) {
            try {
                if (classe.isMemberClass() || classe.isLocalClass() || classe.isAnonymousClass() ||
                        // only consider public classes
                        !Modifier.isPublic(classe.getModifiers())) {
                    return;
                }
                rootClasses.put(classe, new ClassInfo(classe));
            }
            catch (NoClassDefFoundError e) {
                // happens for classes that are not on classpath but in a signature
                // Safe to ignore.
            }
        }

        public String getPackageName() {
            return packageName;
        }

        public Iterable<ClassInfo> getClasses() {
            return rootClasses.values();
        }

        public Iterable<String> getImportedPackages() {
            return rootClasses.values().stream()
                    .flatMap(c -> TypeHintsUtils.getMethods(c).values().stream())
                    .flatMap(Collection::stream)
                    .flatMap(this::getTypes)
                    .flatMap(t -> TypeHintsUtils.getImportedTypes(t).stream())
                    .filter(t -> !TypeHintsUtils.PRIMITIVE_TYPES.contains(t))
                    .map(Class::getPackage)
                    .map(Package::getName)
                    //.map(Packages::pythonPackage)
                    .collect(Collectors.toSet());
        }

        private Stream<Class<?>> getTypes(Method method) {
            Collection<Class<?>> list = new ArrayList<>();
            list.add(method.getReturnType());
            list.addAll(Arrays.asList(method.getParameterTypes()));
            return list.stream();
        }
    }

    public static class ClassInfo {
        private final Class<?> classe;
        private final Map<Class<?>, ClassInfo> innerClasses = new LinkedHashMap<>();

        public ClassInfo(Class<?> classe) {
            this.classe = classe;

            for (Class<?> inner : classe.getDeclaredClasses()) {
                innerClasses.put(inner, new ClassInfo(inner));
            }
        }

        public Class<?> getRootClass() {
            return classe;
        }

        public Collection<ClassInfo> getInnerClasses() {
            return innerClasses.values();
        }

        @Override
        public String toString() {
            return "ClassInfo{" +
                    "classe=" + classe +
                    ", innerClasses=" + innerClasses +
                    '}';
        }
    }

}
