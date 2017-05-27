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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecRowRecogClausePresence implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionMeasurePresence(epService, 0, "B.size()", 1);
        runAssertionMeasurePresence(epService, 0, "100+B.size()", 101);
        runAssertionMeasurePresence(epService, 1000000, "B.anyOf(v=>theString='E2')", true);

        runAssertionDefineNotPresent(epService, true);
        runAssertionDefineNotPresent(epService, false);
    }

    private void runAssertionDefineNotPresent(EPServiceProvider engine, boolean soda) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String epl = "select * from SupportBean " +
                "match_recognize (" +
                " measures A as a, B as b" +
                " pattern (A B)" +
                ")";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(engine, soda, epl);
        stmt.addListener(listener);

        String[] fields = "a,b".split(",");
        SupportBean[] beans = new SupportBean[4];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = new SupportBean("E" + i, i);
        }

        engine.getEPRuntime().sendEvent(beans[0]);
        assertFalse(listener.isInvoked());
        engine.getEPRuntime().sendEvent(beans[1]);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{beans[0], beans[1]});

        engine.getEPRuntime().sendEvent(beans[2]);
        assertFalse(listener.isInvoked());
        engine.getEPRuntime().sendEvent(beans[3]);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{beans[2], beans[3]});

        stmt.destroy();
    }

    private void runAssertionMeasurePresence(EPServiceProvider engine, long baseTime, String select, Object value) {

        engine.getEPRuntime().sendEvent(new CurrentTimeEvent(baseTime));
        String epl = "select * from SupportBean  " +
                "match_recognize (" +
                "    measures A as a, A.theString as id, " + select + " as val " +
                "    pattern (A B*) " +
                "    interval 1 minute " +
                "    define " +
                "        A as (A.intPrimitive=1)," +
                "        B as (B.intPrimitive=2))";
        SupportUpdateListener listener = new SupportUpdateListener();
        engine.getEPAdministrator().createEPL(epl).addListener(listener);

        engine.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        engine.getEPRuntime().sendEvent(new SupportBean("E2", 2));

        engine.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(baseTime + 60 * 1000 * 2));
        assertEquals(value, listener.getNewDataListFlattened()[0].get("val"));

        engine.getEPAdministrator().destroyAllStatements();
    }
}