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

package com.espertech.esper.regression.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.lrreport.LocationReportFactory;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestEnumNestedPerformance extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addImport(LocationReportFactory.class);
        config.addEventType("Bean", SupportBean_ST0_Container.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testPerfNestedUncorrelated() {

        List<SupportBean_ST0> list = new ArrayList<SupportBean_ST0>();
        for (int i = 0; i < 10000; i++) {
            list.add(new SupportBean_ST0("E1", 1000));
        }
        SupportBean_ST0 minEvent = new SupportBean_ST0("E2", 5);
        list.add(minEvent);
        SupportBean_ST0_Container theEvent = new SupportBean_ST0_Container(list);

        // the "contained.min" inner lambda only depends on values within "contained" (a stream's value)
        // and not on the particular "x".
        String eplFragment = "select contained.where(x => x.p00 = contained.min(y => y.p00)) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        long start = System.currentTimeMillis();
        epService.getEPRuntime().sendEvent(theEvent);
        long delta = System.currentTimeMillis() - start;
        assertTrue("delta=" + delta, delta < 100);

        Collection<SupportBean_ST0> result = (Collection<SupportBean_ST0>) listener.assertOneGetNewAndReset().get("val");
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{minEvent}, result.toArray());
    }
}
