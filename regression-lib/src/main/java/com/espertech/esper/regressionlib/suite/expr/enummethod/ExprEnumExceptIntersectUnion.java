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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.*;

import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertST0Id;
import static com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil.assertValuesArrayScalar;
import static org.junit.Assert.assertEquals;

public class ExprEnumExceptIntersectUnion {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumStringArrayIntersection());
        execs.add(new ExprEnumSetLogicWithContained());
        execs.add(new ExprEnumSetLogicWithScalar());
        execs.add(new ExprEnumInheritance());
        execs.add(new ExprEnumInvalid());
        execs.add(new ExprEnumSetLogicWithEvents());
        execs.add(new ExprEnumUnionWhere());
        return execs;
    }

    private static class ExprEnumStringArrayIntersection implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create objectarray schema Event(meta1 string[], meta2 string[]);\n" +
                "@Name('s0') select * from Event(meta1.intersect(meta2).countOf() > 0);\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            sendAndAssert(env, "a,b", "a,b", true);
            sendAndAssert(env, "c,d", "a,b", false);
            sendAndAssert(env, "c,d", "a,d", true);
            sendAndAssert(env, "a,d,a,a", "b,c", false);
            sendAndAssert(env, "a,d,a,a", "b,d", true);

            env.undeployAll();
        }
    }

    private static class ExprEnumSetLogicWithContained implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_ST0_Container");
            builder.expression(fields[0], "contained.except(containedTwo)");
            builder.expression(fields[1], "contained.intersect(containedTwo)");
            builder.expression(fields[2], "contained.union(containedTwo)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)));

            List<SupportBean_ST0> first = SupportBean_ST0_Container.make2ValueList("E1,1", "E2,10", "E3,1", "E4,10", "E5,11");
            List<SupportBean_ST0> second = SupportBean_ST0_Container.make2ValueList("E1,1", "E3,1", "E4,10");
            builder.assertion(new SupportBean_ST0_Container(first, second))
                .verify("c0", val -> assertST0Id(val, "E2,E5"))
                .verify("c1", val -> assertST0Id(val, "E1,E3,E4"))
                .verify("c2", val -> assertST0Id(val, "E1,E2,E3,E4,E5,E1,E3,E4"));

            builder.run(env);
        }
    }

    private static class ExprEnumSetLogicWithEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl =
                "@name('s0') expression last10A {" +
                    " (select * from SupportBean_ST0(key0 like 'A%')#length(2)) " +
                    "}" +
                    "expression last10NonZero {" +
                    " (select * from SupportBean_ST0(p00 > 0)#length(2)) " +
                    "}" +
                    "select " +
                    "last10A().except(last10NonZero()) as val0," +
                    "last10A().intersect(last10NonZero()) as val1, " +
                    "last10A().union(last10NonZero()) as val2 " +
                    "from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            SupportEventPropUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new EPTypeClass[]{EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)});

            env.sendEventBean(new SupportBean_ST0("E1", "A1", 10));    // in both
            env.sendEventBean(new SupportBean());
            assertST0Id(env.listener("s0"), "val0", "");
            assertST0Id(env.listener("s0"), "val1", "E1");
            assertST0Id(env.listener("s0"), "val2", "E1,E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E2", "A1", 0));
            env.sendEventBean(new SupportBean());
            assertST0Id(env.listener("s0"), "val0", "E2");
            assertST0Id(env.listener("s0"), "val1", "E1");
            assertST0Id(env.listener("s0"), "val2", "E1,E2,E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E3", "B1", 0));
            env.sendEventBean(new SupportBean());
            assertST0Id(env.listener("s0"), "val0", "E2");
            assertST0Id(env.listener("s0"), "val1", "E1");
            assertST0Id(env.listener("s0"), "val2", "E1,E2,E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E4", "A2", -1));
            env.sendEventBean(new SupportBean());
            assertST0Id(env.listener("s0"), "val0", "E2,E4");
            assertST0Id(env.listener("s0"), "val1", "");
            assertST0Id(env.listener("s0"), "val2", "E2,E4,E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E5", "A3", -2));
            env.sendEventBean(new SupportBean());
            assertST0Id(env.listener("s0"), "val0", "E4,E5");
            assertST0Id(env.listener("s0"), "val1", "");
            assertST0Id(env.listener("s0"), "val2", "E4,E5,E1");
            env.listener("s0").reset();

            env.sendEventBean(new SupportBean_ST0("E6", "A6", 11));    // in both
            env.sendEventBean(new SupportBean_ST0("E7", "A7", 12));    // in both
            env.sendEventBean(new SupportBean());
            assertST0Id(env.listener("s0"), "val0", "");
            assertST0Id(env.listener("s0"), "val1", "E6,E7");
            assertST0Id(env.listener("s0"), "val2", "E6,E7,E6,E7");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumSetLogicWithScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.except(strvalstwo)");
            builder.expression(fields[1], "strvals.intersect(strvalstwo)");
            builder.expression(fields[2], "strvals.union(strvalstwo)");

            builder.statementConsumer(stmt -> SupportEventPropUtil.assertTypesAllSame(stmt.getEventType(), fields, EPTypeClassParameterized.from(Collection.class, String.class)));

            builder.assertion(SupportCollection.makeString("E1,E2", "E3,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E2"))
                .verify("c1", val -> assertValuesArrayScalar(val))
                .verify("c2", val -> assertValuesArrayScalar(val, "E1", "E2", "E3", "E4"));

            builder.assertion(SupportCollection.makeString(null, "E3,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val, (Object[]) null))
                .verify("c1", val -> assertValuesArrayScalar(val, (Object[]) null))
                .verify("c2", val -> assertValuesArrayScalar(val, (Object[]) null));

            builder.assertion(SupportCollection.makeString("", "E3,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val))
                .verify("c1", val -> assertValuesArrayScalar(val))
                .verify("c2", val -> assertValuesArrayScalar(val, "E3", "E4"));

            builder.assertion(SupportCollection.makeString("E1,E3,E5", "E3,E4"))
                .verify("c0", val -> assertValuesArrayScalar(val, "E1", "E5"))
                .verify("c1", val -> assertValuesArrayScalar(val, "E3"))
                .verify("c2", val -> assertValuesArrayScalar(val, "E1", "E3", "E5", "E3", "E4"));

            builder.run(env);
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select contained.union(true) from SupportBean_ST0_Container";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'contained.union(true)': Enumeration method 'union' requires an expression yielding a collection of events of type");

            epl = "select contained.union(prevwindow(s1)) from SupportBean_ST0_Container#lastevent, SupportBean#keepall s1";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'contained.union(prevwindow(s1))': Enumeration method 'union' expects event type '" + SupportBean_ST0.class.getName() + "' but receives event type 'SupportBean'");

            epl = "select (select * from SupportBean#keepall).union(strvals) from SupportCollection";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'subselect_1.union(strvals)': Enumeration method 'union' requires an expression yielding a collection of events of type 'SupportBean' as input parameter");

            epl = "select strvals.union((select * from SupportBean#keepall)) from SupportCollection";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'strvals.union(subselect_1)': Enumeration method 'union' requires an expression yielding a collection of values of type 'String' as input parameter");
        }
    }

    private static class ExprEnumUnionWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') expression one {" +
                "  x => x.contained.where(y => p00 = 10)" +
                "} " +
                "" +
                "expression two {" +
                "  x => x.contained.where(y => p00 = 11)" +
                "} " +
                "" +
                "select one(bean).union(two(bean)) as val0 from SupportBean_ST0_Container as bean";
            env.compileDeploy(epl).addListener("s0");

            SupportEventPropUtil.assertTypes(env.statement("s0").getEventType(), "val0".split(","), new EPTypeClass[]{EPTypeClassParameterized.from(Collection.class, SupportBean_ST0.class)});

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,10", "E3,1", "E4,10", "E5,11"));
            assertST0Id(env.listener("s0"), "val0", "E2,E4,E5");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,10", "E2,1", "E3,1"));
            assertST0Id(env.listener("s0"), "val0", "E1");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,10", "E4,11"));
            assertST0Id(env.listener("s0"), "val0", "E3,E4");
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value((String[]) null));
            assertST0Id(env.listener("s0"), "val0", null);
            env.listener("s0").reset();

            env.sendEventBean(SupportBean_ST0_Container.make2Value());
            assertST0Id(env.listener("s0"), "val0", "");
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ExprEnumInheritance implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                if (rep.isMapEvent() || rep.isObjectArrayEvent()) {
                    tryAssertionInheritance(env, rep);
                }
            }
        }
    }

    private static void tryAssertionInheritance(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        String epl = eventRepresentationEnum.getAnnotationText() + " create schema BaseEvent as (b1 string);\n";
        epl += eventRepresentationEnum.getAnnotationText() + " create schema SubEvent as (s1 string) inherits BaseEvent;\n";
        epl += eventRepresentationEnum.getAnnotationText() + " create schema OuterEvent as (bases BaseEvent[], subs SubEvent[]);\n";
        epl += eventRepresentationEnum.getAnnotationText() + " @name('s0') select bases.union(subs) as val from OuterEvent;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{new Object[][]{{"b10"}}, new Object[][]{{"b10", "s10"}}}, "OuterEvent");
        } else {
            Map<String, Object> baseEvent = makeMap("b1", "b10");
            Map<String, Object> subEvent = makeMap("s1", "s10");
            Map<String, Object> outerEvent = makeMap("bases", new Map[]{baseEvent}, "subs", new Map[]{subEvent});
            env.sendEventMap(outerEvent, "OuterEvent");
        }

        Collection result = (Collection) env.listener("s0").assertOneGetNewAndReset().get("val");
        assertEquals(2, result.size());

        env.undeployAll();
    }

    private static Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private static Map<String, Object> makeMap(String key, Object value, String key2, Object value2) {
        Map<String, Object> map = makeMap(key, value);
        map.put(key2, value2);
        return map;
    }

    private static void sendAndAssert(RegressionEnvironment env, String metaOne, String metaTwo, boolean expected) {
        env.sendEventObjectArray(new Object[]{metaOne.split(","), metaTwo.split(",")}, "Event");
        assertEquals(expected, env.listener("s0").getIsInvokedAndReset());
    }
}
