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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;

public class ExecExprRelOp implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertion(epService, "theString", "'B'", bean -> bean.setTheString("A"), bean -> bean.setTheString("B"), bean -> bean.setTheString("C"));
        runAssertion(epService, "intPrimitive", "2", bean -> bean.setIntPrimitive(1), bean -> bean.setIntPrimitive(2), bean -> bean.setIntPrimitive(3));
        runAssertion(epService, "longBoxed", "2L", bean -> bean.setLongBoxed(1L), bean -> bean.setLongBoxed(2L), bean -> bean.setLongBoxed(3L));
        runAssertion(epService, "floatPrimitive", "2f", bean -> bean.setFloatPrimitive(1), bean -> bean.setFloatPrimitive(2), bean -> bean.setFloatPrimitive(3));
        runAssertion(epService, "doublePrimitive", "2d", bean -> bean.setDoublePrimitive(1), bean -> bean.setDoublePrimitive(2), bean -> bean.setDoublePrimitive(3));
        runAssertion(epService, "bigDecimal", "BigDecimal.valueOf(2, 0)", bean -> bean.setBigDecimal(BigDecimal.valueOf(1, 0)), bean -> bean.setBigDecimal(BigDecimal.valueOf(2, 0)), bean -> bean.setBigDecimal(BigDecimal.valueOf(3, 0)));
        runAssertion(epService, "intPrimitive", "BigDecimal.valueOf(2, 0)", bean -> bean.setIntPrimitive(1), bean -> bean.setIntPrimitive(2), bean -> bean.setIntPrimitive(3));
        runAssertion(epService, "bigInteger", "BigInteger.valueOf(2)", bean -> bean.setBigInteger(BigInteger.valueOf(1)), bean -> bean.setBigInteger(BigInteger.valueOf(2)), bean -> bean.setBigInteger(BigInteger.valueOf(3)));
        runAssertion(epService, "intPrimitive", "BigInteger.valueOf(2)", bean -> bean.setIntPrimitive(1), bean -> bean.setIntPrimitive(2), bean -> bean.setIntPrimitive(3));
    }

    private void runAssertion(EPServiceProvider epService, String lhs, String rhs, Consumer<SupportBean> one, Consumer<SupportBean> two, Consumer<SupportBean> three) {
        String[] fields = "c0,c1,c2,c3".split(",");
        StringWriter writer = new StringWriter();
        writer.append("select ");
        writer.append(lhs).append(">=").append(rhs).append(" as c0,");
        writer.append(lhs).append(">").append(rhs).append(" as c1,");
        writer.append(lhs).append("<=").append(rhs).append(" as c2,");
        writer.append(lhs).append("<").append(rhs).append(" as c3");
        writer.append(" from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL(writer.toString());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssert(epService, listener, one, fields, false, false, true, true);
        sendAssert(epService, listener, two, fields, true, false, true, false);
        sendAssert(epService, listener, three, fields, true, true, false, false);

        stmt.destroy();
    }

    private void sendAssert(EPServiceProvider epService, SupportUpdateListener listener, Consumer<SupportBean> consumer, String[] fields, Object... expected) {
        SupportBean bean = new SupportBean();
        consumer.accept(bean);
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }
}
