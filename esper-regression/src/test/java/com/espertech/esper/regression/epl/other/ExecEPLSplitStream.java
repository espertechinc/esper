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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.bookexample.OrderBean;
import com.espertech.esper.supportregression.bean.bookexample.OrderBeanFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ExecEPLSplitStream implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("S0", SupportBean_S0.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalid(epService);
        runAssertionFromClause(epService);
        runAssertionSplitPremptiveNamedWindow(epService);
        runAssertion1SplitDefault(epService);
        runAssertion2SplitNoDefaultOutputFirst(epService);
        runAssertionSubquery(epService);
        runAssertion2SplitNoDefaultOutputAll(epService);
        runAssertion3And4SplitDefaultOutputFirst(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "on SupportBean select * where intPrimitive=1 insert into BStream select * where 1=2",
                "Error starting statement: Required insert-into clause is not provided, the clause is required for split-stream syntax");

        SupportMessageAssertUtil.tryInvalid(epService, "on SupportBean insert into AStream select * where intPrimitive=1 group by string insert into BStream select * where 1=2",
                "Error starting statement: A group-by clause, having-clause or order-by clause is not allowed for the split stream syntax");

        SupportMessageAssertUtil.tryInvalid(epService, "on SupportBean insert into AStream select * where intPrimitive=1 insert into BStream select avg(intPrimitive) where 1=2",
                "Error starting statement: Aggregation functions are not allowed in this context");
    }

    private void runAssertionFromClause(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("OrderEvent", OrderBean.class);

        tryAssertionFromClauseBeginBodyEnd(epService);
        tryAssertionFromClauseAsMultiple(epService);
        tryAssertionFromClauseOutputFirstWhere(epService);
        tryAssertionFromClauseDocSample(epService);
    }

    private void runAssertionSplitPremptiveNamedWindow(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionSplitPremptiveNamedWindow(epService, rep);
        }
    }

    private void runAssertion1SplitDefault(EPServiceProvider epService) {
        // test wildcard
        String stmtOrigText = "on SupportBean insert into AStream select *";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtOrigText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportUpdateListener[] listeners = getListeners();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from AStream");
        stmtOne.addListener(listeners[0]);

        sendSupportBean(epService, "E1", 1);
        assertReceivedSingle(listeners, 0, "E1");
        assertFalse(listener.isInvoked());

        // test select
        stmtOrigText = "on SupportBean insert into BStreamABC select 3*intPrimitive as value";
        EPStatement stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);

        stmtOne = epService.getEPAdministrator().createEPL("select value from BStreamABC");
        stmtOne.addListener(listeners[1]);

        sendSupportBean(epService, "E1", 6);
        assertEquals(18, listeners[1].assertOneGetNewAndReset().get("value"));

        // assert type is original type
        assertEquals(SupportBean.class, stmtOrig.getEventType().getUnderlyingType());
        assertFalse(stmtOrig.iterator().hasNext());

        stmtOne.destroy();
    }

    private void runAssertion2SplitNoDefaultOutputFirst(EPServiceProvider epService) {
        String stmtOrigText = "@Audit on SupportBean " +
                "insert into AStream2SP select * where intPrimitive=1 " +
                "insert into BStream2SP select * where intPrimitive=1 or intPrimitive=2";
        EPStatement stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);
        tryAssertion(epService, stmtOrig);

        // statement object model
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setAnnotations(Collections.singletonList(new AnnotationPart("Audit")));
        model.setFromClause(FromClause.create(FilterStream.create("SupportBean")));
        model.setInsertInto(InsertIntoClause.create("AStream2SP"));
        model.setSelectClause(SelectClause.createWildcard());
        model.setWhereClause(Expressions.eq("intPrimitive", 1));
        OnInsertSplitStreamClause clause = OnClause.createOnInsertSplitStream();
        model.setOnExpr(clause);
        OnInsertSplitStreamItem item = OnInsertSplitStreamItem.create(
                InsertIntoClause.create("BStream2SP"),
                SelectClause.createWildcard(),
                Expressions.or(Expressions.eq("intPrimitive", 1), Expressions.eq("intPrimitive", 2)));
        clause.addItem(item);
        assertEquals(stmtOrigText, model.toEPL());
        stmtOrig = epService.getEPAdministrator().create(model);
        tryAssertion(epService, stmtOrig);

        EPStatementObjectModel newModel = epService.getEPAdministrator().compileEPL(stmtOrigText);
        stmtOrig = epService.getEPAdministrator().create(newModel);
        assertEquals(stmtOrigText, newModel.toEPL());
        tryAssertion(epService, stmtOrig);

        SupportModelHelper.compileCreate(epService, stmtOrigText + " output all");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubquery(EPServiceProvider epService) {
        String stmtOrigText = "on SupportBean " +
                "insert into AStreamSub select (select p00 from S0#lastevent) as string where intPrimitive=(select id from S0#lastevent) " +
                "insert into BStreamSub select (select p01 from S0#lastevent) as string where intPrimitive<>(select id from S0#lastevent) or (select id from S0#lastevent) is null";
        EPStatement stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOrig.addListener(listener);

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from AStreamSub");
        SupportUpdateListener listenerAStream = new SupportUpdateListener();
        stmtOne.addListener(listenerAStream);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from BStreamSub");
        SupportUpdateListener listenerBStream = new SupportUpdateListener();
        stmtTwo.addListener(listenerBStream);

        sendSupportBean(epService, "E1", 1);
        assertFalse(listenerAStream.getAndClearIsInvoked());
        assertNull(listenerBStream.assertOneGetNewAndReset().get("string"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "x", "y"));

        sendSupportBean(epService, "E2", 10);
        assertEquals("x", listenerAStream.assertOneGetNewAndReset().get("string"));
        assertFalse(listenerBStream.getAndClearIsInvoked());

        sendSupportBean(epService, "E3", 9);
        assertFalse(listenerAStream.getAndClearIsInvoked());
        assertEquals("y", listenerBStream.assertOneGetNewAndReset().get("string"));

        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void runAssertion2SplitNoDefaultOutputAll(EPServiceProvider epService) {
        String stmtOrigText = "on SupportBean " +
                "insert into AStream2S select theString where intPrimitive=1 " +
                "insert into BStream2S select theString where intPrimitive=1 or intPrimitive=2 " +
                "output all";
        EPStatement stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOrig.addListener(listener);

        SupportUpdateListener[] listeners = getListeners();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from AStream2S");
        stmtOne.addListener(listeners[0]);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from BStream2S");
        stmtTwo.addListener(listeners[1]);

        assertNotSame(stmtOne.getEventType(), stmtTwo.getEventType());
        assertSame(stmtOne.getEventType().getUnderlyingType(), stmtTwo.getEventType().getUnderlyingType());

        sendSupportBean(epService, "E1", 1);
        assertReceivedEach(listeners, new String[]{"E1", "E1"});
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E2", 2);
        assertReceivedEach(listeners, new String[]{null, "E2"});
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E3", 1);
        assertReceivedEach(listeners, new String[]{"E3", "E3"});
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E4", -999);
        assertReceivedEach(listeners, new String[]{null, null});
        assertEquals("E4", listener.assertOneGetNewAndReset().get("theString"));

        stmtOrig.destroy();
        stmtOrigText = "on SupportBean " +
                "insert into AStream2S select theString || '_1' as theString where intPrimitive in (1, 2) " +
                "insert into BStream2S select theString || '_2' as theString where intPrimitive in (2, 3) " +
                "insert into CStream2S select theString || '_3' as theString " +
                "output all";
        stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);
        stmtOrig.addListener(listener);

        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from CStream2S");
        stmtThree.addListener(listeners[2]);

        sendSupportBean(epService, "E1", 2);
        assertReceivedEach(listeners, new String[]{"E1_1", "E1_2", "E1_3"});
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E2", 1);
        assertReceivedEach(listeners, new String[]{"E2_1", null, "E2_3"});
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E3", 3);
        assertReceivedEach(listeners, new String[]{null, "E3_2", "E3_3"});
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E4", -999);
        assertReceivedEach(listeners, new String[]{null, null, "E4_3"});
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion3And4SplitDefaultOutputFirst(EPServiceProvider epService) {
        String stmtOrigText = "on SupportBean as mystream " +
                "insert into AStream34 select mystream.theString||'_1' as theString where intPrimitive=1 " +
                "insert into BStream34 select mystream.theString||'_2' as theString where intPrimitive=2 " +
                "insert into CStream34 select theString||'_3' as theString";
        EPStatement stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOrig.addListener(listener);

        SupportUpdateListener[] listeners = getListeners();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from AStream34");
        stmtOne.addListener(listeners[0]);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from BStream34");
        stmtTwo.addListener(listeners[1]);
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from CStream34");
        stmtThree.addListener(listeners[2]);

        assertNotSame(stmtOne.getEventType(), stmtTwo.getEventType());
        assertSame(stmtOne.getEventType().getUnderlyingType(), stmtTwo.getEventType().getUnderlyingType());

        sendSupportBean(epService, "E1", 1);
        assertReceivedSingle(listeners, 0, "E1_1");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E2", 2);
        assertReceivedSingle(listeners, 1, "E2_2");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E3", 1);
        assertReceivedSingle(listeners, 0, "E3_1");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E4", -999);
        assertReceivedSingle(listeners, 2, "E4_3");
        assertFalse(listener.isInvoked());

        stmtOrigText = "on SupportBean " +
                "insert into AStream34 select theString||'_1' as theString where intPrimitive=10 " +
                "insert into BStream34 select theString||'_2' as theString where intPrimitive=20 " +
                "insert into CStream34 select theString||'_3' as theString where intPrimitive<0 " +
                "insert into DStream34 select theString||'_4' as theString";
        stmtOrig.destroy();
        stmtOrig = epService.getEPAdministrator().createEPL(stmtOrigText);
        stmtOrig.addListener(listener);

        EPStatement stmtFour = epService.getEPAdministrator().createEPL("select * from DStream34");
        stmtFour.addListener(listeners[3]);

        sendSupportBean(epService, "E5", -999);
        assertReceivedSingle(listeners, 2, "E5_3");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E6", 9999);
        assertReceivedSingle(listeners, 3, "E6_4");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E7", 20);
        assertReceivedSingle(listeners, 1, "E7_2");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E8", 10);
        assertReceivedSingle(listeners, 0, "E8_1");
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertReceivedEach(SupportUpdateListener[] listeners, String[] stringValue) {
        for (int i = 0; i < stringValue.length; i++) {
            if (stringValue[i] != null) {
                assertEquals(stringValue[i], listeners[i].assertOneGetNewAndReset().get("theString"));
            } else {
                assertFalse(listeners[i].isInvoked());
            }
        }
    }

    private void assertReceivedSingle(SupportUpdateListener[] listeners, int index, String stringValue) {
        for (int i = 0; i < listeners.length; i++) {
            if (i == index) {
                continue;
            }
            assertFalse(listeners[i].isInvoked());
        }
        assertEquals(stringValue, listeners[index].assertOneGetNewAndReset().get("theString"));
    }

    private void assertReceivedNone(SupportUpdateListener[] listeners) {
        for (int i = 0; i < listeners.length; i++) {
            assertFalse(listeners[i].isInvoked());
        }
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void tryAssertion(EPServiceProvider epService, EPStatement stmtOrig) {
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOrig.addListener(listener);

        SupportUpdateListener[] listeners = getListeners();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from AStream2SP");
        stmtOne.addListener(listeners[0]);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from BStream2SP");
        stmtTwo.addListener(listeners[1]);

        assertNotSame(stmtOne.getEventType(), stmtTwo.getEventType());
        assertSame(stmtOne.getEventType().getUnderlyingType(), stmtTwo.getEventType().getUnderlyingType());

        sendSupportBean(epService, "E1", 1);
        assertReceivedSingle(listeners, 0, "E1");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E2", 2);
        assertReceivedSingle(listeners, 1, "E2");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E3", 1);
        assertReceivedSingle(listeners, 0, "E3");
        assertFalse(listener.isInvoked());

        sendSupportBean(epService, "E4", -999);
        assertReceivedNone(listeners);
        assertEquals("E4", listener.assertOneGetNewAndReset().get("theString"));

        stmtOrig.destroy();
        stmtOne.destroy();
        stmtTwo.destroy();
    }

    private void tryAssertionSplitPremptiveNamedWindow(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TypeTwo(col2 int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema TypeTrigger(trigger int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window WinTwo#keepall as TypeTwo");

        String stmtOrigText = "on TypeTrigger " +
                "insert into OtherStream select 1 " +
                "insert into WinTwo(col2) select 2 " +
                "output all";
        epService.getEPAdministrator().createEPL(stmtOrigText);

        EPStatement stmt = epService.getEPAdministrator().createEPL("on OtherStream select col2 from WinTwo");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // populate WinOne
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));

        // fire trigger
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(new Object[]{null});
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(new HashMap());
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(record("name").fields().optionalInt("trigger").endRecord());
            epService.getEPRuntime().getEventSender("TypeTrigger").sendEvent(event);
        } else {
            fail();
        }

        assertEquals(2, listener.assertOneGetNewAndReset().get("col2"));

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "TypeTwo,TypeTrigger,WinTwo,OtherStream".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void tryAssertionFromClauseBeginBodyEnd(EPServiceProvider epService) {
        tryAssertionFromClauseBeginBodyEnd(epService, false);
        tryAssertionFromClauseBeginBodyEnd(epService, true);
    }

    private void tryAssertionFromClauseAsMultiple(EPServiceProvider epService) {
        tryAssertionFromClauseAsMultiple(epService, false);
        tryAssertionFromClauseAsMultiple(epService, true);
    }

    private void tryAssertionFromClauseAsMultiple(EPServiceProvider epService, boolean soda) {
        String epl = "on OrderEvent as oe " +
                "insert into StartEvent select oe.orderdetail.orderId as oi " +
                "insert into ThenEvent select * from [select oe.orderdetail.orderId as oi, itemId from orderdetail.items] as item " +
                "insert into MoreEvent select oe.orderdetail.orderId as oi, item.itemId as itemId from [select oe, * from orderdetail.items] as item " +
                "output all";
        SupportModelHelper.createByCompileOrParse(epService, soda, epl);

        SupportUpdateListener[] listeners = getListeners();
        epService.getEPAdministrator().createEPL("select * from StartEvent").addListener(listeners[0]);
        epService.getEPAdministrator().createEPL("select * from ThenEvent").addListener(listeners[1]);
        epService.getEPAdministrator().createEPL("select * from MoreEvent").addListener(listeners[2]);

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventOne());
        String[] fieldsOrderId = "oi".split(",");
        String[] fieldsItems = "oi,itemId".split(",");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fieldsOrderId, new Object[]{"PO200901"});
        Object[][] expected = new Object[][]{{"PO200901", "A001"}, {"PO200901", "A002"}, {"PO200901", "A003"}};
        EPAssertionUtil.assertPropsPerRow(listeners[1].getAndResetDataListsFlattened().getFirst(), fieldsItems, expected);
        EPAssertionUtil.assertPropsPerRow(listeners[2].getAndResetDataListsFlattened().getFirst(), fieldsItems, expected);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionFromClauseBeginBodyEnd(EPServiceProvider epService, boolean soda) {
        String epl = "on OrderEvent " +
                "insert into BeginEvent select orderdetail.orderId as orderId " +
                "insert into OrderItem select * from [select orderdetail.orderId as orderId, * from orderdetail.items] " +
                "insert into EndEvent select orderdetail.orderId as orderId " +
                "output all";
        SupportModelHelper.createByCompileOrParse(epService, soda, epl);

        SupportUpdateListener[] listeners = getListeners();
        epService.getEPAdministrator().createEPL("select * from BeginEvent").addListener(listeners[0]);
        epService.getEPAdministrator().createEPL("select * from OrderItem").addListener(listeners[1]);
        epService.getEPAdministrator().createEPL("select * from EndEvent").addListener(listeners[2]);

        EventType typeOrderItem = epService.getEPAdministrator().getConfiguration().getEventType("OrderItem");
        assertEquals("[amount, itemId, price, productId, orderId]", Arrays.toString(typeOrderItem.getPropertyNames()));

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventOne());
        assertFromClauseWContained(listeners, "PO200901", new Object[][]{{"PO200901", "A001"}, {"PO200901", "A002"}, {"PO200901", "A003"}});

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventTwo());
        assertFromClauseWContained(listeners, "PO200902", new Object[][]{{"PO200902", "B001"}});

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventFour());
        assertFromClauseWContained(listeners, "PO200904", new Object[0][]);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionFromClauseOutputFirstWhere(EPServiceProvider epService) {
        tryAssertionFromClauseOutputFirstWhere(epService, false);
        tryAssertionFromClauseOutputFirstWhere(epService, true);
    }

    private void tryAssertionFromClauseOutputFirstWhere(EPServiceProvider epService, boolean soda) {
        String[] fieldsOrderId = "oe.orderdetail.orderId".split(",");
        String epl = "on OrderEvent as oe " +
                "insert into HeaderEvent select orderdetail.orderId as orderId where 1=2 " +
                "insert into StreamOne select * from [select oe, * from orderdetail.items] where productId=\"10020\" " +
                "insert into StreamTwo select * from [select oe, * from orderdetail.items] where productId=\"10022\" " +
                "insert into StreamThree select * from [select oe, * from orderdetail.items] where productId in (\"10020\",\"10025\",\"10022\")";
        SupportModelHelper.createByCompileOrParse(epService, soda, epl);

        SupportUpdateListener[] listeners = getListeners();
        String[] listenerEPL = new String[]{"select * from StreamOne", "select * from StreamTwo", "select * from StreamThree"};
        for (int i = 0; i < listenerEPL.length; i++) {
            epService.getEPAdministrator().createEPL(listenerEPL[i]).addListener(listeners[i]);
            listeners[i].reset();
        }

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventOne());
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fieldsOrderId, new Object[]{"PO200901"});
        assertFalse(listeners[1].isInvoked());
        assertFalse(listeners[2].isInvoked());

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventTwo());
        assertFalse(listeners[0].isInvoked());
        EPAssertionUtil.assertProps(listeners[1].assertOneGetNewAndReset(), fieldsOrderId, new Object[]{"PO200902"});
        assertFalse(listeners[2].isInvoked());

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventThree());
        assertFalse(listeners[0].isInvoked());
        assertFalse(listeners[1].isInvoked());
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fieldsOrderId, new Object[]{"PO200903"});

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventFour());
        assertFalse(listeners[0].isInvoked());
        assertFalse(listeners[1].isInvoked());
        assertFalse(listeners[2].isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionFromClauseDocSample(EPServiceProvider epService) throws Exception {
        String epl =
                "create schema MyOrderItem(itemId string);\n" +
                        "create schema MyOrderEvent(orderId string, items MyOrderItem[]);\n" +
                        "on MyOrderEvent\n" +
                        "  insert into MyOrderBeginEvent select orderId\n" +
                        "  insert into MyOrderItemEvent select * from [select orderId, * from items]\n" +
                        "  insert into MyOrderEndEvent select orderId\n" +
                        "  output all;\n" +
                        "create context MyOrderContext \n" +
                        "  initiated by MyOrderBeginEvent as obe\n" +
                        "  terminated by MyOrderEndEvent(orderId = obe.orderId);\n" +
                        "@Name('count') context MyOrderContext select count(*) as orderItemCount from MyOrderItemEvent output when terminated;\n";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("count").addListener(listener);

        Map<String, Object> event = new HashMap<>();
        event.put("orderId", "1010");
        event.put("items", new Map[]{Collections.singletonMap("itemId", "A0001")});
        epService.getEPRuntime().sendEvent(event, "MyOrderEvent");

        assertEquals(1L, listener.assertOneGetNewAndReset().get("orderItemCount"));

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(result.getDeploymentId());
    }

    private void assertFromClauseWContained(SupportUpdateListener[] listeners, String orderId, Object[][] expected) {
        String[] fieldsOrderId = "orderId".split(",");
        String[] fieldsItems = "orderId,itemId".split(",");
        EPAssertionUtil.assertProps(listeners[0].assertOneGetNewAndReset(), fieldsOrderId, new Object[]{orderId});
        EPAssertionUtil.assertPropsPerRow(listeners[1].getAndResetDataListsFlattened().getFirst(), fieldsItems, expected);
        EPAssertionUtil.assertProps(listeners[2].assertOneGetNewAndReset(), fieldsOrderId, new Object[]{orderId});
    }

    private SupportUpdateListener[] getListeners() {
        SupportUpdateListener[] listeners = new SupportUpdateListener[10];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new SupportUpdateListener();
        }
        return listeners;
    }
}
