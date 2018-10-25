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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLSubselectUnfiltered {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectUnfilteredExpression());
        execs.add(new EPLSubselectUnfilteredUnlimitedStream());
        execs.add(new EPLSubselectUnfilteredLengthWindow());
        execs.add(new EPLSubselectUnfilteredAsAfterSubselect());
        execs.add(new EPLSubselectUnfilteredWithAsWithinSubselect());
        execs.add(new EPLSubselectUnfilteredNoAs());
        execs.add(new EPLSubselectUnfilteredLastEvent());
        execs.add(new EPLSubselectStartStopStatement());
        execs.add(new EPLSubselectSelfSubselect());
        execs.add(new EPLSubselectComputedResult());
        execs.add(new EPLSubselectFilterInside());
        execs.add(new EPLSubselectWhereClauseWithExpression());
        execs.add(new EPLSubselectCustomFunction());
        execs.add(new EPLSubselectUnfilteredStreamPriorOM());
        execs.add(new EPLSubselectUnfilteredStreamPriorCompile());
        execs.add(new EPLSubselectTwoSubqSelect());
        execs.add(new EPLSubselectWhereClauseReturningTrue());
        execs.add(new EPLSubselectJoinUnfiltered());
        execs.add(new EPLSubselectInvalidSubselect());
        return execs;
    }

    private static class EPLSubselectSelfSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "insert into MyCount select count(*) as cnt from SupportBean_S0;\n" +
                "@name('s0') select (select cnt from MyCount#lastevent) as value from SupportBean_S0";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectStartStopStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 where (select true from SupportBean_S1#length(1000))";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(10));
            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            SupportListener listener = env.listener("s0");
            env.undeployAll();
            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(listener.isInvoked());

            env.compileDeployAddListenerMileZero(stmtText, "s0");
            env.sendEventBean(new SupportBean_S0(2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(10));
            env.sendEventBean(new SupportBean_S0(3));
            Assert.assertEquals(3, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectWhereClauseReturningTrue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 where (select true from SupportBean_S1#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S1(10));
            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectWhereClauseWithExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id from SupportBean_S0 where (select p10='X' from SupportBean_S1#length(1000))";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(0));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(10, "X"));
            env.sendEventBean(new SupportBean_S0(0));
            Assert.assertEquals(0, env.listener("s0").assertOneGetNewAndReset().get("id"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectJoinUnfiltered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id from SupportBean_S3#length(1000)) as idS3, (select id from SupportBean_S4#length(1000)) as idS4 from SupportBean_S0#keepall as s0, SupportBean_S1#keepall as s1 where s0.id = s1.id";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // check type
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("idS3"));
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("idS4"));

            // test no event, should return null
            env.sendEventBean(new SupportBean_S0(0));
            env.sendEventBean(new SupportBean_S1(0));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(null, theEvent.get("idS3"));
            Assert.assertEquals(null, theEvent.get("idS4"));

            // send one event
            env.sendEventBean(new SupportBean_S3(-1));
            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S1(1));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(-1, theEvent.get("idS3"));
            Assert.assertEquals(null, theEvent.get("idS4"));

            // send one event
            env.sendEventBean(new SupportBean_S4(-2));
            env.sendEventBean(new SupportBean_S0(2));
            env.sendEventBean(new SupportBean_S1(2));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(-1, theEvent.get("idS3"));
            Assert.assertEquals(-2, theEvent.get("idS4"));

            // send second event
            env.sendEventBean(new SupportBean_S4(-2));
            env.sendEventBean(new SupportBean_S0(3));
            env.sendEventBean(new SupportBean_S1(3));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(-1, theEvent.get("idS3"));
            Assert.assertEquals(null, theEvent.get("idS4"));

            env.sendEventBean(new SupportBean_S3(-2));
            env.sendEventBean(new SupportBean_S0(3));
            env.sendEventBean(new SupportBean_S1(3));
            EventBean[] events = env.listener("s0").getNewDataListFlattened();
            assertEquals(3, events.length);
            for (int i = 0; i < events.length; i++) {
                Assert.assertEquals(null, events[i].get("idS3"));
                Assert.assertEquals(null, events[i].get("idS4"));
            }

            env.undeployAll();
        }
    }

    private static class EPLSubselectInvalidSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select id from SupportBean_S1) from SupportBean_S0",
                "Failed to plan subquery number 1 querying SupportBean_S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window (applies to correlated or non-fully-aggregated subqueries) [");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select dummy from SupportBean_S1#lastevent) as idS1 from SupportBean_S0",
                "Failed to plan subquery number 1 querying SupportBean_S1: Failed to validate select-clause expression 'dummy': Property named 'dummy' is not valid in any stream [select (select dummy from SupportBean_S1#lastevent) as idS1 from SupportBean_S0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select (select id from SupportBean_S1#lastevent) id from SupportBean_S1#lastevent) as idS1 from SupportBean_S0",
                "Invalid nested subquery, subquery-within-subquery is not supported [select (select (select id from SupportBean_S1#lastevent) id from SupportBean_S1#lastevent) as idS1 from SupportBean_S0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select id from SupportBean_S1#lastevent where (sum(id) = 5)) as idS1 from SupportBean_S0",
                "Failed to plan subquery number 1 querying SupportBean_S1: Aggregation functions are not supported within subquery filters, consider using a having-clause or insert-into instead [select (select id from SupportBean_S1#lastevent where (sum(id) = 5)) as idS1 from SupportBean_S0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean_S0(id=5 and (select id from SupportBean_S1))",
                "Failed to validate subquery number 1 querying SupportBean_S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window [select * from SupportBean_S0(id=5 and (select id from SupportBean_S1))]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean_S0 group by id + (select id from SupportBean_S1)",
                "Subselects not allowed within group-by [select * from SupportBean_S0 group by id + (select id from SupportBean_S1)]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select * from SupportBean_S0 order by (select id from SupportBean_S1) asc",
                "Subselects not allowed within order-by clause [select * from SupportBean_S0 order by (select id from SupportBean_S1) asc]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select id from SupportBean_S1#lastevent where 'a') from SupportBean_S0",
                "Failed to plan subquery number 1 querying SupportBean_S1: Subselect filter expression must return a boolean value [select (select id from SupportBean_S1#lastevent where 'a') from SupportBean_S0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select (select id from SupportBean_S1#lastevent where id = p00) from SupportBean_S0",
                "Failed to plan subquery number 1 querying SupportBean_S1: Failed to validate filter expression 'id=p00': Property named 'p00' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\" [select (select id from SupportBean_S1#lastevent where id = p00) from SupportBean_S0]");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select id in (select * from SupportBean_S1#length(1000)) as value from SupportBean_S0",
                "Failed to validate select-clause expression subquery number 1 querying SupportBean_S1: Implicit conversion from datatype 'SupportBean_S1' to 'Integer' is not allowed [select id in (select * from SupportBean_S1#length(1000)) as value from SupportBean_S0]");
        }
    }

    private static class EPLSubselectUnfilteredStreamPriorOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel subquery = new EPStatementObjectModel();
            subquery.setSelectClause(SelectClause.create().add(Expressions.prior(0, "id")));
            subquery.setFromClause(FromClause.create(FilterStream.create("SupportBean_S1").addView("length", Expressions.constant(1000))));

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.subquery(subquery), "idS1"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean_S0")));
            model = SerializableObjectCopier.copyMayFail(model);

            String stmtText = "select (select prior(0,id) from SupportBean_S1#length(1000)) as idS1 from SupportBean_S0";
            Assert.assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");
            runUnfilteredStreamPrior(env);
            env.undeployAll();
        }
    }

    private static class EPLSubselectUnfilteredStreamPriorCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select prior(0,id) from SupportBean_S1#length(1000)) as idS1 from SupportBean_S0";
            env.eplToModelCompileDeploy(stmtText).addListener("s0");
            runUnfilteredStreamPrior(env);
            env.undeployAll();
        }
    }

    private static class EPLSubselectCustomFunction implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select " + SupportStaticMethodLib.class.getName() + ".minusOne(id) from SupportBean_S1#length(1000)) as idS1 from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // check type
            Assert.assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("idS1"));

            // test no event, should return null
            env.sendEventBean(new SupportBean_S0(0));
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            // test one event
            env.sendEventBean(new SupportBean_S1(10));
            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(9d, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            // resend event
            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(9d, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectComputedResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select 100*(select id from SupportBean_S1#length(1000)) as idS1 from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // check type
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("idS1"));

            // test no event, should return null
            env.sendEventBean(new SupportBean_S0(0));
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            // test one event
            env.sendEventBean(new SupportBean_S1(10));
            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(1000, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            // resend event
            env.sendEventBean(new SupportBean_S0(2));
            Assert.assertEquals(1000, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectFilterInside implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id from SupportBean_S1(p10='A')#length(1000)) as idS1 from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S1(1, "X"));
            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            env.sendEventBean(new SupportBean_S1(1, "A"));
            env.sendEventBean(new SupportBean_S0(1));
            Assert.assertEquals(1, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectUnfilteredUnlimitedStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id from SupportBean_S1#length(1000)) as idS1 from SupportBean_S0";
            tryAssertMultiRowUnfiltered(env, stmtText, "idS1");
        }
    }

    private static class EPLSubselectUnfilteredLengthWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id from SupportBean_S1#length(2)) as idS1 from SupportBean_S0";
            tryAssertMultiRowUnfiltered(env, stmtText, "idS1");
        }
    }

    private static class EPLSubselectUnfilteredAsAfterSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id from SupportBean_S1#lastevent) as idS1 from SupportBean_S0";
            tryAssertSingleRowUnfiltered(env, stmtText, "idS1");
        }
    }

    private static class EPLSubselectUnfilteredWithAsWithinSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id as myId from SupportBean_S1#lastevent) from SupportBean_S0";
            tryAssertSingleRowUnfiltered(env, stmtText, "myId");
        }
    }

    private static class EPLSubselectUnfilteredNoAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id from SupportBean_S1#lastevent) from SupportBean_S0";
            tryAssertSingleRowUnfiltered(env, stmtText, "id");
        }
    }

    public static class EPLSubselectUnfilteredLastEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,col".split(",");
            String epl = "@name('s0') select theString, (select p00 from SupportBean_S0#lastevent()) as col from SupportBean";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(11, "S01"));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "S01"});

            env.milestone(2);

            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", "S01"});

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(12, "S02"));
            env.sendEventBean(new SupportBean("E4", 4));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", "S02"});

            env.undeployAll();
        }
    }

    private static class EPLSubselectUnfilteredExpression implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select p10 || p11 from SupportBean_S1#lastevent) as value from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // check type
            Assert.assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("value"));

            // test no event, should return null
            env.sendEventBean(new SupportBean_S0(1));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(null, theEvent.get("value"));

            // test one event
            env.sendEventBean(new SupportBean_S1(-1, "a", "b"));
            env.sendEventBean(new SupportBean_S0(1));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("ab", theEvent.get("value"));

            env.undeployAll();
        }
    }

    private static class EPLSubselectTwoSubqSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select (select id+1 as myId from SupportBean_S1#lastevent) as idS1_0, " +
                "(select id+2 as myId from SupportBean_S1#lastevent) as idS1_1 from SupportBean_S0";

            env.compileDeployAddListenerMileZero(stmtText, "s0");


            // check type
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("idS1_0"));
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("idS1_1"));

            // test no event, should return null
            env.sendEventBean(new SupportBean_S0(1));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(null, theEvent.get("idS1_0"));
            Assert.assertEquals(null, theEvent.get("idS1_1"));

            // test one event
            env.sendEventBean(new SupportBean_S1(10));
            env.sendEventBean(new SupportBean_S0(1));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(11, theEvent.get("idS1_0"));
            Assert.assertEquals(12, theEvent.get("idS1_1"));

            // resend event
            env.sendEventBean(new SupportBean_S0(2));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(11, theEvent.get("idS1_0"));
            Assert.assertEquals(12, theEvent.get("idS1_1"));

            // test second event
            env.sendEventBean(new SupportBean_S1(999));
            env.sendEventBean(new SupportBean_S0(3));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals(1000, theEvent.get("idS1_0"));
            Assert.assertEquals(1001, theEvent.get("idS1_1"));

            env.undeployAll();
        }
    }

    private static void tryAssertSingleRowUnfiltered(RegressionEnvironment env, String stmtText, String columnName) {
        env.compileDeployAddListenerMileZero(stmtText, "s0");

        // check type
        Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(columnName));

        // test no event, should return null
        env.sendEventBean(new SupportBean_S0(0));
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        // test one event
        env.sendEventBean(new SupportBean_S1(10));
        env.sendEventBean(new SupportBean_S0(1));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        // resend event
        env.sendEventBean(new SupportBean_S0(2));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        // test second event
        env.sendEventBean(new SupportBean_S1(999));
        env.sendEventBean(new SupportBean_S0(3));
        Assert.assertEquals(999, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        env.undeployAll();
    }

    private static void runUnfilteredStreamPrior(RegressionEnvironment env) {
        // check type
        Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("idS1"));

        // test no event, should return null
        env.sendEventBean(new SupportBean_S0(0));
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

        // test one event
        env.sendEventBean(new SupportBean_S1(10));
        env.sendEventBean(new SupportBean_S0(1));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

        // resend event
        env.sendEventBean(new SupportBean_S0(2));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("idS1"));

        // test second event
        env.sendEventBean(new SupportBean_S0(3));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("idS1"));
    }

    private static void tryAssertMultiRowUnfiltered(RegressionEnvironment env, String stmtText, String columnName) {
        env.compileDeployAddListenerMileZero(stmtText, "s0");

        // check type
        Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType(columnName));

        // test no event, should return null
        env.sendEventBean(new SupportBean_S0(0));
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        // test one event
        env.sendEventBean(new SupportBean_S1(10));
        env.sendEventBean(new SupportBean_S0(1));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        // resend event
        env.sendEventBean(new SupportBean_S0(2));
        Assert.assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        // test second event
        env.sendEventBean(new SupportBean_S1(999));
        env.sendEventBean(new SupportBean_S0(3));
        Assert.assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get(columnName));

        env.undeployAll();
    }
}
