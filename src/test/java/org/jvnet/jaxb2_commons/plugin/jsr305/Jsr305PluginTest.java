package org.jvnet.jaxb2_commons.plugin.jsr305;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.tools.xjc.Driver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Jsr305PluginTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testPlugin() throws Exception {
        File xsd = new File(getClass().getResource("/sample.xsd").getFile());
        File outputDir = temporaryFolder.newFolder();

        compileSchemaSources(xsd, outputDir);
        compileJavaSources(outputDir);

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{outputDir.toURI().toURL()});
        Class<?> rootClass = Class.forName("generated.Root", true, classLoader);

        // references
        assertMethodIsAnnotated(rootClass, "getNullableReference", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableReference", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNonnullReference", Nonnull.class);
        assertParametersAreAnnotated(rootClass, "setNonnullReference", Nonnull.class);

        // primitive types, nullable
        assertMethodIsAnnotated(rootClass, "getNullableIntField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableIntField", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNullableLongField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableLongField", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNullableFloatField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableFloatField", Nullable.class);
        assertMethodIsAnnotated(rootClass, "isNullableBoolField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableBoolField", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNullableDoubleField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableDoubleField", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNullableByteField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableByteField", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNullableShortField", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableShortField", Nullable.class);

        // primitive types, nonnull
        assertMethodIsNotAnnotated(rootClass, "getNonnullIntField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullIntField");
        assertMethodIsNotAnnotated(rootClass, "getNonnullLongField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullLongField");
        assertMethodIsNotAnnotated(rootClass, "getNonnullFloatField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullFloatField");
        assertMethodIsNotAnnotated(rootClass, "isNonnullBoolField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullBoolField");
        assertMethodIsNotAnnotated(rootClass, "getNonnullDoubleField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullDoubleField");
        assertMethodIsNotAnnotated(rootClass, "getNonnullByteField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullByteField");
        assertMethodIsNotAnnotated(rootClass, "getNonnullShortField");
        assertParameterIsNotAnnotated(rootClass, "setNonnullShortField");

        // nullable types
        assertMethodIsAnnotated(rootClass, "getNullableString", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setNullableString", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getNullableList", Nonnull.class);

        // nonnull types
        assertMethodIsAnnotated(rootClass, "getNonnullString", Nonnull.class);
        assertParametersAreAnnotated(rootClass, "setNonnullString", Nonnull.class);
        assertMethodIsAnnotated(rootClass, "getNonnullList", Nonnull.class);

        // attributes
        assertMethodIsAnnotated(rootClass, "getOptionalAttribute", CheckForNull.class);
        assertParametersAreAnnotated(rootClass, "setOptionalAttribute", Nullable.class);
        assertMethodIsAnnotated(rootClass, "getRequiredAttribute", Nonnull.class);
        assertParametersAreAnnotated(rootClass, "setRequiredAttribute", Nonnull.class);
    }

    private static void compileJavaSources(File outputDir) throws IOException {
        File pckg = new File(outputDir, "generated");
        List<File> javaFiles = Arrays.asList(
                new File(pckg, "ObjectFactory.java"),
                new File(pckg, "Root.java")
        );

        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(errors, null, null);
        try {
            Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(javaFiles);
            compiler.getTask(null, fileManager, errors, null, null, compilationUnit).call();

            if (!errors.getDiagnostics().isEmpty()) {
                StringBuilder message = new StringBuilder();
                for (Diagnostic diagnostic : errors.getDiagnostics()) {
                    message.append("Error on line ");
                    message.append(diagnostic.getLineNumber());
                    message.append(" File: ");
                    message.append(diagnostic.getSource()).append("\n");
                }
                fail("Compilation failure in java: \n" + message);
            }
        } finally {
            fileManager.close();
        }
    }

    private static void compileSchemaSources(File xsdFile, File outputDir) throws Exception {
        Driver.run(
                new String[]{
                        "-d",
                        outputDir.getCanonicalPath(),
                        "-Xjsr305",
                        xsdFile.getCanonicalPath()
                },
                null,
                null);
    }

    private static void assertMethodIsAnnotated(Class<?> clazz, String methodName, Class<? extends Annotation> annotation) throws Exception {
        assertTrue(clazz.getMethod(methodName).isAnnotationPresent(annotation));
    }

    private static void assertMethodIsNotAnnotated(Class<?> clazz, String methodName) {
        Method method = findMethod(clazz, methodName);
        assertEquals(0, method.getAnnotations().length);
    }

    private static void assertParameterIsNotAnnotated(Class<?> clazz, String methodName) {
        Method method = findMethod(clazz, methodName);
        assertEquals(0, method.getParameterAnnotations()[0].length);
    }

    private static void assertParametersAreAnnotated(Class<?> cls, String methodName, Class<?> annotation) {
        Method method = findMethod(cls, methodName);
        for (Annotation[] existingAnnotations : method.getParameterAnnotations()) {
            assertAnnotationPresent(annotation, existingAnnotations);
        }
    }

    private static void assertAnnotationPresent(Class<?> annotation, Annotation[] existingAnnotations) {
        for (Annotation ann : existingAnnotations) {
            if (ann.annotationType().equals(annotation)) {
                return;
            }
        }
        fail("Cannot find annotation " + annotation.getName());
    }

    private static Method findMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Cannot find method " + methodName);
    }
}
