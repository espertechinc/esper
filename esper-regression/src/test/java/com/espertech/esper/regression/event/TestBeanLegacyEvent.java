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
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestBeanLegacyEvent extends TestCase
{
    private SupportLegacyBean legacyBean;
    private EPServiceProvider epService;

    protected void setUp()
    {
        Map<String, String> mappedProperty = new HashMap<String, String>();
        mappedProperty.put("key1", "value1");
        mappedProperty.put("key2", "value2");
        legacyBean = new SupportLegacyBean("leg", new String[] {"a", "b"}, mappedProperty, "nest");
    }

    public void testAddRemoveType()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();

        // test remove type with statement used (no force)
        configOps.addEventType("MyBeanEvent", SupportBean_A.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select id from MyBeanEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyBeanEvent").toArray(), new String[]{"stmtOne"});

        try {
            configOps.removeEventType("MyBeanEvent", false);
        }
        catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyBeanEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyBeanEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyBeanEvent"));
        assertTrue(configOps.removeEventType("MyBeanEvent", false));
        assertFalse(configOps.removeEventType("MyBeanEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyBeanEvent"));
        try {
            epService.getEPAdministrator().createEPL("select id from MyBeanEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        // add back the type
        configOps.addEventType("MyBeanEvent", SupportBean.class);
        assertTrue(configOps.isEventTypeExists("MyBeanEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyBeanEvent").isEmpty());

        // compile
        epService.getEPAdministrator().createEPL("select boolPrimitive from MyBeanEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyBeanEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select id from MyBeanEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyBeanEvent", false);
        }
        catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyBeanEvent"));
        }
        assertTrue(configOps.removeEventType("MyBeanEvent", true));
        assertFalse(configOps.isEventTypeExists("MyBeanEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyBeanEvent").isEmpty());

        // add back the type
        configOps.addEventType("MyBeanEvent", SupportMarketDataBean.class);
        assertTrue(configOps.isEventTypeExists("MyBeanEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select feed from MyBeanEvent");
        try {
            epService.getEPAdministrator().createEPL("select boolPrimitive from MyBeanEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testPublicAccessors()
    {
        tryPublicAccessors(ConfigurationEventTypeLegacy.CodeGeneration.ENABLED);
    }

    public void testPublicAccessorsNoCodeGen()
    {
        tryPublicAccessors(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
    }

    public void testExplicitOnly()
    {
        tryExplicitOnlyAccessors(ConfigurationEventTypeLegacy.CodeGeneration.ENABLED);
    }

    public void testExplicitOnlyNoCodeGen()
    {
        tryExplicitOnlyAccessors(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
    }

    public void testJavaBeanAccessor()
    {
        tryJavaBeanAccessor(ConfigurationEventTypeLegacy.CodeGeneration.ENABLED);
    }

    public void testJavaBeanAccessorNoCodeGen()
    {
        tryJavaBeanAccessor(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
    }

    public void testFinalClass()
    {
        tryFinalClass(ConfigurationEventTypeLegacy.CodeGeneration.ENABLED);
    }

    public void testFinalClassNoCodeGen()
    {
        tryFinalClass(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
    }

    private void tryPublicAccessors(ConfigurationEventTypeLegacy.CodeGeneration codeGeneration)
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        legacyDef.setCodeGeneration(codeGeneration);
        legacyDef.addFieldProperty("explicitFSimple", "fieldLegacyVal");
        legacyDef.addFieldProperty("explicitFIndexed", "fieldStringArray");
        legacyDef.addFieldProperty("explicitFNested", "fieldNested");
        legacyDef.addMethodProperty("explicitMSimple", "readLegacyBeanVal");
        legacyDef.addMethodProperty("explicitMArray", "readStringArray");
        legacyDef.addMethodProperty("explicitMIndexed", "readStringIndexed");
        legacyDef.addMethodProperty("explicitMMapped", "readMapByKey");
        epService.getEPAdministrator().getConfiguration().addEventType("MyLegacyEvent", SupportLegacyBean.class.getName(), legacyDef);

        legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        legacyDef.setCodeGeneration(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        epService.getEPAdministrator().getConfiguration().addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), legacyDef);

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("MyLegacyEvent");
        assertEquals(EventTypeMetadata.ApplicationType.CLASS, type.getMetadata().getOptionalApplicationType());
        assertEquals(1, type.getMetadata().getOptionalSecondaryNames().size());
        assertEquals(SupportLegacyBean.class.getName(), type.getMetadata().getOptionalSecondaryNames().iterator().next());
        assertEquals("MyLegacyEvent", type.getMetadata().getPrimaryName());
        assertEquals("MyLegacyEvent", type.getMetadata().getPublicName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfigured());
        assertEquals(false, type.getMetadata().isApplicationPreConfiguredStatic());

        String statementText = "select " +
                    "fieldLegacyVal as fieldSimple," +
                    "fieldStringArray as fieldArr," +
                    "fieldStringArray[1] as fieldArrIndexed," +
                    "fieldMapped as fieldMap," +
                    "fieldNested as fieldNested," +
                    "fieldNested.readNestedValue as fieldNestedVal," +
                    "readLegacyBeanVal as simple," +
                    "readLegacyNested as nestedObject," +
                    "readLegacyNested.readNestedValue as nested," +
                    "readStringArray[0] as array," +
                    "readStringIndexed[1] as indexed," +
                    "readMapByKey('key1') as mapped," +
                    "readMap as mapItself," +
                    "explicitFSimple, " +
                    "explicitFIndexed[0], " +
                    "explicitFNested, " +
                    "explicitMSimple, " +
                    "explicitMArray[0], " +
                    "explicitMIndexed[1], " +
                    "explicitMMapped('key2')" +
                    " from MyLegacyEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        EventType eventType = statement.getEventType();
        assertEquals(String.class, eventType.getPropertyType("fieldSimple"));
        assertEquals(String[].class, eventType.getPropertyType("fieldArr"));
        assertEquals(String.class, eventType.getPropertyType("fieldArrIndexed"));
        assertEquals(Map.class, eventType.getPropertyType("fieldMap"));
        assertEquals(SupportLegacyBean.LegacyNested.class, eventType.getPropertyType("fieldNested"));
        assertEquals(String.class, eventType.getPropertyType("fieldNestedVal"));
        assertEquals(String.class, eventType.getPropertyType("simple"));
        assertEquals(SupportLegacyBean.LegacyNested.class, eventType.getPropertyType("nestedObject"));
        assertEquals(String.class, eventType.getPropertyType("nested"));
        assertEquals(String.class, eventType.getPropertyType("array"));
        assertEquals(String.class, eventType.getPropertyType("indexed"));
        assertEquals(String.class, eventType.getPropertyType("mapped"));
        assertEquals(String.class, eventType.getPropertyType("explicitFSimple"));
        assertEquals(String.class, eventType.getPropertyType("explicitFIndexed[0]"));
        assertEquals(SupportLegacyBean.LegacyNested.class, eventType.getPropertyType("explicitFNested"));
        assertEquals(String.class, eventType.getPropertyType("explicitMSimple"));
        assertEquals(String.class, eventType.getPropertyType("explicitMArray[0]"));
        assertEquals(String.class, eventType.getPropertyType("explicitMIndexed[1]"));
        assertEquals(String.class, eventType.getPropertyType("explicitMMapped('key2')"));

        epService.getEPRuntime().sendEvent(legacyBean);

        assertEquals(legacyBean.fieldLegacyVal, listener.getLastNewData()[0].get("fieldSimple"));
        assertEquals(legacyBean.fieldStringArray, listener.getLastNewData()[0].get("fieldArr"));
        assertEquals(legacyBean.fieldStringArray[1], listener.getLastNewData()[0].get("fieldArrIndexed"));
        assertEquals(legacyBean.fieldMapped, listener.getLastNewData()[0].get("fieldMap"));
        assertEquals(legacyBean.fieldNested, listener.getLastNewData()[0].get("fieldNested"));
        assertEquals(legacyBean.fieldNested.readNestedValue(), listener.getLastNewData()[0].get("fieldNestedVal"));

        assertEquals(legacyBean.readLegacyBeanVal(), listener.getLastNewData()[0].get("simple"));
        assertEquals(legacyBean.readLegacyNested(), listener.getLastNewData()[0].get("nestedObject"));
        assertEquals(legacyBean.readLegacyNested().readNestedValue(), listener.getLastNewData()[0].get("nested"));
        assertEquals(legacyBean.readStringIndexed(0), listener.getLastNewData()[0].get("array"));
        assertEquals(legacyBean.readStringIndexed(1), listener.getLastNewData()[0].get("indexed"));
        assertEquals(legacyBean.readMapByKey("key1"), listener.getLastNewData()[0].get("mapped"));
        assertEquals(legacyBean.readMap(), listener.getLastNewData()[0].get("mapItself"));

        assertEquals(legacyBean.readLegacyBeanVal(), listener.getLastNewData()[0].get("explicitFSimple"));
        assertEquals(legacyBean.readLegacyBeanVal(), listener.getLastNewData()[0].get("explicitMSimple"));
        assertEquals(legacyBean.readLegacyNested(), listener.getLastNewData()[0].get("explicitFNested"));
        assertEquals(legacyBean.readStringIndexed(0), listener.getLastNewData()[0].get("explicitFIndexed[0]"));
        assertEquals(legacyBean.readStringIndexed(0), listener.getLastNewData()[0].get("explicitMArray[0]"));
        assertEquals(legacyBean.readStringIndexed(1), listener.getLastNewData()[0].get("explicitMIndexed[1]"));
        assertEquals(legacyBean.readMapByKey("key2"), listener.getLastNewData()[0].get("explicitMMapped('key2')"));

        EventTypeSPI stmtType = (EventTypeSPI) statement.getEventType();
        assertEquals(null, stmtType.getMetadata().getOptionalApplicationType());
        assertEquals(null, stmtType.getMetadata().getOptionalSecondaryNames());
        assertNotNull(stmtType.getMetadata().getPrimaryName());
        assertNotNull(stmtType.getMetadata().getPublicName());
        assertNotNull(stmtType.getName());
        assertEquals(EventTypeMetadata.TypeClass.ANONYMOUS, stmtType.getMetadata().getTypeClass());
        assertEquals(false, stmtType.getMetadata().isApplicationConfigured());        
        assertEquals(false, stmtType.getMetadata().isApplicationPreConfigured());
        assertEquals(false, stmtType.getMetadata().isApplicationPreConfiguredStatic());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }

    private void tryExplicitOnlyAccessors(ConfigurationEventTypeLegacy.CodeGeneration codeGeneration)
    {
        Configuration config = SupportConfigFactory.getConfiguration();

        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT);
        legacyDef.setCodeGeneration(codeGeneration);
        legacyDef.addFieldProperty("explicitFNested", "fieldNested");
        legacyDef.addMethodProperty("explicitMNested", "readLegacyNested");
        config.addEventType("MyLegacyEvent", SupportLegacyBean.class.getName(), legacyDef);

        legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT);
        legacyDef.setCodeGeneration(codeGeneration);
        legacyDef.addFieldProperty("fieldNestedClassValue", "fieldNestedValue");
        legacyDef.addMethodProperty("readNestedClassValue", "readNestedValue");
        config.addEventType("MyLegacyNestedEvent", SupportLegacyBean.LegacyNested.class.getName(), legacyDef);

        legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.EXPLICIT);
        legacyDef.setCodeGeneration(codeGeneration);
        config.addEventType("MySupportBean", SupportBean.class.getName(), legacyDef);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String statementText = "select " +
                    "explicitFNested.fieldNestedClassValue as fnested, " +
                    "explicitMNested.readNestedClassValue as mnested" +
                    " from MyLegacyEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        EventType eventType = statement.getEventType();
        assertEquals(String.class, eventType.getPropertyType("fnested"));
        assertEquals(String.class, eventType.getPropertyType("mnested"));

        epService.getEPRuntime().sendEvent(legacyBean);

        assertEquals(legacyBean.fieldNested.readNestedValue(), listener.getLastNewData()[0].get("fnested"));
        assertEquals(legacyBean.fieldNested.readNestedValue(), listener.getLastNewData()[0].get("mnested"));

        try
        {
            // invalid statement, JavaBean-style getters not exposed
            statementText = "select intPrimitive from MySupportBean#length(5)";
            epService.getEPAdministrator().createEPL(statementText);
        }
        catch (EPStatementException ex)
        {
            // expected
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }

    public void tryJavaBeanAccessor(ConfigurationEventTypeLegacy.CodeGeneration codeGeneration)
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.JAVABEAN);
        legacyDef.setCodeGeneration(codeGeneration);
        legacyDef.addFieldProperty("explicitFInt", "fieldIntPrimitive");
        legacyDef.addMethodProperty("explicitMGetInt", "getIntPrimitive");
        legacyDef.addMethodProperty("explicitMReadInt", "readIntPrimitive");
        config.addEventType("MyLegacyEvent", SupportLegacyBeanInt.class.getName(), legacyDef);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String statementText = "select intPrimitive, explicitFInt, explicitMGetInt, explicitMReadInt " +
                    " from MyLegacyEvent#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        EventType eventType = statement.getEventType();

        SupportLegacyBeanInt theEvent = new SupportLegacyBeanInt(10);
        epService.getEPRuntime().sendEvent(theEvent);

        for (String name : new String[] {"intPrimitive", "explicitFInt", "explicitMGetInt", "explicitMReadInt"})
        {
            assertEquals(int.class, eventType.getPropertyType(name));
            assertEquals(10, listener.getLastNewData()[0].get(name));
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }

    private void tryFinalClass(ConfigurationEventTypeLegacy.CodeGeneration codeGeneration)
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.JAVABEAN);
        legacyDef.setCodeGeneration(codeGeneration);
        config.addEventType("MyFinalEvent", SupportBeanFinal.class.getName(), legacyDef);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String statementText = "select intPrimitive " +
                    "from " + SupportBeanFinal.class.getName() + "#length(5)";

        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        SupportBeanFinal theEvent = new SupportBeanFinal(10);
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(10, listener.getLastNewData()[0].get("intPrimitive"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }
}
