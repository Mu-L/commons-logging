/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.logging.security;

import static org.junit.Assert.assertNotEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.PathableClassLoader;
import org.apache.commons.logging.PathableTestSuite;

/**
 * Tests for logging with a security policy that forbids JCL access to anything.
 * <p>
 * This test cannot run on Java 22: {@code java.lang.UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release}.
 * </p>
 * <p>
 * Performing tests with security permissions disabled is tricky, as building error
 * messages on failure requires certain security permissions. If the security manager
 * blocks these, then the test can fail without the error messages being output.
 * </p>
 * <p>
 * This class has only one unit test, as we are (in part) checking behavior in
 * the static block of the LogFactory class. As that class cannot be unloaded after
 * being loaded into a class loader, the only workaround is to use the
 * PathableClassLoader approach to ensure each test is run in its own
 * class loader, and use a separate test class for each test.
 * </p>
 */
public class SecurityForbiddenTestCase extends TestCase {

    /**
     * Dummy special Hashtable, so we can tell JCL to use this instead of the standard one.
     */
    public static class CustomHashtable extends Hashtable<Object, Object> {

        /**
         * Generated serial version ID.
         */
        private static final long serialVersionUID = 7224652794746236024L;
    }

    /**
     * Return the tests included in this test suite.
     */
    public static Test suite() throws Exception {
        final PathableClassLoader parent = new PathableClassLoader(null);
        parent.useExplicitLoader("junit.", Test.class.getClassLoader());
        parent.useExplicitLoader("org.junit.", Test.class.getClassLoader());
        parent.addLogicalLib("commons-logging");
        parent.addLogicalLib("testclasses");
        parent.addLogicalLib("commons-lang3");

        final Class<?> testClass = parent.loadClass("org.apache.commons.logging.security.SecurityForbiddenTestCase");
        return new PathableTestSuite(testClass, parent);
    }

    private SecurityManager oldSecMgr;

    private ClassLoader otherClassLoader;

    /**
     * Loads a class with the given class loader.
     */
    private Object loadClass(final String name, final ClassLoader classLoader) {
        try {
            final Class<?> clazz = classLoader.loadClass(name);
            return clazz.getConstructor().newInstance();
        } catch (final Exception e) {
            final Throwable wrapped = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            wrapped.printStackTrace(pw);
            fail("Unexpected exception:" + wrapped.getMessage() + ":" + sw);
        }
        return null;
    }

    @Override
    public void setUp() {
        // save security manager so it can be restored in tearDown
        oldSecMgr = System.getSecurityManager();

        final PathableClassLoader classLoader = new PathableClassLoader(null);
        classLoader.addLogicalLib("commons-logging");
        classLoader.addLogicalLib("testclasses");

        otherClassLoader = classLoader;
    }

    @Override
    public void tearDown() {
        // Ignore on Java 21 and up
        // TODO Port tests to JUnit 5
        if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_21)) {
            return;
        }
        // Restore, so other tests don't get stuffed up if a test
        // sets a custom security manager.
        // Java 22: java.lang.UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release
        System.setSecurityManager(oldSecMgr);
    }

    /**
     * Tests what happens when JCL is run with absolutely no security
     * privileges at all, including reading system properties. Everything
     * should fall back to the built-in defaults.
     */
    public void testAllForbidden() {
        // Ignore on Java 21 and up
        // TODO Port tests to JUnit 5
        if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_21)) {
            return;
        }
        System.setProperty(
                LogFactory.HASHTABLE_IMPLEMENTATION_PROPERTY,
                CustomHashtable.class.getName());
        final MockSecurityManager mySecurityManager = new MockSecurityManager();
        // Java 22: java.lang.UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release
        System.setSecurityManager(mySecurityManager);

        try {
            // Use reflection so that we can control exactly when the static
            // initializer for the LogFactory class is executed.
            final Class<?> c = this.getClass().getClassLoader().loadClass(
                    "org.apache.commons.logging.LogFactory");
            final Method m = c.getMethod("getLog", Class.class);
            final Log log = (Log) m.invoke(null, this.getClass());
            log.info("testing");

            // check that the default map implementation was loaded, as JCL was
            // forbidden from reading the HASHTABLE_IMPLEMENTATION_PROPERTY property.
            //
            // The default is either the java Hashtable class (java < 1.2) or the
            // JCL WeakHashtable (java >= 1.3).
            // Java 22: java.lang.UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release
            System.setSecurityManager(oldSecMgr);
            final Field factoryField = c.getDeclaredField("factories");
            factoryField.setAccessible(true);
            final Object factoryTable = factoryField.get(null);
            assertNotNull(factoryTable);
            final String ftClassName = factoryTable.getClass().getName();
            assertNotEquals("Custom Hashtable unexpectedly used",
                    CustomHashtable.class.getName(), ftClassName);

            assertEquals(0, mySecurityManager.getUntrustedCodeCount());
        } catch (final Throwable t) {
            // Restore original security manager so output can be generated; the
            // PrintWriter constructor tries to read the line.separator
            // system property.
            // Java 22: java.lang.UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release
            System.setSecurityManager(oldSecMgr);
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            fail("Unexpected exception:" + t.getMessage() + ":" + sw.toString());
        }
    }

    /**
     * Tests what happens when JCL is run with absolutely no security
     * privileges at all and a class loaded with a different class loader
     * than the context class loader of the current thread tries to log something.
     */
    public void testContextClassLoader() {
        // Ignore on Java 21 and up
        // TODO Port tests to JUnit 5
        if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_21)) {
            return;
        }
        System.setProperty(
                LogFactory.HASHTABLE_IMPLEMENTATION_PROPERTY,
                CustomHashtable.class.getName());
        final MockSecurityManager mySecurityManager = new MockSecurityManager();
        // Java 22: java.lang.UnsupportedOperationException: The Security Manager is deprecated and will be removed in a future release
        System.setSecurityManager(mySecurityManager);

        try {
            // load a dummy class with another class loader
            // to force a SecurityException when the LogFactory calls
            // Thread.getCurrentThread().getContextClassLoader()
            loadClass("org.apache.commons.logging.security.DummyClass", otherClassLoader);

            System.setSecurityManager(oldSecMgr);
            assertEquals(0, mySecurityManager.getUntrustedCodeCount());
        } catch (final Throwable t) {
            // Restore original security manager so output can be generated; the
            // PrintWriter constructor tries to read the line.separator
            // system property.
            System.setSecurityManager(oldSecMgr);
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            fail("Unexpected exception:" + t.getMessage() + ":" + sw.toString());
        }
    }
}
