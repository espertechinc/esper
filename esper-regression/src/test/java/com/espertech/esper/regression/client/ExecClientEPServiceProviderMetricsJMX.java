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
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.fail;

public class ExecClientEPServiceProviderMetricsJMX implements RegressionExecution {

    private final static String ENGINE_NAME = ExecClientEPServiceProviderMetricsJMX.class.getSimpleName();
    private final static String FILTER_NAME = "\"com.espertech.esper-" + ENGINE_NAME + "\":type=\"filter\"";
    private final static String RUNTIME_NAME = "\"com.espertech.esper-" + ENGINE_NAME + "\":type=\"runtime\"";
    private final static String SCHEDULE_NAME = "\"com.espertech.esper-" + ENGINE_NAME + "\":type=\"schedule\"";
    private final static String[] ALL = new String[]{FILTER_NAME, RUNTIME_NAME, SCHEDULE_NAME};

    public void run(EPServiceProvider defaultEPService) throws Exception {
        assertNoEngineJMX();

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getMetricsReporting().setJmxEngineMetrics(true);
        EPServiceProvider epService = EPServiceProviderManager.getProvider(ENGINE_NAME, configuration);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec("2002-05-1T08:00:00.000")));
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        epService.getEPAdministrator().createEPL("select * from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B') where timer:within(a.intPrimitive)]");
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 60));

        assertEngineJMX();

        epService.destroy();

        assertNoEngineJMX();
    }

    private void assertEngineJMX() throws Exception {
        for (String name : ALL) {
            assertJMXVisible(name);
        }
    }

    private void assertJMXVisible(String name) throws Exception {
        ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
    }

    private void assertNoEngineJMX() throws Exception {
        for (String name : ALL) {
            assertJMXNotVisible(name);
        }
    }

    private void assertJMXNotVisible(String name) throws Exception {
        try {
            ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
            fail();
        } catch (InstanceNotFoundException ex) {
            // expected
        }
    }
}
