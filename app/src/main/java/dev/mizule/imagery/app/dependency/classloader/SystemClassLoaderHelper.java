package dev.mizule.imagery.app.dependency.classloader;

import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * A reflection-based wrapper around SystemClassLoader for adding URLs to
 * the classpath.
 */
public class SystemClassLoaderHelper extends ClassLoaderHelper {

    /**
     * A reflected method in SystemClassLoader, when invoked adds a URL to the classpath.
     */
    private MethodHandle appendMethodHandle = null;
    private Instrumentation appendInstrumentation = null;

    /**
     * Creates a new SystemClassLoader helper.
     *
     * @param classLoader the class loader to manage
     */
    public SystemClassLoaderHelper(ClassLoader classLoader) {
        super(classLoader);

        try {
            Method appendMethod = classLoader.getClass().getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
            setMethodAccessible(appendMethod, classLoader.getClass().getName() + "#appendToClassPathForInstrumentation(String)",
                                methodHandle -> {
                appendMethodHandle = methodHandle;
                },
                                instrumentation -> {
                appendInstrumentation = instrumentation;
            }
            );
        } catch (Exception e) {
            throw new RuntimeException("Couldn't initialize SystemClassLoaderHelper", e);
        }
    }

    @Override
    public void addToClasspath(@NotNull URL url) {
        try {
            if (appendInstrumentation != null)
                appendInstrumentation.appendToSystemClassLoaderSearch(new JarFile(url.toURI().getPath()));
            else
                appendMethodHandle.invokeWithArguments(url.toURI().getPath());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}