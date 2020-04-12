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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;

public class ExprCoreRelOp implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runAssertion(env, "theString", "'B'", bean -> bean.setTheString("A"), bean -> bean.setTheString("B"), bean -> bean.setTheString("C"));
        runAssertion(env, "intPrimitive", "2", bean -> bean.setIntPrimitive(1), bean -> bean.setIntPrimitive(2), bean -> bean.setIntPrimitive(3));
        runAssertion(env, "longBoxed", "2L", bean -> bean.setLongBoxed(1L), bean -> bean.setLongBoxed(2L), bean -> bean.setLongBoxed(3L));
        runAssertion(env, "floatPrimitive", "2f", bean -> bean.setFloatPrimitive(1), bean -> bean.setFloatPrimitive(2), bean -> bean.setFloatPrimitive(3));
        runAssertion(env, "doublePrimitive", "2d", bean -> bean.setDoublePrimitive(1), bean -> bean.setDoublePrimitive(2), bean -> bean.setDoublePrimitive(3));
        runAssertion(env, "bigDecimal", "BigDecimal.valueOf(2, 0)", bean -> bean.setBigDecimal(BigDecimal.valueOf(1, 0)), bean -> bean.setBigDecimal(BigDecimal.valueOf(2, 0)), bean -> bean.setBigDecimal(BigDecimal.valueOf(3, 0)));
        runAssertion(env, "intPrimitive", "BigDecimal.valueOf(2, 0)", bean -> bean.setIntPrimitive(1), bean -> bean.setIntPrimitive(2), bean -> bean.setIntPrimitive(3));
        runAssertion(env, "bigInteger", "BigInteger.valueOf(2)", bean -> bean.setBigInteger(BigInteger.valueOf(1)), bean -> bean.setBigInteger(BigInteger.valueOf(2)), bean -> bean.setBigInteger(BigInteger.valueOf(3)));
        runAssertion(env, "intPrimitive", "BigInteger.valueOf(2)", bean -> bean.setIntPrimitive(1), bean -> bean.setIntPrimitive(2), bean -> bean.setIntPrimitive(3));
    }

    private static void runAssertion(RegressionEnvironment env, String lhs, String rhs, Consumer<SupportBean> one, Consumer<SupportBean> two, Consumer<SupportBean> three) {
        SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean");
        String[] fields = "c0,c1,c2,c3".split(",");
        builder.expression(fields[0], lhs + ">=" + rhs);
        builder.expression(fields[1], lhs + ">" + rhs);
        builder.expression(fields[2], lhs + "<=" + rhs);
        builder.expression(fields[3], lhs + "<" + rhs);

        SupportBean beanOne = new SupportBean();
        one.accept(beanOne);
        builder.assertion(beanOne).expect(fields, false, false, true, true);

        SupportBean beanTwo = new SupportBean();
        two.accept(beanTwo);
        builder.assertion(beanTwo).expect(fields, true, false, true, false);

        SupportBean beanThree = new SupportBean();
        three.accept(beanThree);
        builder.assertion(beanThree).expect(fields, true, true, false, false);

        builder.run(env);
        env.undeployAll();
    }
}
