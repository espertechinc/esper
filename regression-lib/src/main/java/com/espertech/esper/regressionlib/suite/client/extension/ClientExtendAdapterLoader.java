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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportPluginLoader;
import org.junit.Assert;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClientExtendAdapterLoader implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        // Assure destroy order ESPER-489
        Assert.assertEquals(2, SupportPluginLoader.getNames().size());
        Assert.assertEquals(2, SupportPluginLoader.getPostInitializes().size());
        Assert.assertEquals("MyLoader", SupportPluginLoader.getNames().get(0));
        Assert.assertEquals("MyLoader2", SupportPluginLoader.getNames().get(1));
        Assert.assertEquals("val", SupportPluginLoader.getProps().get(0).get("name"));
        Assert.assertEquals("val2", SupportPluginLoader.getProps().get(1).get("name2"));

        Object loader = getFromEnv(env, "plugin-loader/MyLoader");
        assertTrue(loader instanceof SupportPluginLoader);
        loader = getFromEnv(env, "plugin-loader/MyLoader2");
        assertTrue(loader instanceof SupportPluginLoader);

        SupportPluginLoader.getPostInitializes().clear();
        SupportPluginLoader.getNames().clear();
        env.runtime().initialize();
        Assert.assertEquals(2, SupportPluginLoader.getPostInitializes().size());
        Assert.assertEquals(2, SupportPluginLoader.getNames().size());

        env.runtime().destroy();
        Assert.assertEquals(2, SupportPluginLoader.getDestroys().size());
        Assert.assertEquals("val2", SupportPluginLoader.getDestroys().get(0).get("name2"));
        Assert.assertEquals("val", SupportPluginLoader.getDestroys().get(1).get("name"));

        SupportPluginLoader.reset();
    }

    private Object getFromEnv(RegressionEnvironment env, String name) {
        try {
            return env.runtime().getContext().getEnvironment().get(name);
        } catch (Throwable t) {
            fail(t.getMessage());
            throw new RuntimeException(t);
        }
    }
}
