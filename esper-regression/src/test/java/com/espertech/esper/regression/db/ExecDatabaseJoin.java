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
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.SerializableObjectCopier;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Properties;

import static org.junit.Assert.*;

public class ExecDatabaseJoin implements RegressionExecution {
    private final static String ALL_FIELDS = "mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);

        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion3Stream(epService);
        runAssertionTimeBatchEPL(epService);
        runAssertion2HistoricalStar(epService);
        runAssertion2HistoricalStarInner(epService);
        runAssertionVariables(epService);
        runAssertionTimeBatchOM(epService);
        runAssertionTimeBatchCompile(epService);
        runAssertionInvalidSQL(epService);
        runAssertionInvalidBothHistorical(epService);
        runAssertionInvalidPropertyEvent(epService);
        runAssertionInvalidPropertyHistorical(epService);
        runAssertionInvalid1Stream(epService);
        runAssertionInvalidSubviews(epService);
        runAssertionStreamNamesAndRename(epService);
        runAssertionWithPattern(epService);
        runAssertionPropertyResolution(epService);
        runAssertionSimpleJoinLeft(epService);
        runAssertionRestartStatement(epService);
        runAssertionSimpleJoinRight(epService);
        runAssertionMySQLDatabaseConnection(epService);
    }

    private void runAssertion3Stream(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);

        String stmtText = "select * from SupportBean#lastevent sb, SupportBeanTwo#lastevent sbt, " +
                "sql:MyDB ['select myint from mytesttable'] as s1 " +
                "  where sb.theString = sbt.stringTwo and s1.myint = sbt.intPrimitiveTwo";

        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        assertFalse(statement.getStatementContext().isStatelessSelect());
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("T1", -1));

        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T2", 30));
        epService.getEPRuntime().sendEvent(new SupportBean("T2", -1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("T3", -1));
        epService.getEPRuntime().sendEvent(new SupportBeanTwo("T3", 40));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});

        statement.destroy();
    }

    private void runAssertionTimeBatchEPL(EPServiceProvider epService) {
        String stmtText = "select " + ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ALL_FIELDS + " from mytesttable \n\r where ${intPrimitive} = mytesttable.mybigint'] as s0," +
                SupportBean.class.getName() + "#time_batch(10 sec) as s1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        runtestTimeBatch(epService, stmt);
    }

    private void runAssertion2HistoricalStar(EPServiceProvider epService) {
        String[] fields = "intPrimitive,myint,myvarchar".split(",");
        String stmtText = "select intPrimitive, myint, myvarchar from " +
                SupportBean.class.getName() + "#keepall as s0, " +
                " sql:MyDB ['select myint from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s1," +
                " sql:MyDB ['select myvarchar from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s2 ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendSupportBeanEvent(epService, 6);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{6, 60, "F"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{6, 60, "F"}});

        sendSupportBeanEvent(epService, 9);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{9, 90, "I"});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{6, 60, "F"}, {9, 90, "I"}});

        sendSupportBeanEvent(epService, 20);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{6, 60, "F"}, {9, 90, "I"}});

        stmt.destroy();
    }

    private void runAssertion2HistoricalStarInner(EPServiceProvider epService) {
        String[] fields = "a,b,c,d".split(",");
        String stmtText = "select theString as a, intPrimitive as b, s1.myvarchar as c, s2.myvarchar as d from " +
                SupportBean.class.getName() + "#keepall as s0 " +
                " inner join " +
                " sql:MyDB ['select myvarchar from mytesttable where ${intPrimitive} <> mytesttable.mybigint'] as s1 " +
                " on s1.myvarchar=s0.theString " +
                " inner join " +
                " sql:MyDB ['select myvarchar from mytesttable where ${intPrimitive} <> mytesttable.myint'] as s2 " +
                " on s2.myvarchar=s0.theString ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 3, "B", "B"});

        epService.getEPRuntime().sendEvent(new SupportBean("D", 4));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionVariables(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().createEPL("create variable int queryvar");
        epService.getEPAdministrator().createEPL("on SupportBean set queryvar=intPrimitive");

        String stmtText = "select myint from " +
                " sql:MyDB ['select myint from mytesttable where ${queryvar} = mytesttable.mybigint'] as s0, " +
                "A#keepall as s1";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportBeanEvent(epService, 5);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));

        EventBean received = listener.assertOneGetNewAndReset();
        assertEquals(50, received.get("myint"));
        stmt.destroy();

        stmtText = "select myint from " +
                "A#keepall as s1, " +
                "sql:MyDB ['select myint from mytesttable where ${queryvar} = mytesttable.mybigint'] as s0";

        stmt = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportBeanEvent(epService, 6);
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));

        received = listener.assertOneGetNewAndReset();
        assertEquals(60, received.get("myint"));

        stmt.destroy();
    }

    private void runAssertionTimeBatchOM(EPServiceProvider epService) throws Exception {
        String[] fields = ALL_FIELDS.split(",");
        String sql = "select " + ALL_FIELDS + " from mytesttable where ${intPrimitive} = mytesttable.mybigint";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create(fields));
        FromClause fromClause = FromClause.create(
                SQLStream.create("MyDB", sql, "s0"),
                FilterStream.create(SupportBean.class.getName(), "s1").addView(View.create("time_batch", Expressions.constant(10))
                ));
        model.setFromClause(fromClause);
        SerializableObjectCopier.copy(model);

        assertEquals("select mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal from sql:MyDB[\"select mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal from mytesttable where ${intPrimitive} = mytesttable.mybigint\"] as s0, " + SupportBean.class.getName() + "#time_batch(10) as s1",
                model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        runtestTimeBatch(epService, stmt);
        epService.getEPAdministrator().createEPL(model.toEPL()).destroy();
    }

    private void runAssertionTimeBatchCompile(EPServiceProvider epService) throws Exception {
        String stmtText = "select " + ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ALL_FIELDS + " from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s0," +
                SupportBean.class.getName() + "#time_batch(10 sec) as s1";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        SerializableObjectCopier.copy(model);
        EPStatement stmt = epService.getEPAdministrator().create(model);
        runtestTimeBatch(epService, stmt);
    }

    private void runtestTimeBatch(EPServiceProvider epService, EPStatement statement) {
        String[] fields = new String[]{"myint"};
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, null);

        sendSupportBeanEvent(epService, 10);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{100}});

        sendSupportBeanEvent(epService, 5);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{100}, {50}});

        sendSupportBeanEvent(epService, 2);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{100}, {50}, {20}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EventBean[] received = listener.getLastNewData();
        assertEquals(3, received.length);
        assertEquals(100, received[0].get("myint"));
        assertEquals(50, received[1].get("myint"));
        assertEquals(20, received[2].get("myint"));

        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, null);

        sendSupportBeanEvent(epService, 9);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{90}});

        sendSupportBeanEvent(epService, 8);
        EPAssertionUtil.assertPropsPerRow(statement.iterator(), fields, new Object[][]{{90}, {80}});

        statement.destroy();
    }

    private void runAssertionInvalidSQL(EPServiceProvider epService) {
        String stmtText = "select myvarchar from " +
                " sql:MyDB ['select mychar,, from mytesttable where '] as s0," +
                SupportBeanComplexProps.class.getName() + " as s1";

        SupportMessageAssertUtil.tryInvalid(epService, stmtText,
                "Error starting statement: Error in statement 'select mychar,, from mytesttable where ', failed to obtain result metadata, consider turning off metadata interrogation via configuration, please check the statement, reason: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near ' from mytesttable where' at line 1");
    }

    private void runAssertionInvalidBothHistorical(EPServiceProvider epService) {
        String sqlOne = "sql:MyDB ['select myvarchar from mytesttable where ${mychar} = mytesttable.mybigint']";
        String sqlTwo = "sql:MyDB ['select mychar from mytesttable where ${myvarchar} = mytesttable.mybigint']";
        String stmtText = "select s0.myvarchar as s0Name, s1.mychar as s1Name from " +
                sqlOne + " as s0, " + sqlTwo + "  as s1";

        try {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Circular dependency detected between historical streams [select s0.myvarchar as s0Name, s1.mychar as s1Name from sql:MyDB ['select myvarchar from mytesttable where ${mychar} = mytesttable.mybigint'] as s0, sql:MyDB ['select mychar from mytesttable where ${myvarchar} = mytesttable.mybigint']  as s1]", ex.getMessage());
        }
    }

    private void runAssertionInvalidPropertyEvent(EPServiceProvider epService) {
        String stmtText = "select myvarchar from " +
                " sql:MyDB ['select mychar from mytesttable where ${s1.xxx[0]} = mytesttable.mybigint'] as s0," +
                SupportBeanComplexProps.class.getName() + " as s1";

        try {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate from-clause database-access parameter expression 's1.xxx[0]': Failed to resolve property 's1.xxx[0]' to a stream or nested property in a stream");
        }

        stmtText = "select myvarchar from " +
                " sql:MyDB ['select mychar from mytesttable where ${} = mytesttable.mybigint'] as s0," +
                SupportBeanComplexProps.class.getName() + " as s1";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText,
                "Missing expression within ${...} in SQL statement [");
    }

    private void runAssertionInvalidPropertyHistorical(EPServiceProvider epService) {
        String stmtText = "select myvarchar from " +
                " sql:MyDB ['select myvarchar from mytesttable where ${myvarchar} = mytesttable.mybigint'] as s0," +
                SupportBeanComplexProps.class.getName() + " as s1";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText,
                "Error starting statement: Invalid expression 'myvarchar' resolves to the historical data itself");
    }

    private void runAssertionInvalid1Stream(EPServiceProvider epService) {
        String sql = "sql:MyDB ['select myvarchar, mybigint from mytesttable where ${mybigint} = myint']";
        String stmtText = "select myvarchar as s0Name from " + sql + " as s0";

        try {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Invalid expression 'mybigint' resolves to the historical data itself [select myvarchar as s0Name from sql:MyDB ['select myvarchar, mybigint from mytesttable where ${mybigint} = myint'] as s0]", ex.getMessage());
        }
    }

    private void runAssertionInvalidSubviews(EPServiceProvider epService) {
        String sql = "sql:MyDB ['select myvarchar from mytesttable where ${intPrimitive} = mytesttable.myint']#time(30 sec)";
        String stmtText = "select myvarchar as s0Name from " +
                sql + " as s0, " + SupportBean.class.getName() + " as s1";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText,
                "Error starting statement: Historical data joins do not allow views onto the data, view 'time' is not valid in this context [select myvarchar as s0Name from sql:MyDB [");
    }

    private void runAssertionStreamNamesAndRename(EPServiceProvider epService) {
        String stmtText = "select s1.a as mybigint, " +
                " s1.b as myint," +
                " s1.c as myvarchar," +
                " s1.d as mychar," +
                " s1.e as mybool," +
                " s1.f as mynumeric," +
                " s1.g as mydecimal," +
                " s1.h as mydouble," +
                " s1.i as myreal " +
                " from " + SupportBean_S0.class.getName() + " as s0," +
                " sql:MyDB ['select mybigint as a, " +
                " myint as b," +
                " myvarchar as c," +
                " mychar as d," +
                " mybool as e," +
                " mynumeric as f," +
                " mydecimal as g," +
                " mydouble as h," +
                " myreal as i " +
                "from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEventS0(epService, 1);
        assertReceived(listener, 1, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2, 1.3);
    }

    private void runAssertionWithPattern(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String stmtText = "select mychar from " +
                " sql:MyDB ['select mychar from mytesttable where mytesttable.mybigint = 2'] as s0," +
                " pattern [every timer:interval(5 sec) ]";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        assertEquals("Y", listener.assertOneGetNewAndReset().get("mychar"));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(9999));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        assertEquals("Y", listener.assertOneGetNewAndReset().get("mychar"));

        // with variable
        epService.getEPAdministrator().createEPL("create variable long VarLastTimestamp = 0");
        String epl = "@Name('Poll every 5 seconds') insert into PollStream" +
                " select * from pattern[every timer:interval(5 sec)]," +
                " sql:MyDB ['select mychar from mytesttable where mytesttable.mybigint > ${VarLastTimestamp}'] as s0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        epService.getEPAdministrator().create(model);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPropertyResolution(EPServiceProvider epService) {
        String stmtText = "select " + ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ALL_FIELDS + " from mytesttable where ${s1.arrayProperty[0]} = mytesttable.mybigint'] as s0," +
                SupportBeanComplexProps.class.getName() + " as s1";
        // s1.arrayProperty[0] returns 10 for that bean

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        assertReceived(listener, 10, 100, "J", "P", true, null, new BigDecimal(1000), 10.2, 10.3);

        statement.destroy();
    }

    private void runAssertionSimpleJoinLeft(EPServiceProvider epService) {
        String stmtText = "select " + ALL_FIELDS + " from " +
                SupportBean_S0.class.getName() + " as s0," +
                " sql:MyDB ['select " + ALL_FIELDS + " from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEventS0(epService, 1);
        assertReceived(listener, 1, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2, 1.3);

        statement.destroy();
    }

    private void runAssertionRestartStatement(EPServiceProvider epService) {
        String stmtText = "select mychar from " +
                SupportBean_S0.class.getName() + " as s0," +
                " sql:MyDB ['select mychar from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // Too many connections unless the stop actually relieves them
        for (int i = 0; i < 100; i++) {
            statement.stop();

            sendEventS0(epService, 1);
            assertFalse(listener.isInvoked());

            statement.start();
            sendEventS0(epService, 1);
            assertEquals("Z", listener.assertOneGetNewAndReset().get("mychar"));
        }

        statement.destroy();
    }

    private void runAssertionSimpleJoinRight(EPServiceProvider epService) {
        String stmtText = "select " + ALL_FIELDS + " from " +
                " sql:MyDB ['select " + ALL_FIELDS + " from mytesttable where ${id} = mytesttable.mybigint'] as s0," +
                SupportBean_S0.class.getName() + " as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        EventType eventType = statement.getEventType();
        assertEquals(Long.class, eventType.getPropertyType("mybigint"));
        assertEquals(Integer.class, eventType.getPropertyType("myint"));
        assertEquals(String.class, eventType.getPropertyType("myvarchar"));
        assertEquals(String.class, eventType.getPropertyType("mychar"));
        assertEquals(Boolean.class, eventType.getPropertyType("mybool"));
        assertEquals(BigDecimal.class, eventType.getPropertyType("mynumeric"));
        assertEquals(BigDecimal.class, eventType.getPropertyType("mydecimal"));
        assertEquals(Double.class, eventType.getPropertyType("mydouble"));
        assertEquals(Double.class, eventType.getPropertyType("myreal"));

        sendEventS0(epService, 1);
        assertReceived(listener, 1, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2, 1.3);

        statement.destroy();
    }

    private void assertReceived(SupportUpdateListener listener, long mybigint, int myint, String myvarchar, String mychar, boolean mybool, BigDecimal mynumeric, BigDecimal mydecimal, Double mydouble, Double myreal) {
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertReceived(theEvent, mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal);
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
        Object r = theEvent.get("myreal");
        assertEquals(myreal, theEvent.get("myreal"));
    }

    private void runAssertionMySQLDatabaseConnection(EPServiceProvider epService) throws Exception {
        Class.forName(SupportDatabaseService.DRIVER).newInstance();
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(SupportDatabaseService.FULLURL);
        } catch (SQLException ex) {
            // handle any errors
            throw ex;
        }
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM mytesttable");
        rs.close();
        stmt.close();
        conn.close();

        /**
         * Using JNDI to get a connectiong (for J2EE containers or outside)
         */
        /**
         InitialContext ctx = new InitialContext();
         DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/MySQLDB");
         Connection connection = ds.getConnection();
         */
    }

    private void sendEventS0(EPServiceProvider epService, int id) {
        SupportBean_S0 bean = new SupportBean_S0(id);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
