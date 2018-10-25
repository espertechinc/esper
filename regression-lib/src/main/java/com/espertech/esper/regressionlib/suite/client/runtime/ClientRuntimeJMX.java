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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.fail;

public class ClientRuntimeJMX {
    private final static String JMX_ENGINE_NAME = ClientRuntimeJMX.class.getSimpleName() + "__JMX";
    private final static String JMX_FILTER_NAME = "\"com.espertech.esper-" + JMX_ENGINE_NAME + "\":type=\"filter\"";
    private final static String JMX_RUNTIME_NAME = "\"com.espertech.esper-" + JMX_ENGINE_NAME + "\":type=\"runtime\"";
    private final static String JMX_SCHEDULE_NAME = "\"com.espertech.esper-" + JMX_ENGINE_NAME + "\":type=\"schedule\"";
    private final static String[] ALL = new String[]{JMX_FILTER_NAME, JMX_RUNTIME_NAME, JMX_SCHEDULE_NAME};

    public void run(Configuration configuration) {
        assertNoEngineJMX();

        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getRuntime().getMetricsReporting().setJmxRuntimeMetrics(true);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(JMX_ENGINE_NAME, configuration);

        assertEngineJMX();

        runtime.destroy();

        assertNoEngineJMX();
    }

    private void assertEngineJMX() {
        for (String name : ALL) {
            assertJMXVisible(name);
        }
    }

    private void assertJMXVisible(String name) {
        try {
            ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void assertNoEngineJMX() {
        for (String name : ALL) {
            assertJMXNotVisible(name);
        }
    }

    private void assertJMXNotVisible(String name) {
        try {
            ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
            fail();
        } catch (InstanceNotFoundException ex) {
            // expected
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
