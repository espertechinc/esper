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
package com.espertech.esper.client;

import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.util.EPServiceProviderName;
import junit.framework.TestCase;

public class TestEPServiceProviderManager extends TestCase {
    public void testGetInstance() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setEnableExpression(false);

        EPServiceProvider runtimeDef1 = EPServiceProviderManager.getDefaultProvider(configuration);
        EPServiceProvider runtimeA1 = EPServiceProviderManager.getProvider("A", configuration);
        EPServiceProvider runtimeB = EPServiceProviderManager.getProvider("B", configuration);
        EPServiceProvider runtimeA2 = EPServiceProviderManager.getProvider("A", configuration);
        EPServiceProvider runtimeDef2 = EPServiceProviderManager.getDefaultProvider(configuration);
        EPServiceProvider runtimeA3 = EPServiceProviderManager.getProvider("A", configuration);

        assertNotNull(runtimeDef1);
        assertNotNull(runtimeA1);
        assertNotNull(runtimeB);
        assertTrue(runtimeDef1 == runtimeDef2);
        assertTrue(runtimeA1 == runtimeA2);
        assertTrue(runtimeA1 == runtimeA3);
        assertFalse(runtimeA1 == runtimeDef1);
        assertFalse(runtimeA1 == runtimeB);

        assertEquals("A", runtimeA1.getURI());
        assertEquals("A", runtimeA2.getURI());
        assertEquals("B", runtimeB.getURI());
        assertEquals(EPServiceProviderName.DEFAULT_ENGINE_URI, runtimeDef1.getURI());
        assertEquals(EPServiceProviderName.DEFAULT_ENGINE_URI, runtimeDef2.getURI());

        runtimeDef1.destroy();
        runtimeA1.destroy();
        runtimeB.destroy();
        runtimeA2.destroy();
        runtimeDef2.destroy();
        runtimeA3.destroy();
    }

    public void testInvalid() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.getEngineDefaults().getByteCodeGeneration().setEnableExpression(false);
        configuration.addEventType("x", "xxx.noclass");

        try {
            EPServiceProviderManager.getProvider("someURI", configuration);
            fail();
        } catch (ConfigurationException ex) {
            // Expected
        }
    }

    public void testDefaultNaming() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getByteCodeGeneration().setEnableExpression(false);

        assertEquals("default", EPServiceProviderName.DEFAULT_ENGINE_URI_QUALIFIER);
        EPServiceProvider epNoArg = EPServiceProviderManager.getDefaultProvider(configuration);
        EPServiceProvider epDefault = EPServiceProviderManager.getProvider("default", configuration);
        EPServiceProvider epNull = EPServiceProviderManager.getProvider(null, configuration);

        assertTrue(epNoArg == epDefault);
        assertTrue(epNull == epDefault);
        assertEquals("default", epNull.getURI());
    }
}