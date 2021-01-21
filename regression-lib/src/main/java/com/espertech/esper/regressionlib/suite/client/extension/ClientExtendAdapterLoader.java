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
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.util.SupportPluginLoader;

import java.util.EnumSet;

import static org.junit.Assert.*;

public class ClientExtendAdapterLoader implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        // Assure destroy order ESPER-489
        assertEquals(2, SupportPluginLoader.getNames().size());
        assertEquals(2, SupportPluginLoader.getPostInitializes().size());
        assertEquals("MyLoader", SupportPluginLoader.getNames().get(0));
        assertEquals("MyLoader2", SupportPluginLoader.getNames().get(1));
        assertEquals("val", SupportPluginLoader.getProps().get(0).get("name"));
        assertEquals("val2", SupportPluginLoader.getProps().get(1).get("name2"));

        Object loader = getFromEnv(env, "plugin-loader/MyLoader");
        assertTrue(loader instanceof SupportPluginLoader);
        loader = getFromEnv(env, "plugin-loader/MyLoader2");
        assertTrue(loader instanceof SupportPluginLoader);

        SupportPluginLoader.getPostInitializes().clear();
        SupportPluginLoader.getNames().clear();
        env.runtime().initialize();
        assertEquals(2, SupportPluginLoader.getPostInitializes().size());
        assertEquals(2, SupportPluginLoader.getNames().size());

        env.runtime().destroy();
        assertEquals(2, SupportPluginLoader.getDestroys().size());
        assertEquals("val2", SupportPluginLoader.getDestroys().get(0).get("name2"));
        assertEquals("val", SupportPluginLoader.getDestroys().get(1).get("name"));

        SupportPluginLoader.reset();
    }

    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.STATICHOOK);
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
