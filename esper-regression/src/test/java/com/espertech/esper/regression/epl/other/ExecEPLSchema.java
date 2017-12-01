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

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.EventRepresentationChoice;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.*;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.record;
import static org.apache.avro.SchemaBuilder.unionOf;
import static org.junit.Assert.assertEquals;

public class ExecEPLSchema implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSchemaArrayPrimitiveType(epService);
        runAssertionSchemaWithEventType(epService);
        runAssertionSchemaCopyProperties(epService);
        runAssertionConfiguredNotRemoved(epService);
        runAssertionDestroySameType(epService);
        runAssertionAvroSchemaWAnnotation(epService);
        runAssertionColDefPlain(epService);
        runAssertionModelPOJO(epService);
        runAssertionNestableMapArray(epService);
        runAssertionInherit(epService);
        runAssertionVariantType(epService);
        runAssertionCopyFromOrderObjectArray(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionCopyFromOrderObjectArray(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventOne(p0 string, p1 double)");
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventTwo(p2 string) copyfrom MyEventOne");

        EventType type = epService.getEPAdministrator().getConfiguration().getEventType("MyEventTwo");
        EPAssertionUtil.assertEqualsExactOrder("p0,p1,p2".split(","), type.getPropertyNames());

        epService.getEPAdministrator().createEPL("insert into MyEventTwo select 'abc' as p2, s.* from MyEventOne as s");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0, p1, p2 from MyEventTwo");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {"E1", 10d}, "MyEventOne");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,p1,p2".split(","), new Object[] {"E1", 10d, "abc"});

        stmt.destroy();
    }

    private void runAssertionSchemaArrayPrimitiveType(EPServiceProvider epService) {
        tryAssertionSchemaArrayPrimitiveType(epService, true);
        tryAssertionSchemaArrayPrimitiveType(epService, false);

        tryInvalid(epService, "create schema Invalid (x dummy[primitive])",
                "Error starting statement: Type 'dummy' is not a primitive type [create schema Invalid (x dummy[primitive])]");
        tryInvalid(epService, "create schema Invalid (x int[dummy])",
                "Column type keyword 'dummy' not recognized, expecting '[primitive]'");
    }

    private void tryAssertionSchemaArrayPrimitiveType(EPServiceProvider epService, boolean soda) {
        SupportModelHelper.createByCompileOrParse(epService, soda, "create schema MySchema as (c0 int[primitive], c1 int[])");
        Object[][] expectedType = new Object[][]{{"c0", int[].class}, {"c1", Integer[].class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, epService.getEPAdministrator().getConfiguration().getEventType("MySchema"), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
        epService.getEPAdministrator().getConfiguration().removeEventType("MySchema", true);
    }

    private void runAssertionSchemaWithEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BeanSourceEvent", BeanSourceEvent.class);
        BeanSourceEvent theEvent = new BeanSourceEvent(new SupportBean("E1", 1), new SupportBean_S0[]{new SupportBean_S0(2)});

        // test schema
        EPStatement stmtSchema = epService.getEPAdministrator().createEPL("create schema MySchema (bean SupportBean, beanarray SupportBean_S0[])");
        assertEquals(new EventPropertyDescriptor("bean", SupportBean.class, null, false, false, false, false, true), stmtSchema.getEventType().getPropertyDescriptor("bean"));
        assertEquals(new EventPropertyDescriptor("beanarray", SupportBean_S0[].class, SupportBean_S0.class, false, false, true, false, true), stmtSchema.getEventType().getPropertyDescriptor("beanarray"));

        EPStatement stmtSchemaInsert = epService.getEPAdministrator().createEPL("insert into MySchema select sb as bean, s0Arr as beanarray from BeanSourceEvent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSchemaInsert.addListener(listener);
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[]{"E1", 2});
        stmtSchemaInsert.destroy();

        // test named window
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (bean SupportBean, beanarray SupportBean_S0[])");
        stmtWindow.addListener(listener);
        assertEquals(new EventPropertyDescriptor("bean", SupportBean.class, null, false, false, false, false, true), stmtWindow.getEventType().getPropertyDescriptor("bean"));
        assertEquals(new EventPropertyDescriptor("beanarray", SupportBean_S0[].class, SupportBean_S0.class, false, false, true, false, true), stmtWindow.getEventType().getPropertyDescriptor("beanarray"));

        EPStatement stmtWindowInsert = epService.getEPAdministrator().createEPL("insert into MyWindow select sb as bean, s0Arr as beanarray from BeanSourceEvent");
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[]{"E1", 2});
        stmtWindowInsert.destroy();

        // insert pattern to named window
        EPStatement stmtWindowPattern = epService.getEPAdministrator().createEPL("insert into MyWindow select sb as bean, s0Arr as beanarray from pattern [sb=SupportBean -> s0Arr=SupportBean_S0 until SupportBean_S0(id=0)]");
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(20, "S0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "S0_3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id,beanarray[1].id".split(","), new Object[]{"E2", 10, 20});
        stmtWindowPattern.destroy();

        // test configured Map type
        Map<String, Object> configDef = new HashMap<>();
        configDef.put("bean", "SupportBean");
        configDef.put("beanarray", "SupportBean_S0[]");
        epService.getEPAdministrator().getConfiguration().addEventType("MyConfiguredMap", configDef);

        EPStatement stmtMapInsert = epService.getEPAdministrator().createEPL("insert into MyConfiguredMap select sb as bean, s0Arr as beanarray from BeanSourceEvent");
        stmtMapInsert.addListener(listener);
        epService.getEPRuntime().sendEvent(theEvent);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[]{"E1", 2});
        stmtMapInsert.destroy();
    }

    private void runAssertionSchemaCopyProperties(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionSchemaCopyProperties(epService, rep);
        }
    }

    private void tryAssertionSchemaCopyProperties(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema BaseOne (prop1 String, prop2 int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema BaseTwo (prop3 long)");

        // test define and send
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema E1 () copyfrom BaseOne");
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from E1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);
        assertTrue(eventRepresentationEnum.matchesClass(stmtOne.getEventType().getUnderlyingType()));
        assertEquals(String.class, stmtOne.getEventType().getPropertyType("prop1"));
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(stmtOne.getEventType().getPropertyType("prop2")));

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"v1", 2}, "E1");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("prop1", "v1");
            event.put("prop2", 2);
            epService.getEPRuntime().sendEvent(event, "E1");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record event = new GenericData.Record(record("name").fields().requiredString("prop1").requiredInt("prop2").endRecord());
            event.put("prop1", "v1");
            event.put("prop2", 2);
            epService.getEPRuntime().sendEventAvro(event, "E1");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "prop1,prop2".split(","), new Object[]{"v1", 2});

        // test two copy-from types
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema E2 () copyfrom BaseOne, BaseTwo");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from E2");
        assertEquals(String.class, stmtTwo.getEventType().getPropertyType("prop1"));
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(stmtTwo.getEventType().getPropertyType("prop2")));
        assertEquals(Long.class, JavaClassHelper.getBoxedType(stmtTwo.getEventType().getPropertyType("prop3")));

        // test API-defined type
        if (eventRepresentationEnum.isMapEvent() || eventRepresentationEnum.isObjectArrayEvent()) {
            Map<String, Object> def = new HashMap<>();
            def.put("a", "string");
            def.put("b", String.class);
            def.put("c", "BaseOne");
            def.put("d", "BaseTwo[]");
            epService.getEPAdministrator().getConfiguration().addEventType("MyType", def);
        } else {
            epService.getEPAdministrator().createEPL("create avro schema MyType(a string, b string, c BaseOne, d BaseTwo[])");
        }

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema E3(e long, f BaseOne) copyfrom MyType");
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from E3");
        assertEquals(String.class, stmtThree.getEventType().getPropertyType("a"));
        assertEquals(String.class, stmtThree.getEventType().getPropertyType("b"));
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            assertEquals(Object[].class, stmtThree.getEventType().getPropertyType("c"));
            assertEquals(Object[][].class, stmtThree.getEventType().getPropertyType("d"));
            assertEquals(Object[].class, stmtThree.getEventType().getPropertyType("f"));
        } else if (eventRepresentationEnum.isMapEvent()) {
            assertEquals(Map.class, stmtThree.getEventType().getPropertyType("c"));
            assertEquals(Map[].class, stmtThree.getEventType().getPropertyType("d"));
            assertEquals(Map.class, stmtThree.getEventType().getPropertyType("f"));
        } else if (eventRepresentationEnum.isAvroEvent()) {
            assertEquals(GenericData.Record.class, stmtThree.getEventType().getPropertyType("c"));
            assertEquals(Collection.class, stmtThree.getEventType().getPropertyType("d"));
            assertEquals(GenericData.Record.class, stmtThree.getEventType().getPropertyType("f"));
        } else {
            fail();
        }
        assertEquals(Long.class, JavaClassHelper.getBoxedType(stmtThree.getEventType().getPropertyType("e")));

        // invalid tests
        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema E4(a long) copyFrom MyType",
                "Error starting statement: Duplicate column name 'a' [");
        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema E4(c BaseTwo) copyFrom MyType",
                "Error starting statement: Duplicate column name 'c' [");
        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema E4(c BaseTwo) copyFrom XYZ",
                "Error starting statement: Type by name 'XYZ' could not be located [");
        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema E4 as " + SupportBean.class.getName() + " copyFrom XYZ",
                "Error starting statement: Copy-from types are not allowed with class-provided types [");
        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create variant schema E4(c BaseTwo) copyFrom XYZ",
                "Error starting statement: Copy-from types are not allowed with variant types [");

        // test SODA
        String createEPL = eventRepresentationEnum.getAnnotationText() + " create schema EX as () copyFrom BaseOne, BaseTwo";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(createEPL);
        assertEquals(createEPL.trim(), model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(createEPL.trim(), stmt.getText());

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "BaseOne,BaseTwo,E1,E2,E3,MyType".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionConfiguredNotRemoved(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MapType", new HashMap<>());
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        epService.getEPAdministrator().createEPL("create schema ABCType(col1 int, col2 int)");
        assertTypeExists(epService, "ABCType", false);

        String moduleText = "select * from SupportBean;\n" +
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

    private void runAssertionInvalid(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionInvalid(epService, rep);
        }
    }

    private void tryAssertionInvalid(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) {
        String expectedOne = !eventRepresentationEnum.isAvroEvent() ?
                "Error starting statement: Nestable type configuration encountered an unexpected property type name 'xxxx' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [" :
                "Error starting statement: Type definition encountered an unexpected property type name 'xxxx' for property 'col1', expected the name of a previously-declared Avro type";
        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 xxxx)", expectedOne);

        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 int, col1 string)",
                "Error starting statement: Duplicate column name 'col1' [");

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 string)");
        String expectedTwo = !eventRepresentationEnum.isAvroEvent() ?
                "Error starting statement: Event type named 'MyEventType' has already been declared with differing column name or type information: Type by name 'MyEventType' expects 1 properties but receives 2 properties [" :
                "Error starting statement: Event type named 'MyEventType' has already been declared with differing column name or type information: Type by name 'MyEventType' is not a compatible type (target type underlying is '" + JavaClassHelper.APACHE_AVRO_GENERIC_RECORD_CLASSNAME + "')";
        tryInvalid(epService, "create schema MyEventType as (col1 string, col2 string)", expectedTwo);

        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema MyEventTypeT1 as () inherit ABC",
                "Error in expression: Expected 'inherits', 'starttimestamp', 'endtimestamp' or 'copyfrom' keyword after create-schema clause but encountered 'inherit' [");

        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema MyEventTypeT2 as () inherits ABC",
                "Error starting statement: Supertype by name 'ABC' could not be found [");

        tryInvalid(epService, eventRepresentationEnum.getAnnotationText() + " create schema MyEventTypeT3 as () inherits",
                "Incorrect syntax near end-of-input expecting an identifier but found end-of-input at line 1 column ");

        epService.getEPAdministrator().getConfiguration().removeEventType("MyEventType", true);
    }

    private void runAssertionDestroySameType(EPServiceProvider epService) {
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("create schema MyEventTypeDST as (col1 string)");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("create schema MyEventTypeDST as (col1 string)");

        stmtOne.destroy();
        assertEquals(1, epService.getEPAdministrator().getConfiguration().getEventTypeNameUsedBy("MyEventTypeDST").size());
        assertTrue(epService.getEPAdministrator().getConfiguration().isEventTypeExists("MyEventTypeDST"));

        stmtTwo.destroy();
        assertEquals(0, epService.getEPAdministrator().getConfiguration().getEventTypeNameUsedBy("MyEventTypeDST").size());
        assertFalse(epService.getEPAdministrator().getConfiguration().isEventTypeExists("MyEventTypeDST"));
    }

    private void runAssertionAvroSchemaWAnnotation(EPServiceProvider epService) {
        Schema schema = unionOf().intType().and().stringType().endUnion();
        String epl = "@AvroSchemaField(name='carId',schema='" + schema.toString() + "') create avro schema MyEvent(carId object)";
        epService.getEPAdministrator().createEPL(epl);
        System.out.println(schema);
    }

    private void runAssertionColDefPlain(EPServiceProvider epService) throws Exception {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionColDefPlain(epService, rep);
        }

        // test property classname, either simple or fully-qualified.
        epService.getEPAdministrator().getConfiguration().addImport("java.beans.EventHandler");
        epService.getEPAdministrator().getConfiguration().addImport("java.sql.*");
        epService.getEPAdministrator().createEPL("create schema MySchema (f1 Timestamp, f2 java.beans.BeanDescriptor, f3 EventHandler, f4 null)");

        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType("MySchema");
        assertEquals(java.sql.Timestamp.class, eventType.getPropertyType("f1"));
        assertEquals(java.beans.BeanDescriptor.class, eventType.getPropertyType("f2"));
        assertEquals(java.beans.EventHandler.class, eventType.getPropertyType("f3"));
        assertEquals(null, eventType.getPropertyType("f4"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionColDefPlain(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) throws Exception {
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col1 string, col2 int, col3_col4 int)");
        assertTypeColDef(stmtCreate.getEventType());
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select * from MyEventType");
        assertTypeColDef(stmtSelect.getEventType());

        stmtSelect.destroy();
        stmtCreate.destroy();

        // destroy and create differently 
        stmtCreate = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col3 string, col4 int)");
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(stmtCreate.getEventType().getPropertyType("col4")));
        assertEquals(2, stmtCreate.getEventType().getPropertyDescriptors().length);

        stmtCreate.stop();

        // destroy and create differently
        stmtCreate = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEventType as (col5 string, col6 int)");
        assertTrue(eventRepresentationEnum.matchesClass(stmtCreate.getEventType().getUnderlyingType()));
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(stmtCreate.getEventType().getPropertyType("col6")));
        assertEquals(2, stmtCreate.getEventType().getPropertyDescriptors().length);
        stmtSelect = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select * from MyEventType");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);
        assertTrue(eventRepresentationEnum.matchesClass(stmtSelect.getEventType().getUnderlyingType()));

        // send event
        if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("col5", "abc");
            data.put("col6", 1);
            epService.getEPRuntime().sendEvent(data, "MyEventType");
        } else if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{"abc", 1}, "MyEventType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = (Schema) ((AvroSchemaEventType) epService.getEPAdministrator().getConfiguration().getEventType("MyEventType")).getSchema();
            GenericData.Record event = new GenericData.Record(schema);
            event.put("col5", "abc");
            event.put("col6", 1);
            epService.getEPRuntime().sendEventAvro(event, "MyEventType");
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
        String epl = "create" + eventRepresentationEnum.getOutputTypeCreateSchemaName() + " schema MyEventTypeTwo as (col1 string, col2 int, col3_col4 int)";
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(epl);
        assertTypeColDef(stmtCreateTwo.getEventType());
        assertTrue(eventRepresentationEnum.matchesClass(stmtCreateTwo.getEventType().getUnderlyingType()));
        stmtCreateTwo.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyEventTypeTwo", true);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(model.toEPL(), epl);
        stmtCreateTwo = epService.getEPAdministrator().create(model);
        assertTypeColDef(stmtCreateTwo.getEventType());
        assertTrue(eventRepresentationEnum.matchesClass(stmtCreateTwo.getEventType().getUnderlyingType()));

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyEventType,MyEventTypeTwo".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void runAssertionModelPOJO(EPServiceProvider epService) throws Exception {
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL("create schema SupportBeanOne as " + SupportBean_ST0.class.getName());
        assertEquals(SupportBean_ST0.class, stmtCreateOne.getEventType().getUnderlyingType());

        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL("create schema SupportBeanTwo as " + SupportBean_ST0.class.getName());
        assertEquals(SupportBean_ST0.class, stmtCreateTwo.getEventType().getUnderlyingType());

        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL("select * from SupportBeanOne");
        assertEquals(SupportBean_ST0.class, stmtSelectOne.getEventType().getUnderlyingType());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelectOne.addListener(listener);

        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL("select * from SupportBeanTwo");
        assertEquals(SupportBean_ST0.class, stmtSelectTwo.getEventType().getUnderlyingType());
        stmtSelectTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 2));
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), "id,p00".split(","), new Object[][]{{"E1", 2}, {"E1", 2}});

        // assert type information
        EventTypeSPI typeSPI = (EventTypeSPI) stmtSelectOne.getEventType();
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, typeSPI.getMetadata().getTypeClass());
        assertEquals(typeSPI.getName(), typeSPI.getMetadata().getPublicName());
        assertTrue(typeSPI.getMetadata().isApplicationConfigured());
        assertFalse(typeSPI.getMetadata().isApplicationPreConfiguredStatic());
        assertFalse(typeSPI.getMetadata().isApplicationPreConfigured());
        assertEquals(typeSPI.getName(), typeSPI.getMetadata().getPrimaryName());

        // test keyword
        tryInvalid(epService, "create schema MySchemaInvalid as com.mycompany.event.ABC",
                "Error starting statement: Event type or class named 'com.mycompany.event.ABC' was not found");
        tryInvalid(epService, "create schema MySchemaInvalid as com.mycompany.events.ABC",
                "Error starting statement: Event type or class named 'com.mycompany.events.ABC' was not found");
    }

    private void runAssertionNestableMapArray(EPServiceProvider epService) throws Exception {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionNestableMapArray(epService, rep);
        }
    }

    private void tryAssertionNestableMapArray(EPServiceProvider epService, EventRepresentationChoice eventRepresentationEnum) throws Exception {
        EPStatement stmtInner = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyInnerType as (inn1 string[], inn2 int[])");
        EventType inner = stmtInner.getEventType();
        assertEquals(eventRepresentationEnum.isAvroEvent() ? Collection.class : String[].class, inner.getPropertyType("inn1"));
        assertTrue(inner.getPropertyDescriptor("inn1").isIndexed());
        assertEquals(eventRepresentationEnum.isAvroEvent() ? Collection.class : Integer[].class, inner.getPropertyType("inn2"));
        assertTrue(inner.getPropertyDescriptor("inn2").isIndexed());
        assertTrue(eventRepresentationEnum.matchesClass(inner.getUnderlyingType()));

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
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);
        assertTrue(eventRepresentationEnum.matchesClass(stmtSelect.getEventType().getUnderlyingType()));

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            Object[] innerData = new Object[]{"abc,def".split(","), new int[]{1, 2}};
            Object[] outerData = new Object[]{innerData, new Object[]{innerData, innerData}};
            epService.getEPRuntime().sendEvent(outerData, "MyOuterType");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> innerData = new HashMap<>();
            innerData.put("inn1", "abc,def".split(","));
            innerData.put("inn2", new int[]{1, 2});
            Map<String, Object> outerData = new HashMap<>();
            outerData.put("col1", innerData);
            outerData.put("col2", new Map[]{innerData, innerData});
            epService.getEPRuntime().sendEvent(outerData, "MyOuterType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record innerData = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MyInnerType"));
            innerData.put("inn1", Arrays.asList("abc", "def"));
            innerData.put("inn2", Arrays.asList(1, 2));
            GenericData.Record outerData = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "MyOuterType"));
            outerData.put("col1", innerData);
            outerData.put("col2", Arrays.asList(innerData, innerData));
            epService.getEPRuntime().sendEventAvro(outerData, "MyOuterType");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1.inn1[1],col2[1].inn2[1]".split(","), new Object[]{"def", 2});

        epService.getEPAdministrator().getConfiguration().removeEventType("MyInnerType", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyOuterType", true);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInherit(EPServiceProvider epService) throws Exception {
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

    private void runAssertionVariantType(EPServiceProvider epService) throws Exception {
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
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Selected event type is not a valid event type of the variant stream 'MyVariantPredef' [insert into MyVariantPredef select * from MyTypeTwo]", ex.getMessage());
        }
    }

    private void assertTypeColDef(EventType eventType) {
        assertEquals(String.class, eventType.getPropertyType("col1"));
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType("col2")));
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType("col3_col4")));
        assertEquals(3, eventType.getPropertyDescriptors().length);
    }

    public static class BeanSourceEvent {
        private SupportBean sb;
        private SupportBean_S0[] s0Arr;

        BeanSourceEvent(SupportBean sb, SupportBean_S0[] s0Arr) {
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
