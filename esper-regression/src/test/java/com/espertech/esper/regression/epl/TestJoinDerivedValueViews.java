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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestJoinDerivedValueViews extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testJoinDerivedValue() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        epService.getEPAdministrator().createEPL("select\n" +
                "Math.signum(stream1.slope) as s1,\n" +
                "Math.signum(stream2.slope) as s2\n" +
                "from\n" +
                "SupportBean#length_batch(3)#linest(intPrimitive, longPrimitive) as stream1,\n" +
                "SupportBean#length_batch(2)#linest(intPrimitive, longPrimitive) as stream2").addListener(listener);
        epService.getEPRuntime().sendEvent(makeEvent("E3", 1, 100));
        epService.getEPRuntime().sendEvent(makeEvent("E4", 1, 100));
        assertFalse(listener.isInvoked());
    }

    private SupportBean makeEvent(String id, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(id, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }
}
