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
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.event.EventTypeAssertionEnum;
import com.espertech.esper.support.event.EventTypeAssertionUtil;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import com.espertech.esper.support.util.SupportModelHelper;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestSchema extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSchemaArrayPrimitiveType() {
        runAssertionSchemaArrayPrimitiveType(true);
        runAssertionSchemaArrayPrimitiveType(false);

        SupportMessageAssertUtil.tryInvalid(epService, "create schema Invalid (x dummy[primitive])",
                "Error starting statement: Type 'dummy' is not a primitive type [create schema Invalid (x dummy[primitive])]");
        SupportMessageAssertUtil.tryInvalid(epService, "create schema Invalid (x int[dummy])",
                "Column type keyword 'dummy' not recognized, expecting '[primitive]'");
    }

    private void runAssertionSchemaArrayPrimitiveType(boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create schema MySchema as (c0 int[primitive], c1 int[])");
        Object[][] expectedType = new Object[][]{{"c0", int[].class}, {"c1", Integer[].class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedType, epService.getEPAdministrator().getConfiguration().getEventType("MySchema"), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);
        epService.getEPAdministrator().getConfiguration().removeEventType("MySchema", true);
    }

    public void testSchemaWithEventType() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BeanSourceEvent", BeanSourceEvent.class);
        BeanSourceEvent theEvent = new BeanSourceEvent(new SupportBean("E1", 1), new SupportBean_S0[] {new SupportBean_S0(2)});

        // test schema
        EPStatement stmtSchema = epService.getEPAdministrator().createEPL("create schema MySchema (bean SupportBean, beanarray SupportBean_S0[])");
        assertEquals(new EventPropertyDescriptor("bean", SupportBean.class, null, false, false, false, false, true), stmtSchema.getEventType().getPropertyDescriptor("bean"));
        assertEquals(new EventPropertyDescriptor("beanarray", SupportBean_S0[].class, SupportBean_S0.class, false, false, true, false, true), stmtSchema.getEventType().getPropertyDescriptor("beanarray"));

        EPStatement stmtSchemaInsert = epService.getEPAdministrator().createEPL("insert into MySchema select sb as bean, s0Arr as beanarray from BeanSourceEvent");
        stmtSchemaInsert.addListener(listener);
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[] {"E1", 2});
        stmtSchemaInsert.destroy();

        // test named window
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall() as (bean SupportBean, beanarray SupportBean_S0[])");
        stmtWindow.addListener(listener);
        assertEquals(new EventPropertyDescriptor("bean", SupportBean.class, null, false, false, false, false, true), stmtWindow.getEventType().getPropertyDescriptor("bean"));
        assertEquals(new EventPropertyDescriptor("beanarray", SupportBean_S0[].class, SupportBean_S0.class, false, false, true, false, true), stmtWindow.getEventType().getPropertyDescriptor("beanarray"));

        EPStatement stmtWindowInsert = epService.getEPAdministrator().createEPL("insert into MyWindow select sb as bean, s0Arr as beanarray from BeanSourceEvent");
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[] {"E1", 2});
        stmtWindowInsert.destroy();

        // insert pattern to named window
        EPStatement stmtWindowPattern = epService.getEPAdministrator().createEPL("insert into MyWindow select sb as bean, s0Arr as beanarray from pattern [sb=SupportBean -> s0Arr=SupportBean_S0 until SupportBean_S0(id=0)]");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "S0_3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id,beanarray[1].id".split(","), new Object[] {"E2", 10, 20});
        stmtWindowPattern.destroy();

        // test configured Map type
        Map<String, Object> configDef = new HashMap<String, Object>();
        configDef.put("bean", "SupportBean");
        configDef.put("beanarray", "SupportBean_S0[]");
        epService.getEPAdministrator().getConfiguration().addEventType("MyConfiguredMap", configDef);
        
        EPStatement stmtMapInsert = epService.getEPAdministrator().createEPL("insert into MyConfiguredMap select sb as bean, s0Arr as beanarray from BeanSourceEvent");
        stmtMapInsert.addListener(listener);
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[] {"E1", 2});
        stmtMapInsert.destroy();
    }

    public void testSchemaCopyProperties() {
        runAssertionSchemaCopyProperties(EventRepresentationEnum.OBJECTARRAY);
        runAssertionSchemaCopyProperties(EventRepresentationEnum.DEFAULT);
        runAssertionSchemaCopyProperties(EventRepresentationEnum.MAP);
    }

    private void runAssertionSchemaCopyProperties(EventRepresentationEnum eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema BaseOne (prop1 String, prop2 int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema BaseTwo (prop3 long)");

        // test define and send
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema E1 () copyfrom BaseOne");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from E1");
        stmtOne.addListener(listener);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmtOne.getEventType().getUnderlyingType());
        assertEquals(String.class, stmtOne.getEventType().getPropertyType("prop1"));
        assertEquals(Integer.class, stmtOne.getEventType().getPropertyType("prop2"));

        Map<String, Object> eventE1 = new LinkedHashMap<String, Object>();
        eventE1.put("prop1", "v1");
        eventE1.put("prop2", 2);
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(eventE1.values().toArray(), "E1");
        }
        else {
            epService.getEPRuntime().sendEvent(eventE1, "E1");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "prop1,prop2".split(","), new Object[]{"v1", 2});

        // test two copy-from types
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema E2 () copyfrom BaseOne, BaseTwo");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from E2");
        assertEquals(String.class, stmtTwo.getEventType().getPropertyType("prop1"));
        assertEquals(Integer.class, stmtTwo.getEventType().getPropertyType("prop2"));
        assertEquals(Long.class, stmtTwo.getEventType().getPropertyType("prop3"));

        // test API-defined type
        Map<String, Object> def = new HashMap<String, Object>();
        def.put("a", "string");
        def.put("b", String.class);
        def.put("c", "BaseOne");
        def.put("d", "BaseTwo[]");
        epService.getEPAdministrator().getConfiguration().addEventType("MyType", def);

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema E3(e long, f BaseOne) copyfrom MyType");
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from E3");
        assertEquals(String.class, stmtThree.getEventType().getPropertyType("a"));
        assertEquals(String.class, stmtThree.getEventType().getPropertyType("b"));
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            assertEquals(Object[].class, stmtThree.getEventType().getPropertyType("c"));
            assertEquals(Object[][].class, stmtThree.getEventType().getPropertyType("d"));
            assertEquals(Object[].class, stmtThree.getEventType().getPropertyType("f"));
        }
        else {
            assertEquals(Map.class, stmtThree.getEventType().getPropertyType("c"));
            assertEquals(Map[].class, stmtThree.getEventType().getPropertyType("d"));
            assertEquals(Map.class, stmtThree.getEventType().getPropertyType("f"));
        }
        assertEquals(Long.class, stmtThree.getEventType().getPropertyType("e"));

        // invalid tests
        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema E4(a long) copyFrom MyType",
                "Error starting statement: Type by name 'MyType' contributes property 'a' defined as 'java.lang.String' which overides the same property of type 'java.lang.Long' [");
        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema E4(c BaseTwo) copyFrom MyType",
                "Error starting statement: Property by name 'c' is defined twice by adding type 'MyType' [");
        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema E4(c BaseTwo) copyFrom XYZ",
                "Error starting statement: Type by name 'XYZ' could not be located [");
        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema E4 as " + SupportBean.class.getName() + " copyFrom XYZ",
                "Error starting statement: Copy-from types are not allowed with class-provided types [");
        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create variant schema E4(c BaseTwo) copyFrom XYZ",
                "Error starting statement: Copy-from types are not allowed with variant types [");

        // test SODA
        String createEPL = eventRepresentationEnum.getAnnotationText() + " create schema EX as () copyFrom BaseOne, BaseTwo";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(createEPL);
        assertEquals(createEPL.trim(), model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(createEPL.trim(), stmt.getText());

        epService.initialize();
    }
    
    public void testConfiguredNotRemoved() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", new HashMap<String, Object>());
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        epService.getEPAdministrator().createEPL("create schema ABCType(col1 int, col2 int)");
        assertTypeExists(epService, "ABCType", false);
        
        String moduleText = "select * from SupportBean;\n"+
                            "select * from MapType;\n" +
                            "select * from TestXMLNoSchemaType;\n";
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(moduleText, "uri", "arch", null);

        assertTypeExists(epService, "SupportBean", true);
        assertTypeExists(epService, "MapType", true);
        assertTypeExists(epService, "TestXMLNoSchemaType", true);

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(result.getDeploymentId());

        assertTypeExists(epService, "SupportBean", true);
        assertTypeExists(epService, "MapType", true);
        assertTypeExists(epService, "TestXMLNoSchemaType", true);
    }

    private void assertTypeExists(EPServiceProvider epService, String typeName, boolean isPreconfigured) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        EventTypeSPI type = (EventTypeSPI) spi.getEventAdapterService().getExistsTypeByName(typeName);
        assertTrue(type.getMetadata().isApplicationConfigured());
        assertEquals(isPreconfigured, type.getMetadata().isApplicationPreConfigured());
        assertFalse(type.getMetadata().isApplicationPreConfiguredStatic());
    }

    public void testInvalid() {
        runAssertionInvalid(EventRepresentationEnum.OBJECTARRAY);
        runAssertionInvalid(EventRepresentationEnum.MAP);
        runAssertionInvalid(EventRepresentationEnum.DEFAULT);
    }

    private void runAssertionInvalid(EventRepresentationEnum eventRepresentationEnum) {
        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 xxxx)",
                    "Error starting statement: Nestable type configuration encountered an unexpected property type name 'xxxx' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [");

        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 int, col1 string)",
                    "Error starting statement: Duplicate column name 'col1' [");

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 string)");
        tryInvalid("create schema MyEventType as (col1 string, col2 string)",
                    "Error starting statement: Event type named 'MyEventType' has already been declared with differing column name or type information: Type by name 'MyEventType' expects 1 properties but receives 2 properties [");

        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as () inherit ABC",
                    "Error in expression: Expected 'inherits', 'starttimestamp', 'endtimestamp' or 'copyfrom' keyword after create-schema clause but encountered 'inherit' [");

        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as () inherits ABC",
                    "Error starting statement: Supertype by name 'ABC' could not be found [");

        tryInvalid(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as () inherits",
                    "Incorrect syntax near end-of-input expecting an identifier but found end-of-input at line 1 column ");

        epService.getEPAdministrator().getConfiguration().removeEventType("MyEventType", true);
    }

    public void testDestroySameType() {
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("create schema MyEventType as (col1 string)");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("create schema MyEventType as (col1 string)");
        
        stmtOne.destroy();
        assertEquals(1, epService.getEPAdministrator().getConfiguration().getEventTypeNameUsedBy("MyEventType").size());
        assertTrue(epService.getEPAdministrator().getConfiguration().isEventTypeExists("MyEventType"));

        stmtTwo.destroy();
        assertEquals(0, epService.getEPAdministrator().getConfiguration().getEventTypeNameUsedBy("MyEventType").size());
        assertFalse(epService.getEPAdministrator().getConfiguration().isEventTypeExists("MyEventType"));
    }

    public void testColDefPlain() throws Exception {
        runAssertionColDefPlain(EventRepresentationEnum.DEFAULT);
        runAssertionColDefPlain(EventRepresentationEnum.OBJECTARRAY);
        runAssertionColDefPlain(EventRepresentationEnum.MAP);

        // test property classname, either simple or fully-qualified.
        epService.getEPAdministrator().getConfiguration().addImport("java.beans.EventHandler");
        epService.getEPAdministrator().getConfiguration().addImport("java.sql.*");
        epService.getEPAdministrator().createEPL("create schema MySchema (f1 Timestamp, f2 java.beans.BeanDescriptor, f3 EventHandler, f4 null)");

        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType("MySchema");
        assertEquals(java.sql.Timestamp.class, eventType.getPropertyType("f1"));
        assertEquals(java.beans.BeanDescriptor.class, eventType.getPropertyType("f2"));
        assertEquals(java.beans.EventHandler.class, eventType.getPropertyType("f3"));
        assertEquals(null, eventType.getPropertyType("f4"));
    }

    private void runAssertionColDefPlain(EventRepresentationEnum eventRepresentationEnum) throws Exception
    {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 string, col2 int, sbean " + SupportBean.class.getName() + ", col3.col4 int)");
        assertTypeColDef(stmtCreate.getEventType());
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select * from MyEventType");
        assertTypeColDef(stmtSelect.getEventType());

        stmtSelect.destroy();
        stmtCreate.destroy();

        // destroy and create differently 
        stmtCreate = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col3 string, col4 int)");
        assertEquals(Integer.class, stmtCreate.getEventType().getPropertyType("col4"));
        assertEquals(2, stmtCreate.getEventType().getPropertyDescriptors().length);

        stmtCreate.stop();

        // destroy and create differently
        stmtCreate = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col5 string, col6 int)");
        assertEquals(stmtCreate.getEventType().getUnderlyingType(), eventRepresentationEnum.getOutputClass());
        assertEquals(Integer.class, stmtCreate.getEventType().getPropertyType("col6"));
        assertEquals(2, stmtCreate.getEventType().getPropertyDescriptors().length);
        stmtSelect = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select * from MyEventType");
        stmtSelect.addListener(listener);
        assertEquals(stmtSelect.getEventType().getUnderlyingType(), eventRepresentationEnum.getOutputClass());

        // send event
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("col5", "abc");
        data.put("col6", 1);
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(data.values().toArray(), "MyEventType");
        }
        else {
            epService.getEPRuntime().sendEvent(data, "MyEventType");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col5,col6".split(","), new Object[]{"abc", 1});
        
        // assert type information
        EventTypeSPI typeSPI = (EventTypeSPI) stmtSelect.getEventType();
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, typeSPI.getMetadata().getTypeClass());
        assertEquals(typeSPI.getName(), typeSPI.getMetadata().getPublicName());
        assertTrue(typeSPI.getMetadata().isApplicationConfigured());
        assertFalse(typeSPI.getMetadata().isApplicationPreConfigured());
        assertFalse(typeSPI.getMetadata().isApplicationPreConfiguredStatic());
        assertEquals(typeSPI.getName(), typeSPI.getMetadata().getPrimaryName());

        // test non-enum create-schema
        String epl = "create" + eventRepresentationEnum.getOutputTypeCreateSchemaName() + " schema MyEventTypeTwo as (col1 string, col2 int, sbean " + SupportBean.class.getName() + ", col3.col4 int)";
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(epl);
        assertTypeColDef(stmtCreateTwo.getEventType());
        assertEquals(eventRepresentationEnum.getOutputClass(), stmtCreateTwo.getEventType().getUnderlyingType());
        stmtCreateTwo.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyEventTypeTwo", true);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(model.toEPL(), epl);
        stmtCreateTwo = epService.getEPAdministrator().create(model);
        assertTypeColDef(stmtCreateTwo.getEventType());
        assertEquals(eventRepresentationEnum.getOutputClass(), stmtCreateTwo.getEventType().getUnderlyingType());

        epService.initialize();
    }

    public void testModelPOJO() throws Exception
    {
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL("create schema SupportBeanOne as " + SupportBean.class.getName());
        assertTypeSupportBean(stmtCreateOne.getEventType());

        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL("create schema SupportBeanTwo as " + SupportBean.class.getName());
        assertTypeSupportBean(stmtCreateTwo.getEventType());

        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL("select * from SupportBeanOne");
        assertTypeSupportBean(stmtSelectOne.getEventType());
        stmtSelectOne.addListener(listener);

        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL("select * from SupportBeanTwo");
        assertTypeSupportBean(stmtSelectTwo.getEventType());
        stmtSelectTwo.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 2}, {"E1", 2}});

        // assert type information
        EventTypeSPI typeSPI = (EventTypeSPI) stmtSelectOne.getEventType();
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, typeSPI.getMetadata().getTypeClass());
        assertEquals(typeSPI.getName(), typeSPI.getMetadata().getPublicName());
        assertTrue(typeSPI.getMetadata().isApplicationConfigured());
        assertFalse(typeSPI.getMetadata().isApplicationPreConfiguredStatic());
        assertFalse(typeSPI.getMetadata().isApplicationPreConfigured());
        assertEquals(typeSPI.getName(), typeSPI.getMetadata().getPrimaryName());

        // test keyword
        tryInvalid("create schema MySchema as com.mycompany.event.ABC",
                   "Error starting statement: Event type or class named 'com.mycompany.event.ABC' was not found [create schema MySchema as com.mycompany.event.ABC]");
        tryInvalid("create schema MySchema as com.mycompany.events.ABC",
                "Error starting statement: Event type or class named 'com.mycompany.events.ABC' was not found [create schema MySchema as com.mycompany.events.ABC]");
    }

    public void testNestableMapArray() throws Exception {
        runAssertionNestableMapArray(EventRepresentationEnum.OBJECTARRAY);
        runAssertionNestableMapArray(EventRepresentationEnum.MAP);
        runAssertionNestableMapArray(EventRepresentationEnum.DEFAULT);
    }

    public void runAssertionNestableMapArray(EventRepresentationEnum eventRepresentationEnum) throws Exception
    {
        EPStatement stmtInner = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyInnerType as (col1 string[], col2 int[])");
        EventType inner = stmtInner.getEventType();
        assertEquals(String[].class, inner.getPropertyType("col1"));
        assertTrue(inner.getPropertyDescriptor("col1").isIndexed());
        assertEquals(Integer[].class, inner.getPropertyType("col2"));
        assertTrue(inner.getPropertyDescriptor("col2").isIndexed());
        assertEquals(eventRepresentationEnum.getOutputClass(), inner.getUnderlyingType());

        EPStatement stmtOuter = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyOuterType as (col1 MyInnerType, col2 MyInnerType[])");
        FragmentEventType type = stmtOuter.getEventType().getFragmentType("col1");
        assertEquals("MyInnerType", type.getFragmentType().getName());
        assertFalse(type.isIndexed());
        assertFalse(type.isNative());
        type = stmtOuter.getEventType().getFragmentType("col2");
        assertEquals("MyInnerType", type.getFragmentType().getName());
        assertTrue(type.isIndexed());
        assertFalse(type.isNative());
        
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select * from MyOuterType");
        stmtSelect.addListener(listener);
        assertEquals(eventRepresentationEnum.getOutputClass(), stmtSelect.getEventType().getUnderlyingType());

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            Object[] innerData = new Object[] {"abc,def".split(","), new int[] {1, 2}};
            Object[] outerData = new Object[] {innerData, new Object[] {innerData, innerData}};
            epService.getEPRuntime().sendEvent(outerData, "MyOuterType");
        }
        else {
            Map<String, Object> innerData = new HashMap<String, Object>();
            innerData.put("col1", "abc,def".split(","));
            innerData.put("col2", new int[] {1, 2});
            Map<String, Object> outerData = new HashMap<String, Object>();
            outerData.put("col1", innerData);
            outerData.put("col2", new Map[]{innerData, innerData});
            epService.getEPRuntime().sendEvent(outerData, "MyOuterType");
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1.col1[1],col2[1].col2[1]".split(","), new Object[]{"def", 2});

        epService.getEPAdministrator().getConfiguration().removeEventType("MyInnerType", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyOuterType", true);
        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testInherit() throws Exception
    {
        epService.getEPAdministrator().createEPL("create schema MyParentType as (col1 int, col2 string)");
        EPStatement stmtChild = epService.getEPAdministrator().createEPL("create schema MyChildTypeOne (col3 int) inherits MyParentType");
        assertEquals(Integer.class, stmtChild.getEventType().getPropertyType("col1"));
        assertEquals(String.class, stmtChild.getEventType().getPropertyType("col2"));
        assertEquals(Integer.class, stmtChild.getEventType().getPropertyType("col3"));

        epService.getEPAdministrator().createEPL("create schema MyChildTypeTwo as (col4 boolean)");
        String createText = "create schema MyChildChildType as (col5 short, col6 long) inherits MyChildTypeOne, MyChildTypeTwo";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(createText);
        assertEquals(createText, model.toEPL());
        EPStatement stmtChildChild = epService.getEPAdministrator().create(model);
        assertEquals(Boolean.class, stmtChildChild.getEventType().getPropertyType("col4"));
        assertEquals(Integer.class, stmtChildChild.getEventType().getPropertyType("col3"));
        assertEquals(Short.class, stmtChildChild.getEventType().getPropertyType("col5"));

        EPStatement stmtChildChildTwo = epService.getEPAdministrator().createEPL("create schema MyChildChildTypeTwo () inherits MyChildTypeOne, MyChildTypeTwo");
        assertEquals(Boolean.class, stmtChildChildTwo.getEventType().getPropertyType("col4"));
        assertEquals(Integer.class, stmtChildChildTwo.getEventType().getPropertyType("col3"));
    }

    public void testVariantType() throws Exception
    {
        epService.getEPAdministrator().createEPL("create schema MyTypeZero as (col1 int, col2 string)");
        epService.getEPAdministrator().createEPL("create schema MyTypeOne as (col1 int, col3 string, col4 int)");
        epService.getEPAdministrator().createEPL("create schema MyTypeTwo as (col1 int, col4 boolean, col5 short)");

        EPStatement stmtChildPredef = epService.getEPAdministrator().createEPL("create variant schema MyVariantPredef as MyTypeZero, MyTypeOne");
        EventType variantTypePredef = stmtChildPredef.getEventType();
        assertEquals(Integer.class, variantTypePredef.getPropertyType("col1"));
        assertEquals(1, variantTypePredef.getPropertyDescriptors().length);

        String createText = "create variant schema MyVariantAnyModel as MyTypeZero, MyTypeOne, *";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(createText);
        assertEquals(createText, model.toEPL());
        EPStatement stmtChildAnyModel = epService.getEPAdministrator().create(model);
        EventType predefAnyType = stmtChildAnyModel.getEventType();
        assertEquals(4, predefAnyType.getPropertyDescriptors().length);
        assertEquals(Object.class, predefAnyType.getPropertyType("col1"));
        assertEquals(Object.class, predefAnyType.getPropertyType("col2"));
        assertEquals(Object.class, predefAnyType.getPropertyType("col3"));
        assertEquals(Object.class, predefAnyType.getPropertyType("col4"));

        EPStatement stmtChildAny = epService.getEPAdministrator().createEPL("create variant schema MyVariantAny as *");
        EventType variantTypeAny = stmtChildAny.getEventType();
        assertEquals(0, variantTypeAny.getPropertyDescriptors().length);

        epService.getEPAdministrator().createEPL("insert into MyVariantAny select * from MyTypeZero");
        epService.getEPAdministrator().createEPL("insert into MyVariantAny select * from MyTypeOne");
        epService.getEPAdministrator().createEPL("insert into MyVariantAny select * from MyTypeTwo");

        epService.getEPAdministrator().createEPL("insert into MyVariantPredef select * from MyTypeZero");
        epService.getEPAdministrator().createEPL("insert into MyVariantPredef select * from MyTypeOne");
        try {
            epService.getEPAdministrator().createEPL("insert into MyVariantPredef select * from MyTypeTwo");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Selected event type is not a valid event type of the variant stream 'MyVariantPredef' [insert into MyVariantPredef select * from MyTypeTwo]", ex.getMessage());
        }
    }

    private void tryInvalid(String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertTrue("Expected:\n" + message + "\nActual:\n" + ex.getMessage(), ex.getMessage().startsWith(message));
        }
    }

    private void assertTypeSupportBean(EventType eventType) {
        assertEquals(SupportBean.class, eventType.getUnderlyingType());
    }

    private void assertTypeColDef(EventType eventType) {
        assertEquals(String.class, eventType.getPropertyType("col1"));
        assertEquals(Integer.class, eventType.getPropertyType("col2"));
        assertEquals(SupportBean.class, eventType.getPropertyType("sbean"));
        assertEquals(Integer.class, eventType.getPropertyType("col3.col4"));
        assertEquals(4, eventType.getPropertyDescriptors().length);
    }

    public static class BeanSourceEvent {
        private SupportBean sb;
        private SupportBean_S0[] s0Arr;

        public BeanSourceEvent(SupportBean sb, SupportBean_S0[] s0Arr) {
            this.sb = sb;
            this.s0Arr = s0Arr;
        }

        public SupportBean getSb() {
            return sb;
        }

        public SupportBean_S0[] getS0Arr() {
            return s0Arr;
        }
    }
}
