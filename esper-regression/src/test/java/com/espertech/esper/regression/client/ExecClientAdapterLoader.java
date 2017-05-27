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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.plugin.SupportPluginLoader;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientAdapterLoader implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAdapterLoader();
        runAssertionDestroyObtainTwice();
    }

    private void runAssertionAdapterLoader() throws Exception {
        SupportPluginLoader.reset();
        // Assure destroy order ESPER-489

        Properties props = new Properties();
        props.put("name", "val");
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addPluginLoader("MyLoader", SupportPluginLoader.class.getName(), props);

        props = new Properties();
        props.put("name2", "val2");
        config.addPluginLoader("MyLoader2", SupportPluginLoader.class.getName(), props);

        EPServiceProvider service = EPServiceProviderManager.getProvider("ExecClientAdapterLoader", config);
        assertEquals(2, SupportPluginLoader.getNames().size());
        assertEquals(2, SupportPluginLoader.getPostInitializes().size());
        assertEquals("MyLoader", SupportPluginLoader.getNames().get(0));
        assertEquals("MyLoader2", SupportPluginLoader.getNames().get(1));
        assertEquals("val", SupportPluginLoader.getProps().get(0).get("name"));
        assertEquals("val2", SupportPluginLoader.getProps().get(1).get("name2"));

        Object loader = service.getContext().getEnvironment().get("plugin-loader/MyLoader");
        assertTrue(loader instanceof SupportPluginLoader);
        loader = service.getContext().getEnvironment().get("plugin-loader/MyLoader2");
        assertTrue(loader instanceof SupportPluginLoader);

        SupportPluginLoader.getPostInitializes().clear();
        SupportPluginLoader.getNames().clear();
        service.initialize();
        assertEquals(2, SupportPluginLoader.getPostInitializes().size());
        assertEquals(2, SupportPluginLoader.getNames().size());

        service.destroy();
        assertEquals(2, SupportPluginLoader.getDestroys().size());
        assertEquals("val2", SupportPluginLoader.getDestroys().get(0).get("name2"));
        assertEquals("val", SupportPluginLoader.getDestroys().get(1).get("name"));
    }

    private void runAssertionDestroyObtainTwice() {
        SupportPluginLoader.reset();

        Configuration cf = SupportConfigFactory.getConfiguration();
        cf.addPluginLoader("AP", SupportPluginLoader.class.getName(), null);
        EPServiceProviderManager.getProvider("ExecClientAdapterLoader", cf);
        EPServiceProvider ep = EPServiceProviderManager.getProvider("ExecClientAdapterLoader");
        ep.destroy();
        assertEquals(1, SupportPluginLoader.getDestroys().size());
    }
}
