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

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecAggregateMedianAndDeviation implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionStmt(epService);
        runAssertionStmtJoin_OM(epService);
        runAssertionStmtJoin(epService);
        runAssertionStmt(epService);
    }

    private void runAssertionStmt(EPServiceProvider epService) {
        String epl = "select irstream symbol," +
                "median(all price) as myMedian," +
                "median(distinct price) as myDistMedian," +
                "stddev(all price) as myStdev," +
                "avedev(all price) as myAvedev " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionStmt(epService, listener, stmt);

        // Test NaN sensitivity
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select stddev(price) as val from " + SupportMarketDataBean.class.getName() + "#length(3)");
        stmt.addListener(listener);

        sendEvent(epService, "A", Double.NaN);
        sendEvent(epService, "B", Double.NaN);
        sendEvent(epService, "C", Double.NaN);
        sendEvent(epService, "D", 1d);
        sendEvent(epService, "E", 2d);
        listener.reset();
        sendEvent(epService, "F", 3d);
        Double result = (Double) listener.assertOneGetNewAndReset().get("val");
        assertTrue(result.isNaN());

        stmt.destroy();
    }

    private void runAssertionStmtJoin_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol")
                .add(Expressions.median("price"), "myMedian")
                .add(Expressions.medianDistinct("price"), "myDistMedian")
                .add(Expressions.stddev("price"), "myStdev")
                .add(Expressions.avedev("price"), "myAvedev")
                .streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
        );
        FromClause fromClause = FromClause.create(
                FilterStream.create(SupportBeanString.class.getName(), "one").addView(View.create("length", Expressions.constant(100))),
                FilterStream.create(SupportMarketDataBean.class.getName(), "two").addView(View.create("length", Expressions.constant(5))));
        model.setFromClause(fromClause);
        model.setWhereClause(Expressions.and().add(
                Expressions.or()
                        .add(Expressions.eq("symbol", "DELL"))
                        .add(Expressions.eq("symbol", "IBM"))
                        .add(Expressions.eq("symbol", "GE"))
        )
                .add(Expressions.eqProperty("one.theString", "two.symbol")));
        model.setGroupByClause(GroupByClause.create("symbol"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String epl = "select irstream symbol, " +
                "median(price) as myMedian, " +
                "median(distinct price) as myDistMedian, " +
                "stddev(price) as myStdev, " +
                "avedev(price) as myAvedev " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where (symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\") " +
                "and one.theString=two.symbol " +
                "group by symbol";
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertionStmt(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionStmtJoin(EPServiceProvider epService) {
        String epl = "select irstream symbol," +
                "median(price) as myMedian," +
                "median(distinct price) as myDistMedian," +
                "stddev(price) as myStdev," +
                "avedev(price) as myAvedev " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));
        epService.getEPRuntime().sendEvent(new SupportBeanString("AAA"));

        tryAssertionStmt(epService, listener, stmt);

        stmt.destroy();
    }

    private void tryAssertionStmt(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myMedian"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myDistMedian"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myStdev"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("myAvedev"));

        sendEvent(epService, SYMBOL_DELL, 10);
        assertEvents(listener, SYMBOL_DELL,
                null, null, null, null,
                10d, 10d, null, 0d);

        sendEvent(epService, SYMBOL_DELL, 20);
        assertEvents(listener, SYMBOL_DELL,
                10d, 10d, null, 0d,
                15d, 15d, 7.071067812d, 5d);

        sendEvent(epService, SYMBOL_DELL, 20);
        assertEvents(listener, SYMBOL_DELL,
                15d, 15d, 7.071067812d, 5d,
                20d, 15d, 5.773502692, 4.444444444444444);

        sendEvent(epService, SYMBOL_DELL, 90);
        assertEvents(listener, SYMBOL_DELL,
                20d, 15d, 5.773502692, 4.444444444444444,
                20d, 20d, 36.96845502d, 27.5d);

        sendEvent(epService, SYMBOL_DELL, 5);
        assertEvents(listener, SYMBOL_DELL,
                20d, 20d, 36.96845502d, 27.5d,
                20d, 15d, 34.71310992d, 24.4d);

        sendEvent(epService, SYMBOL_DELL, 90);
        assertEvents(listener, SYMBOL_DELL,
                20d, 15d, 34.71310992d, 24.4d,
                20d, 20d, 41.53311931d, 36d);

        sendEvent(epService, SYMBOL_DELL, 30);
        assertEvents(listener, SYMBOL_DELL,
                20d, 20d, 41.53311931d, 36d,
                30d, 25d, 40.24922359d, 34.4d);
    }

    private void assertEvents(SupportUpdateListener listener, String symbol,
                              Double oldMedian, Double oldDistMedian, Double oldStdev, Double oldAvedev,
                              Double newMedian, Double newDistMedian, Double newStdev, Double newAvedev
    ) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbol, oldData[0].get("symbol"));
        assertEquals("oldData.myMedian wrong", oldMedian, oldData[0].get("myMedian"));
        assertEquals("oldData.myDistMedian wrong", oldDistMedian, oldData[0].get("myDistMedian"));
        assertEquals("oldData.myAvedev wrong", oldAvedev, oldData[0].get("myAvedev"));

        Double oldStdevResult = (Double) oldData[0].get("myStdev");
        if (oldStdevResult == null) {
            assertNull(oldStdev);
        } else {
            assertEquals("oldData.myStdev wrong", Math.round(oldStdev * 1000), Math.round(oldStdevResult * 1000));
        }

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals("newData.myMedian wrong", newMedian, newData[0].get("myMedian"));
        assertEquals("newData.myDistMedian wrong", newDistMedian, newData[0].get("myDistMedian"));
        assertEquals("newData.myAvedev wrong", newAvedev, newData[0].get("myAvedev"));

        Double newStdevResult = (Double) newData[0].get("myStdev");
        if (newStdevResult == null) {
            assertNull(newStdev);
        } else {
            assertEquals("newData.myStdev wrong", Math.round(newStdev * 1000), Math.round(newStdevResult * 1000));
        }

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }
}
