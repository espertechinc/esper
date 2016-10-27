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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.codahale_metrics.metrics.MetricNameFactory;
import com.espertech.esper.metrics.codahale_metrics.metrics.Metrics;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEPServiceProviderMetricsJMX extends TestCase {

    private final static String FILTER_NAME = "\"com.espertech.esper-default\":type=\"filter\"";
    private final static String RUNTIME_NAME = "\"com.espertech.esper-default\":type=\"runtime\"";
    private final static String SCHEDULE_NAME = "\"com.espertech.esper-default\":type=\"schedule\"";
    private final static String[] ALL = new String[] {FILTER_NAME, RUNTIME_NAME, SCHEDULE_NAME};

    public void testMetricsJMX() throws Exception {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getMetricsReporting().setJmxEngineMetrics(true);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec("2002-05-1T08:00:00.000")));
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        epService.getEPAdministrator().createEPL("select * from pattern [every a=SupportBean(theString like 'A%') -> b=SupportBean(theString like 'B') where timer:within(a.intPrimitive)]");
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 60));

        assertEngineJMX();

        epService.destroy();

        assertNoEngineJMX();

        config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getMetricsReporting().setJmxEngineMetrics(false);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        assertNoEngineJMX();

        epService.destroy();
    }

    private void assertEngineJMX() throws Exception {
        for (String name : ALL) {
            assertJMXVisible(name);
        }
    }

    private void assertNoEngineJMX() throws Exception {
        for (String name : ALL) {
            assertJMXNotVisible(name);
        }
    }

    private void assertJMXVisible(String name) throws Exception {
        ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
    }

    private void assertJMXNotVisible(String name) throws Exception {
        try {
            ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
            fail();
        }
        catch (InstanceNotFoundException ex) {
            // expected
        }
    }
}
