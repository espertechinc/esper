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
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.Map;

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

    public void testSubscriberandListener()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("insert into A1 select s.*, 1 as a from SupportBean as s");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select a1.* from A1 as a1");

        SupportUpdateListener listener = new SupportUpdateListener();
        MySubscriberRowByRowObjectArr subscriber = new MySubscriberRowByRowObjectArr();

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

    public void testOutputLimitNoJoin() {
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.OBJECTARRAY);
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.MAP);
        runAssertionOutputLimitNoJoin(EventRepresentationEnum.DEFAULT);
    }

    private void runAssertionOutputLimitNoJoin(EventRepresentationEnum eventRepresentationEnum)
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select theString, intPrimitive from SupportBean output every 2 events");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, subscriber.getAndResetIndicate().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 1}, {"E2", 2}}, subscriber.getAndResetIndicate());

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testOutputLimitJoin()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean.win:keepall(), SupportMarketDataBean.win:keepall() where symbol = theString output every 2 events");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("E1", 0, 1L, ""));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, subscriber.getAndResetIndicate().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 1}, {"E1", 2}}, subscriber.getAndResetIndicate());
    }

    public void testSimpleSelectStatic()
    {
        MySubscriberRowByRowSpecificStatic subscriber = new MySubscriberRowByRowSpecificStatic();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from " + SupportBean.class.getName());
        stmt.setSubscriber(subscriber);

        // send event
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 100}}, subscriber.getAndResetIndicate());
    }

    public void testRStreamSelect()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select rstream s0 from SupportBean.std:unique(theString) as s0");
        stmt.setSubscriber(subscriber);

        // send event
        SupportBean s0 = new SupportBean("E1", 100);
        epService.getEPRuntime().sendEvent(s0);
        assertEquals(0, subscriber.getAndResetIndicate().size());

        SupportBean s1 = new SupportBean("E2", 200);
        epService.getEPRuntime().sendEvent(s1);
        assertEquals(0, subscriber.getAndResetIndicate().size());

        SupportBean s2 = new SupportBean("E1", 300);
        epService.getEPRuntime().sendEvent(s2);
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{s0}}, subscriber.getAndResetIndicate());
    }

    public void testStreamSelectJoin()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select null, s1, s0 from SupportBean.win:keepall() as s0, SupportMarketDataBean.win:keepall() as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        // send event
        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{null, s1, s0}}, subscriber.getAndResetIndicate());
    }

    public void testStreamWildcardJoin()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString || '<', s1.* as s1, s0.* as s0 from SupportBean.win:keepall() as s0, SupportMarketDataBean.win:keepall() as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        // send event
        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1<", s1, s0}}, subscriber.getAndResetIndicate());
    }

    public void testBindWildcardJoin()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean.win:keepall() as s0, SupportMarketDataBean.win:keepall() as s1 where s0.theString = s1.symbol");
        stmt.setSubscriber(subscriber);

        // send event
        SupportBean s0 = new SupportBean("E1", 100);
        SupportMarketDataBean s1 = new SupportMarketDataBean("E1", 0, 0L, "");
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{s0, s1}}, subscriber.getAndResetIndicate());
    }

    public void testBindWildcardPlusProperties()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select *, intPrimitive + 2, 'x'||theString||'x' from " + SupportBean.class.getName());
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        epService.getEPRuntime().sendEvent(s0);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{s0, 102, "xE1x"}, subscriber.getAndResetIndicate().get(0));
    }

    public void testBindWildcardIRStream()
    {
        MySubscriberMultirowUnderlying subscriber = new MySubscriberMultirowUnderlying();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBean.win:length_batch(2)");
        stmt.setSubscriber(subscriber);

        SupportBean s0 = new SupportBean("E1", 100);
        SupportBean s1 = new SupportBean("E2", 200);
        epService.getEPRuntime().sendEvent(s0);
        epService.getEPRuntime().sendEvent(s1);
        assertEquals(1, subscriber.getIndicateArr().size());
        UniformPair<SupportBean[]> beans = subscriber.getAndResetIndicateArr().get(0);
        assertEquals(2, beans.getFirst().length);
        assertEquals(null, beans.getSecond());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{s0, s1}, beans.getFirst());

        SupportBean s2 = new SupportBean("E3", 300);
        SupportBean s3 = new SupportBean("E4", 400);
        epService.getEPRuntime().sendEvent(s2);
        epService.getEPRuntime().sendEvent(s3);
        assertEquals(1, subscriber.getIndicateArr().size());
        beans = subscriber.getAndResetIndicateArr().get(0);
        assertEquals(2, beans.getFirst().length);
        assertEquals(2, beans.getSecond().length);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{s2, s3}, beans.getFirst());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{s0, s1}, beans.getSecond());
    }

    public void testBindUpdateIRStream()
    {
        MySubscriberRowByRowFull subscriber = new MySubscriberRowByRowFull();
        String stmtText = "select irstream theString, intPrimitive from " + SupportBean.class.getName() + ".win:length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, subscriber.getIndicateStart().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(1, subscriber.getIndicateStart().size());
        UniformPair<Integer> pairLength = subscriber.getAndResetIndicateStart().get(0);
        assertEquals(2, (int) pairLength.getFirst());
        assertEquals(0, (int) pairLength.getSecond());
        assertEquals(1, subscriber.getAndResetIndicateEnd().size());
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 1}, {"E2", 2}}, subscriber.getAndResetIndicateIStream());
        EPAssertionUtil.assertEqualsExactOrder(null, subscriber.getAndResetIndicateRStream());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertEquals(0, subscriber.getIndicateStart().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertEquals(1, subscriber.getIndicateStart().size());
        pairLength = subscriber.getAndResetIndicateStart().get(0);
        assertEquals(2, (int) pairLength.getFirst());
        assertEquals(2, (int) pairLength.getSecond());
        assertEquals(1, subscriber.getAndResetIndicateEnd().size());
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E3", 3}, {"E4", 4}}, subscriber.getAndResetIndicateIStream());
        EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{"E1", 1}, {"E2", 2}}, subscriber.getAndResetIndicateRStream());
    }

    public void testBindObjectArr() {
        runAssertionBindObjectArr(EventRepresentationEnum.OBJECTARRAY);
        runAssertionBindObjectArr(EventRepresentationEnum.MAP);
        runAssertionBindObjectArr(EventRepresentationEnum.DEFAULT);
    }

    private void runAssertionBindObjectArr(EventRepresentationEnum eventRepresentationEnum)
    {
        MySubscriberMultirowObjectArr subscriber = new MySubscriberMultirowObjectArr();
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from " + SupportBean.class.getName() + ".win:length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, subscriber.getIndicateArr().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(1, subscriber.getIndicateArr().size());
        UniformPair<Object[][]> result = subscriber.getAndResetIndicateArr().get(0);
        assertNull(result.getSecond());
        assertEquals(2, result.getFirst().length);
        EPAssertionUtil.assertEqualsExactOrder(result.getFirst(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertEquals(0, subscriber.getIndicateArr().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertEquals(1, subscriber.getIndicateArr().size());
        result = subscriber.getAndResetIndicateArr().get(0);
        assertEquals(2, result.getFirst().length);
        assertEquals(2, result.getSecond().length);
        EPAssertionUtil.assertEqualsExactOrder(result.getFirst(), fields, new Object[][]{{"E3", 3}, {"E4", 4}});
        EPAssertionUtil.assertEqualsExactOrder(result.getSecond(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testBindMap() {
        runAssertBindMap(EventRepresentationEnum.OBJECTARRAY);
        runAssertBindMap(EventRepresentationEnum.MAP);
        runAssertBindMap(EventRepresentationEnum.DEFAULT);
    }

    public void runAssertBindMap(EventRepresentationEnum eventRepresentationEnum)
    {
        MySubscriberMultirowMap subscriber = new MySubscriberMultirowMap();
        String stmtText = eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from " + SupportBean.class.getName() + ".win:length_batch(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, subscriber.getIndicateMap().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(1, subscriber.getIndicateMap().size());
        UniformPair<Map[]> result = subscriber.getAndResetIndicateMap().get(0);
        assertNull(result.getSecond());
        assertEquals(2, result.getFirst().length);
        EPAssertionUtil.assertPropsPerRow(result.getFirst(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertEquals(0, subscriber.getIndicateMap().size());
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        assertEquals(1, subscriber.getIndicateMap().size());
        result = subscriber.getAndResetIndicateMap().get(0);
        assertEquals(2, result.getFirst().length);
        assertEquals(2, result.getSecond().length);
        EPAssertionUtil.assertPropsPerRow(result.getFirst(), fields, new Object[][]{{"E3", 3}, {"E4", 4}});
        EPAssertionUtil.assertPropsPerRow(result.getSecond(), fields, new Object[][]{{"E1", 1}, {"E2", 2}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testWidening() {
        runAssertionWidening(EventRepresentationEnum.OBJECTARRAY);
        runAssertionBindObjectArr(EventRepresentationEnum.MAP);
        runAssertionBindObjectArr(EventRepresentationEnum.DEFAULT);
    }

    private void runAssertionWidening(EventRepresentationEnum eventRepresentationEnum)
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
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
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 2L, 3d, 4d}, subscriber.getAndResetIndicate().get(0));

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testWildcard()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean(theString='E2')");
        stmt.setSubscriber(subscriber);

        SupportBean theEvent = new SupportBean("E2", 1);
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent}, subscriber.getAndResetIndicate().get(0));
    }

    public void testNested()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select nested, nested.nestedNested from SupportBeanComplexProps");
        stmt.setSubscriber(subscriber);

        SupportBeanComplexProps theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent.getNested(), theEvent.getNested().getNestedNested()}, subscriber.getAndResetIndicate().get(0));
    }

    public void testEnum()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, supportEnum from SupportBeanWithEnum");
        stmt.setSubscriber(subscriber);

        SupportBeanWithEnum theEvent = new SupportBeanWithEnum("abc", SupportEnum.ENUM_VALUE_1);
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{theEvent.getTheString(), theEvent.getSupportEnum()}, subscriber.getAndResetIndicate().get(0));
    }

    public void testNullType()
    {
        MySubscriberRowByRowSpecific subscriber = new MySubscriberRowByRowSpecific();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select null, longBoxed from SupportBean");
        stmt.setSubscriber(subscriber);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{null, null}, subscriber.getAndResetIndicate().get(0));
        stmt.destroy();

        // test null-delivery for no-parameter subscriber
        LocalSubscriberNoParams subscriberNoParams = new LocalSubscriberNoParams();
        stmt = epService.getEPAdministrator().createEPL("select null from SupportBean");
        stmt.setSubscriber(subscriberNoParams);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertTrue(subscriberNoParams.isCalled());
    }

    public void testObjectArrayDelivery() {
        runAssertionObjectArrayDelivery(EventRepresentationEnum.OBJECTARRAY);
        runAssertionObjectArrayDelivery(EventRepresentationEnum.DEFAULT);
        runAssertionObjectArrayDelivery(EventRepresentationEnum.MAP);
    }

    private void runAssertionObjectArrayDelivery(EventRepresentationEnum eventRepresentationEnum)
    {
        MySubscriberRowByRowObjectArr subscriber = new MySubscriberRowByRowObjectArr();
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select theString, intPrimitive from SupportBean.std:unique(theString)");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertEqualsAnyOrder(subscriber.getAndResetIndicate().get(0), new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertEqualsAnyOrder(subscriber.getAndResetIndicate().get(0), new Object[]{"E2", 10});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testRowMapDelivery() {
        runAssertionRowMapDelivery(EventRepresentationEnum.OBJECTARRAY);
        runAssertionRowMapDelivery(EventRepresentationEnum.DEFAULT);
        runAssertionRowMapDelivery(EventRepresentationEnum.MAP);
    }

    private void runAssertionRowMapDelivery(EventRepresentationEnum eventRepresentationEnum)
    {
        MySubscriberRowByRowMap subscriber = new MySubscriberRowByRowMap();
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select irstream theString, intPrimitive from SupportBean.std:unique(theString)");
        stmt.setSubscriber(subscriber);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap(subscriber.getAndResetIndicateIStream().get(0), fields, new Object[]{"E1", 1});
        assertEquals(0, subscriber.getAndResetIndicateRStream().size());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertPropsMap(subscriber.getAndResetIndicateIStream().get(0), fields, new Object[]{"E2", 10});
        assertEquals(0, subscriber.getAndResetIndicateRStream().size());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsMap(subscriber.getAndResetIndicateIStream().get(0), fields, new Object[]{"E1", 2});
        EPAssertionUtil.assertPropsMap(subscriber.getAndResetIndicateRStream().get(0), fields, new Object[]{"E1", 1});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private static class LocalSubscriberNoParams {

        private boolean called = false;

        public void update() {
            called = true;
        }

        public boolean isCalled() {
            return called;
        }

        public void setCalled(boolean called) {
            this.called = called;
        }
    }
}
