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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.bean.BeanEventBean;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.util.EventRepresentationEnum;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestInsertInto extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener feedListener;
    private SupportUpdateListener resultListenerDelta;
    private SupportUpdateListener resultListenerProduct;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        feedListener = new SupportUpdateListener();
        resultListenerDelta = new SupportUpdateListener();
        resultListenerProduct = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        resultListenerDelta = null;
        feedListener = null;
        resultListenerProduct = null;
    }

    public void testAssertionWildcardRecast() {
        // bean to OA/Map/bean
        runAssertionWildcardRecast(true, null, false, EventRepresentationEnum.OBJECTARRAY);
        runAssertionWildcardRecast(true, null, false, EventRepresentationEnum.MAP);
        try {
            runAssertionWildcardRecast(true, null, true, null);
            fail();
        }
        catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Expression-returned event type 'SourceSchema' with underlying type 'com.espertech.esper.regression.epl.TestInsertInto$MyP0P1EventSource' cannot be converted to target event type 'TargetSchema' with underlying type ");
        }

        // OA to OA/Map/bean
        runAssertionWildcardRecast(false, EventRepresentationEnum.OBJECTARRAY, false, EventRepresentationEnum.OBJECTARRAY);
        runAssertionWildcardRecast(false, EventRepresentationEnum.OBJECTARRAY, false, EventRepresentationEnum.MAP);
        runAssertionWildcardRecast(false, EventRepresentationEnum.OBJECTARRAY, true, null);

        // Map to OA/Map/bean
        runAssertionWildcardRecast(false, EventRepresentationEnum.MAP, false, EventRepresentationEnum.OBJECTARRAY);
        runAssertionWildcardRecast(false, EventRepresentationEnum.MAP, false, EventRepresentationEnum.MAP);
        runAssertionWildcardRecast(false, EventRepresentationEnum.MAP, true, null);
    }

    private void runAssertionWildcardRecast(boolean sourceBean, EventRepresentationEnum sourceType,
                                            boolean targetBean, EventRepresentationEnum targetType) {
        try {
            runAssertionWildcardRecastInternal(sourceBean, sourceType, targetBean, targetType);
        }
        finally {
            // cleanup
            epService.getEPAdministrator().destroyAllStatements();
            epService.getEPAdministrator().getConfiguration().removeEventType("TargetSchema", false);
            epService.getEPAdministrator().getConfiguration().removeEventType("SourceSchema", false);
            epService.getEPAdministrator().getConfiguration().removeEventType("TargetContainedSchema", false);
        }
    }

    private void runAssertionWildcardRecastInternal(boolean sourceBean, EventRepresentationEnum sourceType,
                                            boolean targetBean, EventRepresentationEnum targetType) {
        // declare source type
        if (sourceBean) {
            epService.getEPAdministrator().createEPL("create schema SourceSchema as " + MyP0P1EventSource.class.getName());
        }
        else {
            epService.getEPAdministrator().createEPL("create " + sourceType.getOutputTypeCreateSchemaName() + " schema SourceSchema as (p0 string, p1 int)");
        }

        // declare target type
        if (targetBean) {
            epService.getEPAdministrator().createEPL("create schema TargetSchema as " + MyP0P1EventTarget.class.getName());
        }
        else {
            epService.getEPAdministrator().createEPL("create " + targetType.getOutputTypeCreateSchemaName() + " schema TargetContainedSchema as (c0 int)");
            epService.getEPAdministrator().createEPL("create " + targetType.getOutputTypeCreateSchemaName() + " schema TargetSchema (p0 string, p1 int, c0 TargetContainedSchema)");
        }

        // insert-into and select
        epService.getEPAdministrator().createEPL("insert into TargetSchema select * from SourceSchema");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from TargetSchema");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // send event
        if (sourceBean) {
            epService.getEPRuntime().sendEvent(new MyP0P1EventSource("a", 10));
        }
        else if (sourceType == EventRepresentationEnum.MAP) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("p0", "a");
            map.put("p1", 10);
            epService.getEPRuntime().sendEvent(map, "SourceSchema");
        }
        else {
            epService.getEPRuntime().sendEvent(new Object[] {"a", 10}, "SourceSchema");
        }

        // assert
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,p1,c0".split(","), new Object[]{"a", 10, null});
    }

    public void testVariantRStreamOMToStmt() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setInsertInto(InsertIntoClause.create("Event_1", new String[0], StreamSelector.RSTREAM_ONLY));
        model.setSelectClause(SelectClause.create().add("intPrimitive", "intBoxed"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        EPStatement stmt = epService.getEPAdministrator().create(model, "s1");

        String epl = "insert rstream into Event_1 " +
                      "select intPrimitive, intBoxed " +
                      "from " + SupportBean.class.getName();
        assertEquals(epl, model.toEPL());
        assertEquals(epl, stmt.getText());

        EPStatementObjectModel modelTwo = epService.getEPAdministrator().compileEPL(model.toEPL());
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, modelTwo.toEPL());

        // assert statement-type reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse("Event_1"));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertTrue(stmtNames.contains("s1"));

        stmt.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse("Event_1"));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertFalse(stmtNames.contains("s1"));
    }

    public void testVariantOneOMToStmt() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setInsertInto(InsertIntoClause.create("Event_1", "delta", "product"));
        model.setSelectClause(SelectClause.create().add(Expressions.minus("intPrimitive", "intBoxed"), "deltaTag")
                .add(Expressions.multiply("intPrimitive", "intBoxed"), "productTag"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView(View.create("win", "length", Expressions.constant(100)))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        EPStatement stmt = runAsserts(null, model);

        String epl = "insert into Event_1(delta, product) " +
                      "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";
        assertEquals(epl, model.toEPL());
        assertEquals(epl, stmt.getText());
    }

    public void testVariantOneEPLToOMStmt() throws Exception
    {
        String epl = "insert into Event_1(delta, product) " +
                      "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = runAsserts(null, model);
        assertEquals(epl, stmt.getText());
    }

    public void testVariantOne()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
                      "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";

        runAsserts(stmtText, null);
    }

    public void testVariantOneStateless()
    {
        String stmtTextStateless = "insert into Event_1 (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from " + SupportBean.class.getName();
        runAsserts(stmtTextStateless, null);
    }

    public void testVariantOneWildcard()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
        "select * from " + SupportBean.class.getName() + ".win:length(100)";

        try{
        	epService.getEPAdministrator().createEPL(stmtText);
        	fail();
        }
        catch(EPStatementException ex)
        {
        	// Expected
        }

        // assert statement-type reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertFalse(spi.getStatementEventTypeRef().isInUse("Event_1"));

        // test insert wildcard to wildcard
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        String stmtSelectText = "insert into ABCStream select * from SupportBean";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtSelectText, "resilient i0");
        stmtSelect.addListener(listener);
        assertTrue(stmtSelect.getEventType() instanceof BeanEventType);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNew().get("theString"));
        assertTrue(listener.assertOneGetNew() instanceof BeanEventBean);
    }

    public void testVariantOneJoin()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
                      "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100) as s0," +
                                SupportBean_A.class.getName() + ".win:length(100) as s1 " +
                      " where s0.theString = s1.id";

        runAsserts(stmtText, null);
    }

    public void testVariantOneJoinWildcard()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
        "select * " +
        "from " + SupportBean.class.getName() + ".win:length(100) as s0," +
                  SupportBean_A.class.getName() + ".win:length(100) as s1 " +
        " where s0.theString = s1.id";

        try{
        	epService.getEPAdministrator().createEPL(stmtText);
        	fail();
        }
        catch(EPStatementException ex)
        {
        	// Expected
        }
    }

    public void testVariantTwo()
    {
        String stmtText = "insert into Event_1 " +
                      "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";

        runAsserts(stmtText, null);
    }

    public void testVariantTwoWildcard() throws InterruptedException
    {
        String stmtText = "insert into event1 select * from " + SupportBean.class.getName() + ".win:length(100)";
        String otherText = "select * from default.event1.win:length(10)";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtText, "stmt1");
        assertEquals(StatementType.INSERT_INTO, ((EPStatementSPI) stmtOne).getStatementMetadata().getStatementType());
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(otherText, "stmt2");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        SupportBean theEvent = sendEvent(10, 11);
        assertTrue(listenerOne.getAndClearIsInvoked());
        assertEquals(1, listenerOne.getLastNewData().length);
        assertEquals(10, listenerOne.getLastNewData()[0].get("intPrimitive"));
        assertEquals(11, listenerOne.getLastNewData()[0].get("intBoxed"));
        assertEquals(20, listenerOne.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertSame(theEvent, listenerOne.getLastNewData()[0].getUnderlying());

        assertTrue(listenerTwo.getAndClearIsInvoked());
        assertEquals(1, listenerTwo.getLastNewData().length);
        assertEquals(10, listenerTwo.getLastNewData()[0].get("intPrimitive"));
        assertEquals(11, listenerTwo.getLastNewData()[0].get("intBoxed"));
        assertEquals(20, listenerTwo.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertSame(theEvent, listenerTwo.getLastNewData()[0].getUnderlying());

        // assert statement-type reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse("event1"));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType("event1");
        EPAssertionUtil.assertEqualsAnyOrder(stmtNames.toArray(), new String[]{"stmt1", "stmt2"});
        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(stmtNames.toArray(), new String[]{"stmt1"});

        stmtOne.destroy();
        assertTrue(spi.getStatementEventTypeRef().isInUse("event1"));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType("event1");
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"stmt2"}, stmtNames.toArray());
        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));

        stmtTwo.destroy();
        assertFalse(spi.getStatementEventTypeRef().isInUse("event1"));
    }

    public void testVariantTwoJoin()
    {
        String stmtText = "insert into Event_1 " +
                      "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                        "from " + SupportBean.class.getName() + ".win:length(100) as s0," +
                                  SupportBean_A.class.getName() + ".win:length(100) as s1 " +
                        " where s0.theString = s1.id";

        runAsserts(stmtText, null);

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("Event_1");
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("Event_1", type.getMetadata().getPrimaryName());
        assertEquals("Event_1", type.getMetadata().getPublicName());
        assertEquals("Event_1", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.STREAM, type.getMetadata().getTypeClass());
        assertEquals(false, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());
    }

    public void testVariantTwoJoinWildcard()
    {
        String textOne = "insert into event2 select * " +
        		          "from " + SupportBean.class.getName() + ".win:length(100) as s0, " +
        		          SupportBean_A.class.getName() + ".win:length(5) as s1 " +
        		          "where s0.theString = s1.id";
        String textTwo = "select * from event2.win:length(10)";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        // send event for joins to match on
        SupportBean_A eventA = new SupportBean_A("myId");
        epService.getEPRuntime().sendEvent(eventA);

        SupportBean eventOne = sendEvent(10, 11);
        assertTrue(listenerOne.getAndClearIsInvoked());
        assertEquals(1, listenerOne.getLastNewData().length);
        assertEquals(2, listenerOne.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertTrue(listenerOne.getLastNewData()[0].getEventType().isProperty("s0"));
        assertTrue(listenerOne.getLastNewData()[0].getEventType().isProperty("s1"));
        assertSame(eventOne, listenerOne.getLastNewData()[0].get("s0"));
        assertSame(eventA, listenerOne.getLastNewData()[0].get("s1"));

        assertTrue(listenerTwo.getAndClearIsInvoked());
        assertEquals(1, listenerTwo.getLastNewData().length);
        assertEquals(2, listenerTwo.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertTrue(listenerTwo.getLastNewData()[0].getEventType().isProperty("s0"));
        assertTrue(listenerTwo.getLastNewData()[0].getEventType().isProperty("s1"));
        assertSame(eventOne, listenerOne.getLastNewData()[0].get("s0"));
        assertSame(eventA, listenerOne.getLastNewData()[0].get("s1"));
    }

    public void testInvalidStreamUsed()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
                      "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";
        epService.getEPAdministrator().createEPL(stmtText);

        try
        {
            stmtText = "insert into Event_1(delta) " +
                      "select (intPrimitive - intBoxed) as deltaTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            // expected
            assertEquals("Error starting statement: Event type named 'Event_1' has already been declared with differing column name or type information: Type by name 'Event_1' expects 2 properties but receives 1 properties [insert into Event_1(delta) select (intPrimitive - intBoxed) as deltaTag from com.espertech.esper.support.bean.SupportBean.win:length(100)]", ex.getMessage());
        }
    }

    public void testWithOutputLimitAndSort()
    {
        // NOTICE: we are inserting the RSTREAM (removed events)
        String stmtText = "insert rstream into StockTicks(mySymbol, myPrice) " +
                          "select symbol, price from " + SupportMarketDataBean.class.getName() + ".win:time(60) " +
                          "output every 5 seconds " +
                          "order by symbol asc";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "select mySymbol, sum(myPrice) as pricesum from StockTicks.win:length(100)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(feedListener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        sendEvent("IBM", 50);
        sendEvent("CSC", 10);
        sendEvent("GE", 20);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10 * 1000));
        sendEvent("DEF", 100);
        sendEvent("ABC", 11);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(30 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(40 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(50 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(55 * 1000));

        assertFalse(feedListener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60 * 1000));

        assertTrue(feedListener.isInvoked());
        assertEquals(3, feedListener.getNewDataList().size());
        assertEquals("CSC", feedListener.getNewDataList().get(0)[0].get("mySymbol"));
        assertEquals(10.0, feedListener.getNewDataList().get(0)[0].get("pricesum"));
        assertEquals("GE", feedListener.getNewDataList().get(1)[0].get("mySymbol"));
        assertEquals(30.0, feedListener.getNewDataList().get(1)[0].get("pricesum"));
        assertEquals("IBM", feedListener.getNewDataList().get(2)[0].get("mySymbol"));
        assertEquals(80.0, feedListener.getNewDataList().get(2)[0].get("pricesum"));
        feedListener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(65 * 1000));
        assertFalse(feedListener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(70 * 1000));
        assertEquals("ABC", feedListener.getNewDataList().get(0)[0].get("mySymbol"));
        assertEquals(91.0, feedListener.getNewDataList().get(0)[0].get("pricesum"));
        assertEquals("DEF", feedListener.getNewDataList().get(1)[0].get("mySymbol"));
        assertEquals(191.0, feedListener.getNewDataList().get(1)[0].get("pricesum"));
    }

    public void testStaggeredWithWildcard()
    {
    	String statementOne = "insert into streamA select * from " + SupportBeanSimple.class.getName() + ".win:length(5)";
    	String statementTwo = "insert into streamB select *, myInt+myInt as summed, myString||myString as concat from streamA.win:length(5)";
    	String statementThree = "insert into streamC select * from streamB.win:length(5)";

    	SupportUpdateListener listenerOne = new SupportUpdateListener();
    	SupportUpdateListener listenerTwo = new SupportUpdateListener();
    	SupportUpdateListener listenerThree = new SupportUpdateListener();

    	epService.getEPAdministrator().createEPL(statementOne).addListener(listenerOne);
    	epService.getEPAdministrator().createEPL(statementTwo).addListener(listenerTwo);
    	epService.getEPAdministrator().createEPL(statementThree).addListener(listenerThree);

    	sendSimpleEvent("one", 1);
    	assertSimple(listenerOne, "one", 1, null, 0);
    	assertSimple(listenerTwo, "one", 1, "oneone", 2);
    	assertSimple(listenerThree, "one", 1, "oneone", 2);

    	sendSimpleEvent("two", 2);
    	assertSimple(listenerOne, "two", 2, null, 0);
    	assertSimple(listenerTwo, "two", 2, "twotwo", 4);
    	assertSimple(listenerThree, "two", 2, "twotwo", 4);
    }

    public void testInsertFromPattern()
    {
    	String stmtOneText = "insert into streamA select * from pattern [every " + SupportBean.class.getName() + "]";
    	SupportUpdateListener listenerOne = new SupportUpdateListener();
    	EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneText);
        stmtOne.addListener(listenerOne);

        String stmtTwoText = "insert into streamA select * from pattern [every " + SupportBean.class.getName() + "]";
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoText);
        stmtTwo.addListener(listenerTwo);

        EventType eventType = stmtOne.getEventType();
        assertEquals(Map.class, eventType.getUnderlyingType());
    }

    public void testInsertIntoPlusPattern()
    {
        String stmtOneTxt = "insert into InZone " +
                      "select 111 as statementId, mac, locationReportId " +
                      "from " + SupportRFIDEvent.class.getName() + " " +
                      "where mac in ('1','2','3') " +
                      "and zoneID = '10'";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneTxt);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        String stmtTwoTxt = "insert into OutOfZone " +
                      "select 111 as statementId, mac, locationReportId " +
                      "from " + SupportRFIDEvent.class.getName() + " " +
                      "where mac in ('1','2','3') " +
                      "and zoneID != '10'";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoTxt);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        String stmtThreeTxt = "select 111 as eventSpecId, A.locationReportId as locationReportId " +
                      " from pattern [every A=InZone -> (timer:interval(1 sec) and not OutOfZone(mac=A.mac))]";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtThreeTxt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtThree.addListener(listener);

        // try the alert case with 1 event for the mac in question
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPRuntime().sendEvent(new SupportRFIDEvent("LR1", "1", "10"));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("LR1", theEvent.get("locationReportId"));

        listenerOne.reset();
        listenerTwo.reset();

        // try the alert case with 2 events for zone 10 within 1 second for the mac in question
        epService.getEPRuntime().sendEvent(new SupportRFIDEvent("LR2", "2", "10"));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1500));
        epService.getEPRuntime().sendEvent(new SupportRFIDEvent("LR3", "2", "10"));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));

        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("LR2", theEvent.get("locationReportId"));
    }

    public void testNullType()
    {
        String stmtOneTxt = "insert into InZone select null as dummy from java.lang.String";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneTxt);
        assertTrue(stmtOne.getEventType().isProperty("dummy"));

        String stmtTwoTxt = "select dummy from InZone";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoTxt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent("a");
        assertNull(listener.assertOneGetNewAndReset().get("dummy"));
    }

    private void assertSimple(SupportUpdateListener listener, String myString, int myInt, String additionalString, int additionalInt)
    {
    	assertTrue(listener.getAndClearIsInvoked());
    	EventBean eventBean = listener.getLastNewData()[0];
    	assertEquals(myString, eventBean.get("myString"));
    	assertEquals(myInt, eventBean.get("myInt"));
    	if(additionalString != null)
    	{
    		assertEquals(additionalString, eventBean.get("concat"));
    		assertEquals(additionalInt, eventBean.get("summed"));
    	}
    }

    private void sendEvent(String symbol, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, null, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSimpleEvent(String theString, int val)
    {
    	epService.getEPRuntime().sendEvent(new SupportBeanSimple(theString, val));
    }

    private EPStatement runAsserts(String stmtText, EPStatementObjectModel model)
    {
        // Attach listener to feed
        EPStatement stmt = null;
        if (model != null)
        {
            stmt = epService.getEPAdministrator().create(model, "s1");
        }
        else
        {
            stmt = epService.getEPAdministrator().createEPL(stmtText);
        }
        stmt.addListener(feedListener);

        // send event for joins to match on
        epService.getEPRuntime().sendEvent(new SupportBean_A("myId"));

        // Attach delta statement to statement and add listener
        stmtText = "select MIN(delta) as minD, max(delta) as maxD " +
                   "from Event_1.win:time(60)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtText);
        stmtTwo.addListener(resultListenerDelta);

        // Attach prodict statement to statement and add listener
        stmtText = "select min(product) as minP, max(product) as maxP " +
                   "from Event_1.win:time(60)";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtText);
        stmtThree.addListener(resultListenerProduct);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0)); // Set the time to 0 seconds

        // send events
        sendEvent(20, 10);
        assertReceivedFeed(10, 200);
        assertReceivedMinMax(10, 10, 200, 200);

        sendEvent(50, 25);
        assertReceivedFeed(25, 25 * 50);
        assertReceivedMinMax(10, 25, 200, 1250);

        sendEvent(5, 2);
        assertReceivedFeed(3, 2 * 5);
        assertReceivedMinMax(3, 25, 10, 1250);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10 * 1000)); // Set the time to 10 seconds

        sendEvent(13, 1);
        assertReceivedFeed(12, 13);
        assertReceivedMinMax(3, 25, 10, 1250);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(61 * 1000)); // Set the time to 61 seconds
        assertReceivedMinMax(12, 12, 13, 13);

        return stmt;
    }

    private void assertReceivedMinMax(int minDelta, int maxDelta, int minProduct, int maxProduct)
    {
        assertEquals(1, resultListenerDelta.getNewDataList().size());
        assertEquals(1, resultListenerDelta.getLastNewData().length);
        assertEquals(1, resultListenerProduct.getNewDataList().size());
        assertEquals(1, resultListenerProduct.getLastNewData().length);
        assertEquals(minDelta, resultListenerDelta.getLastNewData()[0].get("minD"));
        assertEquals(maxDelta, resultListenerDelta.getLastNewData()[0].get("maxD"));
        assertEquals(minProduct, resultListenerProduct.getLastNewData()[0].get("minP"));
        assertEquals(maxProduct, resultListenerProduct.getLastNewData()[0].get("maxP"));
        resultListenerDelta.reset();
        resultListenerProduct.reset();
    }

    private void assertReceivedFeed(int delta, int product)
    {
        assertEquals(1, feedListener.getNewDataList().size());
        assertEquals(1, feedListener.getLastNewData().length);
        assertEquals(delta, feedListener.getLastNewData()[0].get("delta"));
        assertEquals(product, feedListener.getLastNewData()[0].get("product"));
        feedListener.reset();
    }

    private SupportBean sendEvent(int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString("myId");
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private static class MyP0P1EventSource {
        private final String p0;
        private final int p1;

        private MyP0P1EventSource(String p0, int p1) {
            this.p0 = p0;
            this.p1 = p1;
        }

        public String getP0() {
            return p0;
        }

        public int getP1() {
            return p1;
        }
    }

    private static class MyP0P1EventTarget {
        private String p0;
        private int p1;
        private Object c0;

        private MyP0P1EventTarget() {
        }

        private MyP0P1EventTarget(String p0, int p1, Object c0) {
            this.p0 = p0;
            this.p1 = p1;
            this.c0 = c0;
        }

        public String getP0() {
            return p0;
        }

        public void setP0(String p0) {
            this.p0 = p0;
        }

        public int getP1() {
            return p1;
        }

        public void setP1(int p1) {
            this.p1 = p1;
        }

        public Object getC0() {
            return c0;
        }

        public void setC0(Object c0) {
            this.c0 = c0;
        }
    }
}
