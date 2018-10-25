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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import org.apache.avro.generic.GenericData;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class ExprCoreNewStruct {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprCoreNewStructNewWRepresentation());
        execs.add(new ExprCoreNewStructDefaultColumnsAndSODA());
        execs.add(new ExprCoreNewStructNewWithCase());
        execs.add(new ExprCoreNewStructInvalid());
        return execs;
    }

    private static class ExprCoreNewStructNewWRepresentation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionNewWRepresentation(env, rep, milestone);
            }
        }
    }

    private static class ExprCoreNewStructDefaultColumnsAndSODA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "case theString" +
                " when \"A\" then new{theString=\"Q\",intPrimitive,col2=theString||\"A\"}" +
                " when \"B\" then new{theString,intPrimitive=10,col2=theString||\"B\"} " +
                "end as val0 from SupportBean as sb";

            env.compileDeploy(epl).addListener("s0");
            tryAssertionDefault(env);
            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");
            tryAssertionDefault(env);
            env.undeployAll();

            // test to-expression string
            epl = "@name('s0') select " +
                "case theString" +
                " when \"A\" then new{theString=\"Q\",intPrimitive,col2=theString||\"A\" }" +
                " when \"B\" then new{theString,intPrimitive = 10,col2=theString||\"B\" } " +
                "end from SupportBean as sb";
            env.compileDeploy(epl).addListener("s0");
            Assert.assertEquals("case theString when \"A\" then new{theString=\"Q\",intPrimitive,col2=theString||\"A\"} when \"B\" then new{theString,intPrimitive=10,col2=theString||\"B\"} end", env.statement("s0").getEventType().getPropertyNames()[0]);
            env.undeployAll();
        }
    }

    private static void tryAssertionDefault(RegressionEnvironment env) {

        Assert.assertEquals(Map.class, env.statement("s0").getEventType().getPropertyType("val0"));
        FragmentEventType fragType = env.statement("s0").getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        Assert.assertEquals(String.class, fragType.getFragmentType().getPropertyType("theString"));
        Assert.assertEquals(Integer.class, fragType.getFragmentType().getPropertyType("intPrimitive"));
        Assert.assertEquals(String.class, fragType.getFragmentType().getPropertyType("col2"));

        String[] fieldsInner = "theString,intPrimitive,col2".split(",");
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{null, null, null});

        env.sendEventBean(new SupportBean("A", 2));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Q", 2, "AA"});

        env.sendEventBean(new SupportBean("B", 3));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"B", 10, "BB"});
    }

    private static class ExprCoreNewStructNewWithCase implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl = "@name('s0') select " +
                "case " +
                "  when theString = 'A' then new { col1 = 'X', col2 = 10 } " +
                "  when theString = 'B' then new { col1 = 'Y', col2 = 20 } " +
                "  when theString = 'C' then new { col1 = null, col2 = null } " +
                "  else new { col1 = 'Z', col2 = 30 } " +
                "end as val0 from SupportBean sb";
            tryAssertion(env, epl, milestone);

            epl = "@name('s0') select " +
                "case theString " +
                "  when 'A' then new { col1 = 'X', col2 = 10 } " +
                "  when 'B' then new { col1 = 'Y', col2 = 20 } " +
                "  when 'C' then new { col1 = null, col2 = null } " +
                "  else new{ col1 = 'Z', col2 = 30 } " +
                "end as val0 from SupportBean sb";
            tryAssertion(env, epl, milestone);
        }
    }

    private static class ExprCoreNewStructInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select case when true then new { col1 = 'a' } else 1 end from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'case when true then new{col1=\"a\"} e...(44 chars)': Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value, check the else-condition [select case when true then new { col1 = 'a' } else 1 end from SupportBean]");

            epl = "select case when true then new { col1 = 'a' } when false then 1 end from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'case when true then new{col1=\"a\"} w...(55 chars)': Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value, check when-condition number 1 [select case when true then new { col1 = 'a' } when false then 1 end from SupportBean]");

            epl = "select case when true then new { col1 = 'a' } else new { col1 = 1 } end from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'case when true then new{col1=\"a\"} e...(54 chars)': Incompatible case-when return types by new-operator in case-when number 1: Type by name 'Case-when number 1' in property 'col1' expected class java.lang.String but receives class java.lang.Integer [select case when true then new { col1 = 'a' } else new { col1 = 1 } end from SupportBean]");

            epl = "select case when true then new { col1 = 'a' } else new { col2 = 'a' } end from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'case when true then new{col1=\"a\"} e...(56 chars)': Incompatible case-when return types by new-operator in case-when number 1: The property 'col1' is not provided but required [select case when true then new { col1 = 'a' } else new { col2 = 'a' } end from SupportBean]");

            epl = "select case when true then new { col1 = 'a', col1 = 'b' } end from SupportBean";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'case when true then new{col1=\"a\",co...(46 chars)': Failed to validate new-keyword property names, property 'col1' has already been declared [select case when true then new { col1 = 'a', col1 = 'b' } end from SupportBean]");
        }
    }

    private static void tryAssertion(RegressionEnvironment env, String epl, AtomicInteger milestone) {
        env.compileDeploy(epl).addListener("s0").milestone(milestone.getAndIncrement());

        Assert.assertEquals(Map.class, env.statement("s0").getEventType().getPropertyType("val0"));
        FragmentEventType fragType = env.statement("s0").getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        Assert.assertEquals(String.class, fragType.getFragmentType().getPropertyType("col1"));
        Assert.assertEquals(Integer.class, fragType.getFragmentType().getPropertyType("col2"));

        String[] fieldsInner = "col1,col2".split(",");
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Z", 30});

        env.sendEventBean(new SupportBean("A", 2));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"X", 10});

        env.sendEventBean(new SupportBean("B", 3));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Y", 20});

        env.sendEventBean(new SupportBean("C", 4));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{null, null});

        env.undeployAll();
    }

    private static void tryAssertionNewWRepresentation(RegressionEnvironment env, EventRepresentationChoice rep, AtomicInteger milestone) {
        String epl = rep.getAnnotationText() + "@name('s0') select new { theString = 'x' || theString || 'x', intPrimitive = intPrimitive + 2} as val0 from SupportBean as sb";
        env.compileDeploy(epl).addListener("s0").milestone(milestone.getAndIncrement());

        Assert.assertEquals(rep.isAvroEvent() ? GenericData.Record.class : Map.class, env.statement("s0").getEventType().getPropertyType("val0"));
        FragmentEventType fragType = env.statement("s0").getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        Assert.assertEquals(String.class, fragType.getFragmentType().getPropertyType("theString"));
        Assert.assertEquals(Integer.class, JavaClassHelper.getBoxedType(fragType.getFragmentType().getPropertyType("intPrimitive")));

        String[] fieldsInner = "theString,intPrimitive".split(",");
        env.sendEventBean(new SupportBean("E1", -5));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        if (rep.isAvroEvent()) {
            SupportAvroUtil.avroToJson(event);
            GenericData.Record inner = (GenericData.Record) event.get("val0");
            Assert.assertEquals("xE1x", inner.get("theString"));
            Assert.assertEquals(-3, inner.get("intPrimitive"));
        } else {
            EPAssertionUtil.assertPropsMap((Map) event.get("val0"), fieldsInner, new Object[]{"xE1x", -3});
        }

        env.undeployAll();
    }
}
