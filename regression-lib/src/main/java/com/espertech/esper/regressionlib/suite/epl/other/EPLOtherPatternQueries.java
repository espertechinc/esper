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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLOtherPatternQueries {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherWhereOM());
        execs.add(new EPLOtherWhereCompile());
        execs.add(new EPLOtherWhere());
        execs.add(new EPLOtherAggregation());
        execs.add(new EPLOtherFollowedByAndWindow());
        execs.add(new EPLOtherPatternWindow());
        return execs;
    }

    private static class EPLOtherWhereOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().addWithAsProvidedName("s0.id", "idS0").addWithAsProvidedName("s1.id", "idS1"));
            PatternExpr pattern = Patterns.or()
                .add(Patterns.everyFilter("SupportBean_S0", "s0"))
                .add(Patterns.everyFilter("SupportBean_S1", "s1")
                );
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model.setWhereClause(Expressions.or()
                .add(Expressions.and()
                    .add(Expressions.isNotNull("s0.id"))
                    .add(Expressions.lt("s0.id", 100))
                )
                .add(Expressions.and()
                    .add(Expressions.isNotNull("s1.id"))
                    .add(Expressions.ge("s1.id", 100))
                ));
            model = SerializableObjectCopier.copyMayFail(model);

            String reverse = model.toEPL();
            String stmtText = "select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=SupportBean_S0" +
                " or every s1=SupportBean_S1] " +
                "where s0.id is not null and s0.id<100 or s1.id is not null and s1.id>=100";
            assertEquals(stmtText, reverse);

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            sendEventS0(env, 1);
            assertEventIds(env, 1, null);

            sendEventS0(env, 101);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 100);
            assertEventIds(env, null, 100);

            env.undeployAll();
        }
    }

    private static class EPLOtherWhereCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=SupportBean_S0" +
                " or every s1=SupportBean_S1] " +
                "where s0.id is not null and s0.id<100 or s1.id is not null and s1.id>=100";
            EPStatementObjectModel model = env.eplToModel(stmtText);
            model = SerializableObjectCopier.copyMayFail(model);

            String reverse = model.toEPL();
            assertEquals(stmtText, reverse);

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            sendEventS0(env, 1);
            assertEventIds(env, 1, null);

            sendEventS0(env, 101);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 100);
            assertEventIds(env, null, 100);

            env.undeployAll();
        }
    }

    private static class EPLOtherWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=SupportBean_S0" +
                " or every s1=SupportBean_S1] " +
                "where (s0.id is not null and s0.id < 100) or (s1.id is not null and s1.id >= 100)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS0(env, 1);
            assertEventIds(env, 1, null);

            sendEventS0(env, 101);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendEventS1(env, 100);
            assertEventIds(env, null, 100);

            env.undeployAll();
        }
    }

    private static class EPLOtherAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select sum(s0.id) as sumS0, sum(s1.id) as sumS1, sum(s0.id + s1.id) as sumS0S1 " +
                "from pattern [every s0=SupportBean_S0" +
                " or every s1=SupportBean_S1]";
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS0(env, 1);
            assertEventSums(env, 1, null, null);

            sendEventS1(env, 2);
            assertEventSums(env, 1, 2, null);

            sendEventS1(env, 10);
            assertEventSums(env, 1, 12, null);

            sendEventS0(env, 20);
            assertEventSums(env, 21, 12, null);

            env.undeployAll();
        }
    }

    private static class EPLOtherFollowedByAndWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream a.id as idA, b.id as idB, " +
                "a.p00 as p00A, b.p00 as p00B from pattern [every a=SupportBean_S0" +
                " -> every b=SupportBean_S0(p00=a.p00)]#time(1)";
            env.compileDeploy(stmtText).addListener("s0");
            env.advanceTime(0);

            sendEvent(env, 1, "e1a");
            assertFalse(env.listener("s0").isInvoked());
            sendEvent(env, 2, "e1a");
            assertNewEvent(env, 1, 2, "e1a");

            env.advanceTime(500);
            sendEvent(env, 10, "e2a");
            sendEvent(env, 11, "e2b");
            sendEvent(env, 12, "e2c");
            assertFalse(env.listener("s0").isInvoked());
            sendEvent(env, 13, "e2b");
            assertNewEvent(env, 11, 13, "e2b");

            env.advanceTime(1000);
            assertOldEvent(env, 1, 2, "e1a");

            env.advanceTime(1500);
            assertOldEvent(env, 11, 13, "e2b");

            env.undeployAll();
        }
    }

    public static class EPLOtherPatternWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from pattern [every(s0=SupportMarketDataBean(symbol='S0') and " +
                "s1=SupportMarketDataBean(symbol='S1'))]#length(1)";
            env.compileDeploy(text).addListener("s0");

            env.milestone(0);

            SupportMarketDataBean eventOne = makeMarketDataEvent("S0");
            env.sendEventBean(eventOne);

            env.milestone(1);

            SupportMarketDataBean eventTwo = makeMarketDataEvent("S1");
            env.sendEventBean(eventTwo);
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(eventOne.getSymbol(), event.get("s0.symbol"));
            assertEquals(eventTwo.getSymbol(), event.get("s1.symbol"));
            env.listener("s0").reset();

            env.milestone(2);

            SupportMarketDataBean eventThree = makeMarketDataEvent("S1");
            env.sendEventBean(eventThree);
            assertFalse(env.listener("s0").isInvoked());

            SupportMarketDataBean eventFour = makeMarketDataEvent("S0");
            env.sendEventBean(eventFour);

            event = env.listener("s0").getLastOldData()[0];
            assertEquals(eventOne.getSymbol(), event.get("s0.symbol"));
            assertEquals(eventTwo.getSymbol(), event.get("s1.symbol"));
            event = env.listener("s0").getLastNewData()[0];
            assertEquals(eventFour.getSymbol(), event.get("s0.symbol"));
            assertEquals(eventThree.getSymbol(), event.get("s1.symbol"));

            env.undeployAll();
        }

        private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
            SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
            bean.setId("1");
            return bean;
        }
    }

    private static void assertNewEvent(RegressionEnvironment env, int idA, int idB, String p00) {
        EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
        compareEvent(eventBean, idA, idB, p00);
    }

    private static void assertOldEvent(RegressionEnvironment env, int idA, int idB, String p00) {
        EventBean eventBean = env.listener("s0").assertOneGetOldAndReset();
        compareEvent(eventBean, idA, idB, p00);
    }

    private static void compareEvent(EventBean eventBean, int idA, int idB, String p00) {
        Assert.assertEquals(idA, eventBean.get("idA"));
        Assert.assertEquals(idB, eventBean.get("idB"));
        Assert.assertEquals(p00, eventBean.get("p00A"));
        Assert.assertEquals(p00, eventBean.get("p00B"));
    }

    private static void sendEvent(RegressionEnvironment env, int id, String p00) {
        SupportBean_S0 theEvent = new SupportBean_S0(id, p00);
        env.sendEventBean(theEvent);
    }

    private static void sendEventS0(RegressionEnvironment env, int id) {
        SupportBean_S0 theEvent = new SupportBean_S0(id);
        env.sendEventBean(theEvent);
    }

    private static void sendEventS1(RegressionEnvironment env, int id) {
        SupportBean_S1 theEvent = new SupportBean_S1(id);
        env.sendEventBean(theEvent);
    }

    private static void assertEventIds(RegressionEnvironment env, Integer idS0, Integer idS1) {
        EventBean eventBean = env.listener("s0").getAndResetLastNewData()[0];
        Assert.assertEquals(idS0, eventBean.get("idS0"));
        Assert.assertEquals(idS1, eventBean.get("idS1"));
        env.listener("s0").reset();
    }

    private static void assertEventSums(RegressionEnvironment env, Integer sumS0, Integer sumS1, Integer sumS0S1) {
        EventBean eventBean = env.listener("s0").getAndResetLastNewData()[0];
        Assert.assertEquals(sumS0, eventBean.get("sumS0"));
        Assert.assertEquals(sumS1, eventBean.get("sumS1"));
        Assert.assertEquals(sumS0S1, eventBean.get("sumS0S1"));
        env.listener("s0").reset();
    }
}
