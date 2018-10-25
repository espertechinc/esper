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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.SupportDatabaseService;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.*;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;

public class EPLDatabaseJoin {
    private final static String ALL_FIELDS = "mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseMySQLDatabaseConnection());
        execs.add(new EPLDatabaseSimpleJoinLeft());
        execs.add(new EPLDatabase2HistoricalStar());
        execs.add(new EPLDatabase2HistoricalStarInner());
        execs.add(new EPLDatabase3Stream());
        execs.add(new EPLDatabaseTimeBatch());
        execs.add(new EPLDatabaseTimeBatchOM());
        execs.add(new EPLDatabaseTimeBatchCompile());
        execs.add(new EPLDatabaseVariables());
        execs.add(new EPLDatabaseInvalidSQL());
        execs.add(new EPLDatabaseInvalidBothHistorical());
        execs.add(new EPLDatabaseInvalidPropertyEvent());
        execs.add(new EPLDatabaseInvalidPropertyHistorical());
        execs.add(new EPLDatabaseInvalid1Stream());
        execs.add(new EPLDatabaseInvalidSubviews());
        execs.add(new EPLDatabaseStreamNamesAndRename());
        execs.add(new EPLDatabaseWithPattern());
        execs.add(new EPLDatabasePropertyResolution());
        execs.add(new EPLDatabaseRestartStatement());
        execs.add(new EPLDatabaseSimpleJoinRight());
        return execs;
    }

    private static class EPLDatabase3Stream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportBean#lastevent sb, SupportBeanTwo#lastevent sbt, " +
                "sql:MyDBWithRetain ['select myint from mytesttable'] as s1 " +
                "  where sb.theString = sbt.stringTwo and s1.myint = sbt.intPrimitiveTwo";
            env.compileDeploy(stmtText).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(new SupportBeanTwo("T1", 2));
            env.sendEventBean(new SupportBean("T1", -1));

            env.sendEventBean(new SupportBeanTwo("T2", 30));
            env.sendEventBean(new SupportBean("T2", -1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T2", "T2", 30});

            env.milestone(0);

            env.sendEventBean(new SupportBean("T3", -1));
            env.sendEventBean(new SupportBeanTwo("T3", 40));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "sb.theString,sbt.stringTwo,s1.myint".split(","), new Object[]{"T3", "T3", 40});

            env.undeployAll();
        }
    }

    private static class EPLDatabaseTimeBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select " + ALL_FIELDS + " from " +
                " sql:MyDBWithRetain ['select " + ALL_FIELDS + " from mytesttable \n\r where ${intPrimitive} = mytesttable.mybigint'] as s0," +
                "SupportBean#time_batch(10 sec) as s1";
            env.compileDeploy(stmtText).addListener("s0");
            runtestTimeBatch(env);
        }
    }

    private static class EPLDatabase2HistoricalStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "intPrimitive,myint,myvarchar".split(",");
            String stmtText = "@name('s0') select intPrimitive, myint, myvarchar from " +
                "SupportBean#keepall as s0, " +
                " sql:MyDBWithRetain ['select myint from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s1," +
                " sql:MyDBWithRetain ['select myvarchar from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s2 ";
            env.compileDeploy(stmtText).addListener("s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            sendSupportBeanEvent(env, 6);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{6, 60, "F"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{6, 60, "F"}});

            sendSupportBeanEvent(env, 9);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{9, 90, "I"});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{6, 60, "F"}, {9, 90, "I"}});

            env.milestone(0);

            sendSupportBeanEvent(env, 20);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{6, 60, "F"}, {9, 90, "I"}});

            env.undeployAll();
        }
    }

    private static class EPLDatabase2HistoricalStarInner implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a,b,c,d".split(",");
            String stmtText = "@name('s0') select theString as a, intPrimitive as b, s1.myvarchar as c, s2.myvarchar as d from " +
                "SupportBean#keepall as s0 " +
                " inner join " +
                " sql:MyDBWithRetain ['select myvarchar from mytesttable where ${intPrimitive} <> mytesttable.mybigint'] as s1 " +
                " on s1.myvarchar=s0.theString " +
                " inner join " +
                " sql:MyDBWithRetain ['select myvarchar from mytesttable where ${intPrimitive} <> mytesttable.myint'] as s2 " +
                " on s2.myvarchar=s0.theString ";
            env.compileDeploy(stmtText).addListener("s0");

            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("A", 1));
            env.sendEventBean(new SupportBean("A", 10));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("B", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 3, "B", "B"});

            env.sendEventBean(new SupportBean("D", 4));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLDatabaseVariables implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int queryvar", path);
            env.compileDeploy("on SupportBean set queryvar=intPrimitive", path);
            String stmtText = "@name('s0') select myint from " +
                " sql:MyDBWithRetain ['select myint from mytesttable where ${queryvar} = mytesttable.mybigint'] as s0, " +
                "SupportBean_A#keepall as s1";
            env.compileDeploy(stmtText, path).addListener("s0");

            sendSupportBeanEvent(env, 5);
            env.sendEventBean(new SupportBean_A("A1"));

            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(50, received.get("myint"));
            env.undeployModuleContaining("s0");

            stmtText = "@name('s0') select myint from " +
                "SupportBean_A#keepall as s1, " +
                "sql:MyDBWithRetain ['select myint from mytesttable where ${queryvar} = mytesttable.mybigint'] as s0";
            env.compileDeploy(stmtText, path).addListener("s0");

            sendSupportBeanEvent(env, 6);
            env.sendEventBean(new SupportBean_A("A1"));

            received = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(60, received.get("myint"));

            env.undeployAll();
        }
    }

    private static class EPLDatabaseTimeBatchOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = ALL_FIELDS.split(",");
            String sql = "select " + ALL_FIELDS + " from mytesttable where ${intPrimitive} = mytesttable.mybigint";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create(fields));
            FromClause fromClause = FromClause.create(
                SQLStream.create("MyDBWithRetain", sql, "s0"),
                FilterStream.create(SupportBean.class.getSimpleName(), "s1").addView(View.create("time_batch", Expressions.constant(10))
                ));
            model.setFromClause(fromClause);
            SerializableObjectCopier.copyMayFail(model);
            assertEquals("select mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal from sql:MyDBWithRetain[\"select mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal from mytesttable where ${intPrimitive} = mytesttable.mybigint\"] as s0, " + "SupportBean#time_batch(10) as s1",
                model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");
            runtestTimeBatch(env);
        }
    }

    private static class EPLDatabaseTimeBatchCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select " + ALL_FIELDS + " from " +
                " sql:MyDBWithRetain ['select " + ALL_FIELDS + " from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s0," +
                "SupportBean#time_batch(10 sec) as s1";

            EPStatementObjectModel model = env.eplToModel(stmtText);
            SerializableObjectCopier.copyMayFail(model);
            env.compileDeploy(model).addListener("s0");
            runtestTimeBatch(env);
        }
    }

    private static class EPLDatabaseInvalidSQL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select myvarchar from " +
                " sql:MyDBWithRetain ['select mychar,, from mytesttable where '] as s0," +
                "SupportBeanComplexProps as s1";
            tryInvalidCompile(env, stmtText,
                "Error in statement 'select mychar,, from mytesttable where ', failed to obtain result metadata, consider turning off metadata interrogation via configuration, please check the statement, reason: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near ' from mytesttable where' at line 1");
        }
    }

    private static class EPLDatabaseInvalidBothHistorical implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String sqlOne = "sql:MyDBWithRetain ['select myvarchar from mytesttable where ${mychar} = mytesttable.mybigint']";
            String sqlTwo = "sql:MyDBWithRetain ['select mychar from mytesttable where ${myvarchar} = mytesttable.mybigint']";
            String stmtText = "@name('s0') select s0.myvarchar as s0Name, s1.mychar as s1Name from " +
                sqlOne + " as s0, " + sqlTwo + "  as s1";

            tryInvalidCompile(env, stmtText, "Circular dependency detected between historical streams");
        }
    }

    private static class EPLDatabaseInvalidPropertyEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select myvarchar from " +
                " sql:MyDBWithRetain ['select mychar from mytesttable where ${s1.xxx[0]} = mytesttable.mybigint'] as s0," +
                "SupportBeanComplexProps as s1";
            tryInvalidCompile(env, stmtText, "Failed to validate from-clause database-access parameter expression 's1.xxx[0]': Failed to resolve property 's1.xxx[0]' to a stream or nested property in a stream");

            stmtText = "@name('s0') select myvarchar from " +
                " sql:MyDBWithRetain ['select mychar from mytesttable where ${} = mytesttable.mybigint'] as s0," +
                "SupportBeanComplexProps as s1";
            tryInvalidCompile(env, stmtText,
                "Missing expression within ${...} in SQL statement [");
        }
    }

    private static class EPLDatabaseInvalidPropertyHistorical implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select myvarchar from " +
                " sql:MyDBWithRetain ['select myvarchar from mytesttable where ${myvarchar} = mytesttable.mybigint'] as s0," +
                "SupportBeanComplexProps as s1";
            tryInvalidCompile(env, stmtText,
                "Invalid expression 'myvarchar' resolves to the historical data itself");
        }
    }

    private static class EPLDatabaseInvalid1Stream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String sql = "sql:MyDBWithRetain ['select myvarchar, mybigint from mytesttable where ${mybigint} = myint']";
            String stmtText = "@name('s0') select myvarchar as s0Name from " + sql + " as s0";
            tryInvalidCompile(env, stmtText, "Invalid expression 'mybigint' resolves to the historical data itself");
        }
    }

    private static class EPLDatabaseInvalidSubviews implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String sql = "sql:MyDBWithRetain ['select myvarchar from mytesttable where ${intPrimitive} = mytesttable.myint']#time(30 sec)";
            String stmtText = "@name('s0') select myvarchar as s0Name from " +
                sql + " as s0, " + "SupportBean as s1";
            tryInvalidCompile(env, stmtText,
                "Historical data joins do not allow views onto the data, view 'time' is not valid in this context");
        }
    }

    private static class EPLDatabaseStreamNamesAndRename implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s1.a as mybigint, " +
                " s1.b as myint," +
                " s1.c as myvarchar," +
                " s1.d as mychar," +
                " s1.e as mybool," +
                " s1.f as mynumeric," +
                " s1.g as mydecimal," +
                " s1.h as mydouble," +
                " s1.i as myreal " +
                " from SupportBean_S0 as s0," +
                " sql:MyDBWithRetain ['select mybigint as a, " +
                " myint as b," +
                " myvarchar as c," +
                " mychar as d," +
                " mybool as e," +
                " mynumeric as f," +
                " mydecimal as g," +
                " mydouble as h," +
                " myreal as i " +
                "from mytesttable where ${id} = mytesttable.mybigint'] as s1";
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS0(env, 1);
            assertReceived(env, 1, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2, 1.3);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseWithPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String stmtText = "@name('s0') select mychar from " +
                " sql:MyDBWithRetain ['select mychar from mytesttable where mytesttable.mybigint = 2'] as s0," +
                " pattern [every timer:interval(5 sec) ]";
            env.compileDeploy(stmtText).addListener("s0");

            env.advanceTime(5000);
            assertEquals("Y", env.listener("s0").assertOneGetNewAndReset().get("mychar"));

            env.advanceTime(9999);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(10000);
            assertEquals("Y", env.listener("s0").assertOneGetNewAndReset().get("mychar"));

            // with variable
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable long VarLastTimestamp = 0", path);
            String epl = "@Name('Poll every 5 seconds') insert into PollStream" +
                " select * from pattern[every timer:interval(5 sec)]," +
                " sql:MyDBWithRetain ['select mychar from mytesttable where mytesttable.mybigint > ${VarLastTimestamp}'] as s0";
            EPStatementObjectModel model = env.eplToModel(epl);
            env.compileDeploy(model, path);
            env.undeployAll();
        }
    }

    private static class EPLDatabasePropertyResolution implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select " + ALL_FIELDS + " from " +
                " sql:MyDBWithRetain ['select " + ALL_FIELDS + " from mytesttable where ${s1.arrayProperty[0]} = mytesttable.mybigint'] as s0," +
                "SupportBeanComplexProps as s1";
            // s1.arrayProperty[0] returns 10 for that bean
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
            assertReceived(env, 10, 100, "J", "P", true, null, new BigDecimal(1000), 10.2, 10.3);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseSimpleJoinLeft implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select " + ALL_FIELDS + " from " +
                "SupportBean_S0 as s0," +
                " sql:MyDBWithRetain ['select " + ALL_FIELDS + " from mytesttable where ${id} = mytesttable.mybigint'] as s1";
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS0(env, 1);
            assertReceived(env, 1, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2, 1.3);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseRestartStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select mychar from " +
                "SupportBean_S0 as s0," +
                " sql:MyDBWithRetain ['select mychar from mytesttable where ${id} = mytesttable.mybigint'] as s1";
            EPCompiled compiled = env.compile(stmtText);
            env.deploy(compiled);

            // Too many connections unless the stop actually relieves them
            for (int i = 0; i < 100; i++) {
                env.undeployModuleContaining("s0");

                sendEventS0(env, 1);

                env.deploy(compiled).addListener("s0");
                sendEventS0(env, 1);
                assertEquals("Z", env.listener("s0").assertOneGetNewAndReset().get("mychar"));
            }

            env.undeployAll();
        }
    }

    private static class EPLDatabaseSimpleJoinRight implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select " + ALL_FIELDS + " from " +
                " sql:MyDBWithRetain ['select " + ALL_FIELDS + " from mytesttable where ${id} = mytesttable.mybigint'] as s0," +
                "SupportBean_S0 as s1";
            env.compileDeploy(stmtText).addListener("s0");

            EventType eventType = env.statement("s0").getEventType();
            assertEquals(Long.class, eventType.getPropertyType("mybigint"));
            assertEquals(Integer.class, eventType.getPropertyType("myint"));
            assertEquals(String.class, eventType.getPropertyType("myvarchar"));
            assertEquals(String.class, eventType.getPropertyType("mychar"));
            assertEquals(Boolean.class, eventType.getPropertyType("mybool"));
            assertEquals(BigDecimal.class, eventType.getPropertyType("mynumeric"));
            assertEquals(BigDecimal.class, eventType.getPropertyType("mydecimal"));
            assertEquals(Double.class, eventType.getPropertyType("mydouble"));
            assertEquals(Double.class, eventType.getPropertyType("myreal"));

            sendEventS0(env, 1);
            assertReceived(env, 1, 10, "A", "Z", true, new BigDecimal(5000), new BigDecimal(100), 1.2, 1.3);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseMySQLDatabaseConnection implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            try {
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
            } catch (Throwable t) {
                fail(t.getMessage());
            }

            /**
             * Using JNDI to get a connectiong (for J2EE containers or outside)
             */
            /**
             InitialContext ctx = new InitialContext();
             DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/MySQLDB");
             Connection connection = ds.getConnection();
             */
        }
    }

    private static void runtestTimeBatch(RegressionEnvironment env) {
        String[] fields = new String[]{"myint"};

        env.advanceTime(0);
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

        sendSupportBeanEvent(env, 10);
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{100}});

        sendSupportBeanEvent(env, 5);
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{100}, {50}});

        env.milestone(0);

        sendSupportBeanEvent(env, 2);
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{100}, {50}, {20}});

        env.advanceTime(10000);
        EventBean[] received = env.listener("s0").getLastNewData();
        assertEquals(3, received.length);
        assertEquals(100, received[0].get("myint"));
        assertEquals(50, received[1].get("myint"));
        assertEquals(20, received[2].get("myint"));

        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

        sendSupportBeanEvent(env, 9);
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{90}});

        sendSupportBeanEvent(env, 8);
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{90}, {80}});

        env.undeployAll();
    }

    private static void assertReceived(RegressionEnvironment env, long mybigint, int myint, String myvarchar, String mychar, boolean mybool, BigDecimal mynumeric, BigDecimal mydecimal, Double mydouble, Double myreal) {
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertReceived(theEvent, mybigint, myint, myvarchar, mychar, mybool, mynumeric, mydecimal, mydouble, myreal);
    }

    private static void assertReceived(EventBean theEvent, Long mybigint, Integer myint, String myvarchar, String mychar, Boolean mybool, BigDecimal mynumeric, BigDecimal mydecimal, Double mydouble, Double myreal) {
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

    private static void sendEventS0(RegressionEnvironment env, int id) {
        SupportBean_S0 bean = new SupportBean_S0(id);
        env.sendEventBean(bean);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
