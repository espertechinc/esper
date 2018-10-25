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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContextKeySegmentedPrioritized implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create context SegmentedByMessage partition by theString from SupportBean", path);

        env.compileDeploy("@name('s0') @Drop @Priority(1) context SegmentedByMessage select 'test1' from SupportBean", path);
        env.addListener("s0");

        env.compileDeploy("@name('s1') @Priority(0) context SegmentedByMessage select 'test2' from SupportBean", path);
        env.addListener("s1");

        env.sendEventBean(new SupportBean("test msg", 1));

        assertTrue(env.listener("s0").isInvoked());
        assertFalse(env.listener("s1").isInvoked());

        env.undeployAll();
    }

}
