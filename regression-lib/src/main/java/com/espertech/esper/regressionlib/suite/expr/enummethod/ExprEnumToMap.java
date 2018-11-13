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

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExprEnumToMap implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        // - duplicate value allowed, latest value wins
        // - null key & value allowed

        String eplFragment = "@name('s0') select contained.toMap(c => id, c=> p00) as val from SupportBean_ST0_Container";
        env.compileDeploy(eplFragment).addListener("s0");

        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), "val".split(","), new Class[]{Map.class});

        env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,5"));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{1, 5, 12});

        env.sendEventBean(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,12", "E1,2"));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{2, 12, 12});

        env.sendEventBean(new SupportBean_ST0_Container(Collections.singletonList(new SupportBean_ST0(null, null))));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{null, null, null});
        env.undeployAll();

        // test scalar-coll with lambda
        String[] fields = "val0".split(",");
        String eplLambda = "@name('s0') select " +
            "strvals.toMap(c => c, c => extractNum(c)) as val0 " +
            "from SupportCollection";
        env.compileDeploy(eplLambda).addListener("s0");
        LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Map.class});

        env.sendEventBean(SupportCollection.makeString("E2,E1,E3"));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), "E1,E2,E3".split(","), new Object[]{1, 2, 3});

        env.sendEventBean(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertPropsMap((Map) env.listener("s0").assertOneGetNewAndReset().get("val0"), "E1".split(","), new Object[]{1});

        env.sendEventBean(SupportCollection.makeString(null));
        assertNull(env.listener("s0").assertOneGetNewAndReset().get("val0"));

        env.sendEventBean(SupportCollection.makeString(""));
        assertEquals(0, ((Map) env.listener("s0").assertOneGetNewAndReset().get("val0")).size());

        env.undeployAll();
    }
}
