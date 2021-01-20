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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBeanParameterizedWFieldSingleIndexed;
import com.espertech.esper.regressionlib.support.bean.SupportBeanParameterizedWFieldSingleMapped;
import com.espertech.esper.regressionlib.support.bean.SupportBeanParameterizedWFieldSinglePlain;

import java.util.*;

import static com.espertech.esper.common.client.type.EPTypeClassParameterized.from;
import static org.junit.Assert.assertEquals;

public class EventBeanSchemaGenericTypeWFields {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventBeanCreateSchemaTypeParamPlain());
        execs.add(new EventBeanCreateSchemaTypeParamMapped());
        execs.add(new EventBeanCreateSchemaTypeParamIndexed());
        return execs;
    }

    private static class EventBeanCreateSchemaTypeParamPlain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('schema') @public @buseventtype create schema MyEvent as " + SupportBeanParameterizedWFieldSinglePlain.class.getName() + "<Integer>;\n" +
                "@name('s0') select simpleProperty as c0, simpleField as c1 from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("schema", statement -> {
                EPTypeClass type = env.statement("schema").getEventType().getUnderlyingEPType();
                assertEquals(EPTypeClassParameterized.from(SupportBeanParameterizedWFieldSinglePlain.class, Integer.class), type);
                SupportEventPropUtil.assertPropsEquals(env.statement("schema").getEventType().getPropertyDescriptors(),
                    new SupportEventPropDesc("simpleProperty", Integer.class),
                    new SupportEventPropDesc("simpleField", Integer.class));

                SupportEventPropUtil.assertPropsEquals(env.statement("s0").getEventType().getPropertyDescriptors(),
                    new SupportEventPropDesc("c0", Integer.class),
                    new SupportEventPropDesc("c1", Integer.class));
            });

            env.sendEventBean(new SupportBeanParameterizedWFieldSinglePlain<>(10), "MyEvent");
            env.assertPropsNew("s0", "c0,c1".split(","), new Object[] {10, 10});

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }

    private static class EventBeanCreateSchemaTypeParamMapped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('schema') @public @buseventtype create schema MyEvent as " + SupportBeanParameterizedWFieldSingleMapped.class.getName() + "<Integer>;\n" +
                "@name('s0') select mapProperty as c0, mapField as c1, mapProperty('key') as c2, mapField('key') as c3, mapKeyed('key') as c4 from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");
            env.assertStatement("schema", statement -> SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("mapProperty", from(Map.class, String.class, Integer.class)).mapped(),
                new SupportEventPropDesc("mapField", from(Map.class, String.class, Integer.class)).mapped(),
                new SupportEventPropDesc("mapKeyed", Integer.class).mapped().mappedRequiresKey()
            ));

            env.assertStatement("s0", statement -> SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("c0", from(Map.class, String.class, Integer.class)).mapped(),
                new SupportEventPropDesc("c1", from(Map.class, String.class, Integer.class)).mapped(),
                new SupportEventPropDesc("c2", Integer.class),
                new SupportEventPropDesc("c3", Integer.class),
                new SupportEventPropDesc("c4", Integer.class)));

            env.sendEventBean(new SupportBeanParameterizedWFieldSingleMapped<>(10), "MyEvent");
            env.assertPropsNew("s0", "c0,c1,c2,c3,c4".split(","), new Object[] {CollectionUtil.buildMap("key", 10), CollectionUtil.buildMap("key", 10), 10, 10, 10});

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }

    private static class EventBeanCreateSchemaTypeParamIndexed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('schema') @public @buseventtype create schema MyEvent as " + SupportBeanParameterizedWFieldSingleIndexed.class.getName() + "<Integer>;\n" +
                    "@name('s0') select indexedArrayProperty as c0, indexedArrayField as c1, indexedArrayProperty[0] as c2, indexedArrayField[0] as c3," +
                    "indexedListProperty as c4, indexedListField as c5, indexedListProperty[0] as c6, indexedListField[0] as c7," +
                    "indexedArrayAtIndex[0] as c8 from MyEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("schema", statement -> {
                SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                    new SupportEventPropDesc("indexedArrayProperty", Integer[].class).indexed(),
                    new SupportEventPropDesc("indexedArrayField", Integer[].class).indexed(),
                    new SupportEventPropDesc("indexedListProperty", EPTypeClassParameterized.from(List.class, Integer.class)).indexed(),
                    new SupportEventPropDesc("indexedListField", EPTypeClassParameterized.from(List.class, Integer.class)).indexed(),
                    new SupportEventPropDesc("indexedArrayAtIndex", Integer.class).indexed().indexedRequiresIndex());
            });

            env.assertStatement("s0", statement -> SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("c0", Integer[].class).indexed(),
                new SupportEventPropDesc("c1", Integer[].class).indexed(),
                new SupportEventPropDesc("c2", Integer.class),
                new SupportEventPropDesc("c3", Integer.class),
                new SupportEventPropDesc("c4", EPTypeClassParameterized.from(List.class, Integer.class)).indexed(),
                new SupportEventPropDesc("c5", EPTypeClassParameterized.from(List.class, Integer.class)).indexed(),
                new SupportEventPropDesc("c6", Integer.class),
                new SupportEventPropDesc("c7", Integer.class),
                new SupportEventPropDesc("c8", Integer.class)));

            env.sendEventBean(new SupportBeanParameterizedWFieldSingleIndexed<>(Integer.class, 10), "MyEvent");
            env.assertPropsNew("s0", "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(","), new Object[] {new Integer[] {10}, new Integer[] {10}, 10, 10,
                Collections.singletonList(10), Collections.singletonList(10), 10, 10, 10});

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }
}
