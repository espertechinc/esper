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

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.EPLExpressionEvaluationContext;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import junit.framework.TestCase;

public class TestCurrentEvaluationContextExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testExecutionContext()
    {
        runAssertionExecCtx(false);
        runAssertionExecCtx(true);
    }

    private void runAssertionExecCtx(boolean soda) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        sendTimer(0);

        String epl = "select " +
                "current_evaluation_context() as c0, " +
                "current_evaluation_context(), " +
                "current_evaluation_context().getEngineURI() as c2 from SupportBean";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl, "my_user_object");
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

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
