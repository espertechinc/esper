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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;

public class ExprCoreRelOp implements RegressionExecution {
    private final static String[] FIELDS = "c0,c1,c2,c3".split(",");

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
        StringWriter writer = new StringWriter();
        writer.append("@name('s0') select ");
        writer.append(lhs).append(">=").append(rhs).append(" as c0,");
        writer.append(lhs).append(">").append(rhs).append(" as c1,");
        writer.append(lhs).append("<=").append(rhs).append(" as c2,");
        writer.append(lhs).append("<").append(rhs).append(" as c3");
        writer.append(" from SupportBean");

        env.compileDeploy(writer.toString()).addListener("s0");

        sendAssert(env, one, FIELDS, false, false, true, true);
        sendAssert(env, two, FIELDS, true, false, true, false);
        sendAssert(env, three, FIELDS, true, true, false, false);

        env.undeployAll();
    }

    private static void sendAssert(RegressionEnvironment env, Consumer<SupportBean> consumer, String[] fields, Object... expected) {
        SupportBean bean = new SupportBean();
        consumer.accept(bean);
        env.sendEventBean(bean);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }
}
