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

import com.espertech.esper.avro.core.AvroGenericDataEventBean;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.event.AvroBackedBean;
import com.espertech.esper.event.MappedEventBean;
import com.espertech.esper.event.ObjectArrayBackedEventBean;
import com.espertech.esper.event.WrapperEventType;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.util.JavaClassHelper;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.apache.avro.SchemaBuilder.record;

public class TestInsertIntoPopulateSingleColByMethodCall extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testStreamSelectConversionFunction() {
        // define Bean
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportMarketDataBean.class);

        // define Map
        Map<String, Object> mapTypeInfo = new HashMap<String, Object>();
        mapTypeInfo.put("one", String.class);
        mapTypeInfo.put("two", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MapOne", mapTypeInfo);
        epService.getEPAdministrator().getConfiguration().addEventType("MapTwo", mapTypeInfo);

        // define OA
        String[] props = {"one", "two"};
        Object[] types = {String.class, String.class};
        epService.getEPAdministrator().getConfiguration().addEventType("OAOne", props, types);
        epService.getEPAdministrator().getConfiguration().addEventType("OATwo", props, types);

        // define Avro
        Schema schema = record("name").fields().requiredString("one").requiredString("two").endRecord();
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("AvroOne", new ConfigurationEventTypeAvro(schema));
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro("AvroTwo", new ConfigurationEventTypeAvro(schema));

        // Bean
        runAssertionConversionImplicitType("Bean", SupportBean.class.getSimpleName(), "convertEvent", BeanEventType.class, SupportBean.class,
                SupportMarketDataBean.class.getName(), new SupportMarketDataBean("ACME", 0, 0L, null), FBEANWTYPE, "theString".split(","), new Object[] {"ACME"});

        // Map
        Map<String, Object> mapEventOne = new HashMap<String, Object>();
        mapEventOne.put("one", "1");
        mapEventOne.put("two", "2");
        runAssertionConversionImplicitType("Map", "MapOne", "convertEventMap", WrapperEventType.class, Map.class,
                "MapTwo", mapEventOne, FMAPWTYPE, "one,two".split(","), new Object[] {"1", "|2|"});

        Map<String, Object> mapEventTwo = new HashMap<String, Object>();
        mapEventTwo.put("one", "3");
        mapEventTwo.put("two", "4");
        runAssertionConversionConfiguredType("MapOne", "convertEventMap", "MapTwo", MapEventBean.class, HashMap.class, mapEventTwo, FMAPWTYPE, "one,two".split(","), new Object[] {"3", "|4|"});

        // Object-Array
        runAssertionConversionImplicitType("OA", "OAOne", "convertEventObjectArray", WrapperEventType.class, Object[].class,
                "OATwo", new Object[] {"1", "2"}, FOAWTYPE, "one,two".split(","), new Object[] {"1", "|2|"});
        runAssertionConversionConfiguredType("OAOne", "convertEventObjectArray", "OATwo", ObjectArrayEventBean.class, Object[].class, new Object[] {"3", "4"}, FOAWTYPE, "one,two".split(","), new Object[] {"3", "|4|"});

        // Avro
        GenericData.Record rowOne = new GenericData.Record(schema);
        rowOne.put("one", "1");
        rowOne.put("two", "2");
        runAssertionConversionImplicitType("Avro", "AvroOne", "convertEventAvro", WrapperEventType.class, GenericData.Record.class,
                "AvroTwo", rowOne, FAVROWTYPE, "one,two".split(","), new Object[] {"1", "|2|"});

        GenericData.Record rowTwo = new GenericData.Record(schema);
        rowTwo.put("one", "3");
        rowTwo.put("two", "4");
        runAssertionConversionConfiguredType("AvroOne", "convertEventAvro", "AvroTwo", AvroGenericDataEventBean.class, GenericData.Record.class, rowTwo, FAVROWTYPE, "one,two".split(","), new Object[] {"3", "|4|"});
    }

    private void runAssertionConversionImplicitType(String prefix,
                                                    String typeNameOrigin,
                                                    String functionName,
                                                    Class eventTypeType,
                                                    Class underlyingType,
                                                    String typeNameEvent,
                                                    Object event,
                                                    SupportEventInfra.FunctionSendEventWType sendEvent,
                                                    String[] propertyName,
                                                    Object[] propertyValues) {
        String streamName = prefix + "_Stream";
        String textOne = "insert into " + streamName + " select * from " + typeNameOrigin;
        String textTwo = "insert into " + streamName + " select " + SupportStaticMethodLib.class.getName() + "." + functionName + "(s0) from " + typeNameEvent + " as s0";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);
        EventType type = stmtOne.getEventType();
        assertEquals(underlyingType, type.getUnderlyingType());

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);
        type = stmtTwo.getEventType();
        assertEquals(underlyingType, type.getUnderlyingType());

        sendEvent.apply(epService, event, typeNameEvent);

        EventBean theEvent = listenerTwo.assertOneGetNewAndReset();
        assertEquals(eventTypeType, theEvent.getEventType().getClass());
        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(theEvent.getUnderlying().getClass(), underlyingType));
        EPAssertionUtil.assertProps(theEvent, propertyName, propertyValues);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionConversionConfiguredType(String typeNameTarget,
                                                      String functionName,
                                                      String typeNameOrigin,
                                                    Class eventBeanType,
                                                    Class underlyingType,
                                                    Object event,
                                                    SupportEventInfra.FunctionSendEventWType sendEvent,
                                                    String[] propertyName,
                                                    Object[] propertyValues) {

        // test native
        epService.getEPAdministrator().createEPL("insert into " + typeNameTarget + " select " + SupportStaticMethodLib.class.getName() + "." + functionName + "(s0) from " + typeNameOrigin + " as s0");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + typeNameTarget);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent.apply(epService, event, typeNameOrigin);

        EventBean eventBean = listener.assertOneGetNewAndReset();
        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(eventBean.getUnderlying().getClass(), underlyingType));
        assertEquals(eventBeanType, eventBean.getClass());
        EPAssertionUtil.assertProps(eventBean, propertyName, propertyValues);

        epService.getEPAdministrator().destroyAllStatements();
    }
}
