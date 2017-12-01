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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
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
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.EventRepresentationChoice;
import com.espertech.esper.util.SerializableObjectCopier;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;

public class ExecInsertInto implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {

        runAssertionAssertionWildcardRecast(epService);
        runAssertionVariantRStreamOMToStmt(epService);
        runAssertionVariantOneOMToStmt(epService);
        runAssertionVariantOneEPLToOMStmt(epService);
        runAssertionVariantOne(epService);
        runAssertionVariantOneStateless(epService);
        runAssertionVariantOneWildcard(epService);
        runAssertionVariantOneJoin(epService);
        runAssertionVariantOneJoinWildcard(epService);
        runAssertionVariantTwo(epService);
        runAssertionVariantTwoWildcard(epService);
        runAssertionVariantTwoJoin(epService);
        runAssertionJoinWildcard(epService);
        runAssertionInvalidStreamUsed(epService);
        runAssertionWithOutputLimitAndSort(epService);
        runAssertionStaggeredWithWildcard(epService);
        runAssertionInsertFromPattern(epService);
        runAssertionInsertIntoPlusPattern(epService);
        runAssertionNullType(epService);
        runAssertionSingleBeanToMulti(epService);
        runAssertionMultiBeanToMulti(epService);
    }

    private void runAssertionMultiBeanToMulti(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MySimpleEventObjectArray.class);
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("insert into MySimpleEventObjectArray select window(*) @eventbean as arr from SupportBean#keepall");
        stmt.addListener(listener);

        SupportBean e1 = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(e1);
        MySimpleEventObjectArray resultOne = (MySimpleEventObjectArray) listener.assertOneGetNewAndReset().getUnderlying();
        EPAssertionUtil.assertEqualsExactOrder(resultOne.arr, new Object[]{e1});

        SupportBean e2 = new SupportBean("E2", 2);
        epService.getEPRuntime().sendEvent(e2);
        MySimpleEventObjectArray resultTwo = (MySimpleEventObjectArray) listener.assertOneGetNewAndReset().getUnderlying();
        EPAssertionUtil.assertEqualsExactOrder(resultTwo.arr, new Object[]{e1, e2});

        epService.getEPAdministrator().destroyAllStatements();
        ;
    }

    private void runAssertionSingleBeanToMulti(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create schema EventOne(sbarr SupportBean[])");
        epService.getEPAdministrator().createEPL("insert into EventOne select maxby(intPrimitive) as sbarr from SupportBean as sb");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from EventOne");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean bean = new SupportBean("E1", 1);
        epService.getEPRuntime().sendEvent(bean);
        EventBean[] events = (EventBean[]) listener.assertOneGetNewAndReset().get("sbarr");
        assertEquals(1, events.length);
        assertSame(bean, events[0].getUnderlying());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAssertionWildcardRecast(EPServiceProvider epService) {
        // bean to OA/Map/bean
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionWildcardRecast(epService, true, null, false, rep);
        }

        try {
            tryAssertionWildcardRecast(epService, true, null, true, null);
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Expression-returned event type 'SourceSchema' with underlying type 'com.espertech.esper.regression.epl.insertinto.ExecInsertInto$MyP0P1EventSource' cannot be converted to target event type 'TargetSchema' with underlying type ");
        }

        // OA
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.ARRAY, false, EventRepresentationChoice.ARRAY);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.ARRAY, false, EventRepresentationChoice.MAP);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.ARRAY, false, EventRepresentationChoice.AVRO);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.ARRAY, true, null);

        // Map
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.ARRAY);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.MAP);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.MAP, false, EventRepresentationChoice.AVRO);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.MAP, true, null);

        // Avro
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.ARRAY);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.MAP);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.AVRO, false, EventRepresentationChoice.AVRO);
        tryAssertionWildcardRecast(epService, false, EventRepresentationChoice.AVRO, true, null);
    }

    private void runAssertionVariantRStreamOMToStmt(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setInsertInto(InsertIntoClause.create("Event_1_RSOM", new String[0], StreamSelector.RSTREAM_ONLY));
        model.setSelectClause(SelectClause.create().add("intPrimitive", "intBoxed"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        EPStatement stmt = epService.getEPAdministrator().create(model, "s1");

        String epl = "insert rstream into Event_1_RSOM " +
                "select intPrimitive, intBoxed " +
                "from " + SupportBean.class.getName();
        assertEquals(epl, model.toEPL());
        assertEquals(epl, stmt.getText());

        EPStatementObjectModel modelTwo = epService.getEPAdministrator().compileEPL(model.toEPL());
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, modelTwo.toEPL());

        // assert statement-type reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse("Event_1_RSOM"));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertTrue(stmtNames.contains("s1"));

        stmt.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse("Event_1_RSOM"));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertFalse(stmtNames.contains("s1"));
    }

    private void runAssertionVariantOneOMToStmt(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setInsertInto(InsertIntoClause.create("Event_1_OMS", "delta", "product"));
        model.setSelectClause(SelectClause.create().add(Expressions.minus("intPrimitive", "intBoxed"), "deltaTag")
                .add(Expressions.multiply("intPrimitive", "intBoxed"), "productTag"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView(View.create("length", Expressions.constant(100)))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        EPStatement stmt = tryAssertsVariant(epService, null, model, "Event_1_OMS");

        String epl = "insert into Event_1_OMS(delta, product) " +
                "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                "from " + SupportBean.class.getName() + "#length(100)";
        assertEquals(epl, model.toEPL());
        assertEquals(epl, stmt.getText());

        stmt.destroy();
    }

    private void runAssertionVariantOneEPLToOMStmt(EPServiceProvider epService) throws Exception {
        String epl = "insert into Event_1_EPL(delta, product) " +
                "select intPrimitive-intBoxed as deltaTag, intPrimitive*intBoxed as productTag " +
                "from " + SupportBean.class.getName() + "#length(100)";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = tryAssertsVariant(epService, null, model, "Event_1_EPL");
        assertEquals(epl, stmt.getText());
        stmt.destroy();
    }

    private void runAssertionVariantOne(EPServiceProvider epService) {
        String stmtText = "insert into Event_1VO (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from " + SupportBean.class.getName() + "#length(100)";

        tryAssertsVariant(epService, stmtText, null, "Event_1VO").destroy();
    }

    private void runAssertionVariantOneStateless(EPServiceProvider epService) {
        String stmtTextStateless = "insert into Event_1VOS (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from " + SupportBean.class.getName();
        tryAssertsVariant(epService, stmtTextStateless, null, "Event_1VOS").destroy();
    }

    private void runAssertionVariantOneWildcard(EPServiceProvider epService) {
        String stmtText = "insert into Event_1W (delta, product) " +
                "select * from " + SupportBean.class.getName() + "#length(100)";

        try {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            // Expected
        }

        // assert statement-type reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertFalse(spi.getStatementEventTypeRef().isInUse("Event_1W"));

        // test insert wildcard to wildcard
        SupportUpdateListener listener = new SupportUpdateListener();

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String stmtSelectText = "insert into ABCStream select * from SupportBean";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtSelectText, "resilient i0");
        stmtSelect.addListener(listener);
        assertTrue(stmtSelect.getEventType() instanceof BeanEventType);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals("E1", listener.assertOneGetNew().get("theString"));
        assertTrue(listener.assertOneGetNew().getUnderlying() instanceof SupportBean);

        stmtSelect.destroy();
    }

    private void runAssertionVariantOneJoin(EPServiceProvider epService) {
        String stmtText = "insert into Event_1J (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from " + SupportBean.class.getName() + "#length(100) as s0," +
                SupportBean_A.class.getName() + "#length(100) as s1 " +
                " where s0.theString = s1.id";

        tryAssertsVariant(epService, stmtText, null, "Event_1J").destroy();
    }

    private void runAssertionVariantOneJoinWildcard(EPServiceProvider epService) {
        String stmtText = "insert into Event_1JW (delta, product) " +
                "select * " +
                "from " + SupportBean.class.getName() + "#length(100) as s0," +
                SupportBean_A.class.getName() + "#length(100) as s1 " +
                " where s0.theString = s1.id";

        try {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            // Expected
        }
    }

    private void runAssertionVariantTwo(EPServiceProvider epService) {
        String stmtText = "insert into Event_1_2 " +
                "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                "from " + SupportBean.class.getName() + "#length(100)";

        tryAssertsVariant(epService, stmtText, null, "Event_1_2").destroy();
    }

    private void runAssertionVariantTwoWildcard(EPServiceProvider epService) throws InterruptedException {
        String stmtText = "insert into event1 select * from " + SupportBean.class.getName() + "#length(100)";
        String otherText = "select * from default.event1#length(10)";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtText, "stmt1");
        assertEquals(StatementType.INSERT_INTO, ((EPStatementSPI) stmtOne).getStatementMetadata().getStatementType());
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(otherText, "stmt2");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        SupportBean theEvent = sendEvent(epService, 10, 11);
        assertTrue(listenerOne.getAndClearIsInvoked());
        assertEquals(1, listenerOne.getLastNewData().length);
        assertEquals(10, listenerOne.getLastNewData()[0].get("intPrimitive"));
        assertEquals(11, listenerOne.getLastNewData()[0].get("intBoxed"));
        assertEquals(21, listenerOne.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertSame(theEvent, listenerOne.getLastNewData()[0].getUnderlying());

        assertTrue(listenerTwo.getAndClearIsInvoked());
        assertEquals(1, listenerTwo.getLastNewData().length);
        assertEquals(10, listenerTwo.getLastNewData()[0].get("intPrimitive"));
        assertEquals(11, listenerTwo.getLastNewData()[0].get("intBoxed"));
        assertEquals(21, listenerTwo.getLastNewData()[0].getEventType().getPropertyNames().length);
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

    private void runAssertionVariantTwoJoin(EPServiceProvider epService) {
        String stmtText = "insert into Event_1_2J " +
                "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                "from " + SupportBean.class.getName() + "#length(100) as s0," +
                SupportBean_A.class.getName() + "#length(100) as s1 " +
                " where s0.theString = s1.id";

        EPStatement stmt = tryAssertsVariant(epService, stmtText, null, "Event_1_2J");

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("Event_1_2J");
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("Event_1_2J", type.getMetadata().getPrimaryName());
        assertEquals("Event_1_2J", type.getMetadata().getPublicName());
        assertEquals("Event_1_2J", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.STREAM, type.getMetadata().getTypeClass());
        assertEquals(false, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        stmt.destroy();
    }

    private void runAssertionJoinWildcard(EPServiceProvider epService) {
        tryAssertionJoinWildcard(epService, true, null);
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionJoinWildcard(epService, false, rep);
        }
    }

    private void runAssertionInvalidStreamUsed(EPServiceProvider epService) {
        String stmtText = "insert into Event_1IS (delta, product) " +
                "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                "from " + SupportBean.class.getName() + "#length(100)";
        epService.getEPAdministrator().createEPL(stmtText);

        try {
            stmtText = "insert into Event_1IS(delta) " +
                    "select (intPrimitive - intBoxed) as deltaTag " +
                    "from " + SupportBean.class.getName() + "#length(100)";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            // expected
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Event type named 'Event_1IS' has already been declared with differing column name or type information: Type by name 'Event_1IS' expects 2 properties but receives 1 properties ");
        }
    }

    private void runAssertionWithOutputLimitAndSort(EPServiceProvider epService) {
        // NOTICE: we are inserting the RSTREAM (removed events)
        String stmtText = "insert rstream into StockTicks(mySymbol, myPrice) " +
                "select symbol, price from " + SupportMarketDataBean.class.getName() + "#time(60) " +
                "output every 5 seconds " +
                "order by symbol asc";
        epService.getEPAdministrator().createEPL(stmtText);

        stmtText = "select mySymbol, sum(myPrice) as pricesum from StockTicks#length(100)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        sendEvent(epService, "IBM", 50);
        sendEvent(epService, "CSC", 10);
        sendEvent(epService, "GE", 20);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10 * 1000));
        sendEvent(epService, "DEF", 100);
        sendEvent(epService, "ABC", 11);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(30 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(40 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(50 * 1000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(55 * 1000));

        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60 * 1000));

        assertTrue(listener.isInvoked());
        assertEquals(3, listener.getNewDataList().size());
        assertEquals("CSC", listener.getNewDataList().get(0)[0].get("mySymbol"));
        assertEquals(10.0, listener.getNewDataList().get(0)[0].get("pricesum"));
        assertEquals("GE", listener.getNewDataList().get(1)[0].get("mySymbol"));
        assertEquals(30.0, listener.getNewDataList().get(1)[0].get("pricesum"));
        assertEquals("IBM", listener.getNewDataList().get(2)[0].get("mySymbol"));
        assertEquals(80.0, listener.getNewDataList().get(2)[0].get("pricesum"));
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(65 * 1000));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(70 * 1000));
        assertEquals("ABC", listener.getNewDataList().get(0)[0].get("mySymbol"));
        assertEquals(91.0, listener.getNewDataList().get(0)[0].get("pricesum"));
        assertEquals("DEF", listener.getNewDataList().get(1)[0].get("mySymbol"));
        assertEquals(191.0, listener.getNewDataList().get(1)[0].get("pricesum"));

        statement.destroy();
    }

    private void runAssertionStaggeredWithWildcard(EPServiceProvider epService) {
        String statementOne = "insert into streamA select * from " + SupportBeanSimple.class.getName() + "#length(5)";
        String statementTwo = "insert into streamB select *, myInt+myInt as summed, myString||myString as concat from streamA#length(5)";
        String statementThree = "insert into streamC select * from streamB#length(5)";

        SupportUpdateListener listenerOne = new SupportUpdateListener();
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        SupportUpdateListener listenerThree = new SupportUpdateListener();

        epService.getEPAdministrator().createEPL(statementOne).addListener(listenerOne);
        epService.getEPAdministrator().createEPL(statementTwo).addListener(listenerTwo);
        epService.getEPAdministrator().createEPL(statementThree).addListener(listenerThree);

        sendSimpleEvent(epService, "one", 1);
        assertSimple(listenerOne, "one", 1, null, 0);
        assertSimple(listenerTwo, "one", 1, "oneone", 2);
        assertSimple(listenerThree, "one", 1, "oneone", 2);

        sendSimpleEvent(epService, "two", 2);
        assertSimple(listenerOne, "two", 2, null, 0);
        assertSimple(listenerTwo, "two", 2, "twotwo", 4);
        assertSimple(listenerThree, "two", 2, "twotwo", 4);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInsertFromPattern(EPServiceProvider epService) {
        String stmtOneText = "insert into streamA1 select * from pattern [every " + SupportBean.class.getName() + "]";
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneText);
        stmtOne.addListener(listenerOne);

        String stmtTwoText = "insert into streamA1 select * from pattern [every " + SupportBean.class.getName() + "]";
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoText);
        stmtTwo.addListener(listenerTwo);

        EventType eventType = stmtOne.getEventType();
        assertEquals(Map.class, eventType.getUnderlyingType());

        stmtOne.destroy();
    }

    private void runAssertionInsertIntoPlusPattern(EPServiceProvider epService) {
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

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNullType(EPServiceProvider epService) {
        String stmtOneTxt = "insert into InZoneTwo select null as dummy from java.lang.String";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtOneTxt);
        assertTrue(stmtOne.getEventType().isProperty("dummy"));

        String stmtTwoTxt = "select dummy from InZoneTwo";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTwoTxt);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent("a");
        assertNull(listener.assertOneGetNewAndReset().get("dummy"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertSimple(SupportUpdateListener listener, String myString, int myInt, String additionalString, int additionalInt) {
        assertTrue(listener.getAndClearIsInvoked());
        EventBean eventBean = listener.getLastNewData()[0];
        assertEquals(myString, eventBean.get("myString"));
        assertEquals(myInt, eventBean.get("myInt"));
        if (additionalString != null) {
            assertEquals(additionalString, eventBean.get("concat"));
            assertEquals(additionalInt, eventBean.get("summed"));
        }
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, null, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSimpleEvent(EPServiceProvider epService, String theString, int val) {
        epService.getEPRuntime().sendEvent(new SupportBeanSimple(theString, val));
    }

    private EPStatement tryAssertsVariant(EPServiceProvider epService, String stmtText, EPStatementObjectModel model, String typeName) {
        // Attach listener to feed
        EPStatement stmt;
        if (model != null) {
            stmt = epService.getEPAdministrator().create(model, "s1");
        } else {
            stmt = epService.getEPAdministrator().createEPL(stmtText);
        }
        SupportUpdateListener feedListener = new SupportUpdateListener();
        stmt.addListener(feedListener);

        // send event for joins to match on
        epService.getEPRuntime().sendEvent(new SupportBean_A("myId"));

        // Attach delta statement to statement and add listener
        stmtText = "select MIN(delta) as minD, max(delta) as maxD " +
                "from " + typeName + "#time(60)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener resultListenerDelta = new SupportUpdateListener();
        stmtTwo.addListener(resultListenerDelta);

        // Attach prodict statement to statement and add listener
        stmtText = "select min(product) as minP, max(product) as maxP " +
                "from " + typeName + "#time(60)";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener resultListenerProduct = new SupportUpdateListener();
        stmtThree.addListener(resultListenerProduct);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0)); // Set the time to 0 seconds

        // send events
        sendEvent(epService, 20, 10);
        assertReceivedFeed(feedListener, 10, 200);
        assertReceivedMinMax(resultListenerDelta, resultListenerProduct, 10, 10, 200, 200);

        sendEvent(epService, 50, 25);
        assertReceivedFeed(feedListener, 25, 25 * 50);
        assertReceivedMinMax(resultListenerDelta, resultListenerProduct, 10, 25, 200, 1250);

        sendEvent(epService, 5, 2);
        assertReceivedFeed(feedListener, 3, 2 * 5);
        assertReceivedMinMax(resultListenerDelta, resultListenerProduct, 3, 25, 10, 1250);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10 * 1000)); // Set the time to 10 seconds

        sendEvent(epService, 13, 1);
        assertReceivedFeed(feedListener, 12, 13);
        assertReceivedMinMax(resultListenerDelta, resultListenerProduct, 3, 25, 10, 1250);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(61 * 1000)); // Set the time to 61 seconds
        assertReceivedMinMax(resultListenerDelta, resultListenerProduct, 12, 12, 13, 13);

        return stmt;
    }

    private void assertReceivedMinMax(SupportUpdateListener resultListenerDelta, SupportUpdateListener resultListenerProduct, int minDelta, int maxDelta, int minProduct, int maxProduct) {
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

    private void assertReceivedFeed(SupportUpdateListener feedListener, int delta, int product) {
        assertEquals(1, feedListener.getNewDataList().size());
        assertEquals(1, feedListener.getLastNewData().length);
        assertEquals(delta, feedListener.getLastNewData()[0].get("delta"));
        assertEquals(product, feedListener.getLastNewData()[0].get("product"));
        feedListener.reset();
    }

    private void assertJoinWildcard(EventRepresentationChoice rep, SupportUpdateListener listener, Object eventS0, Object eventS1) {
        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(2, listener.getLastNewData()[0].getEventType().getPropertyNames().length);
        assertTrue(listener.getLastNewData()[0].getEventType().isProperty("s0"));
        assertTrue(listener.getLastNewData()[0].getEventType().isProperty("s1"));
        assertSame(eventS0, listener.getLastNewData()[0].get("s0"));
        assertSame(eventS1, listener.getLastNewData()[0].get("s1"));
        assertTrue(rep == null || rep.matchesClass(listener.getLastNewData()[0].getUnderlying().getClass()));
    }

    private void tryAssertionJoinWildcard(EPServiceProvider epService, boolean bean, EventRepresentationChoice rep) {
        if (bean) {
            epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean.class);
            epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_A.class);
        } else if (rep.isMapEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventType("S0", Collections.singletonMap("theString", String.class));
            epService.getEPAdministrator().getConfiguration().addEventType("S1", Collections.singletonMap("id", String.class));
        } else if (rep.isObjectArrayEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventType("S0", new String[]{"theString"}, new Object[]{String.class});
            epService.getEPAdministrator().getConfiguration().addEventType("S1", new String[]{"id"}, new Object[]{String.class});
        } else if (rep.isAvroEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("S0", new ConfigurationEventTypeAvro().setAvroSchema(record("S0").fields().requiredString("theString").endRecord()));
            epService.getEPAdministrator().getConfiguration().addEventTypeAvro("S1", new ConfigurationEventTypeAvro().setAvroSchema(record("S1").fields().requiredString("id").endRecord()));
        } else {
            fail();
        }

        String textOne = (bean ? "" : rep.getAnnotationText()) + "insert into event2 select * " +
                "from S0#length(100) as s0, S1#length(5) as s1 " +
                "where s0.theString = s1.id";
        String textTwo = (bean ? "" : rep.getAnnotationText()) + "select * from event2#length(10)";

        // Attach listener to feed
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        // send event for joins to match on
        Object eventS1;
        if (bean) {
            eventS1 = new SupportBean_A("myId");
            epService.getEPRuntime().sendEvent(eventS1);
        } else if (rep.isMapEvent()) {
            eventS1 = Collections.singletonMap("id", "myId");
            epService.getEPRuntime().sendEvent((Map) eventS1, "S1");
        } else if (rep.isObjectArrayEvent()) {
            eventS1 = new Object[]{"myId"};
            epService.getEPRuntime().sendEvent((Object[]) eventS1, "S1");
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "S1"));
            theEvent.put("id", "myId");
            eventS1 = theEvent;
            epService.getEPRuntime().sendEventAvro(theEvent, "S1");
        } else {
            throw new IllegalArgumentException();
        }

        Object eventS0;
        if (bean) {
            eventS0 = new SupportBean("myId", -1);
            epService.getEPRuntime().sendEvent(eventS0);
        } else if (rep.isMapEvent()) {
            eventS0 = Collections.singletonMap("theString", "myId");
            epService.getEPRuntime().sendEvent((Map) eventS0, "S0");
        } else if (rep.isObjectArrayEvent()) {
            eventS0 = new Object[]{"myId"};
            epService.getEPRuntime().sendEvent((Object[]) eventS0, "S0");
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "S0"));
            theEvent.put("theString", "myId");
            eventS0 = theEvent;
            epService.getEPRuntime().sendEventAvro(theEvent, "S0");
        } else {
            throw new IllegalArgumentException();
        }

        assertJoinWildcard(rep, listenerOne, eventS0, eventS1);
        assertJoinWildcard(rep, listenerTwo, eventS0, eventS1);

        stmtOne.destroy();
        stmtTwo.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("S0", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("S1", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("event2", false);
    }

    private SupportBean sendEvent(EPServiceProvider epService, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString("myId");
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void tryAssertionWildcardRecast(EPServiceProvider epService, boolean sourceBean, EventRepresentationChoice sourceType,
                                            boolean targetBean, EventRepresentationChoice targetType) {
        try {
            tryAssertionWildcardRecastInternal(epService, sourceBean, sourceType, targetBean, targetType);
        } finally {
            // cleanup
            epService.getEPAdministrator().destroyAllStatements();
            epService.getEPAdministrator().getConfiguration().removeEventType("TargetSchema", false);
            epService.getEPAdministrator().getConfiguration().removeEventType("SourceSchema", false);
            epService.getEPAdministrator().getConfiguration().removeEventType("TargetContainedSchema", false);
        }
    }

    private void tryAssertionWildcardRecastInternal(EPServiceProvider epService, boolean sourceBean, EventRepresentationChoice sourceType,
                                                    boolean targetBean, EventRepresentationChoice targetType) {
        // declare source type
        if (sourceBean) {
            epService.getEPAdministrator().createEPL("create schema SourceSchema as " + MyP0P1EventSource.class.getName());
        } else {
            epService.getEPAdministrator().createEPL("create " + sourceType.getOutputTypeCreateSchemaName() + " schema SourceSchema as (p0 string, p1 int)");
        }

        // declare target type
        if (targetBean) {
            epService.getEPAdministrator().createEPL("create schema TargetSchema as " + MyP0P1EventTarget.class.getName());
        } else {
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
        } else if (sourceType.isMapEvent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("p0", "a");
            map.put("p1", 10);
            epService.getEPRuntime().sendEvent(map, "SourceSchema");
        } else if (sourceType.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"a", 10}, "SourceSchema");
        } else if (sourceType.isAvroEvent()) {
            Schema schema = record("schema").fields().requiredString("p0").requiredString("p1").requiredString("c0").endRecord();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("p0", "a");
            record.put("p1", 10);
            epService.getEPRuntime().sendEventAvro(record, "SourceSchema");
        } else {
            fail();
        }

        // assert
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,p1,c0".split(","), new Object[]{"a", 10, null});
    }

    public static class MyP0P1EventSource {
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

    public static class MyP0P1EventTarget {
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

    public static class MySimpleEventObjectArray {
        private Object[] arr;

        public Object[] getArr() {
            return arr;
        }

        public void setArr(Object[] arr) {
            this.arr = arr;
        }
    }
}
