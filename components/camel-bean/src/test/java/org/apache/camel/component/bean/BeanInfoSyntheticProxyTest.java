/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.bean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import org.apache.camel.CamelContext;
import org.apache.camel.Handler;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that BeanInfo correctly handles synthetic proxy classes implementing interfaces,
 * such as OSGi Blueprint service reference proxies.
 *
 * When a Blueprint {@code <reference>} injects an OSGi service, the proxy class is synthetic.
 * BeanInfo must detect that the proxy's method overrides the interface method to avoid
 * registering duplicate operations for the same method name.
 */
public class BeanInfoSyntheticProxyTest {

    private CamelContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new DefaultCamelContext();
        context.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    /**
     * Simulates an OSGi Blueprint proxy: a synthetic class that implements a service interface.
     * Without the fix, BeanInfo registers both the interface's echo() and the proxy's echo()
     * as separate operations, resulting in duplicate entries for the same method name.
     */
    @Test
    void testSyntheticInterfaceProxyMethodResolution() throws Exception {
        Object proxy = buildSyntheticInterfaceProxy();

        assertTrue(proxy.getClass().isSynthetic(), "Proxy class should be synthetic");
        assertFalse(proxy.getClass().isHidden(), "Proxy class should not be hidden");

        BeanInfo beanInfo = new BeanInfo(context, proxy.getClass());

        // Use reflection to access the internal operations map and verify
        // that only ONE operation is registered for "echo", not two.
        java.lang.reflect.Field opsField = BeanInfo.class.getDeclaredField("operations");
        opsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<MethodInfo>> operations = (Map<String, List<MethodInfo>>) opsField.get(beanInfo);

        List<MethodInfo> echoOps = operations.get("echo");
        assertNotNull(echoOps, "Should have operations for 'echo'");
        assertEquals(1, echoOps.size(),
                "Should have exactly 1 operation for 'echo' (not 2). "
                + "Two entries means findMostSpecificOverride failed to detect the proxy's "
                + "method as an override of the interface method.");
    }

    /**
     * Verifies that lambda (hidden synthetic) classes still work correctly —
     * the @Handler annotation on the interface should be discoverable.
     */
    @Test
    void testLambdaFunctionalInterfacePreservesAnnotation() {
        MyHandler handler = () -> "result";

        assertTrue(handler.getClass().isSynthetic(), "Lambda class should be synthetic");
        assertTrue(handler.getClass().isHidden(), "Lambda class should be hidden");

        BeanInfo beanInfo = new BeanInfo(context, handler.getClass());
        assertTrue(beanInfo.hasAnyMethodHandlerAnnotation(),
                "Handler annotation on interface should be discoverable through lambda");
    }

    /**
     * Verifies that a synthetic subclass proxy (like CGLIB/ByteBuddy subclass proxies)
     * still preserves annotation discovery on the parent class.
     */
    @Test
    void testSyntheticSubclassProxyPreservesAnnotation() throws Exception {
        Object proxy = new ByteBuddy()
                .subclass(MyHandlerBean.class)
                .modifiers(SyntheticState.SYNTHETIC, Visibility.PUBLIC)
                .method(ElementMatchers.named("handle"))
                .intercept(InvocationHandlerAdapter.of((p, method, args) -> "proxied"))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();

        assertTrue(proxy.getClass().isSynthetic());

        BeanInfo beanInfo = new BeanInfo(context, proxy.getClass());
        assertTrue(beanInfo.hasAnyMethodHandlerAnnotation(),
                "Handler annotation on parent class should be discoverable through subclass proxy");
    }

    private Object buildSyntheticInterfaceProxy() throws Exception {
        return new ByteBuddy()
                .subclass(Object.class)
                .implement(MyService.class)
                .modifiers(SyntheticState.SYNTHETIC, Visibility.PUBLIC)
                .method(ElementMatchers.named("echo"))
                .intercept(InvocationHandlerAdapter.of((p, method, args) -> "Hello " + args[0]))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    // --- Test support types ---

    public interface MyService {
        String echo(String input);
    }

    @FunctionalInterface
    public interface MyHandler {
        @Handler
        String handle();
    }

    public static class MyHandlerBean {
        @Handler
        public String handle() {
            return "original";
        }
    }
}
