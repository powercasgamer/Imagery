package dev.mizule.imagery.app.dependency.classloader;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Consumer;

import static dev.mizule.imagery.app.util.Util.replaceWithDots;
import static java.util.Objects.requireNonNull;

/**
 * An abstract class for reflection-based wrappers around class loaders for adding
 * URLs to the classpath.
 */
public abstract class ClassLoaderHelper {
    /**
     * System property to set to "true" to disable the Unsafe method for initializing the class loader helper.
     */
    public static final String SYSTEM_PROPERTY_DISABLE_UNSAFE = "libby.classloaders.unsafeDisabled";

    /**
     * System property to set to "true" to disable the Java agent method for initializing the class loader helper.
     */
    public static final String SYSTEM_PROPERTY_DISABLE_JAVA_AGENT = "libby.classloaders.javaAgentDisabled";

    /**
     * Environment variable to set to "true" to disable the Unsafe method for initializing the class loader helper.
     */
    public static final String ENV_VAR_DISABLE_UNSAFE = "LIBBY_CLASSLOADERS_UNSAFE_DISABLED";

    /**
     * Environment variable to set to "true" to disable the Java agent method for initializing the class loader helper.
     */
    public static final String ENV_VAR_DISABLE_JAVA_AGENT = "LIBBY_CLASSLOADERS_JAVA_AGENT_DISABLED";

    /**
     * net.bytebuddy.agent.ByteBuddyAgent class name for reflections
     */
    private static final String BYTE_BUDDY_AGENT_CLASS = replaceWithDots("net{}bytebuddy{}agent{}ByteBuddyAgent");

    /**
     * java.lang.Module methods since we build against Java 8
     */
    private static final Method getModuleMethod, addOpensMethod, getNameMethod;

    static {
        Method getModule = null, addOpens = null, getName = null;
        try {
            Class<?> moduleClass = Class.forName("java.lang.Module");
            getModule = Class.class.getMethod("getModule");
            addOpens = moduleClass.getMethod("addOpens", String.class, moduleClass);
            getName = moduleClass.getMethod("getName");
        } catch (Exception ignored) {
        } finally {
            getModuleMethod = getModule;
            addOpensMethod = addOpens;
            getNameMethod = getName;
        }
    }

    /**
     * Unsafe class instance. Used in {@link #getPrivilegedMethodHandle(Method)}.
     */
    private static final Unsafe theUnsafe;

    static {
        Unsafe unsafe = null; // Used to make theUnsafe field final

        // getDeclaredField("theUnsafe") is not used to avoid breakage on JVMs with changed field name
        for (Field f : Unsafe.class.getDeclaredFields()) {
            try {
                if (f.getType() == Unsafe.class && Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    unsafe = (Unsafe) f.get(null);
                }
            } catch (Exception ignored) {
            }
        }
        theUnsafe = unsafe;
    }

    /**
     * Cached {@link Instrumentation} instance. Used by {@link #initInstrumentation()}.
     */
    private static volatile Instrumentation cachedInstrumentation;

    /**
     * The class loader being managed by this helper.
     */
    protected final ClassLoader classLoader;

    /**
     * Creates a new class loader helper.
     *
     * @param classLoader the class loader to manage
     */
    public ClassLoaderHelper(ClassLoader classLoader) {
        this.classLoader = requireNonNull(classLoader, "classLoader");
    }

    /**
     * Adds a URL to the class loader's classpath.
     *
     * @param url the URL to add
     */
    public abstract void addToClasspath(@NotNull URL url);

    /**
     * Adds a path to the class loader's classpath.
     *
     * @param path the path to add
     */
    public void addToClasspath(@NotNull Path path) {
        try {
            addToClasspath(requireNonNull(path, "path").toUri().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Sets the method accessible using reflections, the Unsafe class or a java agent loaded at runtime.
     * <p>The provided consumers are mutually exclusive, i.e. only 1 of the provided consumers will run (if run).
     *
     * @param method the method to set accessible
     * @param methodSignature the signature of the method, used in error messages
     * @param methodHandleConsumer a {@link Consumer} which might get called with a {@link MethodHandle} to the method
     * @param instrumentationConsumer a {@link Consumer} which might get called with an {@link Instrumentation} instance
     */
    protected void setMethodAccessible(Method method, String methodSignature, Consumer<MethodHandle> methodHandleConsumer, Consumer<Instrumentation> instrumentationConsumer) {
        if (Modifier.isPublic(method.getModifiers())) {
            return; // Already public
        }

        try {
            // Try to open the method's module to avoid warnings
            openModule(method.getDeclaringClass());
        } catch (Exception ignored) {}

        try {
            // In Java 8 calling setAccessible(true) is enough
            method.setAccessible(true);
            return;
        } catch (Exception e) {
            handleInaccessibleObjectException(e, methodSignature);
        }

        Exception unsafeException = null; // Used below in error messages handling
        unsafe: if (theUnsafe != null && canUseUnsafe()) {
            MethodHandle methodHandle;
            try {
                methodHandle = getPrivilegedMethodHandle(method).bindTo(classLoader);
            } catch (Exception e) {
                unsafeException = e; // Save exception for later
                break unsafe; // An exception occurred, don't continue
            }
            methodHandleConsumer.accept(methodHandle);
            return;
        }

        Exception javaAgentException = null; // Used below in error messages handling
        javaAgent: if (canUseJavaAgent()) {
            Instrumentation instrumentation;
            try {
                instrumentation = initInstrumentation();
            } catch (Exception e) {
                javaAgentException = e; // Save exception for later
                break javaAgent; // An exception occurred, don't continue
            }
            try {
                // instrumentationConsumer might try to set the method accessible
                instrumentationConsumer.accept(instrumentation);
                return;
            } catch (Exception e) {
                handleInaccessibleObjectException(e, methodSignature);
            }
        }

        // Couldn't init, print errors and throw a RuntimeException
        Logger logger = LoggerFactory.getLogger("LIBBY STYFF");
        if (unsafeException != null) {
            logger.error("Cannot set accessible " + methodSignature + " using unsafe", unsafeException);
        }
        if (javaAgentException != null) {
            logger.error("Cannot set accessible " + methodSignature + " using java agent", javaAgentException);
        }

        String packageName = method.getDeclaringClass().getPackage().getName();
        String moduleName = null;
        try {
            moduleName = (String) getNameMethod.invoke(getModuleMethod.invoke(method.getDeclaringClass()));
        } catch (Exception ignored) {
            // Don't throw an exception in case module reflections failed
        }
        if (moduleName != null) {
            logger.error("Cannot set accessible " + methodSignature + ", if you are using Java 9+ try to add the following option to your java command: --add-opens " + moduleName + "/" + packageName + "=ALL-UNNAMED");
        } else {
            // In case the try-and-catch above failed, should never happen
            logger.error("Cannot set accessible " + methodSignature);
        }

        throw new RuntimeException("Cannot set accessible " + methodSignature);
    }

    private void handleInaccessibleObjectException(Exception exception, String methodSignature) {
        // InaccessibleObjectException has been added in Java 9
        if (!exception.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
            throw new RuntimeException("Cannot set accessible "+ methodSignature, exception);
        }
    }

    /**
     * Opens the module of the provided class using reflections.
     *
     * @param toOpen The class
     * @throws Exception if an error occurs
     */
    protected static void openModule(Class<?> toOpen) throws Exception {
        //
        // Snippet originally from lucko (Luck) <luck@lucko.me>, who used it in his own class loader
        //
        // This is a workaround used to maintain Java 9+ support with reflections
        // Thanks to this you will be able to run this class loader with Java 8+

        // This is effectively calling:
        //
        // toOpen.getModule().addOpens(
        //     toOpen.getPackage().getName(),
        //     ClassLoaderHelper.class.getModule()
        // );
        //
        // We use reflection since we build against Java 8.

        Object urlClassLoaderModule = getModuleMethod.invoke(toOpen);
        Object thisModule = getModuleMethod.invoke(ClassLoaderHelper.class);

        addOpensMethod.invoke(urlClassLoaderModule, toOpen.getPackage().getName(), thisModule);
    }

    /**
     * Try to get a MethodHandle for the given method.
     *
     * @param method the method to get the handle for
     * @return the method handle
     */
    protected MethodHandle getPrivilegedMethodHandle(Method method) {
        // The Unsafe class is used to get a privileged MethodHandles.Lookup instance.

        // Looking for MethodHandles.Lookup#IMPL_LOOKUP private static field
        // getDeclaredField("IMPL_LOOKUP") is not used to avoid breakage on JVMs with changed field name
        for (Field trustedLookup : MethodHandles.Lookup.class.getDeclaredFields()) {
            if (trustedLookup.getType() != MethodHandles.Lookup.class || !Modifier.isStatic(trustedLookup.getModifiers()) || trustedLookup.isSynthetic())
                continue;

            try {
                MethodHandles.Lookup lookup = (MethodHandles.Lookup) theUnsafe.getObject(theUnsafe.staticFieldBase(trustedLookup), theUnsafe.staticFieldOffset(trustedLookup));
                return lookup.unreflect(method);
            } catch (Exception ignored) {
                // Unreflect went wrong, trying the next field
            }
        }

        // Every field has been tried
        throw new RuntimeException("Cannot get privileged method handle.");
    }

    /**
     * Load ByteBuddy agent and return an {@link Instrumentation} instance.
     *
     * @return an {@link Instrumentation} instance
     * @throws Exception if an error occurs
     */
    protected Instrumentation initInstrumentation() throws Exception {
        Instrumentation instr = cachedInstrumentation;
        if (instr != null) {
            return instr;
        }

        // To open the class-loader's module we need permissions.
        // Try to add a java agent at runtime (specifically, ByteBuddy's agent) and use it to open the module,
        // since java agents should have such permission.

        // Download ByteBuddy's agent and load it through an IsolatedClassLoader
        IsolatedClassLoader isolatedClassLoader = new IsolatedClassLoader();
        try {
            if (true) throw new UnsupportedOperationException("cry");
            isolatedClassLoader.addPath(null); // TODO: not null lol

            Class<?> byteBuddyAgent = isolatedClassLoader.loadClass(BYTE_BUDDY_AGENT_CLASS);

            // This is effectively calling:
            //
            // Instrumentation instrumentation = ByteBuddyAgent.install();
            //
            // For more information see https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/Instrumentation.html

            Instrumentation instrumentation = (Instrumentation) byteBuddyAgent.getMethod("install").invoke(null);
            cachedInstrumentation = instrumentation;
            return instrumentation;
        } finally {
            try {
                isolatedClassLoader.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Checks if the Unsafe method can be used to initialize the class loader helper.
     *
     * @return {@code true} if either the system property {@link #SYSTEM_PROPERTY_DISABLE_UNSAFE} or the environment
     *         variable {@link #ENV_VAR_DISABLE_UNSAFE} are set to {@code "true"}, {@code false} otherwise.
     * @see #setMethodAccessible(Method, String, Consumer, Consumer)
     */
    protected boolean canUseUnsafe() {
        return !Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_DISABLE_UNSAFE)) && !Boolean.parseBoolean(System.getenv(ENV_VAR_DISABLE_UNSAFE));
    }

    /**
     * Checks if the Java agent method can be used to initialize the class loader helper.
     *
     * @return {@code true} if either the system property {@link #SYSTEM_PROPERTY_DISABLE_JAVA_AGENT} or the environment
     *         variable {@link #ENV_VAR_DISABLE_JAVA_AGENT} are set to {@code "true"}, {@code false} otherwise.
     * @see #setMethodAccessible(Method, String, Consumer, Consumer)
     */
    protected boolean canUseJavaAgent() {
        return !Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_DISABLE_JAVA_AGENT)) && !Boolean.parseBoolean(System.getenv(ENV_VAR_DISABLE_JAVA_AGENT));
    }
}