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
package com.espertech.esper.regressionlib.suite.event.objectarray;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventObjectArrayCore {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventObjectArrayMetadata());
        execs.add(new EventObjectArrayNestedObjects());
        execs.add(new EventObjectArrayQueryFields());
        execs.add(new EventObjectArrayNestedEventBeanArray());
        execs.add(new EventObjectArrayInvalid());
        return execs;
    }

    private static class EventObjectArrayNestedEventBeanArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String schemas = "@buseventtype @public create objectarray schema NBAL_1(val string);\n" +
                "@buseventtype @public create objectarray schema NBAL_2 (lvl1s NBAL_1[]);\n";
            env.compileDeploy(schemas, path);
            env.compileDeploy("@name('s0') select * from NBAL_1", path).addListener("s0");

            Object[] oa = new Object[]{"somevalue"};
            env.sendEventObjectArray(oa, "NBAL_1");
            env.assertEventNew("s0", event -> {
            });
            env.undeployModuleContaining("s0");

            // add containing-type
            env.compileDeploy("@name('s0') select lvl1s[0] as c0 from NBAL_2", path).addListener("s0");

            env.sendEventObjectArray(new Object[]{new Object[]{oa}}, "NBAL_2");
            env.assertEventNew("s0", event -> assertEquals("somevalue", ((Object[]) event.get("c0"))[0]));

            env.undeployAll();
        }
    }

    private static class EventObjectArrayMetadata implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.assertThat(() -> {
                EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured("MyObjectArrayEvent");
                assertEquals(EventTypeApplicationType.OBJECTARR, type.getMetadata().getApplicationType());
                assertEquals("MyObjectArrayEvent", type.getMetadata().getName());

                SupportEventPropUtil.assertPropsEquals(type.getPropertyDescriptors(),
                    new SupportEventPropDesc("myInt", Integer.class),
                    new SupportEventPropDesc("myString", String.class),
                    new SupportEventPropDesc("beanA", SupportBeanComplexProps.class).fragment());
            });
        }
    }

    private static class EventObjectArrayNestedObjects implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select beanA.simpleProperty as simple," +
                "beanA.nested.nestedValue as nested," +
                "beanA.indexed[1] as indexed," +
                "beanA.nested.nestedNested.nestedNestedValue as nestednested " +
                "from MyObjectArrayEvent#length(5)";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventObjectArray(new Object[]{3, "some string", SupportBeanComplexProps.makeDefaultBean()}, "MyObjectArrayEvent");
            env.assertEventNew("s0", event -> {
                assertEquals("nestedValue", event.get("nested"));
                assertEquals(2, event.get("indexed"));
                assertEquals("nestedNestedValue", event.get("nestednested"));
            });

            env.undeployAll();
        }
    }

    private static class EventObjectArrayQueryFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select myInt + 2 as intVal, 'x' || myString || 'x' as stringVal from MyObjectArrayEvent#length(5)";
            env.compileDeploy(statementText).addListener("s0");

            // send Map<String, Object> event
            env.sendEventObjectArray(new Object[]{3, "some string", SupportBeanComplexProps.makeDefaultBean()}, "MyObjectArrayEvent");
            env.assertEventNew("s0", event -> {
                assertEquals(5, event.get("intVal"));
                assertEquals("xsome stringx", event.get("stringVal"));
            });

            // send Map base event
            env.sendEventObjectArray(new Object[]{4, "string2", null}, "MyObjectArrayEvent");
            env.assertEventNew("s0", event -> {
                assertEquals(6, event.get("intVal"));
                assertEquals("xstring2x", event.get("stringVal"));
            });

            env.undeployAll();
        }
    }

    private static class EventObjectArrayInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select XXX from MyObjectArrayEvent#length(5)", "skip");
            env.tryInvalidCompile("select myString * 2 from MyObjectArrayEvent#length(5)", "skip");
            env.tryInvalidCompile("select String.trim(myInt) from MyObjectArrayEvent#length(5)", "skip");
        }
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
}
