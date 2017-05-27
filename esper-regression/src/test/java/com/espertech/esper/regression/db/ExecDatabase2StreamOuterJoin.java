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
package com.espertech.esper.regression.db;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecDatabase2StreamOuterJoin implements RegressionExecution {
    private final static String ALL_FIELDS = "mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOuterJoinLeftS0(epService);
        runAssertionOuterJoinRightS1(epService);
        runAssertionOuterJoinFullS0(epService);
        runAssertionOuterJoinFullS1(epService);
        runAssertionOuterJoinRightS0(epService);
        runAssertionOuterJoinLeftS1(epService);
        runAssertionLeftOuterJoinOnFilter(epService);
        runAssertionRightOuterJoinOnFilter(epService);
        runAssertionOuterJoinReversedOnFilter(epService);
    }

    private void runAssertionOuterJoinLeftS0(EPServiceProvider epService) {
        String stmtText = "select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                SupportBean.class.getName() + " as s0 left outer join " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 on intPrimitive = mybigint";
        tryOuterJoinResult(epService, stmtText);
    }

    private void runAssertionOuterJoinRightS1(EPServiceProvider epService) {
        String stmtText = "select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 right outer join " +
                SupportBean.class.getName() + " as s0 on intPrimitive = mybigint";
        tryOuterJoinResult(epService, stmtText);
    }

    private void runAssertionOuterJoinFullS0(EPServiceProvider epService) {
        String stmtText = "select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 full outer join " +
                SupportBean.class.getName() + " as s0 on intPrimitive = mybigint";
        tryOuterJoinResult(epService, stmtText);
    }

    private void runAssertionOuterJoinFullS1(EPServiceProvider epService) {
        String stmtText = "select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                SupportBean.class.getName() + " as s0 full outer join " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 on intPrimitive = mybigint";
        tryOuterJoinResult(epService, stmtText);
    }

    private void runAssertionOuterJoinRightS0(EPServiceProvider epService) {
        String stmtText = "select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                SupportBean.class.getName() + " as s0 right outer join " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 on intPrimitive = mybigint";
        tryOuterJoinNoResult(epService, stmtText);
    }

    private void runAssertionOuterJoinLeftS1(EPServiceProvider epService) {
        String stmtText = "select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 left outer join " +
                SupportBean.class.getName() + " as s0 on intPrimitive = mybigint";
        tryOuterJoinNoResult(epService, stmtText);
    }

    private void runAssertionLeftOuterJoinOnFilter(EPServiceProvider epService) {
        String[] fields = "MyInt,myint".split(",");
        String stmtText = "@IterableUnbound select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                SupportBean.class.getName() + " as s0 " +
                " left outer join " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 " +
                "on theString = myvarchar";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, null);

        // Result as the SQL query returns 1 row and therefore the on-clause filters it out, but because of left out still getting a row
        sendEvent(epService, 1, "xxx");
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(1, received.get("MyInt"));
        assertReceived(received, null, null, null, null, null, null, null, null, null);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{1, null}});

        // Result as the SQL query returns 0 rows
        sendEvent(epService, -1, "xxx");
        received = listener.assertOneGetNewAndReset();
        assertEquals(-1, received.get("MyInt"));
        assertReceived(received, null, null, null, null, null, null, null, null, null);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{-1, null}});

        sendEvent(epService, 2, "B");
        received = listener.assertOneGetNewAndReset();
        assertEquals(2, received.get("MyInt"));
        assertReceived(received, 2L, 20, "B", "Y", false, new BigDecimal(100), new BigDecimal(200), 2.2d, 2.3d);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{2, 20}});

        statement.destroy();
    }

    private void runAssertionRightOuterJoinOnFilter(EPServiceProvider epService) {
        String[] fields = "MyInt,myint".split(",");
        String stmtText = "@IterableUnbound select s0.intPrimitive as MyInt, " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ExecDatabase2StreamOuterJoin.ALL_FIELDS + " from mytesttable where ${s0.intPrimitive} = mytesttable.mybigint'] as s1 right outer join " +
                SupportBean.class.getName() + " as s0 on theString = myvarchar";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, null);

        // No result as the SQL query returns 1 row and therefore the on-clause filters it out
        sendEvent(epService, 1, "xxx");
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(1, received.get("MyInt"));
        assertReceived(received, null, null, null, null, null, null, null, null, null);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{1, null}});

        // Result as the SQL query returns 0 rows
        sendEvent(epService, -1, "xxx");
        received = listener.assertOneGetNewAndReset();
        assertEquals(-1, received.get("MyInt"));
        assertReceived(received, null, null, null, null, null, null, null, null, null);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{-1, null}});

        sendEvent(epService, 2, "B");
        received = listener.assertOneGetNewAndReset();
        assertEquals(2, received.get("MyInt"));
        assertReceived(received, 2L, 20, "B", "Y", false, new BigDecimal(100), new BigDecimal(200), 2.2d, 2.3d);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{2, 20}});

        statement.destroy();
    }

    private void runAssertionOuterJoinReversedOnFilter(EPServiceProvider epService) {
        String[] fields = "MyInt,MyVarChar".split(",");
        String stmtText = "select s0.intPrimitive as MyInt, MyVarChar from " +
                SupportBean.class.getName() + "#keepall as s0 " +
                " right outer join " +
                " sql:MyDB ['select myvarchar MyVarChar from mytesttable'] as s1 " +
                "on theString = MyVarChar";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, null);

        // No result as the SQL query returns 1 row and therefore the on-clause filters it out
        sendEvent(epService, 1, "xxx");
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(statement.iterator(), fields, null);

        sendEvent(epService, -1, "A");
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(-1, received.get("MyInt"));
        assertEquals("A", received.get("MyVarChar"));
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{-1, "A"}});

        statement.destroy();
    }

    private void tryOuterJoinNoResult(EPServiceProvider epService, String statementText) {
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, 2);
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(2, received.get("MyInt"));
        assertReceived(received, 2L, 20, "B", "Y", false, new BigDecimal(100), new BigDecimal(200), 2.2d, 2.3d);

        sendEvent(epService, 11);
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void tryOuterJoinResult(EPServiceProvider epService, String statementText) {
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, 1);
        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(1, received.get("MyInt"));
        assertReceived(received, 1L, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2d, 1.3d);

        sendEvent(epService, 11);
        received = listener.assertOneGetNewAndReset();
        assertEquals(11, received.get("MyInt"));
        assertReceived(received, null, null, null, null, null, null, null, null, null);

        statement.destroy();
    }

    private void assertReceived(EventBean theEvent, Long mybigint, Integer myint, String myvarchar, String mychar, Boolean mybool, BigDecimal mynumeric, BigDecimal mydecimal, Double mydouble, Double myreal) {
        assertEquals(mybigint, theEvent.get("mybigint"));
        assertEquals(myint, theEvent.get("myint"));
        assertEquals(myvarchar, theEvent.get("myvarchar"));
        assertEquals(mychar, theEvent.get("mychar"));
        assertEquals(mybool, theEvent.get("mybool"));
        assertEquals(mynumeric, theEvent.get("mynumeric"));
        assertEquals(mydecimal, theEvent.get("mydecimal"));
        assertEquals(mydouble, theEvent.get("mydouble"));
        assertEquals(myreal, theEvent.get("myreal"));
    }

    private void sendEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEvent(EPServiceProvider epService, int intPrimitive, String theString) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }
}
