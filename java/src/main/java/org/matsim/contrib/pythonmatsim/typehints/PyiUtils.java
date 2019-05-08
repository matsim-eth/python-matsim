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
            log.info("generating python pyi files in "+rootPath);
            final File rootDir = new File(rootPath);

            for (Packages.PackageInfo info : scan()) {
                File file = getPackageFile(rootDir, info, ".pyi");

                log.info("generate "+file.getCanonicalPath());

                try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getCanonicalPath())) {
                    writeImports(writer, info.getImportedPackages());

                    for (Packages.ClassTypeInfo classTypeInfo : info.getClasses()) {
                        writeClassHints(writer, classTypeInfo);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeClassHints(BufferedWriter writer, Packages.ClassTypeInfo classTypeInfo) throws IOException {
        writer.write("class ");
        writer.write(classTypeInfo.getClassName());
        writer.write("():");
        writer.newLine();

        for (Packages.MethodTypeInfo method : classTypeInfo.getMethods()) {
            writeMethodHints(writer, method);
        }

        writer.newLine();
        writer.newLine();
    }

    private static void writeMethodHints(BufferedWriter writer, Packages.MethodTypeInfo method) throws IOException {
        writer.write("\t"+"def "+getJPypeName(method)+"(*args)");
        if (method.getReturnType() != null) {
            // no return type might be void or primitive types.
            // both cases are not of fantastic value in python, so ignore it for the moment.
            writer.write(" -> " + method.getReturnType().getPackage() + "." + method.getReturnType().getClassName());
        }
        writer.write(": ...");
        writer.newLine();
    }

    private static String getJPypeName(Packages.MethodTypeInfo method) {
        // TODO
        return method.getMethodName();
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
            // TODO: need __init__.py?
            file.getParentFile().mkdirs();
            return file;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
