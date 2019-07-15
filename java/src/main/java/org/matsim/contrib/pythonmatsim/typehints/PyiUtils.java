
/* *********************************************************************** *
 * project: python-matsim
 * PyiUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.contrib.pythonmatsim.typehints;

import com.google.common.reflect.ClassPath;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PyiUtils {
    private static final Logger log = Logger.getLogger(PyiUtils.class);

    public static Iterable<Packages.PackageInfo> scan() {
        final Collection<Class<?>> classes = new HashSet<>();
        try {
            for (ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    loader != null;
                    loader = loader.getParent()) {
                addClasses(loader, classes);
            }
            addBootstrapClasses(classes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Packages packages = new Packages();
        classes.forEach(packages::addClass);
        return packages.getPackages();
    }

    private static void addClasses(ClassLoader loader, Collection<Class<?>> classes) throws IOException {
         for (ClassPath.ClassInfo c : ClassPath.from(loader).getAllClasses()) {
            try {
                Class<?> loaded = c.load();
                classes.add(loaded);
            }
            catch (LinkageError e) {
                log.warn("could not load class "+c.getName());
            }
        }
    }

    private static void addBootstrapClasses(Collection<Class<?>> classes) throws IOException {
        // XXX See https://openjdk.java.net/jeps/220 for after Java 8
        // get URL of rt.jar, where all java.* classes are (they are not on the classpath and need to be handled ad-hoc)
        try {
            URL fileURL = ((JarURLConnection) ClassLoader.getSystemResource("java/lang/Class.class").openConnection())
                    .getJarFileURL();

            ClassLoader cl = URLClassLoader.newInstance(new URL[]{new URL("jar:" + fileURL.toString() + "!/")});
            addClasses(cl, classes);
        }
        catch (ClassCastException e) {
            FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));

            // with jrt, there is not classpath, so the Guava "getAllClasses" of class loader method does not work.
            // Need to iterate directly

            Files.walkFileTree(
                    fs.getPath("/modules/java.base/"),
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String fileName = file.getFileName().toString();

                            if (fileName.endsWith(".class") && !fileName.contains("$")) {
                                log.debug("found class "+fileName);

                                String className = fileName.substring(0, fileName.length() - 6).replace("/", ".");

                                try {
                                    classes.add(Object.class.getClassLoader().loadClass(className));
                                } catch (ClassNotFoundException ex) {
                                    log.warn("could not load class "+className);
                                }
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
        }
    }

    public static void generatePythonWrappers(final String rootPath, String rootPackage) {
        try {
            generatePyiFiles(rootPath, rootPackage);
            generatePythonFiles(rootPath, rootPackage);
            generateInitFiles(new File(rootPath));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void generatePyiFiles(final String rootPath, final String rootPackage) throws IOException {
        final String packagePath = rootPath +"/"+ rootPackage;
        log.info("generating python .pyi files in "+packagePath);
        final File rootDir = new File(packagePath);

        for (Packages.PackageInfo info : scan()) {
            File file = getPackageFile(rootDir, info, ".pyi");

            log.info("generate "+file.getCanonicalPath());

            try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getCanonicalPath())) {
                writeHeader(writer);

                writeImports(writer, rootPackage, info.getImportedPackages());

                writer.write("from typing import overload");
                writer.newLine();
                writer.newLine();

                for (Packages.ClassInfo classTypeInfo : info.getClasses()) {
                    log.debug("generate class "+classTypeInfo);
                    writeClassHints("", writer, rootPackage, classTypeInfo);
                }
            }
        }
    }

    private static void generatePythonFiles(final String rootPath, String rootPackage) throws IOException {
        final String packagePath = rootPath +"/"+ rootPackage;
        log.info("generating python .py files in "+packagePath);
        final File rootDir = new File(packagePath);

        for (Packages.PackageInfo info : scan()) {
            File file = getPackageFile(rootDir, info, ".py");

            log.info("generate "+file.getCanonicalPath());

            try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getCanonicalPath())) {
                writeHeader(writer);

                writer.write("import jpype");
                writer.newLine();
                writer.newLine();

                for (Packages.ClassInfo classTypeInfo : info.getClasses()) {
                    log.debug("generate class "+classTypeInfo);

                    writePythonJpypeClass(writer, classTypeInfo);
                }
            }
        }
    }

    private static void generateInitFiles(final File rootDir) throws IOException {
        log.info("generating python __init__.py files in "+rootDir.getPath());

        writeInitFile(rootDir);

        for (File directory : rootDir.listFiles(File::isDirectory)) {
           generateInitFiles(directory);
        }
    }

    private static void writeInitFile(File directory) throws IOException {
        // This writes the __init__.py files, importing all defined modules.
        // This seems to be the only way to emulate java-like package structure in python
        // This has the downside that when importing parent1.parent2...child,
        // all classes from parent packages are imported as well.
        try (BufferedWriter writer = IOUtils.getBufferedWriter(directory.getCanonicalPath()+"/__init__.py")) {
            writeHeader(writer);

            for (File pythonFile : directory.listFiles(f -> f.getName().matches("_.*\\.py") && !f.getName().equals("__init__.py"))) {
                final String pack = pythonFile.getName().substring(0, pythonFile.getName().length() - 3);
                writer.newLine();
                writer.write("from ."+pack+" import *");
            }
        }
    }

    private static void writePythonJpypeClass(BufferedWriter writer, Packages.ClassInfo classTypeInfo) throws IOException {
        final String pythonClassName = TypeHintsUtils.pythonClassName(classTypeInfo.getRootClass());

        if (pythonClassName.equals("Any")) {
            log.debug("ABORT class "+classTypeInfo);
            return;
        }

        final String canonicalName = classTypeInfo.getRootClass().getCanonicalName();

        if (canonicalName == null) {
            log.debug("ABORT class "+classTypeInfo);
            return;
        }

        writer.newLine();
        writer.newLine();
        writer.write(pythonClassName);
        writer.write(" = jpype.JClass(\'");
        writer.write(classTypeInfo.getRootClass().getName());
        writer.write("\')");
    }

    private static void writeClassHints(String prefix, BufferedWriter writer, String rootPackage, Packages.ClassInfo classTypeInfo) throws IOException {
        // TODO see if it makes sense to translate javadocs to python docstrings
        final Class<?> rootClass = classTypeInfo.getRootClass();
        String pythonName = TypeHintsUtils.pythonClassName(rootClass);

        // This indicates a non-public type (anonymous, local...)
        if (pythonName.equals("Any")) return;

        if (rootClass.isMemberClass()) {
            String parentName = TypeHintsUtils.pythonClassName(rootClass.getDeclaringClass());
            pythonName = pythonName.substring(parentName.length() +  1);
        }

        writer.write(prefix +"class "+pythonName+":");
        writer.newLine();

        writeConstructorsHints(prefix + '\t', writer, rootPackage, rootClass);

        // TODO add public static fields

        if (classTypeInfo.getRootClass().isEnum()) {
            writeEnumHints(prefix + '\t', writer, rootPackage, classTypeInfo);
        }

        for (Packages.ClassInfo member : classTypeInfo.getInnerClasses()) {
            writeClassHints(prefix+'\t', writer, rootPackage, member);
        }

        for (Map.Entry<String, Collection<Method>> method : TypeHintsUtils.getMethods(classTypeInfo).entrySet()) {
            writeMethodHints(prefix + '\t', writer, rootPackage, method.getKey(), method.getValue());
        }

        writer.newLine();
        writer.newLine();
    }

    private static void writeEnumHints(String prefix, BufferedWriter writer, String rootPackage, Packages.ClassInfo classTypeInfo) throws IOException {
        for (Object constant : classTypeInfo.getRootClass().getEnumConstants()) {
            // not sure whether this is correct
            writer.write(prefix);
            writer.write(((Enum) constant).name()+": ");
            writer.write(TypeHintsUtils.pythonClassName(classTypeInfo.getRootClass()));
            writer.write(" = ...");
            writer.newLine();
        }
    }

    private static void writeConstructorsHints(String prefix, BufferedWriter writer, String rootPackage, Class<?> classe) throws IOException {
        Constructor<?>[] constructors = null;

        try {
            constructors = classe.getConstructors();
        }
        catch (NoClassDefFoundError e){
            return;
        }

        boolean overload = constructors.length > 1;

        for (Constructor<?> constructor : constructors) {
            if (overload) {
                writer.write(prefix);
                writer.write("@overload");
                writer.newLine();
            }

            writer.write(prefix + "def __init__(self, ");

            for (Parameter parameter : constructor.getParameters()) {
                // TODO find how to get name reliably. Seems not to be possible if dependencies were not compiled with -parameters
                // option, although IDEs do manage to get parameter names...
                final String parameterName = parameter.isVarArgs() ? "*"+parameter.getName() : parameter.getName();
                // varargs need to be handled explicitly, as the functionality exists in both languages but in a different way
                final Class<?> parameterType = parameter.isVarArgs() ? parameter.getType().getComponentType() : parameter.getType();
                final String pythonQualifiedClassName = TypeHintsUtils.pythonQualifiedClassName(rootPackage, parameterType);

                writer.write(
                        parameterName+": "+
                                pythonQualifiedClassName+
                                // python allows trailing comas, so no need to handle last parameter specially
                                ", ");
            }

            writer.write("): ...");
            writer.newLine();
        }
    }

    private static void writeMethodHints(String prefix, BufferedWriter writer, String rootPackage, String name, Collection<Method> methods) throws IOException {
        String methodName = TypeHintsUtils.getJPypeName(name);

        boolean overload = methods.size() > 1;

        for (Method method : methods) {
            if (overload) {
                writer.write(prefix);
                writer.write("@overload");
                writer.newLine();
            }
            writeMethodHints(prefix, writer, rootPackage, methodName, method);
        }
    }

    private static void writeMethodHints(String prefix,
                                         BufferedWriter writer,
                                         String rootPackage,
                                         String name,
                                         Method method) throws IOException {
        final boolean isStatic = Modifier.isStatic(method.getModifiers());

        if (isStatic) {
            writer.write(prefix+"@classmethod");
            writer.newLine();
        }

        writer.write(prefix + "def " + name + "(");
        if (!isStatic) {
            writer.write("self, ");
        }

        for (Parameter parameter : method.getParameters()) {
            // TODO find how to get name reliably. Seems not to be possible if dependencies were not compiled with -parameters
            // option, although IDEs do manage to get parameter names...
            final String parameterName = parameter.isVarArgs() ? "*"+parameter.getName() : parameter.getName();
            // varargs need to be handled explicitly, as the functionality exists in both languages but in a different way
            final Class<?> parameterType = parameter.isVarArgs() ? parameter.getType().getComponentType() : parameter.getType();
            final String pythonQualifiedClassName = TypeHintsUtils.pythonQualifiedClassName(rootPackage, parameterType);


            writer.write(
                    parameterName+": "+
                            pythonQualifiedClassName+
                            // python allows trailing comas, so no need to handle last parameter specially
                            ", ");
        }

        writer.write(")");

        if (method.getReturnType() != null) {
            // no return type might be void or primitive types.
            // both cases are not of fantastic value in python, so ignore it for the moment.
            writer.write(" -> " + TypeHintsUtils.pythonQualifiedClassName(rootPackage, method.getReturnType()));
        }

        writer.write(": ...");
        writer.newLine();
    }

    private static void writeImports(BufferedWriter writer, String rootPackage, Iterable<String> importedPackages) throws IOException {
        for (String packageName : importedPackages) {
            writer.write("import "+rootPackage+"."+packageName);
            writer.newLine();
        }

        writer.newLine();
        writer.write("from jpype.types import *");

        writer.newLine();
        writer.write("from typing import Union");
        writer.newLine();

        writer.newLine();
    }

    private static File getPackageFile(File rootDir, Packages.PackageInfo packageInfo, String extension) {
        try {
            final String rootPath = rootDir.getCanonicalPath();
            final String moduleName = packageInfo.getPackageName();

            final int lastPoint = moduleName.lastIndexOf('.');
            final String packageDir = moduleName.replace('.', '/') +"/";
            final String moduleFileName = '_' + moduleName.substring(lastPoint + 1) + extension;

            final String path = rootPath + '/' + packageDir + moduleFileName;

            final File file = new File(path);
            createParentPackageDirs(file, rootDir);
            return file;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createParentPackageDirs(File file, File rootDir) throws IOException {
        final File parent = file.getParentFile();

        if (!file.equals(rootDir)) {
            createParentPackageDirs(parent, rootDir);
        }

        parent.mkdirs();
        //new File(parent, "__init__.py").createNewFile();
    }

    private static void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("################################################################################");
        writer.newLine();
        writer.write("#          This file was automatically generated. Please do not edit.          #");
        writer.newLine();
        writer.write("################################################################################");
        writer.newLine();
        writer.newLine();
    }
}
