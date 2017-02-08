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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestContextPartitionedPrioritized extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private EPServiceProviderSPI spi;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getExecution().setPrioritized(true);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        spi = (EPServiceProviderSPI) epService;

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testFirstEventPrioritized() {
        epService.getEPAdministrator().createEPL(
                "create context SegmentedByMessage partition by theString from SupportBean");

        EPStatement statementWithDropAnnotation = epService.getEPAdministrator().createEPL(
                "@Drop @Priority(1) context SegmentedByMessage select 'test1' from SupportBean");
        SupportUpdateListener statementWithDropAnnotationListener = new SupportUpdateListener();
        statementWithDropAnnotation.addListener(statementWithDropAnnotationListener);

        EPStatement lowPriorityStatement = epService.getEPAdministrator().createEPL(
                "@Priority(0) context SegmentedByMessage select 'test2' from SupportBean");
        SupportUpdateListener lowPriorityStatementListener = new SupportUpdateListener();
        lowPriorityStatement.addListener(lowPriorityStatementListener);

        epService.getEPRuntime().sendEvent(new SupportBean("test msg",1));

        assertTrue(statementWithDropAnnotationListener.isInvoked());
        assertFalse(lowPriorityStatementListener.isInvoked());
    }

}
