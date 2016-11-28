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

package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPContextPartitionAdminImpl;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestContextPartitionedNamedWindow extends TestCase {

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_S0", SupportBean_S0.class);
        configuration.addEventType("SupportBean_S1", SupportBean_S1.class);
        configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(true);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNWFireAndForgetInvalid() {
        epService.getEPAdministrator().createEPL("create context SegmentedByString partition by theString from SupportBean");

        epService.getEPAdministrator().createEPL("context SegmentedByString create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("context SegmentedByString insert into MyWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));

        String expected = "Error executing statement: Named window 'MyWindow' is associated to context 'SegmentedByString' that is not available for querying without context partition selector, use the executeQuery(epl, selector) method instead [select * from MyWindow]";
        try {
            epService.getEPRuntime().executeQuery("select * from MyWindow");
        }
        catch (EPException ex) {
            assertEquals(expected, ex.getMessage());
        }

        EPOnDemandPreparedQueryParameterized prepared = epService.getEPRuntime().prepareQueryWithParameters("select * from MyWindow");
        try {
            epService.getEPRuntime().executeQuery(prepared);
        }
        catch (EPException ex) {
            assertEquals(expected, ex.getMessage());
        }
    }
}
