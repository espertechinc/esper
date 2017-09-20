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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ExecQuerytypeIterator implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPatternNoWindow(epService);
        runAssertionPatternWithWindow(epService);
        runAssertionOrderByWildcard(epService);
        runAssertionOrderByProps(epService);
        runAssertionFilter(epService);
        runAssertionRowPerGroupOrdered(epService);
        runAssertionRowPerGroup(epService);
        runAssertionRowPerGroupHaving(epService);
        runAssertionRowPerGroupComplex(epService);
        runAssertionAggregateGroupedOrdered(epService);
        runAssertionAggregateGrouped(epService);
        runAssertionAggregateGroupedHaving(epService);
        runAssertionRowPerEvent(epService);
        runAssertionRowPerEventOrdered(epService);
        runAssertionRowPerEventHaving(epService);
        runAssertionRowForAll(epService);
        runAssertionRowForAllHaving(epService);
    }

    private void runAssertionPatternNoWindow(EPServiceProvider epService) {
        // Test for Esper-115
        String cepStatementString = "@IterableUnbound select * from pattern " +
                "[every ( addressInfo = " + SupportBean.class.getName() + "(theString='address') " +
                "-> txnWD = " + SupportBean.class.getName() + "(theString='txn') ) ] " +
                "where addressInfo.intBoxed = txnWD.intBoxed";
        EPStatement epStatement = epService.getEPAdministrator().createEPL(cepStatementString);

        SupportBean myEventBean1 = new SupportBean();
        myEventBean1.setTheString("address");
        myEventBean1.setIntBoxed(9001);
        epService.getEPRuntime().sendEvent(myEventBean1);
        assertFalse(epStatement.iterator().hasNext());

        SupportBean myEventBean2 = new SupportBean();
        myEventBean2.setTheString("txn");
        myEventBean2.setIntBoxed(9001);
        epService.getEPRuntime().sendEvent(myEventBean2);
        assertTrue(epStatement.iterator().hasNext());

        Iterator<EventBean> itr = epStatement.iterator();
        EventBean theEvent = itr.next();
        assertEquals(myEventBean1, theEvent.get("addressInfo"));
        assertEquals(myEventBean2, theEvent.get("txnWD"));

        epStatement.destroy();
    }

    private void runAssertionPatternWithWindow(EPServiceProvider epService) {
        String cepStatementString = "select * from pattern " +
                "[every ( addressInfo = " + SupportBean.class.getName() + "(theString='address') " +
                "-> txnWD = " + SupportBean.class.getName() + "(theString='txn') ) ]#lastevent " +
                "where addressInfo.intBoxed = txnWD.intBoxed";
        EPStatement epStatement = epService.getEPAdministrator().createEPL(cepStatementString);

        SupportBean myEventBean1 = new SupportBean();
        myEventBean1.setTheString("address");
        myEventBean1.setIntBoxed(9001);
        epService.getEPRuntime().sendEvent(myEventBean1);

        SupportBean myEventBean2 = new SupportBean();
        myEventBean2.setTheString("txn");
        myEventBean2.setIntBoxed(9001);
        epService.getEPRuntime().sendEvent(myEventBean2);

        Iterator<EventBean> itr = epStatement.iterator();
        EventBean theEvent = itr.next();
        assertEquals(myEventBean1, theEvent.get("addressInfo"));
        assertEquals(myEventBean2, theEvent.get("txnWD"));

        epStatement.destroy();
    }

    private void runAssertionOrderByWildcard(EPServiceProvider epService) {
        String stmtText = "select * from " + SupportMarketDataBean.class.getName() + "#length(5) order by symbol, volume";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        Object eventOne = sendEvent(epService, "SYM", 1);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventOne}, stmt.iterator());

        Object eventTwo = sendEvent(epService, "OCC", 2);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventOne}, stmt.iterator());

        Object eventThree = sendEvent(epService, "TOC", 3);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventOne, eventThree}, stmt.iterator());

        Object eventFour = sendEvent(epService, "SYM", 0);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventFour, eventOne, eventThree}, stmt.iterator());

        Object eventFive = sendEvent(epService, "SYM", 10);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventFour, eventOne, eventFive, eventThree}, stmt.iterator());

        Object eventSix = sendEvent(epService, "SYM", 4);
        EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventFour, eventSix, eventFive, eventThree}, stmt.iterator());

        stmt.destroy();
    }

    private void runAssertionOrderByProps(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "volume"};
        String stmtText = "select symbol, volume from " + SupportMarketDataBean.class.getName() + "#length(3) order by symbol, volume";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 1L}});

        sendEvent(epService, "OCC", 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"OCC", 2L}, {"SYM", 1L}});

        sendEvent(epService, "SYM", 0);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"OCC", 2L}, {"SYM", 0L}, {"SYM", 1L}});

        sendEvent(epService, "OCC", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"OCC", 2L}, {"OCC", 3L}, {"SYM", 0L}});

        stmt.destroy();
    }

    private void runAssertionFilter(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "vol"};
        String stmtText = "select symbol, volume * 10 as vol from " + SupportMarketDataBean.class.getName() + "#length(5)" +
                " where volume < 0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        assertFalse(stmt.iterator().hasNext());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendEvent(epService, "SYM", -1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -10L}});

        sendEvent(epService, "SYM", -6);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -10L}, {"SYM", -60L}});

        sendEvent(epService, "SYM", 1);
        sendEvent(epService, "SYM", 16);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -10L}, {"SYM", -60L}});

        sendEvent(epService, "SYM", -9);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -10L}, {"SYM", -60L}, {"SYM", -90L}});

        sendEvent(epService, "SYM", 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -60L}, {"SYM", -90L}});

        sendEvent(epService, "SYM", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -90L}});

        sendEvent(epService, "SYM", 4);
        sendEvent(epService, "SYM", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -90L}});
        sendEvent(epService, "SYM", 6);
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void runAssertionRowPerGroupOrdered(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumVol"};
        String stmtText = "select symbol, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol " +
                "order by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 100L}});

        sendEvent(epService, "OCC", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"OCC", 5L}, {"SYM", 100L}});

        sendEvent(epService, "SYM", 10);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"OCC", 5L}, {"SYM", 110L}});

        sendEvent(epService, "OCC", 6);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"OCC", 11L}, {"SYM", 110L}});

        sendEvent(epService, "ATB", 8);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"ATB", 8L}, {"OCC", 11L}, {"SYM", 110L}});

        sendEvent(epService, "ATB", 7);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"ATB", 15L}, {"OCC", 11L}, {"SYM", 10L}});

        stmt.destroy();
    }

    private void runAssertionRowPerGroup(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumVol"};
        String stmtText = "select symbol, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 100L}});

        sendEvent(epService, "SYM", 10);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 110L}});

        sendEvent(epService, "TAC", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 110L}, {"TAC", 1L}});

        sendEvent(epService, "SYM", 11);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 121L}, {"TAC", 1L}});

        sendEvent(epService, "TAC", 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 121L}, {"TAC", 3L}});

        sendEvent(epService, "OCC", 55);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 21L}, {"TAC", 3L}, {"OCC", 55L}});

        sendEvent(epService, "OCC", 4);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"TAC", 3L}, {"SYM", 11L}, {"OCC", 59L}});

        sendEvent(epService, "OCC", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 11L}, {"TAC", 2L}, {"OCC", 62L}});

        stmt.destroy();
    }

    private void runAssertionRowPerGroupHaving(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumVol"};
        String stmtText = "select symbol, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol having sum(volume) > 10";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 100L}});

        sendEvent(epService, "SYM", 5);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 105L}});

        sendEvent(epService, "TAC", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 105L}});

        sendEvent(epService, "SYM", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 108L}});

        sendEvent(epService, "TAC", 12);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 108L}, {"TAC", 13L}});

        sendEvent(epService, "OCC", 55);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"TAC", 13L}, {"OCC", 55L}});

        sendEvent(epService, "OCC", 4);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"TAC", 13L}, {"OCC", 59L}});

        sendEvent(epService, "OCC", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"TAC", 12L}, {"OCC", 62L}});

        stmt.destroy();
    }

    private void runAssertionRowPerGroupComplex(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "msg"};
        String stmtText = "insert into Cutoff " +
                "select symbol, (String.valueOf(count(*)) || 'x1000.0') as msg " +
                "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#length(1) " +
                "where price - volume >= 1000.0 group by symbol having count(*) = 1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("SYM", -1, -1L, null));
        assertFalse(stmt.iterator().hasNext());

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("SYM", 100000d, 0L, null));
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", "1x1000.0"}});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("SYM", 1d, 1L, null));
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void runAssertionAggregateGroupedOrdered(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "price", "sumVol"};
        String stmtText = "select symbol, price, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol " +
                "order by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", -1, 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -1d, 100L}});

        sendEvent(epService, "TAC", -2, 12);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 12L}});

        sendEvent(epService, "TAC", -3, 13);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

        sendEvent(epService, "SYM", -4, 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"SYM", -4d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

        sendEvent(epService, "OCC", -5, 99);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"OCC", -5d, 99L}, {"SYM", -1d, 101L}, {"SYM", -4d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

        sendEvent(epService, "TAC", -6, 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"OCC", -5d, 99L}, {"SYM", -4d, 1L}, {"TAC", -2d, 27L}, {"TAC", -3d, 27L}, {"TAC", -6d, 27L}});

        stmt.destroy();
    }

    private void runAssertionAggregateGrouped(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "price", "sumVol"};
        String stmtText = "select symbol, price, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", -1, 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -1d, 100L}});

        sendEvent(epService, "TAC", -2, 12);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 12L}});

        sendEvent(epService, "TAC", -3, 13);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

        sendEvent(epService, "SYM", -4, 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}});

        sendEvent(epService, "OCC", -5, 99);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}, {"OCC", -5d, 99L}});

        sendEvent(epService, "TAC", -6, 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"TAC", -2d, 27L}, {"TAC", -3d, 27L}, {"SYM", -4d, 1L}, {"OCC", -5d, 99L}, {"TAC", -6d, 27L}});

        stmt.destroy();
    }

    private void runAssertionAggregateGroupedHaving(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "price", "sumVol"};
        String stmtText = "select symbol, price, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol having sum(volume) > 20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", -1, 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", -1d, 100L}});

        sendEvent(epService, "TAC", -2, 12);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}});

        sendEvent(epService, "TAC", -3, 13);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

        sendEvent(epService, "SYM", -4, 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}});

        sendEvent(epService, "OCC", -5, 99);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}, {"OCC", -5d, 99L}});

        sendEvent(epService, "TAC", -6, 2);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"TAC", -2d, 27L}, {"TAC", -3d, 27L}, {"OCC", -5d, 99L}, {"TAC", -6d, 27L}});

        stmt.destroy();
    }

    private void runAssertionRowPerEvent(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumVol"};
        String stmtText = "select symbol, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 100L}});

        sendEvent(epService, "TAC", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 101L}, {"TAC", 101L}});

        sendEvent(epService, "MOV", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 104L}, {"TAC", 104L}, {"MOV", 104L}});

        sendEvent(epService, "SYM", 10);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"TAC", 14L}, {"MOV", 14L}, {"SYM", 14L}});

        stmt.destroy();
    }

    private void runAssertionRowPerEventOrdered(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumVol"};
        String stmtText = "select irstream symbol, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                " order by symbol asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 100L}});

        sendEvent(epService, "TAC", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 101L}, {"TAC", 101L}});

        sendEvent(epService, "MOV", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"MOV", 104L}, {"SYM", 104L}, {"TAC", 104L}});

        sendEvent(epService, "SYM", 10);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"MOV", 14L}, {"SYM", 14L}, {"TAC", 14L}});

        stmt.destroy();
    }

    private void runAssertionRowPerEventHaving(EPServiceProvider epService) {
        String[] fields = new String[]{"symbol", "sumVol"};
        String stmtText = "select symbol, sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) having sum(volume) > 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "SYM", 100);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, "TAC", 1);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 101L}, {"TAC", 101L}});

        sendEvent(epService, "MOV", 3);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"SYM", 104L}, {"TAC", 104L}, {"MOV", 104L}});

        sendEvent(epService, "SYM", 10);
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void runAssertionRowForAll(EPServiceProvider epService) {
        String[] fields = new String[]{"sumVol"};
        String stmtText = "select sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{null}});

        sendEvent(epService, 100);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{100L}});

        sendEvent(epService, 50);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{150L}});

        sendEvent(epService, 25);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{175L}});

        sendEvent(epService, 10);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{85L}});

        stmt.destroy();
    }

    private void runAssertionRowForAllHaving(EPServiceProvider epService) {
        String[] fields = new String[]{"sumVol"};
        String stmtText = "select sum(volume) as sumVol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) having sum(volume) > 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, 100);
        assertFalse(stmt.iterator().hasNext());

        sendEvent(epService, 50);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{150L}});

        sendEvent(epService, 25);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{175L}});

        sendEvent(epService, 10);
        assertFalse(stmt.iterator().hasNext());

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price, long volume) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean(symbol, price, volume, null));
    }

    private SupportMarketDataBean sendEvent(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, null);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private void sendEvent(EPServiceProvider epService, long volume) {
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("SYM", 0, volume, null));
    }
}
