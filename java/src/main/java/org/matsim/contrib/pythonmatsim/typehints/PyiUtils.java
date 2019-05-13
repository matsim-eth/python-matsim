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

    public static void generatePyiFiles(final String rootPath) {
        try {
            log.info("generating python .pyi files in "+rootPath);
            final File rootDir = new File(rootPath);

            for (Packages.PackageInfo info : scan()) {
                File file = getPackageFile(rootDir, info, ".pyi");

                log.info("generate "+file.getCanonicalPath());

                try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getCanonicalPath())) {
                    writeImports(writer, info.getImportedPackages());

                    for (Packages.ClassInfo classTypeInfo : info.getClasses()) {
                        log.debug("generate class "+classTypeInfo);
                        writeClassHints("", writer, classTypeInfo);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generatePythonFiles(final String rootPath) {
        try {
            log.info("generating python .py files in "+rootPath);
            final File rootDir = new File(rootPath);

            for (Packages.PackageInfo info : scan()) {
                File file = getPackageFile(rootDir, info, ".py");

                log.info("generate "+file.getCanonicalPath());

                try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getCanonicalPath())) {
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
        catch (IOException e) {
            throw new RuntimeException(e);
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

    private static void writeClassHints(String prefix, BufferedWriter writer, Packages.ClassInfo classTypeInfo) throws IOException {
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
            writeClassHints(prefix+'\t', writer, member);
        }

        for (Method method : TypeHintsUtils.getMethods(classTypeInfo)) {
            writeMethodHints(prefix + '\t', writer, method);
        }

        writer.newLine();
        writer.newLine();
    }

    private static void writeMethodHints(String prefix, BufferedWriter writer, Method method) throws IOException {
        writer.write(prefix+"def "+ TypeHintsUtils.getJPypeName(method)+"(*args)");
        if (method.getReturnType() != null) {
            // no return type might be void or primitive types.
            // both cases are not of fantastic value in python, so ignore it for the moment.
            writer.write(" -> " + TypeHintsUtils.pythonQualifiedClassName(method.getReturnType()));
        }
        writer.write(": ...");
        writer.newLine();
    }


    private static void writeImports(BufferedWriter writer, Iterable<String> importedPackages) throws IOException {
        for (String packageName : importedPackages) {
            writer.write("import ");
            writer.write(packageName);
            writer.newLine();
        }
        writer.newLine();
    }

    private static File getPackageFile(File rootDir, Packages.PackageInfo packageInfo, String extension) {
        try {
            final String path = rootDir.getCanonicalPath()+"/"+
                    packageInfo.getPackageName().replace('.', '/')+
                    extension;

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
        new File(parent, "__init__.py").createNewFile();
    }
}
