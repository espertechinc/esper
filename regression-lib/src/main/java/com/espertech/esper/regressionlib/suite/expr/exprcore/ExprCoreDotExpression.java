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
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.UuidGenerator;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.events.SampleEnumInEventsPackage;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.math.BigDecimal;
import java.util.*;

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
        execs.add(new ExprCoreDotCollectionSelectFromGetAndSize());
        execs.add(new ExprCoreDotToArray());
        execs.add(new ExprCoreDotAggregationSimpleValueMethod());
        return execs;
    }

    private static class ExprCoreDotAggregationSimpleValueMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema MyEvent(col BigDecimal);\n" +
                    "@name('s0') select first(col).abs() as c0 from MyEvent#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventMap(Collections.singletonMap("col", BigDecimal.valueOf(-1)), "MyEvent");
            env.assertPropsNew("s0", "c0".split(","), new Object[]{BigDecimal.valueOf(1)});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotToArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema MyEvent(mycoll Collection);\n" +
                "@name('s0') select mycoll.toArray() as c0," +
                "  mycoll.toArray(new Object[0]) as c1," +
                "  mycoll.toArray(new Object[]{}) as c2 " +
                "from MyEvent";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventMap(Collections.singletonMap("mycoll", new ArrayList<>(Arrays.asList(1, 2))), "MyEvent");
            Object[] expected = new Object[] {1, 2};
            env.assertPropsNew("s0", "c0,c1,c2".split(","), new Object[] {expected, expected, expected});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotCollectionSelectFromGetAndSize implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p01.split(',').selectFrom(v -> v).size() as sz from SupportBean_S0(p00=p01.split(',').selectFrom(v -> v).get(2))";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, "A", "A,B,C", null);
            sendAssert(env, "A", "C,B,A", 3);
            sendAssert(env, "A", "", null);
            sendAssert(env, "A", "A,B,C,A", null);
            sendAssert(env, "A", "A,B,A,B", 4);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String p00, String p01, Integer sizeExpected) {
            env.sendEventBean(new SupportBean_S0(0, p00, p01));
            if (sizeExpected == null) {
                env.assertListenerNotInvoked("s0");
            } else {
                env.assertEqualsNew("s0", "sz", sizeExpected);
            }
        }
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
            String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean", "sb")
                .expression(fields[0], "intPrimitive = SupportEnumTwo.ENUM_VALUE_1.getAssociatedValue()")
                .expression(fields[1], "SupportEnumTwo.ENUM_VALUE_2.checkAssociatedValue(intPrimitive)")
                .expression(fields[2], "SupportEnumTwo.ENUM_VALUE_3.getNested().getValue()")
                .expression(fields[3], "SupportEnumTwo.ENUM_VALUE_2.checkEventBeanPropInt(sb, 'intPrimitive')")
                .expression(fields[4], "SupportEnumTwo.ENUM_VALUE_2.checkEventBeanPropInt(*, 'intPrimitive')")
                .expression(fields[5], "SupportEnumTwo.ENUM_VALUE_2.getMyStringsAsList()")
                .expression(fields[6], "SupportEnumTwo.ENUM_VALUE_2.getNested().getMyStringsNestedAsList()");

            builder.statementConsumer(stmt -> {
                assertEquals(EPTypeClassParameterized.from(List.class, String.class), stmt.getEventType().getPropertyEPType("c5"));
                assertEquals(EPTypeClassParameterized.from(List.class, String.class), stmt.getEventType().getPropertyEPType("c6"));
            });

            List<String> strings = Arrays.asList("2", "0", "0");
            builder.assertion(new SupportBean("E1", 100)).expect(fields, true, false, 300, false, false, strings, strings);
            builder.assertion(new SupportBean("E1", 200)).expect(fields, false, true, 300, true, true, strings, strings);

            builder.run(env);
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

            env.assertStatement("s0", statement -> {
                assertEquals(SupportEventInnerTypeWGetIds.class, statement.getEventType().getPropertyType("c0"));
                assertEquals(SupportEventInnerTypeWGetIds.class, statement.getEventType().getPropertyType("c1"));
                assertEquals(Integer.class, statement.getEventType().getPropertyType("c2"));
                assertEquals(Integer.class, statement.getEventType().getPropertyType("c3"));
            });

            SupportEventTypeErasure event = new SupportEventTypeErasure("key1", 2, Collections.singletonMap("key1", new SupportEventInnerTypeWGetIds(new int[]{20, 30, 40})), new SupportEventInnerTypeWGetIds[]{new SupportEventInnerTypeWGetIds(new int[]{2, 3}), new SupportEventInnerTypeWGetIds(new int[]{4, 5}), new SupportEventInnerTypeWGetIds(new int[]{6, 7, 8})});
            env.sendEventBean(event);
            env.assertPropsNew("s0", "c0,c1,c2,c3,c4,c5,c6,c7".split(","), new Object[]{event.getInnerTypes().get("key1"), event.getInnerTypes().get("key1"), 30, 40, 5, 8, 999999, 999999});

            env.undeployAll();
        }
    }

    private static class ExprCoreDotInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select abc.noSuchMethod() from SupportBean abc",
                "Failed to validate select-clause expression 'abc.noSuchMethod()': Failed to solve 'noSuchMethod' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'noSuchMethod': Could not find enumeration method, date-time method, instance method or property named 'noSuchMethod' in class '" + SupportBean.class.getName() + "' taking no parameters [select abc.noSuchMethod() from SupportBean abc]");
            env.tryInvalidCompile("select abc.getChildOne(\"abc\", 10).noSuchMethod() from SupportChainTop abc",
                "Failed to validate select-clause expression 'abc.getChildOne(\"abc\",10).noSuchMethod()': Failed to solve 'getChildOne' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'noSuchMethod': Could not find enumeration method, date-time method, instance method or property named 'noSuchMethod' in class '" + SupportChainChildOne.class.getName() + "' taking no parameters [select abc.getChildOne(\"abc\", 10).noSuchMethod() from SupportChainTop abc]");

            String epl = "import " + MyHelperWithPrivateModifierAndPublicMethod.class.getName() + ";\n" +
                         "select " + MyHelperWithPrivateModifierAndPublicMethod.class.getSimpleName() + ".callMe() from SupportBean;\n";
            env.tryInvalidCompile(epl,
                "Failed to validate select-clause expression 'MyHelperWithPrivateModifierAndPubli...(51 chars)': Failed to resolve 'MyHelperWithPrivateModifierAndPublicMethod.callMe' to");
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
            env.assertPropsNew("s0", "val0,val1,val2".split(","), new Object[]{"level1:10", "level2:20", "level3:30"});

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
            env.assertStatement("s0", statement -> {
                for (int i = 0; i < rows.length; i++) {
                    EventPropertyDescriptor prop = statement.getEventType().getPropertyDescriptors()[i];
                    assertEquals(rows[i][0], prop.getPropertyName());
                    assertEquals(rows[i][1], prop.getPropertyType());
                }
            });

            env.sendEventBean(bean);
            env.assertPropsNew("s0", "nested.getNestedValue()".split(","), new Object[]{bean.getNested().getNestedValue()});

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
            env.assertStatement("s0", statement -> {
                for (int i = 0; i < rows.length; i++) {
                    EventPropertyDescriptor prop = statement.getEventType().getPropertyDescriptors()[i];
                    assertEquals("failed for " + rows[i][0], rows[i][0], prop.getPropertyName());
                    assertEquals("failed for " + rows[i][0], rows[i][1], prop.getPropertyType());
                }
            });

            env.sendEventBean(bean);
            env.assertPropsNew("s0", "size,get0,get1,get2,get3".split(","),
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
            env.assertStatement("s0", statement -> {
                for (int i = 0; i < rows.length; i++) {
                    EventPropertyDescriptor prop = statement.getEventType().getPropertyDescriptors()[i];
                    assertEquals(rows[i][0], prop.getPropertyName());
                    assertEquals(rows[i][1], prop.getPropertyType());
                }
            });

            env.sendEventBean(bean);
            env.assertPropsNew("s0", "size,get0".split(","),
                new Object[]{bean.getArray().length, bean.getArray()[0].getNestLevOneVal()});

            env.undeployAll();
        }
    }

    private static void assertChainedParam(RegressionEnvironment env, String subexpr) {

        Object[][] rows = new Object[][]{
            {subexpr, SupportChainChildTwo.class}
        };
        env.assertStatement("s0", statement -> {
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = statement.getEventType().getPropertyDescriptors()[i];
                assertEquals(rows[i][0], prop.getPropertyName());
                assertEquals(rows[i][1], prop.getPropertyType());
            }
        });

        env.sendEventBean(new SupportChainTop());
        env.assertEventNew("s0", event -> {
            Object result = event.get(subexpr);
            assertEquals("abcappend", ((SupportChainChildTwo) result).getText());
        });
    }

    private static void sendAssertDotObjectEquals(RegressionEnvironment env, int intPrimitive, boolean expected) {
        env.sendEventBean(new SupportBean(UuidGenerator.generate(), intPrimitive));
        env.assertPropsNew("s0", "c0".split(","), new Object[]{expected});
    }

    private static class MyHelperWithPrivateModifierAndPublicMethod {
        public String callMe() {
            return null;
        }
    }
}
