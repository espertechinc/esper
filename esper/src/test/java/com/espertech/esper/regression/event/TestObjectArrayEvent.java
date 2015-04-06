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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.*;

public class TestObjectArrayEvent extends TestCase
{
    private EPServiceProvider epService;

    protected void setUp()
    {
        String[] names = {"myInt", "myString", "beanA"};
        Object[] types = {Integer.class, String.class, SupportBeanComplexProps.class};

        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("MyObjectArrayEvent", names, types);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testObjectArrayNestedMapEventType() {
        EventAdapterService eventAdapterService = ((EPServiceProviderSPI) epService).getEventAdapterService();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EventType supportBeanType = epService.getEPAdministrator().getConfiguration().getEventType("SupportBean");

        Map<String, Object> lev2def = new HashMap<String, Object>();
        lev2def.put("sb", "SupportBean");
        Map<String, Object> lev1def = new HashMap<String, Object>();
        lev1def.put("lev1name", lev2def);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapNestedObjectArray", new String[]{"lev0name"}, new Object[]{lev1def});
        assertEquals(Object[].class, epService.getEPAdministrator().getConfiguration().getEventType("MyMapNestedObjectArray").getUnderlyingType());

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select lev0name.lev1name.sb.theString as val from MyMapNestedObjectArray").addListener(listener);

        Map<String, Object> lev2data = new HashMap<String, Object>();
        lev2data.put("sb", eventAdapterService.adapterForTypedBean(new SupportBean("E1", 0), supportBeanType));
        Map<String, Object> lev1data = new HashMap<String, Object>();
        lev1data.put("lev1name", lev2data);

        epService.getEPRuntime().sendEvent(new Object[] {lev1data}, "MyMapNestedObjectArray");
        assertEquals("E1", listener.assertOneGetNewAndReset().get("val"));
        
        try {
            epService.getEPRuntime().sendEvent(new HashMap(), "MyMapNestedObjectArray");
            fail();
        }
        catch (EPException ex) {
            assertEquals("Event type named 'MyMapNestedObjectArray' has not been defined or is not a Map event type, the name 'MyMapNestedObjectArray' refers to a java.lang.Object(Array) event type", ex.getMessage());
        }
    }

    public void testMetadata()
    {
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("MyObjectArrayEvent");
        assertEquals(EventTypeMetadata.ApplicationType.OBJECTARR, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyObjectArrayEvent", type.getMetadata().getPrimaryName());
        assertEquals("MyObjectArrayEvent", type.getMetadata().getPublicName());
        assertEquals("MyObjectArrayEvent", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EventType[] types = ((EPServiceProviderSPI)epService).getEventAdapterService().getAllTypes();
        assertEquals(1, types.length);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("myInt", Integer.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("beanA", SupportBeanComplexProps.class, null, false, false, false, false, true),
        }, type.getPropertyDescriptors());
    }

    public void testAddRemoveType()
    {
        // test remove type with statement used (no force)
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select myInt from MyObjectArrayEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").toArray(), new String[]{"stmtOne"});
        
        assertEquals(1, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals(Object[].class, epService.getEPAdministrator().getConfiguration().getEventType("MyObjectArrayEvent").getUnderlyingType());

        try {
            configOps.removeEventType("MyObjectArrayEvent", false);
        }
        catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyObjectArrayEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertTrue(configOps.removeEventType("MyObjectArrayEvent", false));
        assertFalse(configOps.removeEventType("MyObjectArrayEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertEquals(0, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals(null, epService.getEPAdministrator().getConfiguration().getEventType("MyObjectArrayEvent"));
        try {
            epService.getEPAdministrator().createEPL("select myInt from MyObjectArrayEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        // add back the type
        configOps.addEventType("MyObjectArrayEvent", new String[] {"p01"}, new Object[] {String.class});
        assertTrue(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").isEmpty());
        assertEquals(1, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals("MyObjectArrayEvent", epService.getEPAdministrator().getConfiguration().getEventType("MyObjectArrayEvent").getName());

        // compile
        epService.getEPAdministrator().createEPL("select p01 from MyObjectArrayEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select myInt from MyObjectArrayEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyObjectArrayEvent", false);
        }
        catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyObjectArrayEvent"));
        }
        assertTrue(configOps.removeEventType("MyObjectArrayEvent", true));
        assertFalse(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").isEmpty());

        // add back the type
        configOps.addEventType("MyObjectArrayEvent", new String[] {"newprop"}, new Object[] {String.class});
        assertTrue(configOps.isEventTypeExists("MyObjectArrayEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select newprop from MyObjectArrayEvent");
        try {
            epService.getEPAdministrator().createEPL("select p01 from MyObjectArrayEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }
    }

    public void testNestedObjects()
    {
        String statementText = "select beanA.simpleProperty as simple," +
                    "beanA.nested.nestedValue as nested," +
                    "beanA.indexed[1] as indexed," +
                    "beanA.nested.nestedNested.nestedNestedValue as nestednested " +
                    "from MyObjectArrayEvent.win:length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {3, "some string", SupportBeanComplexProps.makeDefaultBean()}, "MyObjectArrayEvent");
        assertEquals("nestedValue", listener.getLastNewData()[0].get("nested"));
        assertEquals(2, listener.getLastNewData()[0].get("indexed"));
        assertEquals("nestedNestedValue", listener.getLastNewData()[0].get("nestednested"));
        statement.stop();
    }

    public void testQueryFields()
    {
        String statementText = "select myInt + 2 as intVal, 'x' || myString || 'x' as stringVal from MyObjectArrayEvent.win:length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // send Map<String, Object> event
        epService.getEPRuntime().sendEvent(new Object[] {3, "some string", SupportBeanComplexProps.makeDefaultBean()}, "MyObjectArrayEvent");
        assertEquals(5, listener.getLastNewData()[0].get("intVal"));
        assertEquals("xsome stringx", listener.getLastNewData()[0].get("stringVal"));

        // send Map base event
        epService.getEPRuntime().sendEvent(new Object[] {4, "string2", null}, "MyObjectArrayEvent");
        assertEquals(6, listener.getLastNewData()[0].get("intVal"));
        assertEquals("xstring2x", listener.getLastNewData()[0].get("stringVal"));

        statement.stop();
    }

    public void testInvalid()
    {
        try
        {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.addEventType("MyInvalidEvent", new String[] {"p00"}, new Object[] {int.class, String.class});
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Number of property names and property types do not match, found 1 property names and 2 property types", ex.getMessage());
        }

        tryInvalid("select XXX from MyObjectArrayEvent.win:length(5)");
        tryInvalid("select myString * 2 from MyObjectArrayEvent.win:length(5)");
        tryInvalid("select String.trim(myInt) from MyObjectArrayEvent.win:length(5)");

        ConfigurationEventTypeObjectArray invalidOAConfig = new ConfigurationEventTypeObjectArray();
        invalidOAConfig.setSuperTypes(new HashSet<String>(Arrays.asList("A", "B")));
        String[] invalidOANames = new String[] {"p00"};
        Object[] invalidOATypes = new Object[] {int.class};
        try
        {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.addEventType("MyInvalidEventTwo", invalidOANames, invalidOATypes, invalidOAConfig);
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Object-array event types only allow a single supertype", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().getConfiguration().addEventType("MyInvalidOA", invalidOANames, invalidOATypes, invalidOAConfig);
            fail();
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Object-array event types only allow a single supertype", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().createEPL("create objectarray schema InvalidOA () inherits A, B");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Object-array event types only allow a single supertype [create objectarray schema InvalidOA () inherits A, B]", ex.getMessage());
        }
    }

    public void testSendMapNative()
    {
        String statementText = "select * from MyObjectArrayEvent.win:length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // send event
        Object[] theEvent = new Object[] {3, "some string", SupportBeanComplexProps.makeDefaultBean()};
        epService.getEPRuntime().sendEvent(theEvent, "MyObjectArrayEvent");

        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertSame(theEvent, listener.getLastNewData()[0].getUnderlying());
        assertEquals(3, listener.getLastNewData()[0].get("myInt"));
        assertEquals("some string", listener.getLastNewData()[0].get("myString"));

        // send event
        theEvent = new Object[] {4, "string2", null};
        epService.getEPRuntime().sendEvent(theEvent, "MyObjectArrayEvent");

        assertTrue(listener.getAndClearIsInvoked());
        assertEquals(1, listener.getLastNewData().length);
        assertEquals(theEvent, listener.getLastNewData()[0].getUnderlying());
        assertEquals(4, listener.getLastNewData()[0].get("myInt"));
        assertEquals("string2", listener.getLastNewData()[0].get("myString"));
    }

    public void testPerformanceOutput() {

        /**
         * Comment-in for manual testing.
         */
        // POJO input, Map output, 10M, 23.45 sec
        // POJO input, Object[] output, 10M, 17.204 sec
        // Map input, Map output, 10M, 25.33 sec
        // Map input, Object[] output, 10M, 19.797 sec
        // Object[] input, Map output, 10M, 22.2 sec
        // Object[] input, Object[] output, 10M, 16.97 sec
        //
        // memory: 608000 POJO for 32m, 521000 Object[] for 32m, 41000 Map for 32m
        //

        // type prep
        /*
        epService.getEPAdministrator().getConfiguration().addEventType(MyPOJOEvent.class);
        Map<String, Object> mapdef = new HashMap<String, Object>();
        Map<String, Object> mapval = new HashMap<String, Object>();
        List<String> propertyNames = new ArrayList<String>();
        List<Object> propertyTypes = new ArrayList<Object>();
        for (int i = 0; i < 10; i++) {
            mapdef.put("p" + i, String.class);
            mapval.put("p" + i, "p" + i);
            propertyNames.add("p" + i);
            propertyTypes.add(String.class);
        }
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapEvent", mapdef);
        epService.getEPAdministrator().getConfiguration().addEventType("MyObjectArray", propertyNames.toArray(new String[propertyNames.size()]), propertyTypes.toArray());

        // stmt prep
        EPStatement stmtPOJO = epService.getEPAdministrator().createEPL(OutputTypeEnum.ARRAY.getAnnotationText() + " select p0,p1,p2,p3,p4,p5,p6,p7,p8,p9 from MyObjectArray");
        assertEquals(OutputTypeEnum.ARRAY.getOutputClass(), stmtPOJO.getEventType().getUnderlyingType());
        stmtPOJO.addListener(new MyDiscardListener());

        // event prep
        //Object event = new MyPOJOEvent("p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p8");
        Object[] objectArrayEvent = {"p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p8"};

        // loop
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            //epService.getEPRuntime().sendEvent(event);
            //epService.getEPRuntime().sendEvent(mapval, "MyMapEvent");
            epService.getEPRuntime().sendEvent(objectArrayEvent, "MyObjectArray");
        }
        long end = System.currentTimeMillis();
        double deltaSec = (end - start) / 1000d;

        System.out.println("Delta: " + deltaSec);
        */
    }

    private void tryInvalid(String statementText)
    {
        try
        {
            epService.getEPAdministrator().createEPL(statementText);
            fail();
        }
        catch (EPException ex)
        {
            // expected
        }
    }

    public static class MyPOJOEvent {
        private String p0;
        private String p1;
        private String p2;
        private String p3;
        private String p4;
        private String p5;
        private String p6;
        private String p7;
        private String p8;
        private String p9;

        public MyPOJOEvent(String p0, String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.p4 = p4;
            this.p5 = p5;
            this.p6 = p6;
            this.p7 = p7;
            this.p8 = p8;
            this.p9 = p9;
        }

        public String getP0() {
            return p0;
        }

        public String getP1() {
            return p1;
        }

        public String getP2() {
            return p2;
        }

        public String getP3() {
            return p3;
        }

        public String getP4() {
            return p4;
        }

        public String getP5() {
            return p5;
        }

        public String getP6() {
            return p6;
        }

        public String getP7() {
            return p7;
        }

        public String getP8() {
            return p8;
        }

        public String getP9() {
            return p9;
        }
    }

    public class MyDiscardListener implements UpdateListener {
        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        }
    }
}
