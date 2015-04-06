/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.deploy.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestDeployOrder extends TestCase
{
    private EPServiceProvider epService;
    private EPDeploymentAdmin deploymentAdmin;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        deploymentAdmin = epService.getEPAdministrator().getDeploymentAdmin();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testOrder() throws Exception {

        Module moduleA = null;
        Module moduleB = null;
        Module moduleC = null;
        Module moduleD = null;
        Module moduleE = null;
        DeploymentOrder order = null;

        // Tree of 4 deep
        moduleA = getModule("A");
        moduleB = getModule("B", "A");
        moduleC = getModule("C", "A", "B", "D");
        moduleD = getModule("D", "A", "B");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleC, moduleD, moduleB, moduleA}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleA, moduleB, moduleD, moduleC}, order);

        // Zero items
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {}), new DeploymentOrderOptions());
        assertOrder(new Module[] {}, order);

        // 1 item
        moduleA = getModule("A");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleA}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleA}, order);

        // 2 item
        moduleA = getModule("A", "B");
        moduleB = getModule("B");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleB, moduleA}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleA}, order);

        // 3 item
        moduleB = getModule("B");
        moduleC = getModule("C", "B");
        moduleD = getModule("D");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleB, moduleC, moduleD}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleC, moduleD}, order);
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleD, moduleC, moduleB}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleD, moduleC}, order);

        // 2 trees of 2 deep
        moduleA = getModule("A", "B");
        moduleB = getModule("B");
        moduleC = getModule("C", "D");
        moduleD = getModule("D");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleC, moduleB, moduleA, moduleD}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleD, moduleC, moduleA}, order);

        // Tree of 5 deep
        moduleA = getModule("A", "C");
        moduleB = getModule("B");
        moduleC = getModule("C", "B");
        moduleD = getModule("D", "C", "E");
        moduleE = getModule("E");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleA, moduleB, moduleC, moduleD, moduleE}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleC, moduleE, moduleA, moduleD}, order);
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleB, moduleE, moduleC, moduleA, moduleD}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleE, moduleC, moduleA, moduleD}, order);
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleA, moduleD, moduleE, moduleC, moduleB}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleE, moduleC, moduleA, moduleD}, order);

        // Tree with null names
        moduleA = getModule(null, "C", "A", "B", "D");
        moduleB = getModule(null, "C");
        moduleC = getModule("A");
        moduleD = getModule("B", "A", "C");
        moduleE = getModule("C");
        DeploymentOrderOptions options = new DeploymentOrderOptions();
        options.setCheckUses(false);
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleA, moduleB, moduleC, moduleD, moduleE}), options);
        assertOrder(new Module[] {moduleC, moduleE, moduleD, moduleA, moduleB}, order);
        assertFalse(deploymentAdmin.isDeployed("C"));

        // Tree with duplicate names
        moduleA = getModule("A", "C");
        moduleB = getModule("B", "C");
        moduleC = getModule("A", "B");
        moduleD = getModule("D", "A");
        moduleE = getModule("C");
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleA, moduleB, moduleC, moduleD, moduleE}), options);
        assertOrder(new Module[] {moduleE, moduleB, moduleA, moduleC, moduleD}, order);
    }

    public void testCircular() throws Exception {

        // Circular 3
        Module moduleB = getModule("B", "C");
        Module moduleC = getModule("C", "D");
        Module moduleD = getModule("D", "B");
        
        try {
            deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleC, moduleD, moduleB}), new DeploymentOrderOptions());
            fail();
        }
        catch (DeploymentOrderException ex) {
            assertEquals("Circular dependency detected in module uses-relationships: module 'C' uses (depends on) module 'D' uses (depends on) module 'B'", ex.getMessage());
        }

        // Circular 1 - this is allowed
        moduleB = getModule("B", "B");
        DeploymentOrder order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleC, moduleD, moduleB}), new DeploymentOrderOptions());
        assertOrder(new Module[] {moduleB, moduleD, moduleC, }, order);

        // Circular 2
        moduleB = getModule("B", "C");
        moduleC = getModule("C", "B");
        try {
            deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleC, moduleB}), new DeploymentOrderOptions());
            fail();
        }
        catch (DeploymentOrderException ex) {
            assertEquals("Circular dependency detected in module uses-relationships: module 'C' uses (depends on) module 'B'", ex.getMessage());
        }

        // turn off circular check
        DeploymentOrderOptions options = new DeploymentOrderOptions();
        options.setCheckCircularDependency(false);
        order = deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleC, moduleB}), options);
        assertOrder(new Module[] {moduleB, moduleC}, order);
    }

    public void testUnresolvedUses() throws Exception {

        // Single module
        Module moduleB = getModule("B", "C");
        try {
            deploymentAdmin.getDeploymentOrder(Arrays.asList(new Module[] {moduleB}), new DeploymentOrderOptions());
            fail();
        }
        catch (DeploymentOrderException ex) {
            assertEquals("Module-dependency not found as declared by module 'B' for uses-declaration 'C'", ex.getMessage());
        }

        // multiple module
        Module[] modules = new Module[] {getModule("B", "C"), getModule("C", "D"), getModule("D", "x")};
        try {
            deploymentAdmin.getDeploymentOrder(Arrays.asList(modules), new DeploymentOrderOptions());
            fail();
        }
        catch (DeploymentOrderException ex) {
            assertEquals("Module-dependency not found as declared by module 'D' for uses-declaration 'x'", ex.getMessage());
        }

        // turn off uses-checks
        DeploymentOrderOptions options = new DeploymentOrderOptions();
        options.setCheckUses(false);
        deploymentAdmin.getDeploymentOrder(Arrays.asList(modules), options);
    }

    private void assertOrder(Module[] ordered, DeploymentOrder order)
    {
        EPAssertionUtil.assertEqualsExactOrder(ordered, order.getOrdered().toArray());
    }

    private Module getModule(String name, String... uses) {
        Set<String> usesSet = new HashSet<String>();
        usesSet.addAll(Arrays.asList(uses));
        return new Module(name, null, usesSet, Collections.EMPTY_SET, Collections.EMPTY_LIST,  null);
    }
}
