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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertFalse;

public class EPLJoinDerivedValueViews implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select\n" +
            "Math.signum(stream1.slope) as s1,\n" +
            "Math.signum(stream2.slope) as s2\n" +
            "from\n" +
            "SupportBean#length_batch(3)#linest(intPrimitive, longPrimitive) as stream1,\n" +
            "SupportBean#length_batch(2)#linest(intPrimitive, longPrimitive) as stream2";
        env.compileDeployAddListenerMileZero(epl, "s0");
        env.sendEventBean(makeEvent("E3", 1, 100));
        env.sendEventBean(makeEvent("E4", 1, 100));
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static SupportBean makeEvent(String id, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(id, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }
}
