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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

import java.util.Properties;

public class TestSuiteEventMap extends TestCase {
    public void testExecEventMap() {
        RegressionRunner.run(new ExecEventMap());
    }

    public void testExecEventMapPropertyConfig() {
        RegressionRunner.run(new ExecEventMapPropertyConfig());
    }

    public void testExecEventMapPropertyDynamic() {
        RegressionRunner.run(new ExecEventMapPropertyDynamic());
    }

    public void testExecEventMapObjectArrayInterUse() {
        RegressionRunner.run(new ExecEventMapObjectArrayInterUse());
    }

    public void testExecEventMapUpdate() {
        RegressionRunner.run(new ExecEventMapUpdate());
    }

    public void testExecEventMapInheritanceInitTime() {
        RegressionRunner.run(new ExecEventMapInheritanceInitTime());
    }

    public void testExecEventMapInheritanceRuntime() {
        RegressionRunner.run(new ExecEventMapInheritanceRuntime());
    }

    public void testExecEventMapNestedEscapeDot() {
        RegressionRunner.run(new ExecEventMapNestedEscapeDot());
    }

    public void testExecEventMapNestedConfigRuntime() {
        RegressionRunner.run(new ExecEventMapNestedConfigRuntime());
    }

    public void testExecEventMapNestedConfigStatic() {
        RegressionRunner.run(new ExecEventMapNestedConfigStatic());
    }

    public void testExecEventMapNested() {
        RegressionRunner.run(new ExecEventMapNested());
    }

    public void testExecEventMapAddIdenticalMapTypes() {
        RegressionRunner.run(new ExecEventMapAddIdenticalMapTypes());
    }

    public void testExecEventMapInvalidType() {
        RegressionRunner.run(new ExecEventMapInvalidType());
    }

    public void testExecEventMapProperties() {
        RegressionRunner.run(new ExecEventMapProperties());
    }

    public void testInvalidConfig() {
        Properties properties = new Properties();
        properties.put("astring", "XXXX");

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("MyInvalidEvent", properties);

        try {
            EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
            epService.initialize();
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
    }
}
