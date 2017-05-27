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
import com.espertech.esper.client.hook.EPLExpressionEvaluationContext;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import static org.junit.Assert.assertEquals;

public class ExecExprCurrentEvaluationContext implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendTimer(epService, 0);

        runAssertionExecCtx(epService, false);
        runAssertionExecCtx(epService, true);
    }

    private void runAssertionExecCtx(EPServiceProvider epService, boolean soda) {
        String epl = "select " +
                "current_evaluation_context() as c0, " +
                "current_evaluation_context(), " +
                "current_evaluation_context().getEngineURI() as c2 from SupportBean";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl, "my_user_object");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(EPLExpressionEvaluationContext.class, stmt.getEventType().getPropertyType("current_evaluation_context()"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean event = listener.assertOneGetNewAndReset();
        EPLExpressionEvaluationContext ctx = (EPLExpressionEvaluationContext) event.get("c0");
        assertEquals(epService.getURI(), ctx.getEngineURI());
        assertEquals(stmt.getName(), ctx.getStatementName());
        assertEquals(-1, ctx.getContextPartitionId());
        assertEquals("my_user_object", ctx.getStatementUserObject());
        assertEquals(epService.getURI(), event.get("c2"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
