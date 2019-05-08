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
        private final Collection<Class<?>> classes = new LinkedHashSet<>();

        public PackageInfo(String packageName) {
            this.packageName = packageName;
        }

        private void addClass(Class<?> classe) {
            classes.add(classe);
        }

        public String getPackageName() {
            return packageName;
        }

        public Iterable<Class<?>> getClasses() {
            return classes;
        }

        public Iterable<String> getImportedPackages() {
            return classes.stream()
                    .flatMap(c -> TypeHintsUtils.getMethods(c).stream())
                    .map(Method::getReturnType)
                    .flatMap(t -> TypeHintsUtils.getImportedTypes(t).stream())
                    .filter(t -> !TypeHintsUtils.PRIMITIVE_TYPES.contains(t))
                    .map(Class::getPackage)
                    .map(Package::getName)
                    //.map(Packages::pythonPackage)
                    .collect(Collectors.toSet());
        }
    }

}
