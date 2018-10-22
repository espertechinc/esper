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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static junit.framework.TestCase.assertEquals;

public class ResultSetAggregateFilteredWMathContext implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        String epl = "@name('s0') select avg(bigdec) as c0 from SupportBeanNumeric";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBeanNumeric(null, makeBigDec(0, 2, RoundingMode.HALF_UP)));
        env.sendEventBean(new SupportBeanNumeric(null, makeBigDec(0, 2, RoundingMode.HALF_UP)));
        env.sendEventBean(new SupportBeanNumeric(null, makeBigDec(1, 2, RoundingMode.HALF_UP)));
        assertEquals(0.33, ((BigDecimal) env.listener("s0").getAndResetLastNewData()[0].get("c0")).doubleValue());

        env.undeployAll();
    }

    private BigDecimal makeBigDec(int value, int scale, RoundingMode rounding) {
        BigDecimal bd = new BigDecimal(value);
        bd.setScale(scale, rounding);
        return bd;
    }
}
