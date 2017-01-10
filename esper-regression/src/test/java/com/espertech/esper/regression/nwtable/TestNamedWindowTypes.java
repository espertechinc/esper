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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.record;

public class TestNamedWindowTypes extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerStmtOne;
    private SupportUpdateListener listenerStmtDelete;

    public void setUp()
    {
        Map<String, Object> types = new HashMap<String, Object>();
        types.put("key", String.class);
        types.put("primitive", long.class);
        types.put("boxed", Long.class);

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyMap", types);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        listenerWindow = new SupportUpdateListener();
        listenerStmtOne = new SupportUpdateListener();
        listenerStmtDelete = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
        listenerStmtOne = null;
        listenerStmtDelete = null;
    }

    public void testEventTypeColumnDef() {
        for (EventRepresentationEnum rep : EventRepresentationEnum.values()) {
            runAssertionEventTypeColumnDef(rep);
        }
    }

    public void runAssertionEventTypeColumnDef(EventRepresentationEnum eventRepresentationEnum) {
        EPStatement stmtSchema = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema SchemaOne(col1 int, col2 int)");
        assertTrue(eventRepresentationEnum.matchesClass(stmtSchema.getEventType().getUnderlyingType()));

        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window SchemaWindow#lastevent as (s1 SchemaOne)");
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        stmt.addListener(listenerWindow);
        epService.getEPAdministrator().createEPL("insert into SchemaWindow (s1) select sone from SchemaOne as sone");
        
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {10, 11}, "SchemaOne");
        }
        else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("col1", 10);
            theEvent.put("col2", 11);
            epService.getEPRuntime().sendEvent(theEvent, "SchemaOne");
        }
        else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType("SchemaOne")).getSchemaAvro();
            GenericData.Record theEvent = new GenericData.Record(schema);
            theEvent.put("col1", 10);
            theEvent.put("col2", 11);
            epService.getEPRuntime().sendEventAvro(theEvent, "SchemaOne");
        }
        else {
            fail();
        }
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), "s1.col1,s1.col2".split(","), new Object[]{10, 11});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("SchemaOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("SchemaWindow", true);
    }

    public void testMapTranspose()
    {
        runAssertionMapTranspose(EventRepresentationEnum.OBJECTARRAY);
        runAssertionMapTranspose(EventRepresentationEnum.MAP);
        runAssertionMapTranspose(EventRepresentationEnum.DEFAULT);
    }

    private void runAssertionMapTranspose(EventRepresentationEnum eventRepresentationEnum) {

        Map<String, Object> innerTypeOne = new HashMap<String, Object>();
        innerTypeOne.put("i1", int.class);
        Map<String, Object> innerTypeTwo = new HashMap<String, Object>();
        innerTypeTwo.put("i2", int.class);
        Map<String, Object> outerType = new HashMap<String, Object>();
        outerType.put("one", "T1");
        outerType.put("two", "T2");
        epService.getEPAdministrator().getConfiguration().addEventType("T1", innerTypeOne);
        epService.getEPAdministrator().getConfiguration().addEventType("T2", innerTypeTwo);
        epService.getEPAdministrator().getConfiguration().addEventType("OuterType", outerType);

        // create window
        String stmtTextCreate = eventRepresentationEnum.getAnnotationText() + " create window MyWindow#keepall as select one, two from OuterType";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        assertTrue(eventRepresentationEnum.matchesClass(stmtCreate.getEventType().getUnderlyingType()));
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertEqualsAnyOrder(stmtCreate.getEventType().getPropertyNames(), new String[]{"one", "two"});
        EventType eventType = stmtCreate.getEventType();
        assertEquals("T1", eventType.getFragmentType("one").getFragmentType().getName());
        assertEquals("T2", eventType.getFragmentType("two").getFragmentType().getName());

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select one, two from OuterType";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        Map<String, Object> innerDataOne = new HashMap<String, Object>();
        innerDataOne.put("i1", 1);
        Map<String, Object> innerDataTwo = new HashMap<String, Object>();
        innerDataTwo.put("i2", 2);
        Map<String, Object> outerData = new HashMap<String, Object>();
        outerData.put("one", innerDataOne);
        outerData.put("two", innerDataTwo);

        epService.getEPRuntime().sendEvent(outerData, "OuterType");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), "one.i1,two.i2".split(","), new Object[]{1, 2});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindow", true);
    }

    public void testNoWildcardWithAs()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, longPrimitive as b, longBoxed as c from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);
        EPAssertionUtil.assertEqualsAnyOrder(stmtCreate.getEventType().getPropertyNames(), new String[]{"a", "b", "c"});
        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("a"));
        assertEquals(long.class, stmtCreate.getEventType().getPropertyType("b"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("c"));

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("MyWindow");
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyWindow", type.getMetadata().getPrimaryName());
        assertEquals("MyWindow", type.getMetadata().getPublicName());
        assertEquals("MyWindow", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.NAMED_WINDOW, type.getMetadata().getTypeClass());
        assertEquals(false, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, longPrimitive as b, longBoxed as c from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextInsertTwo = "insert into MyWindow select symbol as a, volume as b, volume as c from " + SupportMarketDataBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        String stmtTextInsertThree = "insert into MyWindow select key as a, boxed as b, primitive as c from MyMap";
        epService.getEPAdministrator().createEPL(stmtTextInsertThree);

        // create consumer
        String stmtTextSelectOne = "select a, b, c from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertEqualsAnyOrder(stmtSelectOne.getEventType().getPropertyNames(), new String[]{"a", "b", "c"});
        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("a"));
        assertEquals(long.class, stmtCreate.getEventType().getPropertyType("b"));
        assertEquals(Long.class, stmtCreate.getEventType().getPropertyType("c"));

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol = s1.a";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerStmtDelete);

        sendSupportBean("E1", 1L, 10L);
        String[] fields = new String[] {"a", "b", "c"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

        sendMarketBean("S1", 99L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

        sendMap("M1", 100L, 101L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
    }

    public void testNoWildcardNoAs()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString, longPrimitive, longBoxed from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString, longPrimitive, longBoxed from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextInsertTwo = "insert into MyWindow select symbol as theString, volume as longPrimitive, volume as longBoxed from " + SupportMarketDataBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        String stmtTextInsertThree = "insert into MyWindow select key as theString, boxed as longPrimitive, primitive as longBoxed from MyMap";
        epService.getEPAdministrator().createEPL(stmtTextInsertThree);

        // create consumer
        String stmtTextSelectOne = "select theString, longPrimitive, longBoxed from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean("E1", 1L, 10L);
        String[] fields = new String[] {"theString", "longPrimitive", "longBoxed"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

        sendMarketBean("S1", 99L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

        sendMap("M1", 100L, 101L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
    }

    public void testConstantsAs()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select '' as theString, 0L as longPrimitive, 0L as longBoxed from MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString, longPrimitive, longBoxed from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        String stmtTextInsertTwo = "insert into MyWindow select symbol as theString, volume as longPrimitive, volume as longBoxed from " + SupportMarketDataBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        // create consumer
        String stmtTextSelectOne = "select theString, longPrimitive, longBoxed from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean("E1", 1L, 10L);
        String[] fields = new String[] {"theString", "longPrimitive", "longBoxed"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

        sendMarketBean("S1", 99L);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
    }

    public void testCreateSchemaModelAfter() {
        for (EventRepresentationEnum rep : EventRepresentationEnum.values()) {
            runAssertionCreateSchemaModelAfter(rep);
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
    }

    public void runAssertionCreateSchemaModelAfter(EventRepresentationEnum eventRepresentationEnum)
    {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema EventTypeOne (hsi int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema EventTypeTwo (event EventTypeOne)");
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window NamedWindow#unique(event.hsi) as EventTypeTwo");
        epService.getEPAdministrator().createEPL("on EventTypeOne as ev insert into NamedWindow select ev as event");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {10}, "EventTypeOne");
        }
        else if (eventRepresentationEnum.isMapEvent()){
            epService.getEPRuntime().sendEvent(Collections.singletonMap("hsi", 10), "EventTypeOne");
        }
        else if (eventRepresentationEnum.isAvroEvent()){
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType("EventTypeOne")).getSchemaAvro();
            GenericData.Record theEvent = new GenericData.Record(schema);
            theEvent.put("hsi", 10);
            epService.getEPRuntime().sendEventAvro(theEvent, "EventTypeOne");
        }
        else {
            fail();
        }
        EventBean result = stmt.iterator().next();
        EventPropertyGetter getter = result.getEventType().getGetter("event.hsi");
        assertEquals(10, getter.get(result));

        epService.initialize();
    }

    public void testCreateTableArray()
    {
        epService.getEPAdministrator().createEPL("create schema SecurityData (name String, roles String[])");
        epService.getEPAdministrator().createEPL("create window SecurityEvent#time(30 sec) (ipAddress string, userId String, secData SecurityData, historySecData SecurityData[])");

        // create window
        String stmtTextCreate = "create window MyWindow#keepall (myvalue string[])";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select {'a','b'} as myvalue from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);
        
        sendSupportBean("E1", 1L, 10L);
        String[] values = (String[]) listenerWindow.assertOneGetNewAndReset().get("myvalue");
        EPAssertionUtil.assertEqualsExactOrder(values, new String[]{"a", "b"});
    }

    public void testCreateTableSyntax()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall (stringValOne varchar, stringValTwo string, intVal int, longVal long)";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("MyWindow");
        assertEquals(null, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyWindow", type.getMetadata().getPrimaryName());
        assertEquals("MyWindow", type.getMetadata().getPublicName());
        assertEquals("MyWindow", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.NAMED_WINDOW, type.getMetadata().getTypeClass());
        assertEquals(false, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as stringValOne, theString as stringValTwo, cast(longPrimitive, int) as intVal, longBoxed as longVal from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select stringValOne, stringValTwo, intVal, longVal from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean("E1", 1L, 10L);
        String[] fields = "stringValOne,stringValTwo,intVal,longVal".split(",");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1, 10L});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1, 10L});

        // create window with two views
        stmtTextCreate = "create window MyWindowTwo#unique(stringValOne)#keepall (stringValOne varchar, stringValTwo string, intVal int, longVal long)";
        stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        //create window with statement object model
        String text = "create window MyWindowThree#keepall as (a string, b integer, c integer)";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        assertEquals(text, model.toEPL());
        stmtCreate = epService.getEPAdministrator().create(model);
        assertEquals(String.class, stmtCreate.getEventType().getPropertyType("a"));
        assertEquals(Integer.class, stmtCreate.getEventType().getPropertyType("b"));
        assertEquals(Integer.class, stmtCreate.getEventType().getPropertyType("c"));
        assertEquals(text, model.toEPL());

        text = "create window MyWindowFour#unique(a)#unique(b) retain-union as (a string, b integer, c integer)";
        model = epService.getEPAdministrator().compileEPL(text);
        stmtCreate = epService.getEPAdministrator().create(model);
        assertEquals(text, model.toEPL());
    }

    public void testWildcardNoFieldsNoAs()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall select * from " + SupportBean_A.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select id from default.MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[] {"id"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
    }

    public void testModelAfterMap()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall select * from MyMap";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        assertTrue(stmtCreate.getEventType() instanceof MapEventType);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from MyMap";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextInsertOne);
        stmt.addListener(listenerWindow);

        sendMap("k1", 100L, 200L);
        EventBean theEvent = listenerWindow.assertOneGetNewAndReset();
        assertTrue(theEvent instanceof MappedEventBean);
        EPAssertionUtil.assertProps(theEvent, "key,primitive".split(","), new Object[]{"k1", 100L});
    }

    public void testWildcardInheritance()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select * from " + SupportBeanBase.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create insert into
        String stmtTextInsertTwo = "insert into MyWindow select * from " + SupportBean_B.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertTwo);

        // create consumer
        String stmtTextSelectOne = "select id from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[] {"id"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1"});

        epService.getEPRuntime().sendEvent(new SupportBean_B("E2"));
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2"});
    }

    public void testNoSpecificationBean()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as " + SupportBean_A.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select * from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select id from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[] {"id"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1"});
    }

    public void testWildcardWithFields()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select *, id as myid from " + SupportBean_A.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select *, id || 'A' as myid from " + SupportBean_A.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String stmtTextSelectOne = "select id, myid from MyWindow";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        String[] fields = new String[] {"id", "myid"};
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1A"});
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1A"});
    }

    private SupportBean sendSupportBean(String theString, long longPrimitive, Long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol, long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMap(String key, long primitive, Long boxed)
    {
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
