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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;

public class ExecExprCurrentTimestamp implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionGetTimestamp(epService);
        runAssertionGetTimestamp_OM(epService);
        runAssertionGetTimestamp_Compile(epService);
    }

    private void runAssertionGetTimestamp(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String stmtText = "select current_timestamp(), " +
                " current_timestamp as t0, " +
                " current_timestamp() as t1, " +
                " current_timestamp + 1 as t2 " +
                " from " + SupportBean.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("current_timestamp()"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("t0"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("t1"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("t2"));

        sendTimer(epService, 100);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{100L, 100L, 101L});

        sendTimer(epService, 999);
        epService.getEPRuntime().sendEvent(new SupportBean());
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{999L, 999L, 1000L});
        assertEquals(theEvent.get("current_timestamp()"), theEvent.get("t0"));

        stmt.destroy();
    }

    private void runAssertionGetTimestamp_OM(EPServiceProvider epService) throws Exception {
        sendTimer(epService, 0);
        String stmtText = "select current_timestamp() as t0 from " + SupportBean.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.currentTimestamp(), "t0"));
        model.setFromClause(FromClause.create().add(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("t0"));

        sendTimer(epService, 777);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{777L});

        stmt.destroy();
    }

    private void runAssertionGetTimestamp_Compile(EPServiceProvider epService) throws Exception {
        sendTimer(epService, 0);
        String stmtText = "select current_timestamp() as t0 from " + SupportBean.class.getName();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("t0"));

        sendTimer(epService, 777);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{777L});
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void assertResults(EventBean theEvent, Object[] result) {
        for (int i = 0; i < result.length; i++) {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
