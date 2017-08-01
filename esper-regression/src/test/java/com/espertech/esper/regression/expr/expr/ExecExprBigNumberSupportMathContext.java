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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;
import java.math.MathContext;

import static junit.framework.TestCase.assertEquals;

public class ExecExprBigNumberSupportMathContext implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExpression().setMathContext(MathContext.DECIMAL32);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        runAssertionMathContextDivide(epService);
        runAssertionMathContextBigDecConvDivide(epService);
    }

    private void runAssertionMathContextBigDecConvDivide(EPServiceProvider epService) {
        String epl = "select " +
                "10/BigDecimal.valueOf(5,0) as c0" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "c0".split(",");
        assertEquals(BigDecimal.class, stmt.getEventType().getPropertyType("c0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {BigDecimal.valueOf(2,0)});

        stmt.destroy();
    }

    private void runAssertionMathContextDivide(EPServiceProvider epService) {
        // cast and divide
        EPStatement stmtDivide = epService.getEPAdministrator().createEPL("Select cast(1.6, BigDecimal) / cast(9.2, BigDecimal) from SupportBean");
        stmtDivide.setSubscriber(new Object() {
            public void update(BigDecimal value) {
                assertEquals(0.1739130d, value.doubleValue());
            }
        });
        epService.getEPRuntime().sendEvent(new SupportBean());
    }
}
