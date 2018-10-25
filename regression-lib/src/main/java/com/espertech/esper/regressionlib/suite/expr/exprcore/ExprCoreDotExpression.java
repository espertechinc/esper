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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.UuidGenerator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.events.SampleEnumInEventsPackage;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ExprCoreDotExpression {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprCoreDotObjectEquals());
        execs.add(new ExprCoreDotExpressionEnumValue());
        execs.add(new ExprCoreDotMapIndexPropertyRooted());
        execs.add(new ExprCoreDotInvalid());
        execs.add(new ExprCoreDotChainedUnparameterized());
        execs.add(new ExprCoreDotChainedParameterized());
        execs.add(new ExprCoreDotArrayPropertySizeAndGet());
        execs.add(new ExprCoreDotArrayPropertySizeAndGetChained());
        execs.add(new ExprCoreDotNestedPropertyInstanceExpr());
        execs.add(new ExprCoreDotNestedPropertyInstanceNW());
        return execs;
    }

    private static class ExprCoreDotObjectEquals implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sb.equals(maxBy(intPrimitive)) as c0 from SupportBean as sb";
            env.compileDeploy(epl).addListener("s0");

            sendAssertDotObjectEquals(env, 10, true);
            sendAssertDotObjectEquals(env, 9, false);
            sendAssertDotObjectEquals(env, 11, true);
            sendAssertDotObjectEquals(env, 8, false);
            sendAssertDotObjectEquals(env, 11, false);
            sendAssertDotObjectEquals(env, 12, true);

            env.undeployAll();
        }
    }

    private static class ExprCoreDotExpressionEnumValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@name('s0') select " +
                "intPrimitive = SupportEnumTwo.ENUM_VALUE_1.getAssociatedValue() as c0," +
                "SupportEnumTwo.ENUM_VALUE_2.checkAssociatedValue(intPrimitive) as c1," +
                "SupportEnumTwo.ENUM_VALUE_3.getNested().getValue() as c2," +
                "SupportEnumTwo.ENUM_VALUE_2.checkEventBeanPropInt(sb, 'intPrimitive') as c3," +
                "SupportEnumTwo.ENUM_VALUE_2.checkEventBeanPropInt(*, 'intPrimitive') as c4 " +
                "from SupportBean as sb";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, false, 300, false});

            env.sendEventBean(new SupportBean("E1", 200));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, 300, true});

            env.undeployAll();

            // test "events" reserved keyword in package name
            env.compileDeploy("select " + SampleEnumInEventsPackage.class.getName() + ".A from SupportBean");

            env.undeployAll();
        }
    }

    private static class ExprCoreDotMapIndexPropertyRooted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "innerTypes('key1') as c0,\n" +
                "innerTypes(key) as c1,\n" +
                "innerTypes('key1').ids[1] as c2,\n" +
                "innerTypes(key).getIds(subkey) as c3,\n" +
                "innerTypesArray[1].ids[1] as c4,\n" +
                "innerTypesArray(subkey).getIds(subkey) as c5,\n" +
                "innerTypesArray(subkey).getIds(s0, 'xyz') as c6,\n" +
                "innerTypesArray(subkey).getIds(*, 'xyz') as c7\n" +
                "from SupportEventTypeErasure as s0";
            env.compileDeploy(epl).addListener("s0");

            Assert.assertEquals(SupportEventInnerTypeWGetIds.class, env.statement("s0").getEventType().getPropertyType("c0"));
            Assert.assertEquals(SupportEventInnerTypeWGetIds.class, env.statement("s0").getEventType().getPropertyType("c1"));
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c2"));
            Assert.assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c3"));

            SupportEventTypeErasure event = new SupportEventTypeErasure("key1", 2, Collections.singletonMap("key1", new SupportEventInnerTypeWGetIds(new int[]{20, 30, 40})), new SupportEventInnerTypeWGetIds[]{new SupportEventInnerTypeWGetIds(new int[]{2, 3}), new SupportEventInnerTypeWGetIds(new int[]{4, 5}), new SupportEventInnerTypeWGetIds(new int[]{6, 7, 8})});
            env.sendEventBean(event);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7".split(","), new Object[]{event.getInnerTypes().get("key1"), event.getInnerTypes().get("key1"), 30, 40, 5, 8, 999999, 999999});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select abc.noSuchMethod() from SupportBean abc",
                "Failed to validate select-clause expression 'abc.noSuchMethod()': Failed to solve 'noSuchMethod' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'noSuchMethod': Could not find enumeration method, date-time method or instance method named 'noSuchMethod' in class '" + SupportBean.class.getName() + "' taking no parameters [select abc.noSuchMethod() from SupportBean abc]");
            SupportMessageAssertUtil.tryInvalidCompile(env, "select abc.getChildOne(\"abc\", 10).noSuchMethod() from SupportChainTop abc",
                "Failed to validate select-clause expression 'abc.getChildOne(\"abc\",10).noSuchMethod()': Failed to solve 'getChildOne' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'noSuchMethod': Could not find enumeration method, date-time method or instance method named 'noSuchMethod' in class '" + SupportChainChildOne.class.getName() + "' taking no parameters [select abc.getChildOne(\"abc\", 10).noSuchMethod() from SupportChainTop abc]");
        }
    }

    private static class ExprCoreDotNestedPropertyInstanceExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "levelOne.getCustomLevelOne(10) as val0, " +
                "levelOne.levelTwo.getCustomLevelTwo(20) as val1, " +
                "levelOne.levelTwo.levelThree.getCustomLevelThree(30) as val2 " +
                "from SupportLevelZero";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportLevelZero(new SupportLevelOne(new SupportLevelTwo(new SupportLevelThree()))));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "val0,val1,val2".split(","), new Object[]{"level1:10", "level2:20", "level3:30"});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotNestedPropertyInstanceNW implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create window NodeWindow#unique(id) as SupportEventNode;\n";
            epl += "insert into NodeWindow select * from SupportEventNode;\n";
            epl += "create window NodeDataWindow#unique(nodeId) as SupportEventNodeData;\n";
            epl += "insert into NodeDataWindow select * from SupportEventNodeData;\n";
            epl += "create schema NodeWithData(node SupportEventNode, data SupportEventNodeData);\n";
            epl += "create window NodeWithDataWindow#unique(node.id) as NodeWithData;\n";
            epl += "insert into NodeWithDataWindow " +
                "select node, data from NodeWindow node join NodeDataWindow as data on node.id = data.nodeId;\n";
            epl += "@name('s0') select node.id, data.nodeId, data.value, node.compute(data) from NodeWithDataWindow;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportEventNode("1"));
            env.sendEventBean(new SupportEventNode("2"));
            env.sendEventBean(new SupportEventNodeData("1", "xxx"));

            env.undeployAll();
        }
    }

    private static class ExprCoreDotChainedUnparameterized implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "nested.getNestedValue(), " +
                "nested.getNestedNested().getNestedNestedValue() " +
                "from SupportBeanComplexProps";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
            Object[][] rows = new Object[][]{
                {"nested.getNestedValue()", String.class}
            };
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals(rows[i][0], prop.getPropertyName());
                Assert.assertEquals(rows[i][1], prop.getPropertyType());
            }

            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "nested.getNestedValue()".split(","), new Object[]{bean.getNested().getNestedValue()});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotChainedParameterized implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String subexpr = "top.getChildOne(\"abc\",10).getChildTwo(\"append\")";
            String epl = "@name('s0') select " + subexpr + " from SupportChainTop as top";
            env.compileDeploy(epl).addListener("s0");
            assertChainedParam(env, subexpr);
            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");
            assertChainedParam(env, subexpr);
            env.undeployAll();
        }
    }

    private static class ExprCoreDotArrayPropertySizeAndGet implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(arrayProperty).size() as size, " +
                "(arrayProperty).get(0) as get0, " +
                "(arrayProperty).get(1) as get1, " +
                "(arrayProperty).get(2) as get2, " +
                "(arrayProperty).get(3) as get3 " +
                "from SupportBeanComplexProps";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
            Object[][] rows = new Object[][]{
                {"size", Integer.class},
                {"get0", Integer.class},
                {"get1", Integer.class},
                {"get2", Integer.class},
                {"get3", Integer.class}
            };
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals("failed for " + rows[i][0], rows[i][0], prop.getPropertyName());
                Assert.assertEquals("failed for " + rows[i][0], rows[i][1], prop.getPropertyType());
            }

            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "size,get0,get1,get2,get3".split(","),
                new Object[]{bean.getArrayProperty().length, bean.getArrayProperty()[0], bean.getArrayProperty()[1], bean.getArrayProperty()[2], null});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotArrayPropertySizeAndGetChained implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(abc).getArray().size() as size, " +
                "(abc).getArray().get(0).getNestLevOneVal() as get0 " +
                "from SupportBeanCombinedProps as abc";
            env.compileDeploy(epl).addListener("s0");

            SupportBeanCombinedProps bean = SupportBeanCombinedProps.makeDefaultBean();
            Object[][] rows = new Object[][]{
                {"size", Integer.class},
                {"get0", String.class},
            };
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals(rows[i][0], prop.getPropertyName());
                Assert.assertEquals(rows[i][1], prop.getPropertyType());
            }

            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "size,get0".split(","),
                new Object[]{bean.getArray().length, bean.getArray()[0].getNestLevOneVal()});

            env.undeployAll();
        }
    }

    private static void assertChainedParam(RegressionEnvironment env, String subexpr) {

        Object[][] rows = new Object[][]{
            {subexpr, SupportChainChildTwo.class}
        };
        for (int i = 0; i < rows.length; i++) {
            EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
            assertEquals(rows[i][0], prop.getPropertyName());
            assertEquals(rows[i][1], prop.getPropertyType());
        }

        env.sendEventBean(new SupportChainTop());
        Object result = env.listener("s0").assertOneGetNewAndReset().get(subexpr);
        assertEquals("abcappend", ((SupportChainChildTwo) result).getText());
    }

    private static void sendAssertDotObjectEquals(RegressionEnvironment env, int intPrimitive, boolean expected) {
        env.sendEventBean(new SupportBean(UuidGenerator.generate(), intPrimitive));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
    }
}
