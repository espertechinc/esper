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

public class ExecAggregateMaxMinGroupBy implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMinMax(epService);
        runAssertionMinMax_OM(epService);
        runAssertionMinMaxView_Compile(epService);
        runAssertionMinMaxJoin(epService);
        runAssertionMinNoGroupHaving(epService);
        runAssertionMinNoGroupSelectHaving(epService);
    }

    private void runAssertionMinMax(EPServiceProvider epService) {
        String epl = "select irstream symbol, " +
                "min(all volume) as minVol," +
                "max(all volume) as maxVol," +
                "min(distinct volume) as minDistVol," +
                "max(distinct volume) as maxDistVol" +
                " from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionMinMax(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionMinMax_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("symbol")
                .add(Expressions.min("volume"), "minVol")
                .add(Expressions.max("volume"), "maxVol")
                .add(Expressions.minDistinct("volume"), "minDistVol")
                .add(Expressions.maxDistinct("volume"), "maxDistVol")
        );
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).addView("length", Expressions.constant(3))));
        model.setWhereClause(Expressions.or()
                .add(Expressions.eq("symbol", "DELL"))
                .add(Expressions.eq("symbol", "IBM"))
                .add(Expressions.eq("symbol", "GE")));
        model.setGroupByClause(GroupByClause.create("symbol"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String epl = "select irstream symbol, " +
                "min(volume) as minVol, " +
                "max(volume) as maxVol, " +
                "min(distinct volume) as minDistVol, " +
                "max(distinct volume) as maxDistVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionMinMax(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionMinMaxView_Compile(EPServiceProvider epService) {
        String epl = "select irstream symbol, " +
                "min(volume) as minVol, " +
                "max(volume) as maxVol, " +
                "min(distinct volume) as minDistVol, " +
                "max(distinct volume) as maxDistVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionMinMax(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionMinMaxJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol, " +
                "min(volume) as minVol," +
                "max(volume) as maxVol," +
                "min(distinct volume) as minDistVol," +
                "max(distinct volume) as maxDistVol" +
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

        tryAssertionMinMax(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionMinNoGroupHaving(EPServiceProvider epService) {
        String stmtText = "select symbol from " + SupportMarketDataBean.class.getName() + "#time(5 sec) " +
                "having volume > min(volume) * 1.3";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "DELL", 100L);
        sendEvent(epService, "DELL", 105L);
        sendEvent(epService, "DELL", 100L);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "DELL", 131L);
        assertEquals("DELL", listener.assertOneGetNewAndReset().get("symbol"));

        sendEvent(epService, "DELL", 132L);
        assertEquals("DELL", listener.assertOneGetNewAndReset().get("symbol"));

        sendEvent(epService, "DELL", 129L);
        assertFalse(listener.isInvoked());
    }

    private void runAssertionMinNoGroupSelectHaving(EPServiceProvider epService) {
        String stmtText = "select symbol, min(volume) as mymin from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "having volume > min(volume) * 1.3";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "DELL", 100L);
        sendEvent(epService, "DELL", 105L);
        sendEvent(epService, "DELL", 100L);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "DELL", 131L);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("DELL", theEvent.get("symbol"));
        assertEquals(100L, theEvent.get("mymin"));

        sendEvent(epService, "DELL", 132L);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("DELL", theEvent.get("symbol"));
        assertEquals(100L, theEvent.get("mymin"));

        sendEvent(epService, "DELL", 129L);
        sendEvent(epService, "DELL", 125L);
        sendEvent(epService, "DELL", 125L);
        assertFalse(listener.isInvoked());

        sendEvent(epService, "DELL", 170L);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("DELL", theEvent.get("symbol"));
        assertEquals(125L, theEvent.get("mymin"));
    }

    private void tryAssertionMinMax(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("minVol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("maxVol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("minDistVol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("maxDistVol"));

        sendEvent(epService, SYMBOL_DELL, 50L);
        assertEvents(listener, SYMBOL_DELL, null, null, null, null,
                SYMBOL_DELL, 50L, 50L, 50L, 50L
        );

        sendEvent(epService, SYMBOL_DELL, 30L);
        assertEvents(listener, SYMBOL_DELL, 50L, 50L, 50L, 50L,
                SYMBOL_DELL, 30L, 50L, 30L, 50L
        );

        sendEvent(epService, SYMBOL_DELL, 30L);
        assertEvents(listener, SYMBOL_DELL, 30L, 50L, 30L, 50L,
                SYMBOL_DELL, 30L, 50L, 30L, 50L
        );

        sendEvent(epService, SYMBOL_DELL, 90L);
        assertEvents(listener, SYMBOL_DELL, 30L, 50L, 30L, 50L,
                SYMBOL_DELL, 30L, 90L, 30L, 90L
        );

        sendEvent(epService, SYMBOL_DELL, 100L);
        assertEvents(listener, SYMBOL_DELL, 30L, 90L, 30L, 90L,
                SYMBOL_DELL, 30L, 100L, 30L, 100L
        );

        sendEvent(epService, SYMBOL_IBM, 20L);
        sendEvent(epService, SYMBOL_IBM, 5L);
        sendEvent(epService, SYMBOL_IBM, 15L);
        sendEvent(epService, SYMBOL_IBM, 18L);
        assertEvents(listener, SYMBOL_IBM, 5L, 20L, 5L, 20L,
                SYMBOL_IBM, 5L, 18L, 5L, 18L
        );

        sendEvent(epService, SYMBOL_IBM, null);
        assertEvents(listener, SYMBOL_IBM, 5L, 18L, 5L, 18L,
                SYMBOL_IBM, 15L, 18L, 15L, 18L
        );

        sendEvent(epService, SYMBOL_IBM, null);
        assertEvents(listener, SYMBOL_IBM, 15L, 18L, 15L, 18L,
                SYMBOL_IBM, 18L, 18L, 18L, 18L
        );

        sendEvent(epService, SYMBOL_IBM, null);
        assertEvents(listener, SYMBOL_IBM, 18L, 18L, 18L, 18L,
                SYMBOL_IBM, null, null, null, null
        );
    }

    private void assertEvents(SupportUpdateListener listener, String symbolOld, Long minVolOld, Long maxVolOld, Long minDistVolOld, Long maxDistVolOld,
                              String symbolNew, Long minVolNew, Long maxVolNew, Long minDistVolNew, Long maxDistVolNew) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbolOld, oldData[0].get("symbol"));
        assertEquals(minVolOld, oldData[0].get("minVol"));
        assertEquals(maxVolOld, oldData[0].get("maxVol"));
        assertEquals(minDistVolOld, oldData[0].get("minDistVol"));
        assertEquals(maxDistVolOld, oldData[0].get("maxDistVol"));

        assertEquals(symbolNew, newData[0].get("symbol"));
        assertEquals(minVolNew, newData[0].get("minVol"));
        assertEquals(maxVolNew, newData[0].get("maxVol"));
        assertEquals(minDistVolNew, newData[0].get("minDistVol"));
        assertEquals(maxDistVolNew, newData[0].get("maxDistVol"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecAggregateMaxMinGroupBy.class);
}
