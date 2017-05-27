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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanNumeric;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static junit.framework.TestCase.assertEquals;

public class ExecAggregateFilteredWMathContext implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExpression().setMathContext(new MathContext(2, RoundingMode.HALF_UP));
        configuration.addEventType(SupportBeanNumeric.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select avg(bigdec) as c0 from SupportBeanNumeric").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(null, makeBigDec(0, 2, RoundingMode.HALF_UP)));
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(null, makeBigDec(0, 2, RoundingMode.HALF_UP)));
        epService.getEPRuntime().sendEvent(new SupportBeanNumeric(null, makeBigDec(1, 2, RoundingMode.HALF_UP)));
        assertEquals(0.33, ((BigDecimal) listener.getAndResetLastNewData()[0].get("c0")).doubleValue());
    }

    private BigDecimal makeBigDec(int value, int scale, RoundingMode rounding) {
        BigDecimal bd = new BigDecimal(value);
        bd.setScale(scale, rounding);
        return bd;
    }
}
