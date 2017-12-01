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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecNamedWindowTypes implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Map<String, Object> types = new HashMap<String, Object>();
        types.put("key", String.class);
        types.put("primitive", long.class);
        types.put("boxed", Long.class);
        configuration.addEventType("MyMap", types);
    }

    public void run(EPServiceProvider epService) throws Exception {

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionEventTypeColumnDef(epService, rep);
        }

        runAssertionMapTranspose(epService);
        runAssertionNoWildcardWithAs(epService);
        runAssertionNoWildcardNoAs(epService);
        runAssertionConstantsAs(epService);
        runAssertionCreateSchemaModelAfter(epService);
        runAssertionCreateTableArray(epService);
        runAssertionCreateTableSyntax(epService);
        runAssertionWildcardNoFieldsNoAs(epService);
        runAssertionModelAfterMap(epService);
        runAssertionWildcardInheritance(epService);
        runAssertionNoSpecificationBean(epService);
        runAssertionWildcardWithFields(epService);
    }

    private void runAssertionEventTypeColumnDef(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        EPStatement stmtSchema = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema SchemaOne(col1 int, col2 int)");
        assertTrue(eventRepresentationEnum.matchesClass(stmtSchema.getEventType().getUnderlyingType()));

        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window SchemaWindow#lastevent as (s1 SchemaOne)");
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmt.addListener(listenerWindow);
        epService.getEPAdministrator().createEPL("insert into SchemaWindow (s1) select sone from SchemaOne as sone");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{10, 11}, "SchemaOne");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<>();
            theEvent.put("col1", 10);
            theEvent.put("col2", 11);
            epService.getEPRuntime().sendEvent(theEvent, "SchemaOne");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "SchemaOne"));
            theEvent.put("col1", 10);
            theEvent.put("col2", 11);
            epService.getEPRuntime().sendEventAvro(theEvent, "SchemaOne");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), "s1.col1,s1.col2".split(","), new Object[]{10, 11});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("SchemaOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("SchemaWindow", true);
    }

    private void runAssertionMapTranspose(EPServiceProvider epService) {
        tryAssertionMapTranspose(epService, EventRepresentationChoice.ARRAY);
        tryAssertionMapTranspose(epService, EventRepresentationChoice.MAP);
        tryAssertionMapTranspose(epService, EventRepresentationChoice.DEFAULT);
    }

    private void tryAssertionMapTranspose(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {

        Map<String, Object> innerTypeOne = new HashMap<>();
        innerTypeOne.put("i1", int.class);
        Map<String, Object> innerTypeTwo = new HashMap<>();
        innerTypeTwo.put("i2", int.class);
        Map<String, Object> outerType = new HashMap<>();
        outerType.put("one", "T1");
        outerType.put("two", "T2");
        epService.getEPAdministrator().getConfiguration().addEventType("T1", innerTypeOne);
        epService.getEPAdministrator().getConfiguration().addEventType("T2", innerTypeTwo);
        epService.getEPAdministrator().getConfiguration().addEventType("OuterType", outerType);

        // create window
        String stmtTextCreate = eventRepresentationEnum.getAnnotationText() + " create window MyWindowMT#keepall as select one, two from OuterType";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        assertTrue(eventRepresentationEnum.matchesClass(stmtCreate.getEventType().getUnderlyingType()));
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertEqualsAnyOrder(stmtCreate.getEventType().getPropertyNames(), new String[]{"one", "two"});
        EventType eventType = stmtCreate.getEventType();
        assertEquals("T1", eventType.getFragmentType("one").getFragmentType().getName());
        assertEquals("T2", eventType.getFragmentType("two").getFragmentType().getName());

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowMT select one, two from OuterType";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        Map<String, Object> innerDataOne = new HashMap<>();
        innerDataOne.put("i1", 1);
        Map<String, Object> innerDataTwo = new HashMap<>();
        innerDataTwo.put("i2", 2);
        Map<String, Object> outerData = new HashMap<>();
        outerData.put("one", innerDataOne);
        outerData.put("two", innerDataTwo);

        epService.getEPRuntime().sendEvent(outerData, "OuterType");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), "one.i1,two.i2".split(","), new Object[]{1, 2});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowMT", true);
    }

    private void runAssertionNoWildcardWithAs(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowNW#keepall as select theString as a, longPrimitive as b, longBoxed as c from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertEqualsAnyOrder(stmtCreate.getEventType().getPropertyNames(), new String[]{"a", "b", "c"});
        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("a"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("b"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("c"));

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("MyWindowNW");
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyWindowNW", type.getMetadata().getPrimaryName());
        assertEquals("MyWindowNW", type.getMetadata().getPublicName());
        assertEquals("MyWindowNW", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.NAMED_WINDOW, type.getMetadata().getTypeClass());
        assertEquals(false, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowNW select theString as a, longPrimitive as b, longBoxed as c from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextInsertTwo = "insert into MyWindowNW select symbol as a, volume as b, volume as c from " + SupportMarketDataBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        String stmtTextInsertThree = "insert into MyWindowNW select key as a, boxed as b, primitive as c from MyMap";
        epService.getEPAdministrator().createEPL(stmtTextInsertThree);

        // create consumer
        String stmtTextSelectOne = "select a, b, c from MyWindowNW";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertEqualsAnyOrder(stmtSelectOne.getEventType().getPropertyNames(), new String[]{"a", "b", "c"});
        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("a"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("b"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("c"));

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindowNW as s1 where s0.symbol = s1.a";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        SupportUpdateListener listenerStmtDelete = new SupportUpdateListener();
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean(epService, "E1", 1L, 10L);
        String[] fields = new String[]{"a", "b", "c"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

        sendMarketBean(epService, "S1", 99L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

        sendMap(epService, "M1", 100L, 101L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNoWildcardNoAs(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowNWNA#keepall as select theString, longPrimitive, longBoxed from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowNWNA select theString, longPrimitive, longBoxed from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextInsertTwo = "insert into MyWindowNWNA select symbol as theString, volume as longPrimitive, volume as longBoxed from " + SupportMarketDataBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        String stmtTextInsertThree = "insert into MyWindowNWNA select key as theString, boxed as longPrimitive, primitive as longBoxed from MyMap";
        epService.getEPAdministrator().createEPL(stmtTextInsertThree);

        // create consumer
        String stmtTextSelectOne = "select theString, longPrimitive, longBoxed from MyWindowNWNA";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean(epService, "E1", 1L, 10L);
        String[] fields = new String[]{"theString", "longPrimitive", "longBoxed"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

        sendMarketBean(epService, "S1", 99L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

        sendMap(epService, "M1", 100L, 101L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionConstantsAs(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowCA#keepall as select '' as theString, 0L as longPrimitive, 0L as longBoxed from MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowCA select theString, longPrimitive, longBoxed from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextInsertTwo = "insert into MyWindowCA select symbol as theString, volume as longPrimitive, volume as longBoxed from " + SupportMarketDataBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        // create consumer
        String stmtTextSelectOne = "select theString, longPrimitive, longBoxed from MyWindowCA";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean(epService, "E1", 1L, 10L);
        String[] fields = new String[]{"theString", "longPrimitive", "longBoxed"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

        sendMarketBean(epService, "S1", 99L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCreateSchemaModelAfter(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionCreateSchemaModelAfter(epService, rep);
        }

        // test model-after for POJO with inheritance
        epService.getEPAdministrator().createEPL("create window ParentWindow#keepall as select * from " + NWTypesParentClass.class.getName());
        epService.getEPAdministrator().createEPL("insert into ParentWindow select * from " + NWTypesParentClass.class.getName());
        epService.getEPAdministrator().createEPL("create window ChildWindow#keepall as select * from " + NWTypesChildClass.class.getName());
        epService.getEPAdministrator().createEPL("insert into ChildWindow select * from " + NWTypesChildClass.class.getName());

        SupportUpdateListener listener = new SupportUpdateListener();
        String parentQuery = "@Name('Parent') select parent from ParentWindow as parent";
        epService.getEPAdministrator().createEPL(parentQuery).addListener(listener);

        epService.getEPRuntime().sendEvent(new NWTypesChildClass());
        assertEquals(1, listener.getNewDataListFlattened().length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionCreateSchemaModelAfter(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema EventTypeOne (hsi int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema EventTypeTwo (event EventTypeOne)");
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window NamedWindow#unique(event.hsi) as EventTypeTwo");
        epService.getEPAdministrator().createEPL("on EventTypeOne as ev insert into NamedWindow select ev as event");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{10}, "EventTypeOne");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("hsi", 10), "EventTypeOne");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "EventTypeOne"));
            theEvent.put("hsi", 10);
            epService.getEPRuntime().sendEventAvro(theEvent, "EventTypeOne");
        } else {
            fail();
        }
        EventBean result = stmt.iterator().next();
        EventPropertyGetter getter = result.getEventType().getGetter("event.hsi");
        assertEquals(10, getter.get(result));

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "EventTypeOne,EventTypeTwo,NamedWindow".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionCreateTableArray(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema SecurityData (name String, roles String[])");
        epService.getEPAdministrator().createEPL("create window SecurityEvent#time(30 sec) (ipAddress string, userId String, secData SecurityData, historySecData SecurityData[])");

        // create window
        String stmtTextCreate = "create window MyWindowCTA#keepall (myvalue string[])";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowCTA select {'a','b'} as myvalue from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean(epService, "E1", 1L, 10L);
        String[] values = (String[]) listenerWindow.assertOneGetNewAndReset().get("myvalue");
        EPAssertionUtil.assertEqualsExactOrder(values, new String[]{"a", "b"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionCreateTableSyntax(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowCTS#keepall (stringValOne varchar, stringValTwo string, intVal int, longVal long)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("MyWindowCTS");
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyWindowCTS", type.getMetadata().getPrimaryName());
        assertEquals("MyWindowCTS", type.getMetadata().getPublicName());
        assertEquals("MyWindowCTS", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.NAMED_WINDOW, type.getMetadata().getTypeClass());
        assertEquals(false, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowCTS select theString as stringValOne, theString as stringValTwo, cast(longPrimitive, int) as intVal, longBoxed as longVal from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select stringValOne, stringValTwo, intVal, longVal from MyWindowCTS";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean(epService, "E1", 1L, 10L);
        String[] fields = "stringValOne,stringValTwo,intVal,longVal".split(",");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1, 10L});

        // create window with two views
        stmtTextCreate = "create window MyWindowCTSTwo#unique(stringValOne)#keepall (stringValOne varchar, stringValTwo string, intVal int, longVal long)";
        epService.getEPAdministrator().createEPL(stmtTextCreate);

        //create window with statement object model
        String text = "create window MyWindowCTSThree#keepall as (a string, b integer, c integer)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text, model.toEPL());
        stmtCreate = epService.getEPAdministrator().create(model);
        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmtCreate.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmtCreate.getEventType().getPropertyType("c"));
        assertEquals(text, model.toEPL());

        text = "create window MyWindowCTSFour#unique(a)#unique(b) retain-union as (a string, b integer, c integer)";
        model = epService.getEPAdministrator().compileEPL(text);
        epService.getEPAdministrator().create(model);
        assertEquals(text, model.toEPL());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWildcardNoFieldsNoAs(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowWNF#keepall select * from " + SupportBean_A.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowWNF select * from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select id from default.MyWindowWNF";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[]{"id"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionModelAfterMap(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowMAM#keepall select * from MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        assertTrue(stmtCreate.getEventType() instanceof MapEventType);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowMAM select * from MyMap";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextInsertOne);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmt.addListener(listenerWindow);

        sendMap(epService, "k1", 100L, 200L);
        EventBean theEvent = listenerWindow.assertOneGetNewAndReset();
        assertTrue(theEvent instanceof MappedEventBean);
        EPAssertionUtil.assertProps(theEvent, "key,primitive".split(","), new Object[]{"k1", 100L});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWildcardInheritance(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowWI#keepall as select * from " + SupportBeanBase.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowWI select * from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create insert into
        String stmtTextInsertTwo = "insert into MyWindowWI select * from " + SupportBean_B.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        // create consumer
        String stmtTextSelectOne = "select id from MyWindowWI";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[]{"id"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean_B("E2"));
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNoSpecificationBean(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowNSB#keepall as " + SupportBean_A.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowNSB select * from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select id from MyWindowNSB";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[]{"id"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWildcardWithFields(EPServiceProvider epService) {
        // create window
        String stmtTextCreate = "create window MyWindowWWF#keepall as select *, id as myid from " + SupportBean_A.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindowWWF select *, id || 'A' as myid from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select id, myid from MyWindowWWF";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[]{"id", "myid"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1A"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1A"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, long longPrimitive, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMap(EPServiceProvider epService, String key, long primitive, Long boxed) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", key);
        map.put("primitive", primitive);
        map.put("boxed", boxed);
        epService.getEPRuntime().sendEvent(map, "MyMap");
    }

    public static class NWTypesParentClass {
    }

    public static class NWTypesChildClass extends NWTypesParentClass {
    }
}
