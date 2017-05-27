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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExexAggregateCountWGroupBy implements RegressionExecution {
    private static final String SYMBOL_DELL = "DELL";
    private static final String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCountOneViewOM(epService);
        runAssertionGroupByCountNestedAggregationAvg(epService);
        runAssertionCountOneViewCompile(epService);
        runAssertionCountOneView(epService);
        runAssertionCountJoin(epService);
    }

    private void runAssertionCountOneViewOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("symbol")
                .add(Expressions.countStar(), "countAll")
                .add(Expressions.countDistinct("volume"), "countDistVol")
                .add(Expressions.count("volume"), "countVol"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(3))));
        model.setWhereClause(Expressions.or()
                .add(Expressions.eq("symbol", "DELL"))
                .add(Expressions.eq("symbol", "IBM"))
                .add(Expressions.eq("symbol", "GE")));
        model.setGroupByClause(GroupByClause.create("symbol"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String epl = "select irstream symbol, " +
                "count(*) as countAll, " +
                "count(distinct volume) as countDistVol, " +
                "count(volume) as countVol" +
                " from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionCount(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionGroupByCountNestedAggregationAvg(EPServiceProvider epService) throws Exception {
        // test for ESPER-328
        String epl = "select symbol, count(*) as cnt, avg(count(*)) as val from " + SupportMarketDataBean.class.getName() + "#length(3)" +
                "group by symbol order by symbol asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, SYMBOL_DELL, 50L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 1L, 1d});

        sendEvent(epService, SYMBOL_DELL, 51L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 2L, 1.5d});

        sendEvent(epService, SYMBOL_DELL, 52L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 3L, 2d});

        sendEvent(epService, "IBM", 52L);
        EventBean[] events = listener.getLastNewData();
        EPAssertionUtil.assertProps(events[0], "symbol,cnt,val".split(","), new Object[]{"DELL", 2L, 2d});
        EPAssertionUtil.assertProps(events[1], "symbol,cnt,val".split(","), new Object[]{"IBM", 1L, 1d});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 53L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 2L, 2.5d});

        stmt.destroy();
    }

    private void runAssertionCountOneViewCompile(EPServiceProvider epService) throws Exception {
        String epl = "select irstream symbol, " +
                "count(*) as countAll, " +
                "count(distinct volume) as countDistVol, " +
                "count(volume) as countVol" +
                " from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionCount(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionCountOneView(EPServiceProvider epService) {
        String epl = "select irstream symbol, " +
                "count(*) as countAll," +
                "count(distinct volume) as countDistVol," +
                "count(all volume) as countVol" +
                " from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionCount(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionCountJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol, " +
                "count(*) as countAll," +
                "count(distinct volume) as countDistVol," +
                "count(volume) as countVol " +
                " from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        tryAssertionCount(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssertionCount(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("countAll"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("countDistVol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("countVol"));

        sendEvent(epService, SYMBOL_DELL, 50L);
        assertEvents(listener, SYMBOL_DELL, 0L, 0L, 0L,
                SYMBOL_DELL, 1L, 1L, 1L
        );

        sendEvent(epService, SYMBOL_DELL, null);
        assertEvents(listener, SYMBOL_DELL, 1L, 1L, 1L,
                SYMBOL_DELL, 2L, 1L, 1L
        );

        sendEvent(epService, SYMBOL_DELL, 25L);
        assertEvents(listener, SYMBOL_DELL, 2L, 1L, 1L,
                SYMBOL_DELL, 3L, 2L, 2L
        );

        sendEvent(epService, SYMBOL_DELL, 25L);
        assertEvents(listener, SYMBOL_DELL, 3L, 2L, 2L,
                SYMBOL_DELL, 3L, 1L, 2L
        );

        sendEvent(epService, SYMBOL_DELL, 25L);
        assertEvents(listener, SYMBOL_DELL, 3L, 1L, 2L,
                SYMBOL_DELL, 3L, 1L, 3L
        );

        sendEvent(epService, SYMBOL_IBM, 1L);
        sendEvent(epService, SYMBOL_IBM, null);
        sendEvent(epService, SYMBOL_IBM, null);
        sendEvent(epService, SYMBOL_IBM, null);
        assertEvents(listener, SYMBOL_IBM, 3L, 1L, 1L,
                SYMBOL_IBM, 3L, 0L, 0L
        );
    }

    private void assertEvents(SupportUpdateListener listener, String symbolOld, Long countAllOld, Long countDistVolOld, Long countVolOld,
                              String symbolNew, Long countAllNew, Long countDistVolNew, Long countVolNew) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbolOld, oldData[0].get("symbol"));
        assertEquals(countAllOld, oldData[0].get("countAll"));
        assertEquals(countDistVolOld, oldData[0].get("countDistVol"));
        assertEquals(countVolOld, oldData[0].get("countVol"));

        assertEquals(symbolNew, newData[0].get("symbol"));
        assertEquals(countAllNew, newData[0].get("countAll"));
        assertEquals(countDistVolNew, newData[0].get("countDistVol"));
        assertEquals(countVolNew, newData[0].get("countVol"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExexAggregateCountWGroupBy.class);
}
