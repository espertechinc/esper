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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.event.core.MappedEventBean;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowTypes {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraMapTranspose());
        execs.add(new InfraNoWildcardWithAs());
        execs.add(new InfraNoWildcardNoAs());
        execs.add(new InfraConstantsAs());
        execs.add(new InfraCreateTableSyntax());
        execs.add(new InfraWildcardNoFieldsNoAs());
        execs.add(new InfraModelAfterMap());
        execs.add(new InfraWildcardInheritance());
        execs.add(new InfraNoSpecificationBean());
        execs.add(new InfraWildcardWithFields());
        execs.add(new InfraCreateTableArray());
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            execs.add(new InfraEventTypeColumnDef(rep));
        }
        execs.add(new InfraCreateSchemaModelAfter());
        return execs;
    }

    private static class InfraEventTypeColumnDef implements RegressionExecution {
        private final EventRepresentationChoice eventRepresentationEnum;

        public InfraEventTypeColumnDef(EventRepresentationChoice eventRepresentationEnum) {
            this.eventRepresentationEnum = eventRepresentationEnum;
        }

        public void run(RegressionEnvironment env) {
            String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSchemaOne.class) + " @name('schema') create schema SchemaOne(col1 int, col2 int);\n";
            epl += eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSchemaWindow.class) + " @name('create') create window SchemaWindow#lastevent as (s1 SchemaOne);\n";
            epl += "insert into SchemaWindow (s1) select sone from SchemaOne as sone;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("create");

            assertTrue(eventRepresentationEnum.matchesClass(env.statement("schema").getEventType().getUnderlyingType()));
            assertTrue(eventRepresentationEnum.matchesClass(env.statement("create").getEventType().getUnderlyingType()));

            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{10, 11}, "SchemaOne");
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map<String, Object> theEvent = new LinkedHashMap<>();
                theEvent.put("col1", 10);
                theEvent.put("col2", 11);
                env.sendEventMap(theEvent, "SchemaOne");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("SchemaOne")));
                theEvent.put("col1", 10);
                theEvent.put("col2", 11);
                env.eventService().sendEventAvro(theEvent, "SchemaOne");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                env.eventService().sendEventJson("{\"col1\": 10, \"col2\": 11}", "SchemaOne");
            } else {
                fail();
            }
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), "s1.col1,s1.col2".split(","), new Object[]{10, 11});

            env.undeployAll();
        }
    }

    private static class InfraMapTranspose implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionMapTranspose(env, EventRepresentationChoice.OBJECTARRAY);
            tryAssertionMapTranspose(env, EventRepresentationChoice.MAP);
            tryAssertionMapTranspose(env, EventRepresentationChoice.DEFAULT);
        }

        private void tryAssertionMapTranspose(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

            // create window
            String epl = eventRepresentationEnum.getAnnotationText() + " @name('create') create window MyWindowMT#keepall as select one, two from OuterType;\n" +
                "insert into MyWindowMT select one, two from OuterType;\n";
            env.compileDeploy(epl).addListener("create");

            EventType eventType = env.statement("create").getEventType();
            assertTrue(eventRepresentationEnum.matchesClass(eventType.getUnderlyingType()));
            EPAssertionUtil.assertEqualsAnyOrder(eventType.getPropertyNames(), new String[]{"one", "two"});
            assertEquals("T1", eventType.getFragmentType("one").getFragmentType().getName());
            assertEquals("T2", eventType.getFragmentType("two").getFragmentType().getName());

            Map<String, Object> innerDataOne = new HashMap<>();
            innerDataOne.put("i1", 1);
            Map<String, Object> innerDataTwo = new HashMap<>();
            innerDataTwo.put("i2", 2);
            Map<String, Object> outerData = new HashMap<>();
            outerData.put("one", innerDataOne);
            outerData.put("two", innerDataTwo);

            env.sendEventMap(outerData, "OuterType");
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), "one.i1,two.i2".split(","), new Object[]{1, 2});

            env.undeployAll();
        }
    }

    private static class InfraNoWildcardWithAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowNW#keepall as select theString as a, longPrimitive as b, longBoxed as c from SupportBean;\n" +
                "insert into MyWindowNW select theString as a, longPrimitive as b, longBoxed as c from SupportBean;\n" +
                "insert into MyWindowNW select symbol as a, volume as b, volume as c from SupportMarketDataBean;\n" +
                "insert into MyWindowNW select key as a, boxed as b, primitive as c from MyMapWithKeyPrimitiveBoxed;\n" +
                "@name('s1') select a, b, c from MyWindowNW;\n" +
                "@name('delete') on SupportMarketDataBean as s0 delete from MyWindowNW as s1 where s0.symbol = s1.a;\n";
            env.compileDeploy(epl).addListener("create").addListener("s1").addListener("delete");

            EventType eventType = env.statement("create").getEventType();
            EPAssertionUtil.assertEqualsAnyOrder(eventType.getPropertyNames(), new String[]{"a", "b", "c"});
            assertEquals(String.class, eventType.getPropertyType("a"));
            assertEquals(Long.class, eventType.getPropertyType("b"));
            assertEquals(Long.class, eventType.getPropertyType("c"));

            // assert type metadata
            EventType type = env.deployment().getStatement(env.deploymentId("create"), "create").getEventType();
            assertEquals(EventTypeTypeClass.NAMED_WINDOW, type.getMetadata().getTypeClass());
            assertEquals("MyWindowNW", type.getMetadata().getName());
            assertEquals(EventTypeApplicationType.MAP, type.getMetadata().getApplicationType());

            eventType = env.statement("s1").getEventType();
            EPAssertionUtil.assertEqualsAnyOrder(eventType.getPropertyNames(), new String[]{"a", "b", "c"});
            assertEquals(String.class, eventType.getPropertyType("a"));
            assertEquals(Long.class, eventType.getPropertyType("b"));
            assertEquals(Long.class, eventType.getPropertyType("c"));

            sendSupportBean(env, "E1", 1L, 10L);
            String[] fields = new String[]{"a", "b", "c"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

            sendMarketBean(env, "S1", 99L);
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

            sendMap(env, "M1", 100L, 101L);
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});

            env.undeployAll();
        }
    }

    private static class InfraNoWildcardNoAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowNWNA#keepall as select theString, longPrimitive, longBoxed from SupportBean;\n" +
                "insert into MyWindowNWNA select theString, longPrimitive, longBoxed from SupportBean;\n" +
                "insert into MyWindowNWNA select symbol as theString, volume as longPrimitive, volume as longBoxed from SupportMarketDataBean;\n" +
                "insert into MyWindowNWNA select key as theString, boxed as longPrimitive, primitive as longBoxed from MyMapWithKeyPrimitiveBoxed;\n" +
                "@name('select') select theString, longPrimitive, longBoxed from MyWindowNWNA;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            sendSupportBean(env, "E1", 1L, 10L);
            String[] fields = new String[]{"theString", "longPrimitive", "longBoxed"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

            sendMarketBean(env, "S1", 99L);
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

            sendMap(env, "M1", 100L, 101L);
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"M1", 101L, 100L});

            env.undeployAll();
        }
    }

    private static class InfraConstantsAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowCA#keepall as select '' as theString, 0L as longPrimitive, 0L as longBoxed from MyMapWithKeyPrimitiveBoxed;\n" +
                "insert into MyWindowCA select theString, longPrimitive, longBoxed from SupportBean;\n" +
                "insert into MyWindowCA select symbol as theString, volume as longPrimitive, volume as longBoxed from SupportMarketDataBean;\n" +
                "@name('select') select theString, longPrimitive, longBoxed from MyWindowCA;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            sendSupportBean(env, "E1", 1L, 10L);
            String[] fields = new String[]{"theString", "longPrimitive", "longBoxed"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, 10L});

            sendMarketBean(env, "S1", 99L);
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S1", 99L, 99L});

            env.undeployAll();
        }
    }

    private static class InfraCreateSchemaModelAfter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionCreateSchemaModelAfter(env, rep);
            }

            // test model-after for POJO with inheritance
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window ParentWindow#keepall as select * from SupportBeanAtoFBase", path);
            env.compileDeploy("insert into ParentWindow select * from SupportBeanAtoFBase", path);
            env.compileDeploy("create window ChildWindow#keepall as select * from SupportBean_A", path);
            env.compileDeploy("insert into ChildWindow select * from SupportBean_A", path);

            String parentQuery = "@Name('s0') select parent from ParentWindow as parent";
            env.compileDeploy(parentQuery, path).addListener("s0");

            env.sendEventBean(new SupportBean_A("E1"));
            assertEquals(1, env.listener("s0").getNewDataListFlattened().length);

            env.undeployAll();
        }

        private void tryAssertionCreateSchemaModelAfter(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
            String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedEventTypeOne.class) + " create schema EventTypeOne (hsi int);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedEventTypeTwo.class) + " create schema EventTypeTwo (event EventTypeOne);\n" +
                "@name('create') create window NamedWindow#unique(event.hsi) as EventTypeTwo;\n" +
                "on EventTypeOne as ev insert into NamedWindow select ev as event;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath());

            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{10}, "EventTypeOne");
            } else if (eventRepresentationEnum.isMapEvent()) {
                env.sendEventMap(Collections.singletonMap("hsi", 10), "EventTypeOne");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("EventTypeOne")));
                theEvent.put("hsi", 10);
                env.eventService().sendEventAvro(theEvent, "EventTypeOne");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                env.eventService().sendEventJson("{\"hsi\": 10}", "EventTypeOne");
            } else {
                fail();
            }
            EventBean result = env.statement("create").iterator().next();
            EventPropertyGetter getter = result.getEventType().getGetter("event.hsi");
            assertEquals(10, getter.get(result));

            env.undeployAll();
        }
    }

    public static class InfraCreateTableArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema SecurityData (name String, roles String[]);\n" +
                "create window SecurityEvent#time(30 sec) (ipAddress string, userId String, secData SecurityData, historySecData SecurityData[]);\n" +
                "@name('create') create window MyWindowCTA#keepall (myvalue string[]);\n" +
                "insert into MyWindowCTA select {'a','b'} as myvalue from SupportBean;\n";
            env.compileDeploy(epl).addListener("create");

            sendSupportBean(env, "E1", 1L, 10L);
            String[] values = (String[]) env.listener("create").assertOneGetNewAndReset().get("myvalue");
            EPAssertionUtil.assertEqualsExactOrder(values, new String[]{"a", "b"});

            env.undeployAll();
        }
    }

    private static class InfraCreateTableSyntax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowCTS#keepall (stringValOne varchar, stringValTwo string, intVal int, longVal long);\n" +
                "insert into MyWindowCTS select theString as stringValOne, theString as stringValTwo, cast(longPrimitive, int) as intVal, longBoxed as longVal from SupportBean;\n" +
                "@name('select') select stringValOne, stringValTwo, intVal, longVal from MyWindowCTS;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            sendSupportBean(env, "E1", 1L, 10L);
            String[] fields = "stringValOne,stringValTwo,intVal,longVal".split(",");
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1, 10L});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 1, 10L});

            env.undeployAll();

            // create window with two views
            epl = "create window MyWindowCTSTwo#unique(stringValOne)#keepall (stringValOne varchar, stringValTwo string, intVal int, longVal long)";
            env.compileDeploy(epl).undeployAll();

            //create window with statement object model
            String text = "@name('create') create window MyWindowCTSThree#keepall as (a string, b integer, c integer)";
            env.eplToModelCompileDeploy(text);
            assertEquals(String.class, env.statement("create").getEventType().getPropertyType("a"));
            assertEquals(Integer.class, env.statement("create").getEventType().getPropertyType("b"));
            assertEquals(Integer.class, env.statement("create").getEventType().getPropertyType("c"));
            env.undeployAll();

            text = "create window MyWindowCTSFour#unique(a)#unique(b) retain-union as (a string, b integer, c integer)";
            env.eplToModelCompileDeploy(text);

            env.undeployAll();
        }
    }

    private static class InfraWildcardNoFieldsNoAs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowWNF#keepall select * from SupportBean_A;\n" +
                "insert into MyWindowWNF select * from SupportBean_A;" +
                "@name('select') select id from MyWindowWNF;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            env.sendEventBean(new SupportBean_A("E1"));
            String[] fields = new String[]{"id"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class InfraModelAfterMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowMAM#keepall select * from MyMapWithKeyPrimitiveBoxed;\n" +
                "@name('insert') insert into MyWindowMAM select * from MyMapWithKeyPrimitiveBoxed;\n";
            env.compileDeploy(epl).addListener("create");
            assertTrue(env.statement("create").getEventType() instanceof MapEventType);

            sendMap(env, "k1", 100L, 200L);
            EventBean theEvent = env.listener("create").assertOneGetNewAndReset();
            assertTrue(theEvent instanceof MappedEventBean);
            EPAssertionUtil.assertProps(theEvent, "key,primitive".split(","), new Object[]{"k1", 100L});

            env.undeployAll();
        }
    }

    private static class InfraWildcardInheritance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowWI#keepall as select * from SupportBeanAtoFBase;\n" +
                "insert into MyWindowWI select * from SupportBean_A;\n" +
                "insert into MyWindowWI select * from SupportBean_B;\n" +
                "@name('select') select id from MyWindowWI;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            env.sendEventBean(new SupportBean_A("E1"));
            String[] fields = new String[]{"id"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.sendEventBean(new SupportBean_B("E2"));
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E2"});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E2"});

            env.undeployAll();
        }
    }

    private static class InfraNoSpecificationBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowNSB#keepall as SupportBean_A;\n" +
                "insert into MyWindowNSB select * from SupportBean_A;\n" +
                "@name('select') select id from MyWindowNSB;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            env.sendEventBean(new SupportBean_A("E1"));
            String[] fields = new String[]{"id"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1"});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1"});

            env.undeployAll();
        }
    }

    private static class InfraWildcardWithFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindowWWF#keepall as select *, id as myid from SupportBean_A;\n" +
                "insert into MyWindowWWF select *, id || 'A' as myid from SupportBean_A;\n" +
                "@name('select') select id, myid from MyWindowWWF;\n";
            env.compileDeploy(epl).addListener("select").addListener("create");

            env.sendEventBean(new SupportBean_A("E1"));
            String[] fields = new String[]{"id", "myid"};
            EPAssertionUtil.assertProps(env.listener("create").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1A"});
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1A"});

            env.undeployAll();
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, long longPrimitive, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(bean);
    }

    private static void sendMap(RegressionEnvironment env, String key, long primitive, Long boxed) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", key);
        map.put("primitive", primitive);
        map.put("boxed", boxed);
        env.sendEventMap(map, "MyMapWithKeyPrimitiveBoxed");
    }

    public static class NWTypesParentClass {
    }

    public static class NWTypesChildClass extends NWTypesParentClass {
    }

    public static class MyLocalJsonProvidedSchemaOne implements Serializable {
        public int col1;
        public int col2;
    }

    public static class MyLocalJsonProvidedSchemaWindow implements Serializable {
        public MyLocalJsonProvidedSchemaOne s1;
    }

    public static class MyLocalJsonProvidedEventTypeOne implements Serializable {
        public int hsi;
    }

    public static class MyLocalJsonProvidedEventTypeTwo implements Serializable {
        public MyLocalJsonProvidedEventTypeOne event;
    }
}
