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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestRowPatternRecognitionEnumMethod extends TestCase {

    public void testNamedWindowOnDeleteOutOfSeq()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(SupportBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String[] fields = "c0,c1".split(",");
        String epl = "select * from SupportBean match_recognize ("
                + "partition by theString "
                + "measures A.theString as c0, C.intPrimitive as c1 "
                + "pattern (A B+ C) "
                + "define "
                + "B as B.intPrimitive > A.intPrimitive, "
                + "C as C.doublePrimitive > B.firstOf().intPrimitive)";
                // can also be expressed as: B[0].intPrimitive
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        sendEvent(epService, "E1", 10, 0);
        sendEvent(epService, "E1", 11, 50);
        sendEvent(epService, "E1", 12, 11);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E2", 10, 0);
        sendEvent(epService, "E2", 11, 50);
        sendEvent(epService, "E2", 12, 12);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 12});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void sendEvent(EPServiceProvider epService, String theString, int intPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}