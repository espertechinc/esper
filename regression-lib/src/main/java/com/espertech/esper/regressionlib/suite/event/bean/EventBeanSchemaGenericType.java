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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBeanParameterizedSingle;
import com.espertech.esper.regressionlib.support.bean.SupportBeanParameterizedTwo;

import java.nio.ByteBuffer;
import java.util.*;

import static com.espertech.esper.common.client.type.EPTypeClassParameterized.from;
import static org.junit.Assert.assertEquals;

public class EventBeanSchemaGenericType {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventBeanSchemaParamsSingleParameter());
        execs.add(new EventBeanSchemaParamsTwoParameter());
        execs.add(new EventBeanSchemaParamsInvalid());
        return execs;
    }

    private static class EventBeanSchemaParamsInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "create schema MyEvent as " + MyLocalUnparameterized.class.getName() + "<Integer>";
            env.tryInvalidCompile(epl,
                "Number of type parameters mismatch, the class '" + MyLocalUnparameterized.class.getName() + "' has 0 type parameters but specified are 1 type parameters");

            epl = "create schema MyEvent as " + MyLocalOneParameter.class.getName() + "<Integer, String>";
            env.tryInvalidCompile(epl,
                "Number of type parameters mismatch, the class '" + MyLocalOneParameter.class.getName() + "' has 1 type parameters but specified are 2 type parameters");

            epl = "create schema MyEvent as " + MyLocalUnparameterized.class.getName() + "[]";
            env.tryInvalidCompile(epl,
                "Array dimensions are not allowed");

            epl = "create schema MyEvent as " + MyLocalOneParameter.class.getName() + "<Dummy>";
            env.tryInvalidCompile(epl,
                "Failed to resolve type parameter 0 of type 'Dummy': Could not load class by name 'Dummy', please check imports");

            epl = "create schema MyEvent as " + MyLocalBoundParameter.class.getName() + "<String>";
            env.tryInvalidCompile(epl,
                "Bound type parameters 0 named 'T' expects 'java.lang.Number' but receives 'java.lang.String'");

            epl = "create schema MyEvent as " + MyLocalBoundParameter.class.getName() + "<int>";
            env.tryInvalidCompile(epl,
                "Failed to resolve type parameter 0 of type 'int': Could not load class by name 'int', please check imports");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    public static class EventBeanSchemaParamsSingleParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String single = SupportBeanParameterizedSingle.class.getName();

            runAssertionSingleParam(env, single + "<Integer>", EPTypeClassParameterized.from(SupportBeanParameterizedSingle.class, Integer.class), new EPTypeClass(Integer.class),
                new SupportBeanParameterizedSingle<>(10), 10);

            runAssertionSingleParam(env, single + "<String>", EPTypeClassParameterized.from(SupportBeanParameterizedSingle.class, String.class), new EPTypeClass(String.class),
                new SupportBeanParameterizedSingle<>("x"), "x");

            String[] data = "a,b".split(",");
            runAssertionSingleParam(env, single + "<String[]>", EPTypeClassParameterized.from(SupportBeanParameterizedSingle.class, String[].class), new EPTypeClass(String[].class),
                new SupportBeanParameterizedSingle<>(data), data);

            runAssertionSingleParam(env, single, new EPTypeClass(SupportBeanParameterizedSingle.class), new EPTypeClass(Object.class),
                new SupportBeanParameterizedSingle<>(100L), 100L);

            EPTypeClass optionalLong = EPTypeClassParameterized.from(Optional.class, Long.class);
            Optional<Long> optionalLongValue = Optional.of(10L);
            runAssertionSingleParam(env, single + "<java.util.Optional<Long>>", from(SupportBeanParameterizedSingle.class, optionalLong), optionalLong,
                new SupportBeanParameterizedSingle<>(optionalLongValue), optionalLongValue);

            runAssertionSingleParam(env, MyLocalBoundParameter.class.getName() + "<Long>", EPTypeClassParameterized.from(MyLocalBoundParameter.class, Long.class), new EPTypeClass(Long.class),
                new MyLocalBoundParameter<>(100L), 100L);
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }

    public static class EventBeanSchemaParamsTwoParameter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String two = SupportBeanParameterizedTwo.class.getName();

            runAssertionTwoParam(env, two + "<Double, String>", EPTypeClassParameterized.from(SupportBeanParameterizedTwo.class, Double.class, String.class),
                new EPTypeClass(Double.class), new EPTypeClass(String.class), new SupportBeanParameterizedTwo<>(10d, "A"), 10d, "A");

            Calendar cal = GregorianCalendar.getInstance();
            ByteBuffer buf = ByteBuffer.wrap(new byte[]{1, 2});
            runAssertionTwoParam(env, two + "<java.nio.ByteBuffer, java.util.Calendar>", EPTypeClassParameterized.from(SupportBeanParameterizedTwo.class, ByteBuffer.class, Calendar.class),
                new EPTypeClass(ByteBuffer.class), new EPTypeClass(Calendar.class), new SupportBeanParameterizedTwo<>(buf, cal), buf, cal);

            runAssertionTwoParam(env, two, new EPTypeClass(SupportBeanParameterizedTwo.class),
                new EPTypeClass(Object.class), new EPTypeClass(Object.class), new SupportBeanParameterizedTwo<>(1, "a"), 1, "a");
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.SERDEREQUIRED);
        }
    }

    private static void runAssertionSingleParam(RegressionEnvironment env, String typeName, EPTypeClass expectedUnderlying, EPTypeClass expectedProperty, Object event, Object expected) {
        String epl =
            "@name('schema') @public @buseventtype create schema MyEvent as " + typeName + ";\n" +
                "@name('s0') select simpleProperty as c0 from MyEvent;\n";
        env.compileDeploy(epl).addListener("s0");

        env.assertStatement("schema", statement -> {
            EventType schemaType = statement.getEventType();
            assertEquals(expectedUnderlying, schemaType.getUnderlyingEPType());
            EventPropertyDescriptor[] received = schemaType.getPropertyDescriptors();
            boolean fragment = received[0].isFragment(); // ignore fragment, mapped, indexed flags
            boolean indexed = received[0].isIndexed(); // ignore fragment, mapped, indexed flags
            boolean mapped = received[0].isMapped(); // ignore fragment, mapped, indexed flags
            SupportEventPropUtil.assertPropsEquals(received, new SupportEventPropDesc("simpleProperty", expectedProperty).fragment(fragment).indexed(indexed).mapped(mapped));

            SupportEventPropUtil.assertPropsEquals(env.statement("s0").getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("c0", expectedProperty).fragment(fragment).indexed(indexed).mapped(mapped));
        });

        env.sendEventBean(event, "MyEvent");
        env.assertEqualsNew("s0", "c0", expected);

        env.undeployAll();
    }

    private static void runAssertionTwoParam(RegressionEnvironment env, String typeName, EPTypeClass expectedUnderlying, EPTypeClass expectedOne, EPTypeClass expectedTwo, Object event, Object valueOne, Object valueTwo) {
        String epl =
            "@name('schema') @public @buseventtype create schema MyEvent as " + typeName + ";\n" +
                "@name('s0') select one as c0, two as c1 from MyEvent;\n";
        env.compileDeploy(epl).addListener("s0");

        env.assertStatement("s0", statement -> {
            EventType schemaType = env.statement("schema").getEventType();
            assertEquals(expectedUnderlying, schemaType.getUnderlyingEPType());
            assertEquals(expectedOne, schemaType.getPropertyEPType("one"));
            assertEquals(expectedTwo, schemaType.getPropertyEPType("two"));

            EventType s0Type = statement.getEventType();
            assertEquals(expectedOne, s0Type.getPropertyEPType("c0"));
            assertEquals(expectedTwo, s0Type.getPropertyEPType("c1"));
        });

        env.sendEventBean(event, "MyEvent");
        env.assertPropsNew("s0", "c0,c1".split(","), new Object[]{valueOne, valueTwo});

        env.undeployAll();
    }

    public static class MyLocalUnparameterized {
    }

    public static class MyLocalOneParameter<T> {
    }

    public static class MyLocalBoundParameter<T extends Number> {
        private T simpleProperty;

        public MyLocalBoundParameter(T simpleProperty) {
            this.simpleProperty = simpleProperty;
        }

        public T getSimpleProperty() {
            return simpleProperty;
        }
    }
}
