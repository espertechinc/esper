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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecEventObjectArray implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        String[] names = {"myInt", "myString", "beanA"};
        Object[] types = {Integer.class, String.class, SupportBeanComplexProps.class};
        configuration.addEventType("MyObjectArrayEvent", names, types);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMetadata(epService);
        runAssertionNestedObjects(epService);
        runAssertionQueryFields(epService);
        runAssertionInvalid(epService);
        runAssertionNestedEventBeanArray(epService);
        runAssertionAddRemoveType(epService);
    }

    private void runAssertionNestedEventBeanArray(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema NBALvl1(val string)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from NBALvl1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {"somevalue"}, "NBALvl1");
        EventBean event = listener.assertOneGetNewAndReset();
        stmt.destroy();

        // add containing-type via API
        epService.getEPAdministrator().getConfiguration().addEventType("NBALvl0", new String[] {"lvl1s"}, new Object[] {new EventType[] {event.getEventType()}});
        stmt = epService.getEPAdministrator().createEPL("select lvl1s[0] as c0 from NBALvl0");
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new Object[] {new EventBean[] {event}}, "NBALvl0");
        assertEquals("somevalue", ((Object[]) listener.assertOneGetNewAndReset().get("c0"))[0]);

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("NBALvl1", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("NBALvl0", true);
    }

    protected static Object getNestedKeyOA(Object[] array, int index, String keyTwo) {
        Map map = (Map) array[index];
        return map.get(keyTwo);
    }

    protected static Object getNestedKeyOA(Object[] array, int index, String keyTwo, String keyThree) {
        Map map = (Map) array[index];
        map = (Map) map.get(keyTwo);
        return map.get(keyThree);
    }

    private void runAssertionMetadata(EPServiceProvider epService) {
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("MyObjectArrayEvent");
        assertEquals(EventTypeMetadata.ApplicationType.OBJECTARR, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("MyObjectArrayEvent", type.getMetadata().getPrimaryName());
        assertEquals("MyObjectArrayEvent", type.getMetadata().getPublicName());
        assertEquals("MyObjectArrayEvent", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EventType[] types = ((EPServiceProviderSPI) epService).getEventAdapterService().getAllTypes();
        assertEquals(1, types.length);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("myInt", Integer.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("beanA", SupportBeanComplexProps.class, null, false, false, false, false, true),
        }, type.getPropertyDescriptors());
    }

    private void runAssertionAddRemoveType(EPServiceProvider epService) {
        // test remove type with statement used (no force)
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select myInt from MyObjectArrayEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").toArray(), new String[]{"stmtOne"});

        int numTypes = epService.getEPAdministrator().getConfiguration().getEventTypes().length;
        assertEquals(Object[].class, epService.getEPAdministrator().getConfiguration().getEventType("MyObjectArrayEvent").getUnderlyingType());

        try {
            configOps.removeEventType("MyObjectArrayEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyObjectArrayEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertTrue(configOps.removeEventType("MyObjectArrayEvent", false));
        assertFalse(configOps.removeEventType("MyObjectArrayEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertEquals(numTypes - 1, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals(null, epService.getEPAdministrator().getConfiguration().getEventType("MyObjectArrayEvent"));
        try {
            epService.getEPAdministrator().createEPL("select myInt from MyObjectArrayEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // add back the type
        configOps.addEventType("MyObjectArrayEvent", new String[]{"p01"}, new Object[]{String.class});
        assertTrue(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").isEmpty());
        assertEquals(numTypes, epService.getEPAdministrator().getConfiguration().getEventTypes().length);
        assertEquals("MyObjectArrayEvent", epService.getEPAdministrator().getConfiguration().getEventType("MyObjectArrayEvent").getName());

        // compile
        epService.getEPAdministrator().createEPL("select p01 from MyObjectArrayEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select myInt from MyObjectArrayEvent");
            fail();
        } catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyObjectArrayEvent", false);
        } catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyObjectArrayEvent"));
        }
        assertTrue(configOps.removeEventType("MyObjectArrayEvent", true));
        assertFalse(configOps.isEventTypeExists("MyObjectArrayEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyObjectArrayEvent").isEmpty());

        // add back the type
        configOps.addEventType("MyObjectArrayEvent", new String[]{"newprop"}, new Object[]{String.class});
        assertTrue(configOps.isEventTypeExists("MyObjectArrayEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select newprop from MyObjectArrayEvent");
        try {
            epService.getEPAdministrator().createEPL("select p01 from MyObjectArrayEvent");
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
                "from MyObjectArrayEvent#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{3, "some string", SupportBeanComplexProps.makeDefaultBean()}, "MyObjectArrayEvent");
        assertEquals("nestedValue", listener.getLastNewData()[0].get("nested"));
        assertEquals(2, listener.getLastNewData()[0].get("indexed"));
        assertEquals("nestedNestedValue", listener.getLastNewData()[0].get("nestednested"));
        statement.stop();
    }

    private void runAssertionQueryFields(EPServiceProvider epService) {
        String statementText = "select myInt + 2 as intVal, 'x' || myString || 'x' as stringVal from MyObjectArrayEvent#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        // send Map<String, Object> event
        epService.getEPRuntime().sendEvent(new Object[]{3, "some string", SupportBeanComplexProps.makeDefaultBean()}, "MyObjectArrayEvent");
        assertEquals(5, listener.getLastNewData()[0].get("intVal"));
        assertEquals("xsome stringx", listener.getLastNewData()[0].get("stringVal"));

        // send Map base event
        epService.getEPRuntime().sendEvent(new Object[]{4, "string2", null}, "MyObjectArrayEvent");
        assertEquals(6, listener.getLastNewData()[0].get("intVal"));
        assertEquals("xstring2x", listener.getLastNewData()[0].get("stringVal"));

        statement.stop();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        try {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.addEventType("MyInvalidEvent", new String[]{"p00"}, new Object[]{int.class, String.class});
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Number of property names and property types do not match, found 1 property names and 2 property types", ex.getMessage());
        }

        tryInvalid(epService, "select XXX from MyObjectArrayEvent#length(5)");
        tryInvalid(epService, "select myString * 2 from MyObjectArrayEvent#length(5)");
        tryInvalid(epService, "select String.trim(myInt) from MyObjectArrayEvent#length(5)");

        ConfigurationEventTypeObjectArray invalidOAConfig = new ConfigurationEventTypeObjectArray();
        invalidOAConfig.setSuperTypes(new HashSet<String>(Arrays.asList("A", "B")));
        String[] invalidOANames = new String[]{"p00"};
        Object[] invalidOATypes = new Object[]{int.class};
        try {
            Configuration configuration = SupportConfigFactory.getConfiguration();
            configuration.addEventType("MyInvalidEventTwo", invalidOANames, invalidOATypes, invalidOAConfig);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Object-array event types only allow a single supertype", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().getConfiguration().addEventType("MyInvalidOA", invalidOANames, invalidOATypes, invalidOAConfig);
            fail();
        } catch (ConfigurationException ex) {
            assertEquals("Object-array event types only allow a single supertype", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().createEPL("create objectarray schema InvalidOA () inherits A, B");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Object-array event types only allow a single supertype [create objectarray schema InvalidOA () inherits A, B]", ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
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
