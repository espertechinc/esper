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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertTrue;

public class EPLSubselectOrderOfEvalNoPreeval implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String epl = "@name('s0') @name('s0')select * from SupportBean(intPrimitive<10) where intPrimitive not in (select intPrimitive from SupportBean#unique(intPrimitive))";
        env.compileDeployAddListenerMileZero(epl, "s0");

        env.sendEventBean(new SupportBean("E1", 5));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();

        String eplTwo = "@name('s0') select * from SupportBean where intPrimitive not in (select intPrimitive from SupportBean(intPrimitive<10)#unique(intPrimitive))";
        env.compileDeployAddListenerMile(eplTwo, "s0", 1);

        env.sendEventBean(new SupportBean("E1", 5));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }
}