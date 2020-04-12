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

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExprCoreEqualsIs {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreEqualsIsCoercion());
        executions.add(new ExprCoreEqualsIsCoercionSameType());
        executions.add(new ExprCoreEqualsIsMultikeyWArray());
        executions.add(new ExprCoreEqualsInvalid());
        return executions;
    }

    private static class ExprCoreEqualsInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportMessageAssertUtil.tryInvalidCompile(env, "select intOne=booleanOne from SupportEventWithManyArray",
                "Failed to validate select-clause expression 'intOne=booleanOne': Implicit conversion from datatype 'boolean[]' to 'int[]' is not allowed");

            SupportMessageAssertUtil.tryInvalidCompile(env, "select objectOne=booleanOne from SupportEventWithManyArray",
                "skip");
        }
    }

    private static class ExprCoreEqualsIsMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportEventWithManyArray")
                .expression("c0", "intOne=intTwo")
                .expression("c1", "intOne is intTwo")
                .expression("c2", "intBoxedOne=intBoxedTwo")
                .expression("c3", "intBoxedOne is intBoxedTwo")
                .expression("c4", "int2DimOne=int2DimTwo")
                .expression("c5", "int2DimOne is int2DimTwo")
                .expression("c6", "objectOne=objectTwo")
                .expression("c7", "objectOne is objectTwo");

            SupportEventWithManyArray array = new SupportEventWithManyArray("E1");
            array.withIntOne(new int[]{1, 2});
            array.withIntTwo(new int[]{1, 2});
            array.withIntBoxedOne(new Integer[]{1, 2});
            array.withIntBoxedTwo(new Integer[]{1, 2});
            array.withObjectOne(new Object[]{'a', new Object[]{1}});
            array.withObjectTwo(new Object[]{'a', new Object[]{1}});
            array.withInt2DimOne(new int[][]{{1, 2}, {3, 4}});
            array.withInt2DimTwo(new int[][]{{1, 2}, {3, 4}});
            builder.assertion(array).expect(fields, true, true, true, true, true, true, true, true);

            array = new SupportEventWithManyArray("E1");
            array.withIntOne(new int[]{1, 2});
            array.withIntTwo(new int[]{1});
            array.withIntBoxedOne(new Integer[]{1, 2});
            array.withIntBoxedTwo(new Integer[]{1});
            array.withObjectOne(new Object[]{'a', 2});
            array.withObjectTwo(new Object[]{'a'});
            array.withInt2DimOne(new int[][]{{1, 2}, {3, 4}});
            array.withInt2DimTwo(new int[][]{{1, 2}, {3}});
            builder.assertion(array).expect(fields, false, false, false, false, false, false, false, false);

            builder.run(env);
            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsIsCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "intPrimitive=longPrimitive", "intPrimitive is longPrimitive");
            builder.assertion(makeBean(1, 1L)).expect(fields, true, true);
            builder.assertion(makeBean(1, 2L)).expect(fields, false, false);
            builder.run(env);
            env.undeployAll();
        }

        private static SupportBean makeBean(int intPrimitive, long longPrimitive) {
            SupportBean bean = new SupportBean();
            bean.setIntPrimitive(intPrimitive);
            bean.setLongPrimitive(longPrimitive);
            return bean;
        }
    }

    private static class ExprCoreEqualsIsCoercionSameType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_S0")
                .expressions(fields, "p00 = p01", "id = id", "p02 is not null");
            builder.assertion(new SupportBean_S0(1, "a", "a", "a")).expect(fields, true, true, true);
            builder.assertion(new SupportBean_S0(1, "a", "b", null)).expect(fields, false, true, false);
            builder.run(env);
            env.undeployAll();
        }
    }
}
