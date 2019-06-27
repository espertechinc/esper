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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.subscriber.*;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.EPSubscriberException;
import com.espertech.esper.runtime.client.UpdateListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ClientRuntimeSubscriber {
    private final static String[] FIELDS = "theString,intPrimitive".split(",");

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeSubscriberBindings());
        execs.add(new ClientRuntimeSubscriberSubscriberAndListener());
        execs.add(new ClientRuntimeSubscriberBindWildcardJoin());
        execs.add(new ClientRuntimeSubscriberInvocationTargetEx());
        execs.add(new ClientRuntimeSubscriberNamedWindow());
        execs.add(new ClientRuntimeSubscriberStartStopStatement());
        execs.add(new ClientRuntimeSubscriberVariables());
        execs.add(new ClientRuntimeSubscriberSimpleSelectUpdateOnly());
        execs.add(new ClientRuntimeSubscriberPerformanceSyntheticUndelivered());
        execs.add(new ClientRuntimeSubscriberPerformanceSynthetic());
        return execs;
    }

    private static class ClientRuntimeSubscriberBindings implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // just wildcard
            EPStatement stmtJustWildcard = env.compileDeploy("@name('s0') select * from SupportBean(theString='E2')").statement("s0");
            tryAssertionJustWildcard(env, stmtJustWildcard, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionJustWildcard(env, stmtJustWildcard, new SupportSubscriberRowByRowSpecificWStmt());
            env.undeployAll();

            // wildcard with props
            EPStatement stmtWildcardWProps = env.compileDeploy("@name('s0') select *, intPrimitive + 2, 'x'||theString||'x' from SupportBean").statement("s0");
            tryAssertionWildcardWProps(env, stmtWildcardWProps, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionWildcardWProps(env, stmtWildcardWProps, new SupportSubscriberRowByRowSpecificWStmt());
            env.undeployAll();

            // nested
            EPStatement stmtNested = env.compileDeploy("@name('s0') select nested, nested.nestedNested from SupportBeanComplexProps").statement("s0");
            tryAssertionNested(env, stmtNested, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionNested(env, stmtNested, new SupportSubscriberRowByRowSpecificWStmt());
            env.undeployAll();

            // enum
            EPStatement stmtEnum = env.compileDeploy("@name('s0') select theString, supportEnum from SupportBeanWithEnum").statement("s0");
            tryAssertionEnum(env, stmtEnum, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionEnum(env, stmtEnum, new SupportSubscriberRowByRowSpecificWStmt());
            env.undeployAll();

            // null-typed select value
            EPStatement stmtNullSelected = env.compileDeploy("@name('s0') select null, longBoxed from SupportBean").statement("s0");
            tryAssertionNullSelected(env, stmtNullSelected, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionNullSelected(env, stmtNullSelected, new SupportSubscriberRowByRowSpecificWStmt());
            env.undeployAll();

            // widening
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionWidening(env, rep, new SupportSubscriberRowByRowSpecificNStmt());
            }
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionWidening(env, rep, new SupportSubscriberRowByRowSpecificWStmt());
            }

            // r-stream select
            tryAssertionRStreamSelect(env, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionRStreamSelect(env, new SupportSubscriberRowByRowSpecificWStmt());

            // stream-selected join
            tryAssertionStreamSelectWJoin(env, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionStreamSelectWJoin(env, new SupportSubscriberRowByRowSpecificWStmt());

            // stream-wildcard join
            tryAssertionStreamWildcardJoin(env, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionStreamWildcardJoin(env, new SupportSubscriberRowByRowSpecificWStmt());

            // bind wildcard join
            tryAssertionBindWildcardJoin(env, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionBindWildcardJoin(env, new SupportSubscriberRowByRowSpecificWStmt());

            // output limit
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionOutputLimitNoJoin(env, rep, new SupportSubscriberRowByRowSpecificNStmt());
            }
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionOutputLimitNoJoin(env, rep, new SupportSubscriberRowByRowSpecificWStmt());
            }

            // output limit join
            tryAssertionOutputLimitJoin(env, new SupportSubscriberRowByRowSpecificNStmt());
            tryAssertionOutputLimitJoin(env, new SupportSubscriberRowByRowSpecificWStmt());

            // binding-to-map
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionBindMap(env, rep, new SupportSubscriberMultirowMapNStmt());
            }
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionBindMap(env, rep, new SupportSubscriberMultirowMapWStmt());
            }

            // binding-to-objectarray
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionBindObjectArr(env, rep, new SupportSubscriberMultirowObjectArrayNStmt());
            }
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionBindObjectArr(env, rep, new SupportSubscriberMultirowObjectArrayWStmt());
            }

            // binding-to-underlying-array
            tryAssertionBindWildcardIRStream(env, new SupportSubscriberMultirowUnderlyingNStmt());
            tryAssertionBindWildcardIRStream(env, new SupportSubscriberMultirowUnderlyingWStmt());

            // Object[] and "Object..." binding
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionObjectArrayDelivery(env, rep, new SupportSubscriberRowByRowObjectArrayPlainNStmt());
                tryAssertionObjectArrayDelivery(env, rep, new SupportSubscriberRowByRowObjectArrayPlainWStmt());
                tryAssertionObjectArrayDelivery(env, rep, new SupportSubscriberRowByRowObjectArrayVarargNStmt());
                tryAssertionObjectArrayDelivery(env, rep, new SupportSubscriberRowByRowObjectArrayVarargWStmt());
            }

            // Map binding
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionRowMapDelivery(env, rep, new SupportSubscriberRowByRowMapNStmt());
                tryAssertionRowMapDelivery(env, rep, new SupportSubscriberRowByRowMapWStmt());
            }

            // static methods
            tryAssertionStaticMethod(env);

            // IR stream individual calls
            tryAssertionBindUpdateIRStream(env, new SupportSubscriberRowByRowFullNStmt());
            tryAssertionBindUpdateIRStream(env, new SupportSubscriberRowByRowFullWStmt());

            // no-params subscriber
            EPStatement stmtNoParamsSubscriber = env.compileDeploy("@name('s0') select null from SupportBean").statement("s0");
            tryAssertionNoParams(env, stmtNoParamsSubscriber, new SupportSubscriberNoParamsBaseNStmt());
            tryAssertionNoParams(env, stmtNoParamsSubscriber, new SupportSubscriberNoParamsBaseWStmt());
            env.undeployAll();

            // named-method subscriber
            EPStatement stmtNamedMethod = env.compileDeploy("@name('s0') select theString from SupportBean").statement("s0");
            tryAssertionNamedMethod(env, stmtNamedMethod, new SupportSubscriberMultirowUnderlyingNamedMethodNStmt());
            tryAssertionNamedMethod(env, stmtNamedMethod, new SupportSubscriberMultirowUnderlyingNamedMethodWStmt());
            env.undeployAll();

            // prefer the EPStatement-footprint over the non-EPStatement footprint
            tryAssertionPreferEPStatement(env);

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberSubscriberAndListener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("insert into A1 select s.*, 1 as a from SupportBean as s", path);
            EPStatement stmt = env.compileDeploy("@name('s0') select a1.* from A1 as a1", path).statement("s0");

            SupportUpdateListener listener = new SupportUpdateListener();
            SupportSubscriberRowByRowObjectArrayPlainNStmt subscriber = new SupportSubscriberRowByRowObjectArrayPlainNStmt();

            stmt.addListener(listener);
            stmt.setSubscriber(subscriber);
            env.sendEventBean(new SupportBean("E1", 1));

            EventBean theEvent = listener.assertOneGetNewAndReset();
            Assert.assertEquals("E1", theEvent.get("theString"));
            Assert.assertEquals(1, theEvent.get("intPrimitive"));
            assertTrue(theEvent.getUnderlying() instanceof Pair);

            for (String property : stmt.getEventType().getPropertyNames()) {
                EventPropertyGetter getter = stmt.getEventType().getGetter(property);
                getter.get(theEvent);
            }

            env.undeployAll();
        }
    }

    private static void tryAssertionJustWildcard(RegressionEnvironment env, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);
        SupportBean theEvent = new SupportBean("E2", 1);
        env.sendEventBean(theEvent);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{theEvent});
    }

    private static void tryAssertionBindUpdateIRStream(RegressionEnvironment env, SupportSubscriberRowByRowFullBase subscriber) {
        String stmtText = "@name('s0') select irstream theString, intPrimitive from SupportBean" + "#length_batch(2)";
        EPStatement stmt = env.compileDeploy(stmtText).statement("s0");
        stmt.setSubscriber(subscriber);

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, 2, 0, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        env.sendEventBean(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, 2, 2, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        env.undeployAll();
    }

    private static void tryAssertionBindObjectArr(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberMultirowObjectArrayBase subscriber) {
        String stmtText = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStringInt.class) + " @name('s0') select irstream theString, intPrimitive from SupportBean" + "#length_batch(2)";
        EPStatement stmt = env.compileDeploy(stmtText).statement("s0");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        env.sendEventBean(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        env.undeployAll();
    }

    private static void tryAssertionBindMap(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberMultirowMapBase subscriber) {
        String stmtText = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStringInt.class) + " @name('s0') select irstream theString, intPrimitive from SupportBean" + "#length_batch(2)";
        EPStatement stmt = env.compileDeploy(stmtText).statement("s0");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        env.sendEventBean(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        env.undeployAll();
    }

    private static void tryAssertionWidening(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedWidenedEvent.class) + " @name('s0') select bytePrimitive, intPrimitive, longPrimitive, floatPrimitive from SupportBean(theString='E1')").statement("s0");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        SupportBean bean = new SupportBean();
        bean.setTheString("E1");
        bean.setBytePrimitive((byte) 1);
        bean.setIntPrimitive(2);
        bean.setLongPrimitive(3);
        bean.setFloatPrimitive(4);
        env.sendEventBean(bean);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{1, 2L, 3d, 4d});

        env.undeployAll();
    }

    private static void tryAssertionObjectArrayDelivery(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowObjectArrayBase subscriber) {
        EPStatement stmt = env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStringInt.class) + " @name('s0') select theString, intPrimitive from SupportBean#unique(theString)").statement("s0");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertOneAndReset(stmt, new Object[]{"E1", 1});

        env.sendEventBean(new SupportBean("E2", 10));
        subscriber.assertOneAndReset(stmt, new Object[]{"E2", 10});

        env.undeployAll();
    }

    private static void tryAssertionRowMapDelivery(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowMapBase subscriber) {
        EPStatement stmt = env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStringInt.class) + " @name('s0') select irstream theString, intPrimitive from SupportBean#unique(theString)").statement("s0");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertIRStreamAndReset(stmt, FIELDS, new Object[]{"E1", 1}, null);

        env.sendEventBean(new SupportBean("E2", 10));
        subscriber.assertIRStreamAndReset(stmt, FIELDS, new Object[]{"E2", 10}, null);

        env.sendEventBean(new SupportBean("E1", 2));
        subscriber.assertIRStreamAndReset(stmt, FIELDS, new Object[]{"E1", 2}, new Object[]{"E1", 1});

        env.undeployAll();
    }

    private static void tryAssertionNested(RegressionEnvironment env, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);

        SupportBeanComplexProps theEvent = SupportBeanComplexProps.makeDefaultBean();
        env.sendEventBean(theEvent);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{theEvent.getNested(), theEvent.getNested().getNestedNested()});
    }

    private static void tryAssertionEnum(RegressionEnvironment env, EPStatement stmtEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmtEnum.setSubscriber(subscriber);

        SupportBeanWithEnum theEvent = new SupportBeanWithEnum("abc", SupportEnum.ENUM_VALUE_1);
        env.sendEventBean(theEvent);
        subscriber.assertOneReceivedAndReset(stmtEnum, new Object[]{theEvent.getTheString(), theEvent.getSupportEnum()});
    }

    private static void tryAssertionNullSelected(RegressionEnvironment env, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);
        env.sendEventBean(new SupportBean());
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{null, null});
    }

    private static void tryAssertionStreamSelectWJoin(RegressionEnvironment env, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy("@name('s0') select null, s1, s0 from SupportBean#keepall as s0, SupportMarketDataBean#keepall as s1 where s0.theString = s1.symbol").statement("s0");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        env.sendEventBean(s0);
        env.sendEventBean(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{null, s1, s0});

        env.undeployAll();
    }

    private static void tryAssertionBindWildcardJoin(RegressionEnvironment env, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy("@name('s0') select * from SupportBean#keepall as s0, SupportMarketDataBean#keepall as s1 where s0.theString = s1.symbol").statement("s0");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        env.sendEventBean(s0);
        env.sendEventBean(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, s1});

        env.undeployAll();
    }

    private static void tryAssertionStreamWildcardJoin(RegressionEnvironment env, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy("@name('s0') select theString || '<', s1.* as s1, s0.* as s0 from SupportBean#keepall as s0, SupportMarketDataBean#keepall as s1 where s0.theString = s1.symbol").statement("s0");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        env.sendEventBean(s0);
        env.sendEventBean(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1<", s1, s0});

        env.undeployAll();
    }

    private static void tryAssertionWildcardWProps(RegressionEnvironment env, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        env.sendEventBean(s0);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, 102, "xE1x"});
    }

    private static void tryAssertionOutputLimitNoJoin(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedStringInt.class) + " @name('s0') select theString, intPrimitive from SupportBean output every 2 events").statement("s0");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E2", 2));
        subscriber.assertMultipleReceivedAndReset(stmt, new Object[][]{{"E1", 1}, {"E2", 2}});

        env.undeployAll();
    }

    private static void tryAssertionOutputLimitJoin(RegressionEnvironment env, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy("@name('s0') select theString, intPrimitive from SupportBean#keepall, SupportMarketDataBean#keepall where symbol = theString output every 2 events").statement("s0");
        stmt.setSubscriber(subscriber);

        env.sendEventBean(new SupportMarketDataBean("E1", 0, 1L, ""));
        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        env.sendEventBean(new SupportBean("E1", 2));
        subscriber.assertMultipleReceivedAndReset(stmt, new Object[][]{{"E1", 1}, {"E1", 2}});
        env.undeployAll();
    }

    private static void tryAssertionRStreamSelect(RegressionEnvironment env, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = env.compileDeploy("@name('s0') select rstream s0 from SupportBean#unique(theString) as s0").statement("s0");
        stmt.setSubscriber(subscriber);

        // send event
        SupportBean s0 = new SupportBean("E1", 100);
        env.sendEventBean(s0);
        subscriber.assertNoneReceived();

        SupportBean s1 = new SupportBean("E2", 200);
        env.sendEventBean(s1);
        subscriber.assertNoneReceived();

        SupportBean s2 = new SupportBean("E1", 300);
        env.sendEventBean(s2);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0});

        env.undeployAll();
    }

    private static void tryAssertionBindWildcardIRStream(RegressionEnvironment env, SupportSubscriberMultirowUnderlyingBase subscriber) {
        EPStatement stmt = env.compileDeploy("@name('s0') select irstream * from SupportBean#length_batch(2)").statement("s0");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportBean s1 = new SupportBean("E2", 200);
        env.sendEventBean(s0);
        env.sendEventBean(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, s1}, null);

        SupportBean s2 = new SupportBean("E3", 300);
        SupportBean s3 = new SupportBean("E4", 400);
        env.sendEventBean(s2);
        env.sendEventBean(s3);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s2, s3}, new Object[]{s0, s1});

        env.undeployAll();
    }

    private static void tryAssertionStaticMethod(RegressionEnvironment env) {
        EPStatement stmt = env.compileDeploy("@name('s0') select theString, intPrimitive from SupportBean").statement("s0");

        SupportSubscriberRowByRowStatic subscriber = new SupportSubscriberRowByRowStatic();
        stmt.setSubscriber(subscriber);
        env.sendEventBean(new SupportBean("E1", 100));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 100}}, SupportSubscriberRowByRowStatic.getAndResetIndicate());

        SupportSubscriberRowByRowStaticWStatement subscriberWStmt = new SupportSubscriberRowByRowStaticWStatement();
        stmt.setSubscriber(subscriberWStmt);
        env.sendEventBean(new SupportBean("E2", 200));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E2", 200}}, SupportSubscriberRowByRowStaticWStatement.getIndicate());
        Assert.assertEquals(stmt, SupportSubscriberRowByRowStaticWStatement.getStatements().get(0));
        subscriberWStmt.reset();

        env.undeployAll();
    }

    private static void tryAssertionNoParams(RegressionEnvironment env, EPStatement stmt, SupportSubscriberNoParamsBase subscriber) {
        stmt.setSubscriber(subscriber);

        env.sendEventBean(new SupportBean());
        subscriber.assertCalledAndReset(stmt);
    }

    private static void tryAssertionNamedMethod(RegressionEnvironment env, EPStatement stmt, SupportSubscriberMultirowUnderlyingBase subscriber) {
        stmt.setSubscriber(subscriber, "someNewDataMayHaveArrived");

        env.sendEventBean(new SupportBean("E1", 1));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1"}, null);
    }

    private static void tryAssertionPreferEPStatement(RegressionEnvironment env) {
        SupportSubscriberUpdateBothFootprints subscriber = new SupportSubscriberUpdateBothFootprints();
        EPStatement stmt = env.compileDeploy("@name('s0') select theString, intPrimitive from SupportBean").statement("s0");
        stmt.setSubscriber(subscriber);

        env.sendEventBean(new SupportBean("E1", 10));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1", 10});

        env.undeployAll();
    }

    private static class ClientRuntimeSubscriberBindWildcardJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatement stmtOne = env.compileDeploy("@name('s0') select * from SupportBean").statement("s0");
            tryInvalid(this, stmtOne, "Subscriber object does not provide a public method by name 'update'");
            tryInvalid(new DummySubscriberEmptyUpd(), stmtOne, "No suitable subscriber method named 'update' found, expecting a method that takes 1 parameter of type SupportBean");
            tryInvalid(new DummySubscriberMultipleUpdate(), stmtOne, "No suitable subscriber method named 'update' found, expecting a method that takes 1 parameter of type SupportBean");
            tryInvalid(new DummySubscriberUpdate(), stmtOne, "Subscriber method named 'update' for parameter number 1 is not assignable, expecting type 'SupportBean' but found type 'SupportMarketDataBean'");
            tryInvalid(new DummySubscriberPrivateUpd(), stmtOne, "Subscriber object does not provide a public method by name 'update'");
            env.undeployModuleContaining("s0");

            EPStatement stmtTwo = env.compileDeploy("@name('s0') select intPrimitive from SupportBean").statement("s0");
            String message = "Subscriber 'updateRStream' method footprint must match 'update' method footprint";
            tryInvalid(new DummySubscriberMismatchUpdateRStreamOne(), stmtTwo, message);
            tryInvalid(new DummySubscriberMismatchUpdateRStreamTwo(), stmtTwo, message);

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberInvocationTargetEx implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // smoke test, need to consider log file; test for ESPER-331
            EPStatement stmt = env.compileDeploy("@name('s0') select * from SupportMarketDataBean").statement("s0");
            stmt.setSubscriber(new DummySubscriberException());
            stmt.addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    throw new RuntimeException("test exception 2");
                }
            });
            stmt.addListenerWithReplay(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    throw new RuntimeException("test exception 3");
                }
            });

            // no exception expected
            env.sendEventBean(new SupportMarketDataBean("IBM", 0, 0L, ""));

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberStartStopStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SubscriberInterface subscriber = new SubscriberInterface();

            EPCompiled compiled = env.compile("@name('s0') select * from SupportMarkerInterface");
            EPStatement stmt = env.deploy(compiled).statement("s0");
            stmt.setSubscriber(subscriber);

            SupportBean_A a1 = new SupportBean_A("A1");
            env.sendEventBean(a1);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{a1}, subscriber.getAndResetIndicate().toArray());

            SupportBean_B b1 = new SupportBean_B("B1");
            env.sendEventBean(b1);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{b1}, subscriber.getAndResetIndicate().toArray());

            env.undeployAll();

            SupportBean_C c1 = new SupportBean_C("C1");
            env.sendEventBean(c1);
            assertEquals(0, subscriber.getAndResetIndicate().size());

            env.deploy(compiled).statement("s0").setSubscriber(subscriber);

            SupportBean_D d1 = new SupportBean_D("D1");
            env.sendEventBean(d1);
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{d1}, subscriber.getAndResetIndicate().toArray());

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberVariables implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "myvar".split(",");
            SubscriberMap subscriberCreateVariable = new SubscriberMap();
            String stmtTextCreate = "@name('s0') create variable string myvar = 'abc'";
            EPStatement stmt = env.compileDeploy(stmtTextCreate, path).statement("s0");
            stmt.setSubscriber(subscriberCreateVariable);

            SubscriberMap subscriberSetVariable = new SubscriberMap();
            String stmtTextSet = "@name('s1') on SupportBean set myvar = theString";
            stmt = env.compileDeploy(stmtTextSet, path).statement("s1");
            stmt.setSubscriber(subscriberSetVariable);

            env.sendEventBean(new SupportBean("def", 1));
            EPAssertionUtil.assertPropsMap(subscriberCreateVariable.getAndResetIndicate().get(0), fields, new Object[]{"def"});
            EPAssertionUtil.assertPropsMap(subscriberSetVariable.getAndResetIndicate().get(0), fields, new Object[]{"def"});

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionNamedWindow(env, EventRepresentationChoice.MAP);
        }
    }

    private static void tryAssertionNamedWindow(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        String[] fields = "key,value".split(",");
        RegressionPath path = new RegressionPath();
        SubscriberMap subscriberNamedWindow = new SubscriberMap();
        String stmtTextCreate = eventRepresentationEnum.getAnnotationText() + " @name('create') create window MyWindow#keepall as select theString as key, intPrimitive as value from SupportBean";
        EPStatement stmt = env.compileDeploy(stmtTextCreate, path).statement("create");
        stmt.setSubscriber(subscriberNamedWindow);

        SubscriberFields subscriberInsertInto = new SubscriberFields();
        String stmtTextInsertInto = "@name('insert') insert into MyWindow select theString as key, intPrimitive as value from SupportBean";
        stmt = env.compileDeploy(stmtTextInsertInto, path).statement("insert");
        stmt.setSubscriber(subscriberInsertInto);

        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap(subscriberNamedWindow.getAndResetIndicate().get(0), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 1}}, subscriberInsertInto.getAndResetIndicate());

        // test on-delete
        SubscriberMap subscriberDelete = new SubscriberMap();
        String stmtTextDelete = "@name('ondelete') on SupportMarketDataBean s0 delete from MyWindow s1 where s0.symbol = s1.key";
        stmt = env.compileDeploy(stmtTextDelete, path).statement("ondelete");
        stmt.setSubscriber(subscriberDelete);

        env.sendEventBean(new SupportMarketDataBean("E1", 0, 1L, ""));
        EPAssertionUtil.assertPropsMap(subscriberDelete.getAndResetIndicate().get(0), fields, new Object[]{"E1", 1});

        // test on-select
        SubscriberMap subscriberSelect = new SubscriberMap();
        String stmtTextSelect = "@name('onselect') on SupportMarketDataBean s0 select key, value from MyWindow s1";
        stmt = env.compileDeploy(stmtTextSelect, path).statement("onselect");
        stmt.setSubscriber(subscriberSelect);

        env.sendEventBean(new SupportBean("E2", 2));
        env.sendEventBean(new SupportMarketDataBean("M1", 0, 1L, ""));
        EPAssertionUtil.assertPropsMap(subscriberSelect.getAndResetIndicate().get(0), fields, new Object[]{"E2", 2});

        env.undeployAll();
    }

    private static class ClientRuntimeSubscriberSimpleSelectUpdateOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportSubscriberRowByRowSpecificNStmt subscriber = new SupportSubscriberRowByRowSpecificNStmt();
            EPStatement stmt = env.compileDeploy("@name('s0') select theString, intPrimitive from SupportBean#lastevent").statement("s0");
            stmt.setSubscriber(subscriber);

            // get statement, attach listener
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            // send event
            env.sendEventBean(new SupportBean("E1", 100));
            subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1", 100});
            EPAssertionUtil.assertPropsPerRow(stmt.iterator(), FIELDS, new Object[][]{{"E1", 100}});
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E1", 100});

            // remove listener
            stmt.removeAllListeners();

            // send event
            env.sendEventBean(new SupportBean("E2", 200));
            subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E2", 200});
            EPAssertionUtil.assertPropsPerRow(stmt.iterator(), FIELDS, new Object[][]{{"E2", 200}});
            assertFalse(listener.isInvoked());

            // add listener
            SupportUpdateListener stmtAwareListener = new SupportUpdateListener();
            stmt.addListener(stmtAwareListener);

            // send event
            env.sendEventBean(new SupportBean("E3", 300));
            subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E3", 300});
            EPAssertionUtil.assertPropsPerRow(stmt.iterator(), FIELDS, new Object[][]{{"E3", 300}});
            EPAssertionUtil.assertProps(stmtAwareListener.assertOneGetNewAndReset(), FIELDS, new Object[]{"E3", 300});

            // subscriber with EPStatement in the footprint
            stmt.removeAllListeners();
            SupportSubscriberRowByRowSpecificWStmt subsWithStatement = new SupportSubscriberRowByRowSpecificWStmt();
            stmt.setSubscriber(subsWithStatement);
            env.sendEventBean(new SupportBean("E10", 999));
            subsWithStatement.assertOneReceivedAndReset(stmt, new Object[]{"E10", 999});

            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberPerformanceSyntheticUndelivered implements RegressionExecution {
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            final int numLoop = 100000;
            env.compileDeploy("select theString, intPrimitive from SupportBean(intPrimitive > 10)");

            long start = System.currentTimeMillis();
            for (int i = 0; i < numLoop; i++) {
                env.sendEventBean(new SupportBean("E1", 1000 + i));
            }
            long end = System.currentTimeMillis();

            assertTrue("delta=" + (end - start), end - start < 1000);
            env.undeployAll();
        }
    }

    private static class ClientRuntimeSubscriberPerformanceSynthetic implements RegressionExecution {
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            final int numLoop = 100000;
            EPStatement stmt = env.compileDeploy("@name('s0') select theString, intPrimitive from SupportBean(intPrimitive > 10)").statement("s0");
            final List<Object[]> results = new ArrayList<Object[]>();

            UpdateListener listener = new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    String theString = (String) newEvents[0].get("theString");
                    int val = (Integer) newEvents[0].get("intPrimitive");
                    results.add(new Object[]{theString, val});
                }
            };
            stmt.addListener(listener);

            long start = System.currentTimeMillis();
            for (int i = 0; i < numLoop; i++) {
                env.sendEventBean(new SupportBean("E1", 1000 + i));
            }
            long end = System.currentTimeMillis();

            assertEquals(numLoop, results.size());
            for (int i = 0; i < numLoop; i++) {
                EPAssertionUtil.assertEqualsAnyOrder(results.get(i), new Object[]{"E1", 1000 + i});
            }
            assertTrue("delta=" + (end - start), end - start < 1000);

            env.undeployAll();
        }
    }

    private static void tryInvalid(Object subscriber, EPStatement stmt, String message) {
        try {
            stmt.setSubscriber(subscriber);
            fail();
        } catch (EPSubscriberException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    public static class DummySubscriberException {
        public void update(SupportMarketDataBean bean) {
            throw new RuntimeException("DummySubscriberException-generated");
        }
    }

    public static class DummySubscriberEmptyUpd {
        public void update() {
        }
    }

    public static class DummySubscriberPrivateUpd {
        private void update(SupportBean bean) {
        }
    }

    public static class DummySubscriberUpdate {
        public void update(SupportMarketDataBean dummy) {
        }
    }

    public static class DummySubscriberMultipleUpdate {
        public void update(long x) {
        }

        public void update(int x) {
        }
    }

    public static class DummySubscriberMismatchUpdateRStreamOne {
        public void update(int value) {
        }

        public void updateRStream(EPStatement stmt, int value) {
        }
    }

    public static class DummySubscriberMismatchUpdateRStreamTwo {
        public void update(EPStatement stmt, int value) {
        }

        public void updateRStream(int value) {
        }
    }

    public static class SubscriberFields {
        private ArrayList<Object[]> indicate = new ArrayList<>();

        public void update(String key, int value) {
            indicate.add(new Object[]{key, value});
        }

        List<Object[]> getAndResetIndicate() {
            List<Object[]> result = indicate;
            indicate = new ArrayList<>();
            return result;
        }
    }

    public static class SubscriberInterface {
        private ArrayList<SupportMarkerInterface> indicate = new ArrayList<>();

        public void update(SupportMarkerInterface impl) {
            indicate.add(impl);
        }

        List<SupportMarkerInterface> getAndResetIndicate() {
            List<SupportMarkerInterface> result = indicate;
            indicate = new ArrayList<>();
            return result;
        }
    }

    public static class SubscriberMap {
        private ArrayList<Map> indicate = new ArrayList<>();

        public void update(Map row) {
            indicate.add(row);
        }

        List<Map> getAndResetIndicate() {
            List<Map> result = indicate;
            indicate = new ArrayList<>();
            return result;
        }
    }

    public static class MyLocalJsonProvidedWidenedEvent implements Serializable {
        public byte bytePrimitive;
        public int intPrimitive;
        public long longPrimitive;
        public float floatPrimitive;
    }

    public static class MyLocalJsonProvidedStringInt implements Serializable {
        public String theString;
        public Integer intPrimitive;
    }
}
