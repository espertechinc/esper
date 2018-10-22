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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

public class ExprEnumMinMaxBy implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String[] fields = "val0,val1,val2,val3".split(",");
        String eplFragment = "@name('s0') select " +
            "contained.minBy(x => p00) as val0," +
            "contained.maxBy(x => p00) as val1," +
            "contained.minBy(x => p00).id as val2," +
            "contained.maxBy(x => p00).p00 as val3 " +
            "from SupportBean_ST0_Container";
        env.compileDeploy(eplFragment).addListener("s0");

        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{SupportBean_ST0.class, SupportBean_ST0.class, String.class, Integer.class});

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2");
        env.sendEventBean(bean);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{bean.getContained().get(2), bean.getContained().get(0), "E2", 12});

        bean = SupportBean_ST0_Container.make2Value("E1,12");
        env.sendEventBean(bean);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{bean.getContained().get(0), bean.getContained().get(0), "E1", 12});

        env.sendEventBean(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{null, null, null, null});

        env.sendEventBean(SupportBean_ST0_Container.make2Value());
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{null, null, null, null});
        env.undeployAll();

        // test scalar-coll with lambda
        String[] fieldsLambda = "val0,val1".split(",");
        String eplLambda = "@name('s0') select " +
            "strvals.minBy(v => extractNum(v)) as val0, " +
            "strvals.maxBy(v => extractNum(v)) as val1 " +
            "from SupportCollection";
        env.compileDeploy(eplLambda).addListener("s0");
        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fieldsLambda, new Class[]{String.class, String.class});

        env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{"E1", "E5"});

        env.sendEventBean(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{"E1", "E1"});
        env.listener("s0").reset();

        env.sendEventBean(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});
        env.listener("s0").reset();

        env.sendEventBean(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

        env.undeployAll();
    }
}
