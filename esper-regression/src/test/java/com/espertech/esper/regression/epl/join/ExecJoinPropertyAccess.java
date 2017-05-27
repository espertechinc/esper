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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExecJoinPropertyAccess implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionRegularJoin(epService);
        runAssertionOuterJoin(epService);
    }

    private void runAssertionRegularJoin(EPServiceProvider epService) {
        SupportBeanCombinedProps combined = SupportBeanCombinedProps.makeDefaultBean();
        SupportBeanComplexProps complex = SupportBeanComplexProps.makeDefaultBean();
        assertEquals("0ma0", combined.getIndexed(0).getMapped("0ma").getValue());

        String epl = "select nested.nested, s1.indexed[0], nested.indexed[1] from " +
                SupportBeanComplexProps.class.getName() + "#length(3) nested, " +
                SupportBeanCombinedProps.class.getName() + "#length(3) s1" +
                " where mapped('keyOne') = indexed[2].mapped('2ma').value and" +
                " indexed[0].mapped('0ma').value = '0ma0'";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        epService.getEPRuntime().sendEvent(combined);
        epService.getEPRuntime().sendEvent(complex);

        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertSame(complex.getNested(), theEvent.get("nested.nested"));
        assertSame(combined.getIndexed(0), theEvent.get("s1.indexed[0]"));
        assertEquals(complex.getIndexed(1), theEvent.get("nested.indexed[1]"));

        stmt.destroy();
    }

    private void runAssertionOuterJoin(EPServiceProvider epService) {
        String epl = "select * from " +
                SupportBeanComplexProps.class.getName() + "#length(3) s0" +
                " left outer join " +
                SupportBeanCombinedProps.class.getName() + "#length(3) s1" +
                " on mapped('keyOne') = indexed[2].mapped('2ma').value";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        SupportBeanCombinedProps combined = SupportBeanCombinedProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(combined);
        SupportBeanComplexProps complex = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(complex);

        // double check that outer join criteria match
        assertEquals(complex.getMapped("keyOne"), combined.getIndexed(2).getMapped("2ma").getValue());

        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("simple", theEvent.get("s0.simpleProperty"));
        assertSame(complex, theEvent.get("s0"));
        assertSame(combined, theEvent.get("s1"));

        stmt.destroy();
    }
}
