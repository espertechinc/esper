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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResultSetAggregateMaxMinGroupBy {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateMinMax());
        execs.add(new ResultSetAggregateMinMaxOM());
        execs.add(new ResultSetAggregateMinMaxViewCompile());
        execs.add(new ResultSetAggregateMinMaxJoin());
        execs.add(new ResultSetAggregateMinNoGroupHaving());
        execs.add(new ResultSetAggregateMinNoGroupSelectHaving());
        return execs;
    }

    private static class ResultSetAggregateMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select irstream symbol, " +
                "min(all volume) as minVol," +
                "max(all volume) as maxVol," +
                "min(distinct volume) as minDistVol," +
                "max(distinct volume) as maxDistVol" +
                " from SupportMarketDataBean#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionMinMax(env, milestone);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinMaxOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("symbol")
                .add(Expressions.min("volume"), "minVol")
                .add(Expressions.max("volume"), "maxVol")
                .add(Expressions.minDistinct("volume"), "minDistVol")
                .add(Expressions.maxDistinct("volume"), "maxDistVol")
            );
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).addView("length", Expressions.constant(3))));
            model.setWhereClause(Expressions.or()
                .add(Expressions.eq("symbol", "DELL"))
                .add(Expressions.eq("symbol", "IBM"))
                .add(Expressions.eq("symbol", "GE")));
            model.setGroupByClause(GroupByClause.create("symbol"));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select irstream symbol, " +
                "min(volume) as minVol, " +
                "max(volume) as maxVol, " +
                "min(distinct volume) as minDistVol, " +
                "max(distinct volume) as maxDistVol " +
                "from SupportMarketDataBean#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
            assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryAssertionMinMax(env, new AtomicInteger());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinMaxViewCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, " +
                "min(volume) as minVol, " +
                "max(volume) as maxVol, " +
                "min(distinct volume) as minDistVol, " +
                "max(distinct volume) as maxDistVol " +
                "from SupportMarketDataBean#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionMinMax(env, new AtomicInteger());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinMaxJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, " +
                "min(volume) as minVol," +
                "max(volume) as maxVol," +
                "min(distinct volume) as minDistVol," +
                "max(distinct volume) as maxDistVol" +
                " from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

            tryAssertionMinMax(env, new AtomicInteger());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinNoGroupHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol from SupportMarketDataBean#time(5 sec) " +
                "having volume > min(volume) * 1.3";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendEvent(env, "DELL", 100L);
            sendEvent(env, "DELL", 105L);
            sendEvent(env, "DELL", 100L);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendEvent(env, "DELL", 131L);
            assertEquals("DELL", env.listener("s0").assertOneGetNewAndReset().get("symbol"));

            sendEvent(env, "DELL", 132L);
            assertEquals("DELL", env.listener("s0").assertOneGetNewAndReset().get("symbol"));

            sendEvent(env, "DELL", 129L);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinNoGroupSelectHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, min(volume) as mymin from SupportMarketDataBean#length(5) " +
                "having volume > min(volume) * 1.3";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendEvent(env, "DELL", 100L);
            sendEvent(env, "DELL", 105L);
            sendEvent(env, "DELL", 100L);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "DELL", 131L);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("DELL", theEvent.get("symbol"));
            assertEquals(100L, theEvent.get("mymin"));

            env.milestone(1);

            sendEvent(env, "DELL", 132L);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("DELL", theEvent.get("symbol"));
            assertEquals(100L, theEvent.get("mymin"));

            sendEvent(env, "DELL", 129L);
            sendEvent(env, "DELL", 125L);
            sendEvent(env, "DELL", 125L);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "DELL", 170L);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEquals("DELL", theEvent.get("symbol"));
            assertEquals(125L, theEvent.get("mymin"));

            env.undeployAll();
        }
    }

    private static void tryAssertionMinMax(RegressionEnvironment env, AtomicInteger milestone) {
        // assert select result type
        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("minVol"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("maxVol"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("minDistVol"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("maxDistVol"));

        sendEvent(env, SYMBOL_DELL, 50L);
        assertEvents(env, SYMBOL_DELL, null, null, null, null,
            SYMBOL_DELL, 50L, 50L, 50L, 50L
        );

        env.milestone(0);

        sendEvent(env, SYMBOL_DELL, 30L);
        assertEvents(env, SYMBOL_DELL, 50L, 50L, 50L, 50L,
            SYMBOL_DELL, 30L, 50L, 30L, 50L
        );

        sendEvent(env, SYMBOL_DELL, 30L);
        assertEvents(env, SYMBOL_DELL, 30L, 50L, 30L, 50L,
            SYMBOL_DELL, 30L, 50L, 30L, 50L
        );

        env.milestone(1);

        sendEvent(env, SYMBOL_DELL, 90L);
        assertEvents(env, SYMBOL_DELL, 30L, 50L, 30L, 50L,
            SYMBOL_DELL, 30L, 90L, 30L, 90L
        );

        sendEvent(env, SYMBOL_DELL, 100L);
        assertEvents(env, SYMBOL_DELL, 30L, 90L, 30L, 90L,
            SYMBOL_DELL, 30L, 100L, 30L, 100L
        );

        sendEvent(env, SYMBOL_IBM, 20L);
        sendEvent(env, SYMBOL_IBM, 5L);
        sendEvent(env, SYMBOL_IBM, 15L);
        sendEvent(env, SYMBOL_IBM, 18L);
        assertEvents(env, SYMBOL_IBM, 5L, 20L, 5L, 20L,
            SYMBOL_IBM, 5L, 18L, 5L, 18L
        );

        env.milestone(2);

        sendEvent(env, SYMBOL_IBM, null);
        assertEvents(env, SYMBOL_IBM, 5L, 18L, 5L, 18L,
            SYMBOL_IBM, 15L, 18L, 15L, 18L
        );

        sendEvent(env, SYMBOL_IBM, null);
        assertEvents(env, SYMBOL_IBM, 15L, 18L, 15L, 18L,
            SYMBOL_IBM, 18L, 18L, 18L, 18L
        );

        sendEvent(env, SYMBOL_IBM, null);
        assertEvents(env, SYMBOL_IBM, 18L, 18L, 18L, 18L,
            SYMBOL_IBM, null, null, null, null
        );
    }

    private static void assertEvents(RegressionEnvironment env, String symbolOld, Long minVolOld, Long maxVolOld, Long minDistVolOld, Long maxDistVolOld,
                                     String symbolNew, Long minVolNew, Long maxVolNew, Long minDistVolNew, Long maxDistVolNew) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

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

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, null);
        env.sendEventBean(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ResultSetAggregateMaxMinGroupBy.class);
}
