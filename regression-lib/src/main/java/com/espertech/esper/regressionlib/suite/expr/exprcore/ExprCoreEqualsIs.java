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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.bean.core.BeanEventBean;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ExprCoreEqualsIs {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreEqualsIsCoercion());
        executions.add(new ExprCoreEqualsIsCoercionSameType());
        executions.add(new ExprCoreEqualsIsMultikeyWArray());
        executions.add(new ExprCoreEqualsIsMultikeyWArrayExprEval());
        executions.add(new ExprCoreEqualsMultikeyWArrayInvalid());
        return executions;
    }

    private static class ExprCoreEqualsMultikeyWArrayInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select intOne=booleanOne from SupportEventWithManyArray",
                "Failed to validate select-clause expression 'intOne=booleanOne': Implicit conversion from datatype 'boolean[]' to 'int[]' is not allowed");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select objectOne=booleanOne from SupportEventWithManyArray",
                "skip");
        }
    }

    private static class ExprCoreEqualsIsMultikeyWArrayExprEval implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportEventWithManyArray array = new SupportEventWithManyArray("E1");
            array.withBooleanOne(new boolean[]{true, false}).withBooleanTwo(new boolean[]{true, false});
            array.withByteOne(new byte[]{1, 2}).withByteTwo(new byte[]{1, 2});
            array.withCharOne(new char[]{1, 2}).withCharTwo(new char[]{1, 2});
            array.withShortOne(new short[]{1, 2}).withShortTwo(new short[]{1, 2});
            array.withIntOne(new int[]{1, 2}).withIntTwo(new int[]{1, 2});
            array.withLongOne(new long[]{1, 2}).withLongTwo(new long[]{1, 2});
            array.withFloatOne(new float[]{1, 2}).WithFloatTwo(new float[]{1, 2});
            array.withDoubleOne(new double[]{1, 2}).withDoubleTwo(new double[]{1, 2});
            array.withObjectOne(new Object[]{1, new Object[]{"A"}}).withObjectTwo(new Object[]{1, new Object[]{"A"}});
            array.withInt2DimOne(new int[][]{{1, 2}, {3, 4}}).withInt2DimTwo(new int[][]{{1, 2}, {3, 4}});

            for (String field : "boolean,byte,char,short,int,long,float,double,object,int2Dim".split(",")) {
                String left = field + "One";
                String right = field + "Two";
                compileEvaluate(env, left + "=" + right, array, true);
                compileEvaluate(env, left + " is " + right, array, true);
            }
        }

        private void compileEvaluate(RegressionEnvironment env, String expression, SupportEventWithManyArray bean, boolean expected) {
            EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured("SupportEventWithManyArray");
            ExprNode node = ((EPRuntimeSPI) env.runtime()).getReflectiveCompileSvc().reflectiveCompileExpression(expression, new EventType[]{eventType}, new String[]{"a"});
            assertEquals("Failed for " + expression, expected, node.getForge().getExprEvaluator().evaluate(new EventBean[]{new BeanEventBean(bean, eventType)}, true, null));
        }
    }

    private static class ExprCoreEqualsIsMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            String epl = "@name('s0') select " +
                "intOne=intTwo as c0," +
                "intOne is intTwo as c1," +
                "intBoxedOne=intBoxedTwo as c2," +
                "intBoxedOne is intBoxedTwo as c3," +
                "int2DimOne=int2DimTwo as c4," +
                "int2DimOne is int2DimTwo as c5," +
                "objectOne=objectTwo as c6, " +
                "objectOne is objectTwo as c7 " +
                " from SupportEventWithManyArray";
            env.compileDeploy(epl).addListener("s0");

            SupportEventWithManyArray array = new SupportEventWithManyArray("E1");
            array.withIntOne(new int[]{1, 2});
            array.withIntTwo(new int[]{1, 2});
            array.withIntBoxedOne(new Integer[]{1, 2});
            array.withIntBoxedTwo(new Integer[]{1, 2});
            array.withObjectOne(new Object[]{'a', new Object[]{1}});
            array.withObjectTwo(new Object[]{'a', new Object[]{1}});
            array.withInt2DimOne(new int[][]{{1, 2}, {3, 4}});
            array.withInt2DimTwo(new int[][]{{1, 2}, {3, 4}});
            env.sendEventBean(array);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, true, true, true, true});

            array = new SupportEventWithManyArray("E1");
            array.withIntOne(new int[]{1, 2});
            array.withIntTwo(new int[]{1});
            array.withIntBoxedOne(new Integer[]{1, 2});
            array.withIntBoxedTwo(new Integer[]{1});
            array.withObjectOne(new Object[]{'a', 2});
            array.withObjectTwo(new Object[]{'a'});
            array.withInt2DimOne(new int[][]{{1, 2}, {3, 4}});
            array.withInt2DimTwo(new int[][]{{1, 2}, {3}});
            env.sendEventBean(array);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, false, false, false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsIsCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive=longPrimitive as c0, intPrimitive is longPrimitive as c1 from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1".split(",");

            makeSendBean(env, 1, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            makeSendBean(env, 1, 2L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsIsCoercionSameType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 = p01 as c0, id = id as c1, p02 is not null as c2 from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1,c2".split(",");

            env.sendEventBean(new SupportBean_S0(1, "a", "a", "a"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean_S0(1, "a", "b", null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false});

            env.undeployAll();
        }
    }

    private static void makeSendBean(RegressionEnvironment env, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }
}
