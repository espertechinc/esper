/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.regressionlib.suite.client.compile;

import com.espertech.esper.common.client.module.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientCompileModuleUses {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientCompileModuleUsesOrder());
        execs.add(new ClientCompileModuleUsesCircular());
        execs.add(new ClientCompileModuleUsesUnresolvedUses());
        return execs;
    }

    private static class ClientCompileModuleUsesOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            try {
                Module moduleA = null;
                Module moduleB = null;
                Module moduleC = null;
                Module moduleD = null;
                Module moduleE = null;
                ModuleOrder order = null;

                // Tree of 4 deep
                moduleA = getModule("A");
                moduleB = getModule("B", "A");
                moduleC = getModule("C", "A", "B", "D");
                moduleD = getModule("D", "A", "B");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleC, moduleD, moduleB, moduleA}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleA, moduleB, moduleD, moduleC}, order);

                // Zero items
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{}, order);

                // 1 item
                moduleA = getModule("A");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleA}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleA}, order);

                // 2 item
                moduleA = getModule("A", "B");
                moduleB = getModule("B");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleB, moduleA}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleA}, order);

                // 3 item
                moduleB = getModule("B");
                moduleC = getModule("C", "B");
                moduleD = getModule("D");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleB, moduleC, moduleD}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleC, moduleD}, order);
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleD, moduleC, moduleB}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleD, moduleC}, order);

                // 2 trees of 2 deep
                moduleA = getModule("A", "B");
                moduleB = getModule("B");
                moduleC = getModule("C", "D");
                moduleD = getModule("D");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleC, moduleB, moduleA, moduleD}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleD, moduleC, moduleA}, order);

                // Tree of 5 deep
                moduleA = getModule("A", "C");
                moduleB = getModule("B");
                moduleC = getModule("C", "B");
                moduleD = getModule("D", "C", "E");
                moduleE = getModule("E");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleA, moduleB, moduleC, moduleD, moduleE}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleC, moduleE, moduleA, moduleD}, order);
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleB, moduleE, moduleC, moduleA, moduleD}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleE, moduleC, moduleA, moduleD}, order);
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleA, moduleD, moduleE, moduleC, moduleB}), Collections.emptySet(), new ModuleOrderOptions());
                assertOrder(new Module[]{moduleB, moduleE, moduleC, moduleA, moduleD}, order);

                // Tree with null names
                moduleA = getModule(null, "C", "A", "B", "D");
                moduleB = getModule(null, "C");
                moduleC = getModule("A");
                moduleD = getModule("B", "A", "C");
                moduleE = getModule("C");
                ModuleOrderOptions options = new ModuleOrderOptions();
                options.setCheckUses(false);
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleA, moduleB, moduleC, moduleD, moduleE}), Collections.emptySet(), options);
                assertOrder(new Module[]{moduleC, moduleE, moduleD, moduleA, moduleB}, order);

                // Tree with duplicate names
                moduleA = getModule("A", "C");
                moduleB = getModule("B", "C");
                moduleC = getModule("A", "B");
                moduleD = getModule("D", "A");
                moduleE = getModule("C");
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleA, moduleB, moduleC, moduleD, moduleE}), Collections.emptySet(), options);
                assertOrder(new Module[]{moduleE, moduleB, moduleA, moduleC, moduleD}, order);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    private static class ClientCompileModuleUsesCircular implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // Circular 3
            Module moduleB = getModule("B", "C");
            Module moduleC = getModule("C", "D");
            Module moduleD = getModule("D", "B");

            try {
                ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleC, moduleD, moduleB}), Collections.emptySet(), new ModuleOrderOptions());
                fail();
            } catch (ModuleOrderException ex) {
                assertEquals("Circular dependency detected in module uses-relationships: module 'C' uses (depends on) module 'D' uses (depends on) module 'B'", ex.getMessage());
            }

            // Circular 1 - this is allowed
            moduleB = getModule("B", "B");
            ModuleOrder order = null;
            try {
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleC, moduleD, moduleB}), Collections.emptySet(), new ModuleOrderOptions());
            } catch (ModuleOrderException e) {
                throw new RuntimeException(e);
            }
            assertOrder(new Module[]{moduleB, moduleD, moduleC}, order);

            // Circular 2
            moduleB = getModule("B", "C");
            moduleC = getModule("C", "B");
            try {
                ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleC, moduleB}), Collections.emptySet(), new ModuleOrderOptions());
                fail();
            } catch (ModuleOrderException ex) {
                assertEquals("Circular dependency detected in module uses-relationships: module 'C' uses (depends on) module 'B'", ex.getMessage());
            }

            // turn off circular check
            ModuleOrderOptions options = new ModuleOrderOptions();
            options.setCheckCircularDependency(false);
            try {
                order = ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleC, moduleB}), Collections.emptySet(), options);
            } catch (ModuleOrderException e) {
                throw new RuntimeException(e);
            }
            assertOrder(new Module[]{moduleB, moduleC}, order);
        }
    }

    private static class ClientCompileModuleUsesUnresolvedUses implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // Single module
            Module moduleB = getModule("B", "C");
            try {
                ModuleOrderUtil.getModuleOrder(Arrays.asList(new Module[]{moduleB}), Collections.emptySet(), new ModuleOrderOptions());
                fail();
            } catch (ModuleOrderException ex) {
                assertEquals("Module-dependency not found as declared by module 'B' for uses-declaration 'C'", ex.getMessage());
            }

            // multiple module
            Module[] modules = new Module[]{getModule("B", "C"), getModule("C", "D"), getModule("D", "x")};
            try {
                ModuleOrderUtil.getModuleOrder(Arrays.asList(modules), Collections.emptySet(), new ModuleOrderOptions());
                fail();
            } catch (ModuleOrderException ex) {
                assertEquals("Module-dependency not found as declared by module 'D' for uses-declaration 'x'", ex.getMessage());
            }

            // turn off uses-checks
            ModuleOrderOptions options = new ModuleOrderOptions();
            options.setCheckUses(false);
            try {
                ModuleOrderUtil.getModuleOrder(Arrays.asList(modules), Collections.emptySet(), options);
            } catch (ModuleOrderException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void assertOrder(Module[] ordered, ModuleOrder order) {
        EPAssertionUtil.assertEqualsExactOrder(ordered, order.getOrdered().toArray());
    }

    private static Module getModule(String name, String... uses) {
        Set<String> usesSet = new HashSet<String>();
        usesSet.addAll(Arrays.asList(uses));
        return new Module(name, null, usesSet, Collections.EMPTY_SET, Collections.EMPTY_LIST, null);
    }
}

