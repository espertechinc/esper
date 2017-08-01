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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

import java.util.Properties;

public class TestSuiteEventBean extends TestCase {
    public void testExecEventBeanEventPropertyDynamicPerformance() {
        RegressionRunner.run(new ExecEventBeanEventPropertyDynamicPerformance());
    }

    public void testExecEventBeanAddRemoveType() {
        RegressionRunner.run(new ExecEventBeanAddRemoveType());
    }

    public void testExecEventBeanPrivateClass() {
        RegressionRunner.run(new ExecEventBeanPrivateClass());
    }

    public void testExecEventBeanPublicAccessors() {
        RegressionRunner.run(new ExecEventBeanPublicAccessors(true));
        RegressionRunner.run(new ExecEventBeanPublicAccessors(false));
    }

    public void testExecEventBeanExplicitOnly() {
        RegressionRunner.run(new ExecEventBeanExplicitOnly(true));
        RegressionRunner.run(new ExecEventBeanExplicitOnly(false));
    }

    public void testExecEventBeanJavaBeanAccessor() {
        RegressionRunner.run(new ExecEventBeanJavaBeanAccessor(true));
        RegressionRunner.run(new ExecEventBeanJavaBeanAccessor(false));
    }

    public void testExecEventBeanFinalClass() {
        RegressionRunner.run(new ExecEventBeanFinalClass(true));
        RegressionRunner.run(new ExecEventBeanFinalClass(false));
    }

    public void testExecEventBeanMappedIndexedPropertyExpression() {
        RegressionRunner.run(new ExecEventBeanMappedIndexedPropertyExpression());
    }

    public void testExecEventBeanPropertyResolutionWDefaults() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionWDefaults());
    }

    public void testExecEventBeanPropertyResolutionCaseInsensitive() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionCaseInsensitive());
    }

    public void testExecEventBeanPropertyResolutionAccessorStyleGlobalPublic() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionAccessorStyleGlobalPublic());
    }

    public void testExecEventBeanPropertyResolutionCaseDistinctInsensitive() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionCaseDistinctInsensitive());
    }

    public void testExecEventBeanPropertyResolutionCaseInsensitiveEngineDefault() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionCaseInsensitiveEngineDefault());
    }

    public void testExecEventBeanPropertyResolutionCaseInsensitiveConfigureType() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionCaseInsensitiveConfigureType());
    }

    public void testExecEventBeanPropertyResolutionFragment() {
        RegressionRunner.run(new ExecEventBeanPropertyResolutionFragment());
    }

    public void testExecEventBeanPropertyIterableMapList() {
        RegressionRunner.run(new ExecEventBeanPropertyIterableMapList());
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
