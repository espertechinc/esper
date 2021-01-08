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

import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static org.junit.Assert.assertTrue;

public class EPLJoin3StreamInKeywordPerformance implements RegressionExecution {

    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED);
    }

    public void run(RegressionEnvironment env) {

        String epl = "@name('s0') select s0.id as val from " +
            "SupportBean_S0#keepall s0, " +
            "SupportBean_S1#keepall s1, " +
            "SupportBean_S2#keepall s2 " +
            "where p00 in (p10, p20)";
        String[] fields = "val".split(",");
        env.compileDeployAddListenerMileZero(epl, "s0");

        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean_S0(i, "P00_" + i));
        }
        env.sendEventBean(new SupportBean_S1(0, "x"));

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(new SupportBean_S2(1, "P00_6541"));
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{6541}});
        }
        long delta = System.currentTimeMillis() - startTime;
        assertTrue("delta=" + delta, delta < 500);
        log.info("delta=" + delta);

        env.undeployAll();
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin3StreamInKeywordPerformance.class);
}
