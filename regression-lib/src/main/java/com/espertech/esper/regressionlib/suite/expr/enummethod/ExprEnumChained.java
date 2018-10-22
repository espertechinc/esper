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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.sales.PersonSales;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import static org.junit.Assert.assertEquals;

public class ExprEnumChained implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String eplFragment = "@name('s0') select sales.where(x => x.cost > 1000).min(y => y.buyer.age) as val from PersonSales";
        env.compileDeploy(eplFragment).addListener("s0");

        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{Integer.class});

        PersonSales bean = PersonSales.make();
        env.sendEventBean(bean);
        assertEquals(50, env.listener("s0").assertOneGetNewAndReset().get("val"));

        env.undeployAll();
    }
}
