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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanTwo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWIndowFAFQueryJoinPerformance implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window W1#unique(s1) as SupportSimpleBeanOne", path);
        env.compileDeploy("insert into W1 select * from SupportSimpleBeanOne", path);

        env.compileDeploy("create window W2#unique(s2) as SupportSimpleBeanTwo", path);
        env.compileDeploy("insert into W2 select * from SupportSimpleBeanTwo", path);

        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(new SupportSimpleBeanOne("A" + i, 0, 0, 0));
            env.sendEventBean(new SupportSimpleBeanTwo("A" + i, 0, 0, 0));
        }

        long start = System.currentTimeMillis();
        EPCompiled compiled = env.compileFAF("select * from W1 as w1, W2 as w2 where w1.s1 = w2.s2", path);
        EPFireAndForgetPreparedQuery prepared = env.runtime().getFireAndForgetService().prepareQuery(compiled);
        for (int i = 0; i < 100; i++) {
            EPFireAndForgetQueryResult result = prepared.execute();
            assertEquals(1000, result.getArray().length);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        System.out.println("Delta=" + delta);
        assertTrue("Delta=" + delta, delta < 1000);

        env.undeployAll();
    }
}
