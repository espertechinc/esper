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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.subscriber.*;
import com.espertech.esper.support.EventRepresentationChoice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientSubscriberBind implements RegressionExecution {
    private final static String[] FIELDS = "theString,intPrimitive".split(",");

    public void configure(Configuration configuration) throws Exception {
        String pkg = SupportBean.class.getPackage().getName();
        configuration.addEventTypeAutoName(pkg);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionBindings(epService);
        runAssertionSubscriberAndListener(epService);
    }

    private void runAssertionBindings(EPServiceProvider epService) {

        // just wildcard
        EPStatement stmtJustWildcard = epService.getEPAdministrator().createEPL("select * from SupportBean(theString='E2')");
        tryAssertionJustWildcard(epService, stmtJustWildcard, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionJustWildcard(epService, stmtJustWildcard, new SupportSubscriberRowByRowSpecificWStmt());
        stmtJustWildcard.destroy();

        // wildcard with props
        EPStatement stmtWildcardWProps = epService.getEPAdministrator().createEPL("select *, intPrimitive + 2, 'x'||theString||'x' from " + SupportBean.class.getName());
        tryAssertionWildcardWProps(epService, stmtWildcardWProps, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionWildcardWProps(epService, stmtWildcardWProps, new SupportSubscriberRowByRowSpecificWStmt());
        stmtWildcardWProps.destroy();

        // nested
        EPStatement stmtNested = epService.getEPAdministrator().createEPL("select nested, nested.nestedNested from SupportBeanComplexProps");
        tryAssertionNested(epService, stmtNested, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionNested(epService, stmtNested, new SupportSubscriberRowByRowSpecificWStmt());
        stmtNested.destroy();

        // enum
        EPStatement stmtEnum = epService.getEPAdministrator().createEPL("select theString, supportEnum from SupportBeanWithEnum");
        tryAssertionEnum(epService, stmtEnum, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionEnum(epService, stmtEnum, new SupportSubscriberRowByRowSpecificWStmt());
        stmtEnum.destroy();

        // null-typed select value
        EPStatement stmtNullSelected = epService.getEPAdministrator().createEPL("select null, longBoxed from SupportBean");
        tryAssertionNullSelected(epService, stmtNullSelected, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionNullSelected(epService, stmtNullSelected, new SupportSubscriberRowByRowSpecificWStmt());
        stmtNullSelected.destroy();

        // widening
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionWidening(epService, rep, new SupportSubscriberRowByRowSpecificNStmt());
        }
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionWidening(epService, rep, new SupportSubscriberRowByRowSpecificWStmt());
        }

        // r-stream select
        tryAssertionRStreamSelect(epService, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionRStreamSelect(epService, new SupportSubscriberRowByRowSpecificWStmt());

        // stream-selected join
        tryAssertionStreamSelectWJoin(epService, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionStreamSelectWJoin(epService, new SupportSubscriberRowByRowSpecificWStmt());

        // stream-wildcard join
        tryAssertionStreamWildcardJoin(epService, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionStreamWildcardJoin(epService, new SupportSubscriberRowByRowSpecificWStmt());

        // bind wildcard join
        tryAssertionBindWildcardJoin(epService, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionBindWildcardJoin(epService, new SupportSubscriberRowByRowSpecificWStmt());

        // output limit
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionOutputLimitNoJoin(epService, rep, new SupportSubscriberRowByRowSpecificNStmt());
        }
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionOutputLimitNoJoin(epService, rep, new SupportSubscriberRowByRowSpecificWStmt());
        }

        // output limit join
        tryAssertionOutputLimitJoin(epService, new SupportSubscriberRowByRowSpecificNStmt());
        tryAssertionOutputLimitJoin(epService, new SupportSubscriberRowByRowSpecificWStmt());

        // binding-to-map
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionBindMap(epService, rep, new SupportSubscriberMultirowMapNStmt());
        }
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionBindMap(epService, rep, new SupportSubscriberMultirowMapWStmt());
        }

        // binding-to-objectarray
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionBindObjectArr(epService, rep, new SupportSubscriberMultirowObjectArrayNStmt());
        }
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionBindObjectArr(epService, rep, new SupportSubscriberMultirowObjectArrayWStmt());
        }

        // binding-to-underlying-array
        tryAssertionBindWildcardIRStream(epService, new SupportSubscriberMultirowUnderlyingNStmt());
        tryAssertionBindWildcardIRStream(epService, new SupportSubscriberMultirowUnderlyingWStmt());

        // Object[] and "Object..." binding
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionObjectArrayDelivery(epService, rep, new SupportSubscriberRowByRowObjectArrayPlainNStmt());
            tryAssertionObjectArrayDelivery(epService, rep, new SupportSubscriberRowByRowObjectArrayPlainWStmt());
            tryAssertionObjectArrayDelivery(epService, rep, new SupportSubscriberRowByRowObjectArrayVarargNStmt());
            tryAssertionObjectArrayDelivery(epService, rep, new SupportSubscriberRowByRowObjectArrayVarargWStmt());
        }

        // Map binding
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionRowMapDelivery(epService, rep, new SupportSubscriberRowByRowMapNStmt());
            tryAssertionRowMapDelivery(epService, rep, new SupportSubscriberRowByRowMapWStmt());
        }

        // static methods
        tryAssertionStaticMethod(epService);

        // IR stream individual calls
        tryAssertionBindUpdateIRStream(epService, new SupportSubscriberRowByRowFullNStmt());
        tryAssertionBindUpdateIRStream(epService, new SupportSubscriberRowByRowFullWStmt());

        // no-params subscriber
        EPStatement stmtNoParamsSubscriber = epService.getEPAdministrator().createEPL("select null from SupportBean");
        tryAssertionNoParams(epService, stmtNoParamsSubscriber, new SupportSubscriberNoParamsBaseNStmt());
        tryAssertionNoParams(epService, stmtNoParamsSubscriber, new SupportSubscriberNoParamsBaseWStmt());
        stmtNoParamsSubscriber.destroy();

        // named-method subscriber
        EPStatement stmtNamedMethod = epService.getEPAdministrator().createEPL("select theString from SupportBean");
        tryAsserionNamedMethod(epService, stmtNamedMethod, new SupportSubscriberMultirowUnderlyingNamedMethodNStmt());
        tryAsserionNamedMethod(epService, stmtNamedMethod, new SupportSubscriberMultirowUnderlyingNamedMethodWStmt());
        stmtNamedMethod.destroy();

        // prefer the EPStatement-footprint over the non-EPStatement footprint
        tryAssertionPreferEPStatement(epService);
    }

    private void runAssertionSubscriberAndListener(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("insert into A1 select s.*, 1 as a from SupportBean as s");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select a1.* from A1 as a1");

        SupportUpdateListener listener = new SupportUpdateListener();
        SupportSubscriberRowByRowObjectArrayPlainNStmt subscriber = new SupportSubscriberRowByRowObjectArrayPlainNStmt();

        stmt.addListener(listener);
        stmt.setSubscriber(subscriber);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("E1", theEvent.get("theString"));
        assertEquals(1, theEvent.get("intPrimitive"));
        assertTrue(theEvent.getUnderlying() instanceof Pair);

        for (String property : stmt.getEventType().getPropertyNames()) {
            EventPropertyGetter getter = stmt.getEventType().getGetter(property);
            getter.get(theEvent);
        }
    }

    private void tryAssertionBindUpdateIRStream(EPServiceProvider epService, SupportSubscriberRowByRowFullBase subscriber) {
        String stmtText = "select irstream theString, intPrimitive from " + SupportBean.class.getName() + "#length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, 2, 0, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, 2, 2, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});
    }

    private void tryAssertionBindObjectArr(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberMultirowObjectArrayBase subscriber) {
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from " + SupportBean.class.getName() + "#length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void tryAssertionBindMap(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberMultirowMapBase subscriber) {
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from " + SupportBean.class.getName() + "#length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, FIELDS, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void tryAssertionWidening(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select bytePrimitive, intPrimitive, longPrimitive, floatPrimitive from SupportBean(theString='E1')");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        SupportBean bean = new SupportBean();
        bean.setTheString("E1");
        bean.setBytePrimitive((byte) 1);
        bean.setIntPrimitive(2);
        bean.setLongPrimitive(3);
        bean.setFloatPrimitive(4);
        epService.getEPRuntime().sendEvent(bean);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{1, 2L, 3d, 4d});

        stmt.destroy();
    }

    private void tryAssertionObjectArrayDelivery(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowObjectArrayBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select theString, intPrimitive from SupportBean#unique(theString)");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertOneAndReset(stmt, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        subscriber.assertOneAndReset(stmt, new Object[]{"E2", 10});

        stmt.destroy();
    }

    private void tryAssertionRowMapDelivery(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowMapBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from SupportBean#unique(theString)");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertIRStreamAndReset(stmt, FIELDS, new Object[]{"E1", 1}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        subscriber.assertIRStreamAndReset(stmt, FIELDS, new Object[]{"E2", 10}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        subscriber.assertIRStreamAndReset(stmt, FIELDS, new Object[]{"E1", 2}, new Object[]{"E1", 1});

        stmt.destroy();
    }

    private void tryAssertionNested(EPServiceProvider epService, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);

        SupportBeanComplexProps theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{theEvent.getNested(), theEvent.getNested().getNestedNested()});
    }

    private void tryAssertionEnum(EPServiceProvider epService, EPStatement stmtEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmtEnum.setSubscriber(subscriber);

        SupportBeanWithEnum theEvent = new SupportBeanWithEnum("abc", SupportEnum.ENUM_VALUE_1);
        epService.getEPRuntime().sendEvent(theEvent);
        subscriber.assertOneReceivedAndReset(stmtEnum, new Object[]{theEvent.getTheString(), theEvent.getSupportEnum()});
    }

    private void tryAssertionNullSelected(EPServiceProvider epService, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);
        epService.getEPRuntime().sendEvent(new SupportBean());
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{null, null});
    }

    private void tryAssertionStreamSelectWJoin(EPServiceProvider epService, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select null, s1, s0 from SupportBean#keepall as s0, SupportMarketDataBean#keepall as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{null, s1, s0});

        stmt.destroy();
    }

    private void tryAssertionBindWildcardJoin(EPServiceProvider epService, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean#keepall as s0, SupportMarketDataBean#keepall as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, s1});

        stmt.destroy();
    }

    private void tryAssertionJustWildcard(EPServiceProvider epService, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);
        SupportBean theEvent = new SupportBean("E2", 1);
        epService.getEPRuntime().sendEvent(theEvent);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{theEvent});
    }

    private void tryAssertionStreamWildcardJoin(EPServiceProvider epService, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString || '<', s1.* as s1, s0.* as s0 from SupportBean#keepall as s0, SupportMarketDataBean#keepall as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1<", s1, s0});

        stmt.destroy();
    }

    private void tryAssertionWildcardWProps(EPServiceProvider epService, EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        epService.getEPRuntime().sendEvent(s0);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, 102, "xE1x"});
    }

    private void tryAssertionOutputLimitNoJoin(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select theString, intPrimitive from SupportBean output every 2 events");
        stmt.setSubscriber(subscriber);
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertMultipleReceivedAndReset(stmt, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void tryAssertionOutputLimitJoin(EPServiceProvider epService, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean#keepall, SupportMarketDataBean#keepall where symbol = theString output every 2 events");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E1", 0, 1L, ""));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        subscriber.assertMultipleReceivedAndReset(stmt, new Object[][]{{"E1", 1}, {"E1", 2}});
        stmt.destroy();
    }

    private void tryAssertionRStreamSelect(EPServiceProvider epService, SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream s0 from SupportBean#unique(theString) as s0");
        stmt.setSubscriber(subscriber);

        // send event
        SupportBean s0 = new SupportBean("E1", 100);
        epService.getEPRuntime().sendEvent(s0);
        subscriber.assertNoneReceived();

        SupportBean s1 = new SupportBean("E2", 200);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertNoneReceived();

        SupportBean s2 = new SupportBean("E1", 300);
        epService.getEPRuntime().sendEvent(s2);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0});

        stmt.destroy();
    }

    private void tryAssertionBindWildcardIRStream(EPServiceProvider epService, SupportSubscriberMultirowUnderlyingBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean#length_batch(2)");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportBean s1 = new SupportBean("E2", 200);
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, s1}, null);

        SupportBean s2 = new SupportBean("E3", 300);
        SupportBean s3 = new SupportBean("E4", 400);
        epService.getEPRuntime().sendEvent(s2);
        epService.getEPRuntime().sendEvent(s3);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s2, s3}, new Object[]{s0, s1});

        stmt.destroy();
    }

    private void tryAssertionStaticMethod(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from " + SupportBean.class.getName());

        SupportSubscriberRowByRowStatic subscriber = new SupportSubscriberRowByRowStatic();
        stmt.setSubscriber(subscriber);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 100}}, SupportSubscriberRowByRowStatic.getAndResetIndicate());

        SupportSubscriberRowByRowStaticWStatement subscriberWStmt = new SupportSubscriberRowByRowStaticWStatement();
        stmt.setSubscriber(subscriberWStmt);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 200));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E2", 200}}, SupportSubscriberRowByRowStaticWStatement.getIndicate());
        assertEquals(stmt, SupportSubscriberRowByRowStaticWStatement.getStatements().get(0));
        subscriberWStmt.reset();

        stmt.destroy();
    }

    private void tryAssertionNoParams(EPServiceProvider epService, EPStatement stmt, SupportSubscriberNoParamsBase subscriber) {
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean());
        subscriber.assertCalledAndReset(stmt);
    }

    private void tryAsserionNamedMethod(EPServiceProvider epService, EPStatement stmt, SupportSubscriberMultirowUnderlyingBase subscriber) {
        stmt.setSubscriber(subscriber, "someNewDataMayHaveArrived");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1"}, null);
    }

    private void tryAssertionPreferEPStatement(EPServiceProvider epService) {
        SupportSubscriberUpdateBothFootprints subscriber = new SupportSubscriberUpdateBothFootprints();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1", 10});

        stmt.destroy();
    }
}
