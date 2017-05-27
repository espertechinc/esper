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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class ExecEventMap implements RegressionExecution {
    private Properties properties;
    private Map<String, Object> map;

    public ExecEventMap() {
        properties = new Properties();
        properties.put("myInt", "int");
        properties.put("myString", "string");
        properties.put("beanA", SupportBeanComplexProps.class.getName());
        properties.put("myStringArray", "string[]");

        map = new HashMap<String, Object>();
        map.put("myInt", 3);
        map.put("myString", "some string");
        map.put("beanA", SupportBeanComplexProps.makeDefaultBean());
    }

    public void configure(Configuration configuration) {
        configuration.addEventType("myMapEvent", properties);
    }

    public void run(EPServiceProvider epService) {
        runAssertionMapNestedEventType(epService);
        runAssertionMetadata(epService);
        runAssertionNestedObjects(epService);
        runAssertionQueryFields(epService);
        runAssertionInvalidStatement(epService);
        runAssertionAddRemoveType(epService);
    }

    public static Map<String, Object> makeMap(String nameValuePairs) {
        Map result = new HashMap<String, Object>();
        String[] elements = nameValuePairs.split(",");
        for (int i = 0; i < elements.length; i++) {
            String[] pair = elements[i].split("=");
            if (pair.length == 2) {
                result.put(pair[0], pair[1]);
            }
        }
        return result;
    }

    public static Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        if (entries == null) {
            return result;
        }
        for (int i = 0; i < entries.length; i++) {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }

    public static Properties makeProperties(Object[][] entries) {
        Properties result = new Properties();
        for (int i = 0; i < entries.length; i++) {
            Class clazz = (Class) entries[i][1];
            result.put(entries[i][0], clazz.getName());
        }
        return result;
    }

    public static Object getNestedKeyMap(Map<String, Object> root, String keyOne, String keyTwo) {
        Map map = (Map) root.get(keyOne);
        return map.get(keyTwo);
    }

    protected static Object getNestedKeyMap(Map<String, Object> root, String keyOne, String keyTwo, String keyThree) {
        Map map = (Map) root.get(keyOne);
        map = (Map) map.get(keyTwo);
        return map.get(keyThree);
    }

    private void runAssertionMapNestedEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EventType supportBeanType = epService.getEPAdministrator().getConfiguration().getEventType("SupportBean");

        Map<String, Object> lev2def = new HashMap<String, Object>();
        lev2def.put("sb", "SupportBean");
        Map<String, Object> lev1def = new HashMap<String, Object>();
        lev1def.put("lev1name", lev2def);
        Map<String, Object> lev0def = new HashMap<String, Object>();
        lev0def.put("lev0name", lev1def);

        epService.getEPAdministrator().getConfiguration().addEventType("MyMap", lev0def);
        assertNotNull(epService.getEPAdministrator().getConfiguration().getEventType("MyMap"));

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select lev0name.lev1name.sb.theString as val from MyMap").addListener(listener);

        EventAdapterService eventAdapterService = ((EPServiceProviderSPI) epService).getEventAdapterService();
        Map<String, Object> lev2data = new HashMap<String, Object>();
        lev2data.put("sb", eventAdapterService.adapterForTypedBean(new SupportBean("E1", 0), supportBeanType));
        Map<String, Object> lev1data = new HashMap<String, Object>();
        lev1data.put("lev1name", lev2data);
        Map<String, Object> lev0data = new HashMap<String, Object>();
        lev0data.put("lev0name", lev1data);

        epService.getEPRuntime().sendEvent(lev0data, "MyMap");
        assertEquals("E1", listener.assertOneGetNewAndReset().get("val"));

        try {
            epService.getEPRuntime().sendEvent(new Object[0], "MyMap");
            fail();
        } catch (EPException ex) {
            assertEquals("Event type named 'MyMap' has not been defined or is not a Object-array event type, the name 'MyMap' refers to a java.util.Map event type", ex.getMessage());
        }
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMetadata(EPServiceProvider epService) {
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("myMapEvent");
        assertEquals(EventTypeMetadata.ApplicationType.MAP, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("myMapEvent", type.getMetadata().getPrimaryName());
        assertEquals("myMapEvent", type.getMetadata().getPublicName());
        assertEquals("myMapEvent", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("myInt", Integer.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("beanA", SupportBeanComplexProps.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("myStringArray", String[].class, String.class, false, false, true, false, false),
        }, type.getPropertyDescriptors());
    }

    private void runAssertionAddRemoveType(EPServiceProvider epService) {
        epService.getEPAdministrator().destroyAllStatements();

        // test remove type with statement used (no force)
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select myInt from myMapEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("myMapEvent").toArray(), new String[]{"stmtOne"});

        int numTypes = epService.getEPAdministrator().getConfiguration().getEventTypes().length;
        assertEquals("myMapEvent", epService.getEPAdministrator().getConfiguration().getEventType("myMapEvent").getName());

        try {
            configOps.removeEventType("myMapEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("myMapEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("myMapEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("myMapEvent"));
        assertTrue(configOps.removeEventType("myMapEvent", false));
        assertFalse(configOps.removeEventType("myMapEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("myMapEvent"));
        assertEquals(numTypes - 1, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals(null, epService.getEPAdministrator().getConfiguration().getEventType("myMapEvent"));
        try {
            epService.getEPAdministrator().createEPL("select myInt from myMapEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // add back the type
        Properties properties = new Properties();
        properties.put("p01", "string");
        configOps.addEventType("myMapEvent", properties);
        assertTrue(configOps.isEventTypeExists("myMapEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("myMapEvent").isEmpty());
        assertEquals(numTypes, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals("myMapEvent", epService.getEPAdministrator().getConfiguration().getEventType("myMapEvent").getName());

        // compile
        epService.getEPAdministrator().createEPL("select p01 from myMapEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("myMapEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select myInt from myMapEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("myMapEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("myMapEvent"));
        }
        assertTrue(configOps.removeEventType("myMapEvent", true));
        assertFalse(configOps.isEventTypeExists("myMapEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("myMapEvent").isEmpty());

        // add back the type
        properties = new Properties();
        properties.put("newprop", "string");
        configOps.addEventType("myMapEvent", properties);
        assertTrue(configOps.isEventTypeExists("myMapEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select newprop from myMapEvent");
        try {
            epService.getEPAdministrator().createEPL("select p01 from myMapEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedObjects(EPServiceProvider epService) {
        String statementText = "select beanA.simpleProperty as simple," +
                "beanA.nested.nestedValue as nested," +
                "beanA.indexed[1] as indexed," +
                "beanA.nested.nestedNested.nestedNestedValue as nestednested " +
                "from myMapEvent#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(map, "myMapEvent");
        assertEquals("nestedValue", listener.getLastNewData()[0].get("nested"));
        assertEquals(2, listener.getLastNewData()[0].get("indexed"));
        assertEquals("nestedNestedValue", listener.getLastNewData()[0].get("nestednested"));
        statement.stop();
    }

    private void runAssertionQueryFields(EPServiceProvider epService) {
        String statementText = "select myInt as intVal, myString as stringVal from myMapEvent#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // send Map<String, Object> event
        epService.getEPRuntime().sendEvent(map, "myMapEvent");
        assertEquals(3, listener.getLastNewData()[0].get("intVal"));
        assertEquals("some string", listener.getLastNewData()[0].get("stringVal"));

        // send Map base event
        Map mapNoType = new HashMap();
        mapNoType.put("myInt", 4);
        mapNoType.put("myString", "string2");
        epService.getEPRuntime().sendEvent(mapNoType, "myMapEvent");
        assertEquals(4, listener.getLastNewData()[0].get("intVal"));
        assertEquals("string2", listener.getLastNewData()[0].get("stringVal"));

        statement.destroy();
    }

    private void runAssertionInvalidStatement(EPServiceProvider epService) {
        tryInvalid(epService, "select XXX from myMapEvent#length(5)");
        tryInvalid(epService, "select myString * 2 from myMapEvent#length(5)");
        tryInvalid(epService, "select String.trim(myInt) from myMapEvent#length(5)");
    }

    private void tryInvalid(EPServiceProvider epService, String statementText) {
        try {
            epService.getEPAdministrator().createEPL(statementText);
            fail();
        } catch (EPException ex) {
            // expected
        }
    }
}
