package org.matsim.contrib.pythonmatsim.typehints;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;

public class PyiUtils {
    private static final Logger log = Logger.getLogger(PyiUtils.class);

    public static Iterable<Packages.PackageInfo> scan() {
        final Configuration configuration =
                new ConfigurationBuilder()
                        .setScanners(new SubTypesScanner(false))
                        .addUrls(ClasspathHelper.forJavaClassPath())
                        .addClassLoaders(
                                ClasspathHelper.contextClassLoader(),
                                ClasspathHelper.staticClassLoader());
        final Reflections reflections = new Reflections(configuration);
        final Packages packages = new Packages();

        reflections.getSubTypesOf(Object.class).forEach(packages::addClass);

        return packages.getPackages();
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

        for (Packages.ClassInfo member : classTypeInfo.getInnerClasses()) {
            writePythonJpypeClass(writer, member);
        }
    }

    private static void writeClassHints(String prefix, BufferedWriter writer, String rootPackage, Packages.ClassInfo classTypeInfo) throws IOException {
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

        for (Packages.ClassInfo member : classTypeInfo.getInnerClasses()) {
            writeClassHints(prefix+'\t', writer, rootPackage, member);
        }

        for (Method method : TypeHintsUtils.getMethods(classTypeInfo)) {
            writeMethodHints(prefix + '\t', writer, rootPackage, method);
        }

        writer.newLine();
        writer.newLine();
    }

    private static void writeMethodHints(String prefix, BufferedWriter writer, String rootPackage, Method method) throws IOException {
        writer.write(prefix+"def "+ TypeHintsUtils.getJPypeName(method)+"(*args)");
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
    }

    private static File getPackageFile(File rootDir, Packages.PackageInfo packageInfo, String extension) {
        try {
            final String rootPath = rootDir.getCanonicalPath();
            final String moduleName = packageInfo.getPackageName();

            final int lastPoint = moduleName.lastIndexOf('.');
            final String packageDir = moduleName.substring(0, lastPoint + 1).replace('.', '/');
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
