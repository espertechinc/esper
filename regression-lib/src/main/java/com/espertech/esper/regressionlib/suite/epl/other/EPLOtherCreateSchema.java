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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanSourceEvent;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.runtime.client.EPStatement;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.junit.Assert;

import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class EPLOtherCreateSchema {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherCreateSchemaPathSimple());
        execs.add(new EPLOtherCreateSchemaPublicSimple());
        execs.add(new EPLOtherCreateSchemaArrayPrimitiveType());
        execs.add(new EPLOtherCreateSchemaCopyProperties());
        execs.add(new EPLOtherCreateSchemaConfiguredNotRemoved());
        execs.add(new EPLOtherCreateSchemaAvroSchemaWAnnotation());
        execs.add(new EPLOtherCreateSchemaColDefPlain());
        execs.add(new EPLOtherCreateSchemaModelPOJO());
        execs.add(new EPLOtherCreateSchemaNestableMapArray());
        execs.add(new EPLOtherCreateSchemaInherit());
        execs.add(new EPLOtherCreateSchemaCopyFromOrderObjectArray());
        execs.add(new EPLOtherCreateSchemaInvalid());
        execs.add(new EPLOtherCreateSchemaWithEventType());
        execs.add(new EPLOtherCreateSchemaVariantType());
        execs.add(new EPLOtherCreateSchemaSameCRC());
        execs.add(new EPLOtherCreateSchemaBeanImport());
        execs.add(new EPLOtherCreateSchemaCopyFromDeepWithValueObject());
        return execs;
    }

    private static class EPLOtherCreateSchemaCopyFromDeepWithValueObject implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema SchemaA (account string, foo " + MyLocalValueObject.class.getName() + ");\n" +
                "create schema SchemaB (symbol string) copyfrom SchemaA;\n" +
                "create schema SchemaC () copyfrom SchemaB;\n" +
                "create schema SchemaD () copyfrom SchemaB;\n" +
                "insert into SchemaD select account, " + EPLOtherCreateSchema.class.getName() + ".getLocalValueObject() as foo, symbol from SchemaC;\n";
            env.compileDeploy(epl).undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaBeanImport implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("create schema MyEvent as Rectangle");

            tryInvalidCompile(env, "create schema MyEvent as XXUnknown", "Could not load class by name 'XXUnknown', please check imports");

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaSameCRC implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            try {
                env.compileDeployWBusPublicType(
                    "create schema b5a7b602ab754d7ab30fb42c4fb28d82();\n" +
                        "create schema d19f2e9e82d14b96be4fa12b8a27ee9f();", new RegressionPath());
            } catch (Throwable t) {
                assertEquals("Test failed due to exception: Event type by name 'd19f2e9e82d14b96be4fa12b8a27ee9f' has a public crc32 id overlap with event type by name 'b5a7b602ab754d7ab30fb42c4fb28d82', please consider renaming either of these types", t.getMessage());
            }
        }
    }

    private static class EPLOtherCreateSchemaPathSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('schema') create schema SimpleSchema(p0 string, p1 int);" +
                "@name('s0') select * from SimpleSchema;\n" +
                "insert into SimpleSchema select theString as p0, intPrimitive as p1 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(StatementType.CREATE_SCHEMA, env.statement("schema").getProperty(StatementProperty.STATEMENTTYPE));
            assertEquals("SimpleSchema", env.statement("schema").getProperty(StatementProperty.CREATEOBJECTNAME));

            env.sendEventBean(new SupportBean("a", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{"a", 20});

            assertNull(env.runtime().getEventTypeService().getBusEventType("SimpleSchema"));

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaPublicSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema MySchema as (p0 string, p1 int);\n" +
                "@name('s0') select p0, p1 from MySchema;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            env.sendEventMap(CollectionUtil.buildMap("p0", "a", "p1", 20), "MySchema");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{"a", 20});

            EventType eventType = env.runtime().getEventTypeService().getBusEventType("MySchema");
            assertEquals("MySchema", eventType.getName());

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaCopyFromOrderObjectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@name('s1') create objectarray schema MyEventOne(p0 string, p1 double);\n " +
                "create objectarray schema MyEventTwo(p2 string) copyfrom MyEventOne;\n";
            env.compileDeployWBusPublicType(epl, path);

            EventType type = env.runtime().getEventTypeService().getEventType(env.deploymentId("s1"), "MyEventTwo");
            EPAssertionUtil.assertEqualsExactOrder("p0,p1,p2".split(","), type.getPropertyNames());

            epl = "insert into MyEventTwo select 'abc' as p2, s.* from MyEventOne as s;\n" +
                "@name('s0') select p0, p1, p2 from MyEventTwo;\n";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventObjectArray(new Object[]{"E1", 10d}, "MyEventOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1,p2".split(","), new Object[]{"E1", 10d, "abc"});

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaArrayPrimitiveType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionSchemaArrayPrimitiveType(env, true);
            tryAssertionSchemaArrayPrimitiveType(env, false);

            tryInvalidCompile(env, "create schema Invalid (x dummy[primitive])",
                "Type 'dummy' is not a primitive type [create schema Invalid (x dummy[primitive])]");
            tryInvalidCompile(env, "create schema Invalid (x int[dummy])",
                "Invalid array keyword 'dummy', expected 'primitive'");
        }

        private static void tryAssertionSchemaArrayPrimitiveType(RegressionEnvironment env, boolean soda) {
            compileDeployWExport("@name('schema') create schema MySchema as (c0 int[primitive], c1 int[])", soda, env);
            Object[][] expectedType = new Object[][]{{"c0", int[].class}, {"c1", Integer[].class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, getTypeStmt(env, "schema"), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaWithEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportBeanSourceEvent theEvent = new SupportBeanSourceEvent(new SupportBean("E1", 1), new SupportBean_S0[]{new SupportBean_S0(2)});

            // test schema
            env.compileDeploy("@name('schema') create schema MySchema (bean SupportBean, beanarray SupportBean_S0[])");
            EventType stmtSchemaType = env.statement("schema").getEventType();
            Assert.assertEquals(new EventPropertyDescriptor("bean", SupportBean.class, null, false, false, false, false, true), stmtSchemaType.getPropertyDescriptor("bean"));
            Assert.assertEquals(new EventPropertyDescriptor("beanarray", SupportBean_S0[].class, SupportBean_S0.class, false, false, true, false, true), stmtSchemaType.getPropertyDescriptor("beanarray"));

            env.compileDeploy("@name('s0') insert into MySchema select sb as bean, s0Arr as beanarray from SupportBeanSourceEvent").addListener("s0");
            env.sendEventBean(theEvent);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[]{"E1", 2});
            env.undeployModuleContaining("s0");

            // test named window
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('window') create window MyWindow#keepall as (bean SupportBean, beanarray SupportBean_S0[])", path).addListener("window");
            EventType stmtWindowType = env.statement("window").getEventType();
            Assert.assertEquals(new EventPropertyDescriptor("bean", SupportBean.class, null, false, false, false, false, true), stmtWindowType.getPropertyDescriptor("bean"));
            Assert.assertEquals(new EventPropertyDescriptor("beanarray", SupportBean_S0[].class, SupportBean_S0.class, false, false, true, false, true), stmtWindowType.getPropertyDescriptor("beanarray"));

            env.compileDeploy("@name('windowInsertOne') insert into MyWindow select sb as bean, s0Arr as beanarray from SupportBeanSourceEvent", path);
            env.sendEventBean(theEvent);
            EPAssertionUtil.assertProps(env.listener("window").assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[]{"E1", 2});
            env.undeployModuleContaining("windowInsertOne");

            // insert pattern to named window
            env.compileDeploy("@name('windowInsertOne') insert into MyWindow select sb as bean, s0Arr as beanarray from pattern [sb=SupportBean -> s0Arr=SupportBean_S0 until SupportBean_S0(id=0)]", path);
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean_S0(10, "S0_1"));
            env.sendEventBean(new SupportBean_S0(20, "S0_2"));
            env.sendEventBean(new SupportBean_S0(0, "S0_3"));
            EPAssertionUtil.assertProps(env.listener("window").assertOneGetNewAndReset(), "bean.theString,beanarray[0].id,beanarray[1].id".split(","), new Object[]{"E2", 10, 20});
            env.undeployModuleContaining("windowInsertOne");

            // test configured Map type
            env.compileDeploy("@name('s0') insert into MyConfiguredMap select sb as bean, s0Arr as beanarray from SupportBeanSourceEvent").addListener("s0");
            env.sendEventBean(theEvent);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "bean.theString,beanarray[0].id".split(","), new Object[]{"E1", 2});

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaCopyProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionSchemaCopyProperties(env, rep);
            }
        }

        private static void tryAssertionSchemaCopyProperties(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
            RegressionPath path = new RegressionPath();
            String epl =
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedBaseOne.class) + " create schema BaseOne (prop1 String, prop2 int);\n" +
                    eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedBaseTwo.class) + " create schema BaseTwo (prop3 long);\n" +
                    eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedE1.class) + " create schema E1 () copyfrom BaseOne;\n";
            env.compileDeployWBusPublicType(epl, path);

            env.compileDeploy("@name('s0') select * from E1", path).addListener("s0");
            TestCase.assertTrue(eventRepresentationEnum.matchesClass(env.statement("s0").getEventType().getUnderlyingType()));
            Assert.assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("prop1"));
            Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(env.statement("s0").getEventType().getPropertyType("prop2")));

            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{"v1", 2}, "E1");
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("prop1", "v1");
                event.put("prop2", 2);
                env.sendEventMap(event, "E1");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record event = new GenericData.Record(SchemaBuilder.record("name").fields().requiredString("prop1").requiredInt("prop2").endRecord());
                event.put("prop1", "v1");
                event.put("prop2", 2);
                env.sendEventAvro(event, "E1");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                JsonObject object = new JsonObject();
                object.add("prop1", "v1");
                object.add("prop2", 2);
                env.sendEventJson(object.toString(), "E1");
            } else {
                fail();
            }
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "prop1,prop2".split(","), new Object[]{"v1", 2});
            env.undeployModuleContaining("s0");

            // test two copy-from types
            env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedE2.class) + " create schema E2 () copyfrom BaseOne, BaseTwo", path);
            env.compileDeploy("@name('s0') select * from E2", path);
            EventType stmtEventType = env.statement("s0").getEventType();
            Assert.assertEquals(String.class, stmtEventType.getPropertyType("prop1"));
            Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(stmtEventType.getPropertyType("prop2")));
            Assert.assertEquals(Long.class, JavaClassHelper.getBoxedType(stmtEventType.getPropertyType("prop3")));
            env.undeployModuleContaining("s0");

            // test API-defined type
            if (!eventRepresentationEnum.isAvroEvent() || eventRepresentationEnum.isObjectArrayEvent() || eventRepresentationEnum.isJsonEvent()) {
                env.compileDeploy("create schema MyType(a string, b string, c BaseOne, d BaseTwo[])", path);
            } else if (eventRepresentationEnum.isJsonProvidedClassEvent()) {
                env.compileDeploy("@JsonSchema(className='" + MyLocalJsonProvidedMyType.class.getName() + "') create json schema MyType(a string, b string, c BaseOne, d BaseTwo[])", path);
            } else {
                env.compileDeploy("create avro schema MyType(a string, b string, c BaseOne, d BaseTwo[])", path);
            }

            env.compileDeploy(eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedE3.class) + " create schema E3(e long, f BaseOne) copyfrom MyType", path);
            env.compileDeploy("@name('s0') select * from E3", path);
            EPStatement stmtThree = env.statement("s0");
            Assert.assertEquals(String.class, stmtThree.getEventType().getPropertyType("a"));
            Assert.assertEquals(String.class, stmtThree.getEventType().getPropertyType("b"));
            if (eventRepresentationEnum.isObjectArrayEvent()) {
                Assert.assertEquals(Object[].class, stmtThree.getEventType().getPropertyType("c"));
                Assert.assertEquals(Object[][].class, stmtThree.getEventType().getPropertyType("d"));
                Assert.assertEquals(Object[].class, stmtThree.getEventType().getPropertyType("f"));
            } else if (eventRepresentationEnum.isMapEvent()) {
                Assert.assertEquals(Map.class, stmtThree.getEventType().getPropertyType("c"));
                Assert.assertEquals(Map[].class, stmtThree.getEventType().getPropertyType("d"));
                Assert.assertEquals(Map.class, stmtThree.getEventType().getPropertyType("f"));
            } else if (eventRepresentationEnum.isAvroEvent()) {
                Assert.assertEquals(GenericData.Record.class, stmtThree.getEventType().getPropertyType("c"));
                Assert.assertEquals(Collection.class, stmtThree.getEventType().getPropertyType("d"));
                Assert.assertEquals(GenericData.Record.class, stmtThree.getEventType().getPropertyType("f"));
            } else if (eventRepresentationEnum.isJsonEvent()) {
                assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(stmtThree.getEventType().getPropertyType("c"), JsonEventObject.class));
                assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(stmtThree.getEventType().getPropertyType("d").getComponentType(), JsonEventObject.class));
                assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(stmtThree.getEventType().getPropertyType("f"), JsonEventObject.class));
            } else if (eventRepresentationEnum.isJsonProvidedClassEvent()) {
                Assert.assertEquals(MyLocalJsonProvidedBaseOne.class, stmtThree.getEventType().getPropertyType("c"));
                Assert.assertEquals(MyLocalJsonProvidedBaseTwo[].class, stmtThree.getEventType().getPropertyType("d"));
                Assert.assertEquals(MyLocalJsonProvidedBaseOne.class, stmtThree.getEventType().getPropertyType("f"));
            } else {
                fail();
            }
            Assert.assertEquals(Long.class, JavaClassHelper.getBoxedType(stmtThree.getEventType().getPropertyType("e")));

            // invalid tests
            String prefix = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedDummy.class);
            tryInvalidCompile(env, path, prefix + " create schema E4(a long) copyFrom MyType",
                "Duplicate column name 'a' [");
            tryInvalidCompile(env, path, prefix + " create schema E4(c BaseTwo) copyFrom MyType",
                "Duplicate column name 'c' [");
            tryInvalidCompile(env, path, prefix + " create schema E4(c BaseTwo) copyFrom XYZ",
                "Type by name 'XYZ' could not be located [");
            tryInvalidCompile(env, path, prefix + " create schema E4 as " + SupportBean.class.getName() + " copyFrom XYZ",
                "Copy-from types are not allowed with class-provided types [");
            tryInvalidCompile(env, path, prefix + " create variant schema E4(c BaseTwo) copyFrom XYZ",
                "Copy-from types are not allowed with variant types [");

            // test SODA
            prefix = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedE2.class);
            String createEPL = prefix + " create schema EX as () copyFrom BaseOne, BaseTwo";
            env.eplToModelCompileDeploy(createEPL, path).undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaConfiguredNotRemoved implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('s1') create schema ABCType(col1 int, col2 int)", path);
            String deploymentIdS1 = env.deploymentId("s1");
            assertNotNull(env.runtime().getEventTypeService().getEventType(deploymentIdS1, "ABCType"));
            env.undeployAll();
            assertNull(env.runtime().getEventTypeService().getEventType(deploymentIdS1, "ABCType"));

            assertTypeExistsPreconfigured(env, "SupportBean");
            assertTypeExistsPreconfigured(env, "MapTypeEmpty");
            assertTypeExistsPreconfigured(env, "TestXMLNoSchemaType");
        }
    }

    private static class EPLOtherCreateSchemaInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionInvalid(env, rep);
            }

            tryInvalidCompile(env, "create objectarray schema A();\n" +
                    "create objectarray schema B();\n" +
                    "create objectarray schema InvalidOA () inherits A, B;\n",
                "Object-array event types only allow a single supertype");
        }

        private static void tryAssertionInvalid(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
            String expectedOne = !eventRepresentationEnum.isAvroEvent() ?
                "Nestable type configuration encountered an unexpected property type name 'xxxx' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared event type [" :
                "Type definition encountered an unexpected property type name 'xxxx' for property 'col1', expected the name of a previously-declared Avro type";
            String prefix = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedDummy.class);
            tryInvalidCompile(env, prefix + " create schema MyEventType as (col1 xxxx)", expectedOne);

            tryInvalidCompile(env, prefix + " create schema MyEventType as (col1 int, col1 string)",
                "Duplicate column name 'col1' [");

            RegressionPath path = new RegressionPath();
            env.compileDeploy(prefix + " create schema MyEventType as (col1 string)", path);
            String expectedTwo = "Event type named 'MyEventType' has already been declared";
            tryInvalidCompile(env, path, "create schema MyEventType as (col1 string, col2 string)", expectedTwo);

            tryInvalidCompile(env, prefix + " create schema MyEventTypeT1 as () inherit ABC",
                "Expected 'inherits', 'starttimestamp', 'endtimestamp' or 'copyfrom' keyword after create-schema clause but encountered 'inherit' [");

            tryInvalidCompile(env, prefix + " create schema MyEventTypeT2 as () inherits ABC",
                "Supertype by name 'ABC' could not be found [");

            tryInvalidCompile(env, prefix + " create schema MyEventTypeT3 as () inherits",
                "Incorrect syntax near end-of-input expecting an identifier but found end-of-input at line 1 column ");

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaAvroSchemaWAnnotation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Schema schema = SchemaBuilder.unionOf().intType().and().stringType().endUnion();
            String epl = "@AvroSchemaField(name='carId',schema='" + schema.toString() + "') create avro schema MyEvent(carId object)";
            env.compileDeploy(epl);
            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaColDefPlain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionColDefPlain(env, rep);
            }

            // test property classname, either simple or fully-qualified.
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create schema MySchema (f1 Timestamp, f2 java.beans.BeanDescriptor, f3 EventHandler, f4 null)", path);

            EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("create"), "MySchema");
            Assert.assertEquals(java.sql.Timestamp.class, eventType.getPropertyType("f1"));
            Assert.assertEquals(java.beans.BeanDescriptor.class, eventType.getPropertyType("f2"));
            Assert.assertEquals(java.beans.EventHandler.class, eventType.getPropertyType("f3"));
            Assert.assertEquals(null, eventType.getPropertyType("f4"));

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaModelPOJO implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String schema = "@name('c1') create schema SupportBeanOne as " + SupportBean_ST0.class.getName() + ";\n" +
                "@name('c2') create schema SupportBeanTwo as " + SupportBean_ST0.class.getName() + ";\n";
            env.compileDeployWBusPublicType(schema, path);

            Assert.assertEquals(SupportBean_ST0.class, env.statement("c1").getEventType().getUnderlyingType());
            Assert.assertEquals(SupportBean_ST0.class, env.statement("c2").getEventType().getUnderlyingType());

            env.compileDeploy("@name('s0') select * from SupportBeanOne", path).addListener("s0");
            Assert.assertEquals(SupportBean_ST0.class, env.statement("s0").getEventType().getUnderlyingType());

            env.compileDeploy("@name('s1') select * from SupportBeanTwo", path).addListener("s1");
            Assert.assertEquals(SupportBean_ST0.class, env.statement("s1").getEventType().getUnderlyingType());

            env.sendEventBean(new SupportBean_ST0("E1", 2), "SupportBeanOne");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "id,p00".split(","), new Object[]{"E1", 2});
            assertFalse(env.listener("s1").isInvoked());

            env.sendEventBean(new SupportBean_ST0("E2", 3), "SupportBeanTwo");
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "id,p00".split(","), new Object[]{"E2", 3});
            assertFalse(env.listener("s0").isInvoked());

            // assert type information
            EventType type = env.statement("s0").getEventType();
            Assert.assertEquals(EventTypeTypeClass.STREAM, type.getMetadata().getTypeClass());

            // test keyword
            tryInvalidCompile(env, "create schema MySchemaInvalid as com.mycompany.event.ABC",
                "Could not load class by name 'com.mycompany.event.ABC', please check imports");
            tryInvalidCompile(env, "create schema MySchemaInvalid as com.mycompany.events.ABC",
                "Could not load class by name 'com.mycompany.events.ABC', please check imports");

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaNestableMapArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionNestableMapArray(env, rep);
            }
        }
    }

    private static class EPLOtherCreateSchemaInherit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema MyParentType as (col1 int, col2 string)", path);
            env.compileDeploy("@name('child') create schema MyChildTypeOne (col3 int) inherits MyParentType", path);

            EventType childType = env.statement("child").getEventType();
            Assert.assertEquals(Integer.class, childType.getPropertyType("col1"));
            Assert.assertEquals(String.class, childType.getPropertyType("col2"));
            Assert.assertEquals(Integer.class, childType.getPropertyType("col3"));

            env.compileDeploy("create schema MyChildTypeTwo as (col4 boolean)", path);
            String createText = "@name('childchild') create schema MyChildChildType as (col5 short, col6 long) inherits MyChildTypeOne, MyChildTypeTwo";
            EPStatementObjectModel model = env.eplToModel(createText);
            Assert.assertEquals(createText, model.toEPL());
            env.compileDeploy(model, path);
            EventType stmtChildChildType = env.statement("childchild").getEventType();
            Assert.assertEquals(Boolean.class, stmtChildChildType.getPropertyType("col4"));
            Assert.assertEquals(Integer.class, stmtChildChildType.getPropertyType("col3"));
            Assert.assertEquals(Short.class, stmtChildChildType.getPropertyType("col5"));

            env.compileDeploy("@name('cc2') create schema MyChildChildTypeTwo () inherits MyChildTypeOne, MyChildTypeTwo", path);
            EventType eventTypeCC2 = env.statement("cc2").getEventType();
            Assert.assertEquals(Boolean.class, eventTypeCC2.getPropertyType("col4"));
            Assert.assertEquals(Integer.class, eventTypeCC2.getPropertyType("col3"));

            env.undeployAll();
        }
    }

    private static class EPLOtherCreateSchemaVariantType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create schema MyTypeZero as (col1 int, col2 string);\n" +
                "create schema MyTypeOne as (col1 int, col3 string, col4 int);\n" +
                "create schema MyTypeTwo as (col1 int, col4 boolean, col5 short)";
            env.compileDeploy(epl, path);

            // try predefined
            env.compileDeploy("@name('predef') create variant schema MyVariantPredef as MyTypeZero, MyTypeOne", path);
            EventType variantTypePredef = env.statement("predef").getEventType();
            Assert.assertEquals(Integer.class, variantTypePredef.getPropertyType("col1"));
            Assert.assertEquals(1, variantTypePredef.getPropertyDescriptors().length);

            env.compileDeploy("insert into MyVariantPredef select * from MyTypeZero", path);
            env.compileDeploy("insert into MyVariantPredef select * from MyTypeOne", path);
            tryInvalidCompile(env, path, "insert into MyVariantPredef select * from MyTypeTwo",
                "Selected event type is not a valid event type of the variant stream 'MyVariantPredef' [insert into MyVariantPredef select * from MyTypeTwo]");

            // try predefined with any
            String createText = "@name('predef_any') create variant schema MyVariantAnyModel as MyTypeZero, MyTypeOne, *";
            EPStatementObjectModel model = env.eplToModel(createText);
            Assert.assertEquals(createText, model.toEPL());
            env.compileDeploy(model, path);
            EventType predefAnyType = env.statement("predef_any").getEventType();
            Assert.assertEquals(4, predefAnyType.getPropertyDescriptors().length);
            Assert.assertEquals(Object.class, predefAnyType.getPropertyType("col1"));
            Assert.assertEquals(Object.class, predefAnyType.getPropertyType("col2"));
            Assert.assertEquals(Object.class, predefAnyType.getPropertyType("col3"));
            Assert.assertEquals(Object.class, predefAnyType.getPropertyType("col4"));

            // try "any"
            env.compileDeploy("@name('any') create variant schema MyVariantAny as *", path);
            EventType variantTypeAny = env.statement("any").getEventType();
            Assert.assertEquals(0, variantTypeAny.getPropertyDescriptors().length);

            env.compileDeploy("insert into MyVariantAny select * from MyTypeZero", path);
            env.compileDeploy("insert into MyVariantAny select * from MyTypeOne", path);
            env.compileDeploy("insert into MyVariantAny select * from MyTypeTwo", path);

            env.undeployAll();
        }
    }

    private static void tryAssertionColDefPlain(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('create') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventTypeCol1To4.class) + " create schema MyEventType as (col1 string, col2 int, col3col4 int)", path);
        assertTypeColDef(env.statement("create").getEventType());
        env.compileDeploy("@name('select') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventTypeCol1To4.class) + " select * from MyEventType", path);
        assertTypeColDef(env.statement("select").getEventType());
        env.undeployAll();

        // destroy and create differently
        env.compileDeploy("@name('create') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventTypCol34.class) + " create schema MyEventType as (col3 string, col4 int)");
        Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(env.statement("create").getEventType().getPropertyType("col4")));
        Assert.assertEquals(2, env.statement("create").getEventType().getPropertyDescriptors().length);
        env.undeployAll();

        // destroy and create differently
        path.clear();
        String schemaEPL = "@name('create') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventTypCol56.class) + " create schema MyEventType as (col5 string, col6 int)";
        env.compileDeployWBusPublicType(schemaEPL, path);

        TestCase.assertTrue(eventRepresentationEnum.matchesClass(env.statement("create").getEventType().getUnderlyingType()));
        Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(env.statement("create").getEventType().getPropertyType("col6")));
        Assert.assertEquals(2, env.statement("create").getEventType().getPropertyDescriptors().length);
        env.compileDeploy("@name('select') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventTypCol56.class) + " select * from MyEventType", path).addListener("select");
        TestCase.assertTrue(eventRepresentationEnum.matchesClass(env.statement("select").getEventType().getUnderlyingType()));

        // send event
        if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("col5", "abc");
            data.put("col6", 1);
            env.sendEventMap(data, "MyEventType");
        } else if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"abc", 1}, "MyEventType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            EventType avroType = env.runtime().getEventTypeService().getEventType(env.deploymentId("create"), "MyEventType");
            Schema schema = AvroSchemaUtil.resolveAvroSchema(avroType);
            GenericData.Record event = new GenericData.Record(schema);
            event.put("col5", "abc");
            event.put("col6", 1);
            env.sendEventAvro(event, "MyEventType");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("col5", "abc");
            object.add("col6", 1);
            env.sendEventJson(object.toString(), "MyEventType");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), "col5,col6".split(","), new Object[]{"abc", 1});

        // assert type information
        EventType type = env.statement("select").getEventType();
        Assert.assertEquals(EventTypeTypeClass.STREAM, type.getMetadata().getTypeClass());
        Assert.assertEquals(type.getName(), type.getMetadata().getName());

        // test non-enum create-schema
        String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEventTypeTwo.class) + " @name('c2') create schema MyEventTypeTwo as (col1 string, col2 int, col3col4 int)";
        env.compileDeploy(epl);
        assertTypeColDef(env.statement("c2").getEventType());
        assertTrue(eventRepresentationEnum.matchesClass(env.statement("c2").getEventType().getUnderlyingType()));
        env.undeployModuleContaining("c2");

        env.eplToModelCompileDeploy(epl);
        assertTypeColDef(env.statement("c2").getEventType());
        assertTrue(eventRepresentationEnum.matchesClass(env.statement("c2").getEventType().getUnderlyingType()));

        env.undeployAll();
    }

    private static void tryAssertionNestableMapArray(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        String schema =
                "@name('innerType') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedNestableArray.class) + " create schema MyInnerType as (inn1 string[], inn2 int[]);\n" +
                "@name('outerType') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedNestableOuter.class) + " create schema MyOuterType as (col1 MyInnerType, col2 MyInnerType[]);\n";
        env.compileDeployWBusPublicType(schema, path);

        EventType innerType = env.statement("innerType").getEventType();
        Assert.assertEquals(eventRepresentationEnum.isAvroEvent() ? Collection.class : String[].class, innerType.getPropertyType("inn1"));
        TestCase.assertTrue(innerType.getPropertyDescriptor("inn1").isIndexed());
        Assert.assertEquals(eventRepresentationEnum.isAvroEvent() ? Collection.class : Integer[].class, innerType.getPropertyType("inn2"));
        TestCase.assertTrue(innerType.getPropertyDescriptor("inn2").isIndexed());
        TestCase.assertTrue(eventRepresentationEnum.matchesClass(innerType.getUnderlyingType()));

        EventType outerType = env.statement("outerType").getEventType();
        FragmentEventType type = outerType.getFragmentType("col1");
        assertFalse(type.isIndexed());
        assertEquals(false, type.isNative());
        type = outerType.getFragmentType("col2");
        assertTrue(type.isIndexed());
        assertEquals(false, type.isNative());

        env.compileDeploy("@name('s0') select * from MyOuterType", path).addListener("s0");
        assertTrue(eventRepresentationEnum.matchesClass(env.statement("s0").getEventType().getUnderlyingType()));

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            Object[] innerData = new Object[]{"abc,def".split(","), new int[]{1, 2}};
            Object[] outerData = new Object[]{innerData, new Object[]{innerData, innerData}};
            env.sendEventObjectArray(outerData, "MyOuterType");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> innerData = new HashMap<>();
            innerData.put("inn1", "abc,def".split(","));
            innerData.put("inn2", new int[]{1, 2});
            Map<String, Object> outerData = new HashMap<>();
            outerData.put("col1", innerData);
            outerData.put("col2", new Map[]{innerData, innerData});
            env.sendEventMap(outerData, "MyOuterType");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record innerData = new GenericData.Record(SupportAvroUtil.getAvroSchema(innerType));
            innerData.put("inn1", Arrays.asList("abc", "def"));
            innerData.put("inn2", Arrays.asList(1, 2));
            GenericData.Record outerData = new GenericData.Record(SupportAvroUtil.getAvroSchema(outerType));
            outerData.put("col1", innerData);
            outerData.put("col2", Arrays.asList(innerData, innerData));
            env.sendEventAvro(outerData, "MyOuterType");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonArray inn1 = new JsonArray().add("abc").add("def");
            JsonArray inn2 = new JsonArray().add(1).add(2);
            JsonObject inn = new JsonObject().add("inn1", inn1).add("inn2", inn2);
            JsonObject outer = new JsonObject().add("col1", inn);
            JsonArray col2 = new JsonArray().add(inn).add(inn);
            outer.add("col2", col2);
            env.sendEventJson(outer.toString(), "MyOuterType");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col1.inn1[1],col2[1].inn2[1]".split(","), new Object[]{"def", 2});

        env.undeployAll();
    }

    private static void compileDeployWExport(String epl, boolean soda, RegressionEnvironment env) {
        EPCompiled compiled;
        try {
            if (!soda) {
                compiled = env.compileWBusPublicType(epl);
            } else {
                EPStatementObjectModel model = env.eplToModel(epl);
                Module module = new Module();
                module.getItems().add(new ModuleItem(model));

                CompilerArguments args = new CompilerArguments();
                args.setConfiguration(env.getConfiguration());
                args.getOptions().setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC).setBusModifierEventType(ctx -> EventTypeBusModifier.BUS);
                compiled = EPCompilerProvider.getCompiler().compile(module, args);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        env.deploy(compiled);
    }

    private static void assertTypeExistsPreconfigured(RegressionEnvironment env, String typeName) {
        assertNotNull(env.runtime().getEventTypeService().getEventTypePreconfigured(typeName));
    }

    private static EventType getTypeStmt(RegressionEnvironment env, String statementName) {
        return env.statement(statementName).getEventType();
    }

    private static void assertTypeColDef(EventType eventType) {
        Assert.assertEquals(String.class, eventType.getPropertyType("col1"));
        Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType("col2")));
        Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType("col3col4")));
        Assert.assertEquals(3, eventType.getPropertyDescriptors().length);
    }

    public static class MyLocalJsonProvidedMyEventTypeCol1To4 implements Serializable {
        public String col1;
        public int col2;
        public int col3col4;
    }

    public static class MyLocalJsonProvidedMyEventTypCol34 implements Serializable {
        public String col3;
        public int col4;
    }

    public static class MyLocalJsonProvidedMyEventTypCol56 implements Serializable {
        public String col5;
        public int col6;
    }

    public static class MyLocalJsonProvidedNestableArray implements Serializable {
        public String[] inn1;
        public Integer[] inn2;
    }

    public static class MyLocalJsonProvidedNestableOuter implements Serializable {
        public MyLocalJsonProvidedNestableArray col1;
        public MyLocalJsonProvidedNestableArray[] col2;
    }

    public static class MyLocalJsonProvidedBaseOne implements Serializable {
        public String prop1;
        public int prop2;
    }

    public static class MyLocalJsonProvidedBaseTwo implements Serializable {
        public long prop3;
    }

    public static class MyLocalJsonProvidedE1 implements Serializable {
        public String prop1;
        public int prop2;
    }

    public static class MyLocalJsonProvidedE2 implements Serializable {
        public String prop1;
        public int prop2;
        public long prop3;
    }

    public static class MyLocalJsonProvidedE3 implements Serializable {
        public String a;
        public String b;
        public MyLocalJsonProvidedBaseOne c;
        public MyLocalJsonProvidedBaseTwo[] d;
        public MyLocalJsonProvidedBaseOne f;
        public long e;
    }

    public static class MyLocalJsonProvidedDummy implements Serializable {
        public String col1;
    }

    public static class MyLocalJsonProvidedMyType implements Serializable {
        public String a;
        public String b;
        public MyLocalJsonProvidedBaseOne c;
        public MyLocalJsonProvidedBaseTwo[] d;
    }

    public static class MyLocalJsonProvidedMyEventTypeTwo implements Serializable {
        public String col1;
        public int col2;
        public int col3col4;
    }

    public static MyLocalValueObject getLocalValueObject() {
        return new MyLocalValueObject();
    }

    public static class MyLocalValueObject {

    }
}
