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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

// Ranked-Window tests.
//   - retains the last event per unique key as long as within rank
//   - retains the newest event for a given rank: same-rank new events push out old events within the same rank when overflowing.
//
// Related to the ranked data window is the following:
// ext:sort(10, p00)                            Maintain the top 10 events sorted by p00-value
// std:groupwin(p00)#sort(10, p01 desc)     For each p00-value maintain the top 10 events sorted by p01 desc
// SupportBean#unique(string)#sort(3, intPrimitive)  Intersection NOT applicable because E1-3, E2-2, E3-1 then E2-4 causes E2-2 to go out of window
// ... order by p00 desc limit 8 offset 2       This can rank, however it may retain too data (such as count per word); also cannot use window(*) on rank data
// - it is a data window because it retains events, works with 'prev' (its sorted), works with 'window(*)', is iterable
// - is is not an aggregation (regular or data window) because aggregations don't decide how many events to retain
public class ExecViewRank implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        runAssertionPrevAndGroupWin(epService);
        runAssertionMultiexpression(epService);
        runAssertionRemoveStream(epService);
        runAssertionRanked(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionPrevAndGroupWin(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select prevwindow(ev) as win, prev(0, ev) as prev0, prev(1, ev) as prev1, prev(2, ev) as prev2, prev(3, ev) as prev3, prev(4, ev) as prev4 " +
                "from SupportBean#rank(theString, 3, intPrimitive) as ev");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 100, 0L));
        assertWindowAggAndPrev(listener, new Object[][]{{"E1", 100, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 99, 0L));
        assertWindowAggAndPrev(listener, new Object[][]{{"E2", 99, 0L}, {"E1", 100, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 98, 1L));
        assertWindowAggAndPrev(listener, new Object[][]{{"E1", 98, 1L}, {"E2", 99, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 98, 0L));
        assertWindowAggAndPrev(listener, new Object[][]{{"E1", 98, 1L}, {"E3", 98, 0L}, {"E2", 99, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 97, 1L));
        assertWindowAggAndPrev(listener, new Object[][]{{"E2", 97, 1L}, {"E1", 98, 1L}, {"E3", 98, 0L}});
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#groupwin(theString)#rank(intPrimitive, 2, doublePrimitive) as ev");
        stmt.addListener(listener);

        String[] fields = "theString,intPrimitive,longPrimitive,doublePrimitive".split(",");
        epService.getEPRuntime().sendEvent(makeEvent("E1", 100, 0L, 1d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 100, 0L, 1d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 0L, 1d}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 100, 0L, 2d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, 0L, 2d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 0L, 1d}, {"E2", 100, 0L, 2d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 200, 0L, 0.5d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 200, 0L, 0.5d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 200, 0L, 0.5d}, {"E1", 100, 0L, 1d}, {"E2", 100, 0L, 2d}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 200, 0L, 2.5d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 200, 0L, 2.5d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 200, 0L, 0.5d}, {"E1", 100, 0L, 1d}, {"E2", 100, 0L, 2d}, {"E2", 200, 0L, 2.5d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 300, 0L, 0.1d));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E1", 300, 0L, 0.1d}, new Object[]{"E1", 100, 0L, 1d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 300, 0L, 0.1d}, {"E1", 200, 0L, 0.5d}, {"E2", 100, 0L, 2d}, {"E2", 200, 0L, 2.5d}});

        stmt.destroy();
    }

    private void runAssertionMultiexpression(EPServiceProvider epService) {
        String[] fields = "theString,intPrimitive,longPrimitive,doublePrimitive".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#rank(theString, intPrimitive, 3, longPrimitive, doublePrimitive)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeEvent("E1", 100, 1L, 10d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 100, 1L, 10d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 1L, 10d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 200, 1L, 9d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 200, 1L, 9d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 200, 1L, 9d}, {"E1", 100, 1L, 10d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 150, 1L, 11d));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 150, 1L, 11d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 200, 1L, 9d}, {"E1", 100, 1L, 10d}, {"E1", 150, 1L, 11d}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 100, 1L, 8d));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E1", 100, 1L, 8d}, new Object[]{"E1", 100, 1L, 10d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E1", 200, 1L, 9d}, {"E1", 150, 1L, 11d}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 300, 2L, 7d));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E2", 300, 2L, 7d}, new Object[]{"E2", 300, 2L, 7d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E1", 200, 1L, 9d}, {"E1", 150, 1L, 11d}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 300, 1L, 8.5d));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E3", 300, 1L, 8.5d}, new Object[]{"E1", 150, 1L, 11d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E3", 300, 1L, 8.5d}, {"E1", 200, 1L, 9d}});

        epService.getEPRuntime().sendEvent(makeEvent("E4", 400, 1L, 9d));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E4", 400, 1L, 9d}, new Object[]{"E1", 200, 1L, 9d});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 100, 1L, 8d}, {"E3", 300, 1L, 8.5d}, {"E4", 400, 1L, 9d}});

        stmt.destroy();
    }

    private void runAssertionRemoveStream(EPServiceProvider epService) {
        String[] fields = "theString,intPrimitive,longPrimitive".split(",");
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL("create window MyWindow#rank(theString, 3, intPrimitive asc) as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        EPStatement stmtListen = epService.getEPAdministrator().createEPL("select irstream * from MyWindow");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtListen.addListener(listener);
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from MyWindow mw where theString = id");

        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 50, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 50, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10, 0L}, {"E2", 50, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 5, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 5, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 5, 0L}, {"E1", 10, 0L}, {"E2", 50, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E4", 5, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E4", 5, 0L}, new Object[]{"E2", 50, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 5, 0L}, {"E4", 5, 0L}, {"E1", 10, 0L}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E3", 5, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E4", 5, 0L}, {"E1", 10, 0L}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E4", 5, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 10, 0L}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[0][]);

        epService.getEPRuntime().sendEvent(makeEvent("E3", 100, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 100, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 100, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 101, 1L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E3", 101, 1L}, new Object[]{"E3", 100, 0L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E3", 101, 1L}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetOldAndReset(), fields, new Object[]{"E3", 101, 1L});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[0][]);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRanked(EPServiceProvider epService) {
        String[] fields = "theString,intPrimitive,longPrimitive".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#rank(theString, 4, intPrimitive desc)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // sorting-related testing
        epService.getEPRuntime().sendEvent(makeEvent("E1", 10, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 10, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 30, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 30, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 30, 0L}, {"E1", 10, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 50, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E1", 50, 0L}, new Object[]{"E1", 10, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 50, 0L}, {"E2", 30, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 40, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 40, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 50, 0L}, {"E3", 40, 0L}, {"E2", 30, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 45, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E2", 45, 0L}, new Object[]{"E2", 30, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E1", 50, 0L}, {"E2", 45, 0L}, {"E3", 40, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 43, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E1", 43, 0L}, new Object[]{"E1", 50, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E3", 40, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 50, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E3", 50, 0L}, new Object[]{"E3", 40, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 50, 0L}, {"E2", 45, 0L}, {"E1", 43, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E3", 10, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E3", 10, 0L}, new Object[]{"E3", 50, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E3", 10, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E4", 43, 0L));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E4", 43, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E4", 43, 0L}, {"E3", 10, 0L}});

        // in-place replacement
        epService.getEPRuntime().sendEvent(makeEvent("E4", 43, 1L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E4", 43, 1L}, new Object[]{"E4", 43, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 0L}, {"E1", 43, 0L}, {"E4", 43, 1L}, {"E3", 10, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E2", 45, 1L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E2", 45, 1L}, new Object[]{"E2", 45, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E1", 43, 0L}, {"E4", 43, 1L}, {"E3", 10, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E1", 43, 1L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E1", 43, 1L}, new Object[]{"E1", 43, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E3", 10, 0L}});

        // out-of-space: pushing out the back end
        epService.getEPRuntime().sendEvent(makeEvent("E5", 10, 2L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E5", 10, 2L}, new Object[]{"E3", 10, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E5", 10, 2L}});

        epService.getEPRuntime().sendEvent(makeEvent("E5", 11, 3L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E5", 11, 3L}, new Object[]{"E5", 10, 2L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E5", 11, 3L}});

        epService.getEPRuntime().sendEvent(makeEvent("E6", 43, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E6", 43, 0L}, new Object[]{"E5", 11, 3L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E2", 45, 1L}, {"E4", 43, 1L}, {"E1", 43, 1L}, {"E6", 43, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E7", 50, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E7", 50, 0L}, new Object[]{"E4", 43, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E7", 50, 0L}, {"E2", 45, 1L}, {"E1", 43, 1L}, {"E6", 43, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E8", 45, 0L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E8", 45, 0L}, new Object[]{"E1", 43, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E7", 50, 0L}, {"E2", 45, 1L}, {"E8", 45, 0L}, {"E6", 43, 0L}});

        epService.getEPRuntime().sendEvent(makeEvent("E8", 46, 1L));
        EPAssertionUtil.assertProps(listener.assertPairGetIRAndReset(), fields, new Object[]{"E8", 46, 1L}, new Object[]{"E8", 45, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E7", 50, 0L}, {"E8", 46, 1L}, {"E2", 45, 1L}, {"E6", 43, 0L}});

        stmt.destroy();
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        return makeEvent(theString, intPrimitive, longPrimitive, 0d);
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        return bean;
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from SupportBean#rank(1, intPrimitive desc)",
                "Error starting statement: Error attaching view to event stream: Rank view requires a list of expressions providing unique keys, a numeric size parameter and a list of expressions providing sort keys [select * from SupportBean#rank(1, intPrimitive desc)]");

        tryInvalid(epService, "select * from SupportBean#rank(1, intPrimitive, theString desc)",
                "Error starting statement: Error attaching view to event stream: Failed to find unique value expressions that are expected to occur before the numeric size parameter [select * from SupportBean#rank(1, intPrimitive, theString desc)]");

        tryInvalid(epService, "select * from SupportBean#rank(theString, intPrimitive, 1)",
                "Error starting statement: Error attaching view to event stream: Failed to find sort key expressions after the numeric size parameter [select * from SupportBean#rank(theString, intPrimitive, 1)]");

        tryInvalid(epService, "select * from SupportBean#rank(theString, intPrimitive, theString desc)",
                "Error starting statement: Error attaching view to event stream: Failed to find constant value for the numeric size parameter [select * from SupportBean#rank(theString, intPrimitive, theString desc)]");

        tryInvalid(epService, "select * from SupportBean#rank(theString, 1, 1, intPrimitive, theString desc)",
                "Error starting statement: Error attaching view to event stream: Invalid view parameter expression 2 for Rank view, the expression returns a constant result value, are you sure? [select * from SupportBean#rank(theString, 1, 1, intPrimitive, theString desc)]");

        tryInvalid(epService, "select * from SupportBean#rank(theString, intPrimitive, 1, intPrimitive, 1, theString desc)",
                "Error starting statement: Error attaching view to event stream: Invalid view parameter expression 4 for Rank view, the expression returns a constant result value, are you sure? [select * from SupportBean#rank(theString, intPrimitive, 1, intPrimitive, 1, theString desc)]");
    }

    private void assertWindowAggAndPrev(SupportUpdateListener listener, Object[][] expected) {
        String[] fields = "theString,intPrimitive,longPrimitive".split(",");
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertPropsPerRow((Object[]) event.get("win"), fields, expected);
        for (int i = 0; i < 5; i++) {
            Object prevValue = event.get("prev" + i);
            if (prevValue == null && expected.length <= i) {
                continue;
            }
            EPAssertionUtil.assertProps(prevValue, fields, expected[i]);
        }
    }
}
