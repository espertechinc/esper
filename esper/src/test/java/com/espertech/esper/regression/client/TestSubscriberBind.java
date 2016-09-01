/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.subscriber.*;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

public class TestSubscriberBind extends TestCase
{
    private EPServiceProvider epService;
    private final String fields[] = "theString,intPrimitive".split(",");

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        String pkg = SupportBean.class.getPackage().getName();
        config.addEventTypeAutoName(pkg);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testBindings() {

        // just wildcard
        EPStatement stmtJustWildcard = epService.getEPAdministrator().createEPL("select * from SupportBean(theString='E2')");
        runAssertionJustWildcard(stmtJustWildcard, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionJustWildcard(stmtJustWildcard, new SupportSubscriberRowByRowSpecificWStmt());
        stmtJustWildcard.destroy();

        // wildcard with props
        EPStatement stmtWildcardWProps = epService.getEPAdministrator().createEPL("select *, intPrimitive + 2, 'x'||theString||'x' from " + SupportBean.class.getName());
        runAssertionWildcardWProps(stmtWildcardWProps, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionWildcardWProps(stmtWildcardWProps, new SupportSubscriberRowByRowSpecificWStmt());
        stmtWildcardWProps.destroy();

        // nested
        EPStatement stmtNested = epService.getEPAdministrator().createEPL("select nested, nested.nestedNested from SupportBeanComplexProps");
        runAssertionNested(stmtNested, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionNested(stmtNested, new SupportSubscriberRowByRowSpecificWStmt());
        stmtNested.destroy();

        // enum
        EPStatement stmtEnum = epService.getEPAdministrator().createEPL("select theString, supportEnum from SupportBeanWithEnum");
        runAssertionEnum(stmtEnum, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionEnum(stmtEnum, new SupportSubscriberRowByRowSpecificWStmt());
        stmtEnum.destroy();

        // null-typed select value
        EPStatement stmtNullSelected = epService.getEPAdministrator().createEPL("select null, longBoxed from SupportBean");
        runAssertionNullSelected(stmtNullSelected, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionNullSelected(stmtNullSelected, new SupportSubscriberRowByRowSpecificWStmt());
        stmtNullSelected.destroy();

        // widening
        runAssertionWidening(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionWidening(EventRepresentationEnum.MAP, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionWidening(EventRepresentationEnum.DEFAULT, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionWidening(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberRowByRowSpecificWStmt());
        runAssertionWidening(EventRepresentationEnum.MAP, new SupportSubscriberRowByRowSpecificWStmt());
        runAssertionWidening(EventRepresentationEnum.DEFAULT, new SupportSubscriberRowByRowSpecificWStmt());

        // r-stream select
        runAssertionRStreamSelect(new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionRStreamSelect(new SupportSubscriberRowByRowSpecificWStmt());

        // stream-selected join
        runAssertionStreamSelectWJoin(new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionStreamSelectWJoin(new SupportSubscriberRowByRowSpecificWStmt());

        // stream-wildcard join
        runAssertionStreamWildcardJoin(new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionStreamWildcardJoin(new SupportSubscriberRowByRowSpecificWStmt());

        // bind wildcard join
        runAssertionBindWildcardJoin(new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionBindWildcardJoin(new SupportSubscriberRowByRowSpecificWStmt());

        // output limit
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.MAP, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.DEFAULT, new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberRowByRowSpecificWStmt());
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.MAP, new SupportSubscriberRowByRowSpecificWStmt());
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.DEFAULT, new SupportSubscriberRowByRowSpecificWStmt());

        // output limit join
        runAssertionOutputLimitJoin(new SupportSubscriberRowByRowSpecificNStmt());
        runAssertionOutputLimitJoin(new SupportSubscriberRowByRowSpecificWStmt());

        // binding-to-map
        runAssertionBindMap(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberMultirowMapNStmt());
        runAssertionBindMap(EventRepresentationEnum.MAP, new SupportSubscriberMultirowMapNStmt());
        runAssertionBindMap(EventRepresentationEnum.DEFAULT, new SupportSubscriberMultirowMapNStmt());
        runAssertionBindMap(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberMultirowMapWStmt());
        runAssertionBindMap(EventRepresentationEnum.MAP, new SupportSubscriberMultirowMapWStmt());
        runAssertionBindMap(EventRepresentationEnum.DEFAULT, new SupportSubscriberMultirowMapWStmt());

        // binding-to-objectarray
        runAssertionBindObjectArr(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberMultirowObjectArrayNStmt());
        runAssertionBindObjectArr(EventRepresentationEnum.MAP, new SupportSubscriberMultirowObjectArrayNStmt());
        runAssertionBindObjectArr(EventRepresentationEnum.DEFAULT, new SupportSubscriberMultirowObjectArrayNStmt());
        runAssertionBindObjectArr(EventRepresentationEnum.OBJECTARRAY, new SupportSubscriberMultirowObjectArrayWStmt());
        runAssertionBindObjectArr(EventRepresentationEnum.MAP, new SupportSubscriberMultirowObjectArrayWStmt());
        runAssertionBindObjectArr(EventRepresentationEnum.DEFAULT, new SupportSubscriberMultirowObjectArrayWStmt());

        // binding-to-underlying-array
        runAssertionBindWildcardIRStream(new SupportSubscriberMultirowUnderlyingNStmt());
        runAssertionBindWildcardIRStream(new SupportSubscriberMultirowUnderlyingWStmt());

        // Object[] and "Object..." binding
        for (EventRepresentationEnum rep : new EventRepresentationEnum[] {EventRepresentationEnum.OBJECTARRAY, EventRepresentationEnum.DEFAULT, EventRepresentationEnum.MAP}) {
            runAssertionObjectArrayDelivery(rep, new SupportSubscriberRowByRowObjectArrayPlainNStmt());
            runAssertionObjectArrayDelivery(rep, new SupportSubscriberRowByRowObjectArrayPlainWStmt());
            runAssertionObjectArrayDelivery(rep, new SupportSubscriberRowByRowObjectArrayVarargNStmt());
            runAssertionObjectArrayDelivery(rep, new SupportSubscriberRowByRowObjectArrayVarargWStmt());
        }

        // Map binding
        for (EventRepresentationEnum rep : new EventRepresentationEnum[] {EventRepresentationEnum.OBJECTARRAY, EventRepresentationEnum.DEFAULT, EventRepresentationEnum.MAP}) {
            runAssertionRowMapDelivery(rep, new SupportSubscriberRowByRowMapNStmt());
            runAssertionRowMapDelivery(rep, new SupportSubscriberRowByRowMapWStmt());
        }

        // static methods
        runAssertionStaticMethod();

        // IR stream individual calls
        runAssertionBindUpdateIRStream(new SupportSubscriberRowByRowFullNStmt());
        runAssertionBindUpdateIRStream(new SupportSubscriberRowByRowFullWStmt());

        // no-params subscriber
        EPStatement stmtNoParamsSubscriber = epService.getEPAdministrator().createEPL("select null from SupportBean");
        runAssertionNoParams(stmtNoParamsSubscriber, new SupportSubscriberNoParamsBaseNStmt());
        runAssertionNoParams(stmtNoParamsSubscriber, new SupportSubscriberNoParamsBaseWStmt());
        stmtNoParamsSubscriber.destroy();

        // named-method subscriber
        EPStatement stmtNamedMethod = epService.getEPAdministrator().createEPL("select theString from SupportBean");
        runAsserionNamedMethod(stmtNamedMethod, new SupportSubscriberMultirowUnderlyingNamedMethodNStmt());
        runAsserionNamedMethod(stmtNamedMethod, new SupportSubscriberMultirowUnderlyingNamedMethodWStmt());
        stmtNamedMethod.destroy();

        // prefer the EPStatement-footprint over the non-EPStatement footprint
        runAssertionPreferEPStatement();
    }

    public void testSubscriberAndListener()
    {
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

        for (String property : stmt.getEventType().getPropertyNames())
        {
            EventPropertyGetter getter = stmt.getEventType().getGetter(property);
            getter.get(theEvent);
        }
    }

    private void runAssertionBindUpdateIRStream(SupportSubscriberRowByRowFullBase subscriber)
    {
        String stmtText = "select irstream theString, intPrimitive from " + SupportBean.class.getName() + ".win:length_batch(2)";
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

    private void runAssertionBindObjectArr(EventRepresentationEnum eventRepresentationEnum, SupportSubscriberMultirowObjectArrayBase subscriber)
    {
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from " + SupportBean.class.getName() + ".win:length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, fields, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, fields, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void runAssertionBindMap(EventRepresentationEnum eventRepresentationEnum, SupportSubscriberMultirowMapBase subscriber)
    {
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from " + SupportBean.class.getName() + ".win:length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertOneReceivedAndReset(stmt, fields, new Object[][]{{"E1", 1}, {"E2", 2}}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        subscriber.assertOneReceivedAndReset(stmt, fields, new Object[][]{{"E3", 3}, {"E4", 4}}, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void runAssertionWidening(EventRepresentationEnum eventRepresentationEnum, SupportSubscriberRowByRowSpecificBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select bytePrimitive, intPrimitive, longPrimitive, floatPrimitive from SupportBean(theString='E1')");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        SupportBean bean = new SupportBean();
        bean.setTheString("E1");
        bean.setBytePrimitive((byte)1);
        bean.setIntPrimitive(2);
        bean.setLongPrimitive(3);
        bean.setFloatPrimitive(4);
        epService.getEPRuntime().sendEvent(bean);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{1, 2L, 3d, 4d});

        stmt.destroy();
    }

    private void runAssertionObjectArrayDelivery(EventRepresentationEnum eventRepresentationEnum, SupportSubscriberRowByRowObjectArrayBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select theString, intPrimitive from SupportBean.std:unique(theString)");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertOneAndReset(stmt, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        subscriber.assertOneAndReset(stmt, new Object[]{"E2", 10});

        stmt.destroy();
    }

    private void runAssertionRowMapDelivery(EventRepresentationEnum eventRepresentationEnum, SupportSubscriberRowByRowMapBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from SupportBean.std:unique(theString)");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertIRStreamAndReset(stmt, fields, new Object[]{"E1", 1}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        subscriber.assertIRStreamAndReset(stmt, fields, new Object[]{"E2", 10}, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        subscriber.assertIRStreamAndReset(stmt, fields, new Object[]{"E1", 2}, new Object[]{"E1", 1});

        stmt.destroy();
    }

    private void runAssertionNested(EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);

        SupportBeanComplexProps theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{theEvent.getNested(), theEvent.getNested().getNestedNested()});
    }

    private void runAssertionEnum(EPStatement stmtEnum, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmtEnum.setSubscriber(subscriber);

        SupportBeanWithEnum theEvent = new SupportBeanWithEnum("abc", SupportEnum.ENUM_VALUE_1);
        epService.getEPRuntime().sendEvent(theEvent);
        subscriber.assertOneReceivedAndReset(stmtEnum, new Object[]{theEvent.getTheString(), theEvent.getSupportEnum()});
    }

    private void runAssertionNullSelected(EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);
        epService.getEPRuntime().sendEvent(new SupportBean());
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{null, null});
    }

    private void runAssertionStreamSelectWJoin(SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select null, s1, s0 from SupportBean.win:keepall() as s0, SupportMarketDataBean.win:keepall() as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{null, s1, s0});

        stmt.destroy();
    }

    private void runAssertionBindWildcardJoin(SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.win:keepall() as s0, SupportMarketDataBean.win:keepall() as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, s1});

        stmt.destroy();
    }

    private void runAssertionJustWildcard(EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);
        SupportBean theEvent = new SupportBean("E2", 1);
        epService.getEPRuntime().sendEvent(theEvent);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{theEvent});
    }

    private void runAssertionStreamWildcardJoin(SupportSubscriberRowByRowSpecificBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString || '<', s1.* as s1, s0.* as s0 from SupportBean.win:keepall() as s0, SupportMarketDataBean.win:keepall() as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{"E1<", s1, s0});

        stmt.destroy();
    }

    private void runAssertionWildcardWProps(EPStatement stmt, SupportSubscriberRowByRowSpecificBase subscriber) {
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        epService.getEPRuntime().sendEvent(s0);
        subscriber.assertOneReceivedAndReset(stmt, new Object[]{s0, 102, "xE1x"});
    }

    private void runAssertionOutputLimitNoJoin(EventRepresentationEnum eventRepresentationEnum, SupportSubscriberRowByRowSpecificBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select theString, intPrimitive from SupportBean output every 2 events");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        subscriber.assertMultipleReceivedAndReset(stmt, new Object[][]{{"E1", 1}, {"E2", 2}});

        stmt.destroy();
    }

    private void runAssertionOutputLimitJoin(SupportSubscriberRowByRowSpecificBase subscriber) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean.win:keepall(), SupportMarketDataBean.win:keepall() where symbol = theString output every 2 events");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E1", 0, 1L, ""));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertNoneReceived();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        subscriber.assertMultipleReceivedAndReset(stmt, new Object[][]{{"E1", 1}, {"E1", 2}});
        stmt.destroy();
    }

    private void runAssertionRStreamSelect(SupportSubscriberRowByRowSpecificBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream s0 from SupportBean.std:unique(theString) as s0");
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

    private void runAssertionBindWildcardIRStream(SupportSubscriberMultirowUnderlyingBase subscriber)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean.win:length_batch(2)");
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

    private void runAssertionStaticMethod()
    {
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

    private void runAssertionNoParams(EPStatement stmt, SupportSubscriberNoParamsBase subscriber) {
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean());
        subscriber.assertCalledAndReset(stmt);
    }

    private void runAsserionNamedMethod(EPStatement stmt, SupportSubscriberMultirowUnderlyingBase subscriber) {
        stmt.setSubscriber(subscriber, "someNewDataMayHaveArrived");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        subscriber.assertOneReceivedAndReset(stmt, new Object[] {"E1"}, null);
    }

    private void runAssertionPreferEPStatement() {
        SupportSubscriberUpdateBothFootprints subscriber = new SupportSubscriberUpdateBothFootprints();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        subscriber.assertOneReceivedAndReset(stmt, new Object[] {"E1", 10});

        stmt.destroy();
    }
}
