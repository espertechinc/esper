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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowRemoveStream implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        String[] fields = new String[]{"theString"};
        env.compileDeploy("@name('c1') create window W1#length(2) as select * from SupportBean", path);
        env.compileDeploy("@name('c2') create window W2#length(2) as select * from SupportBean", path);
        env.compileDeploy("@name('c3') create window W3#length(2) as select * from SupportBean", path);

        env.compileDeploy("insert into W1 select * from SupportBean", path);
        env.compileDeploy("insert rstream into W2 select rstream * from W1", path);
        env.compileDeploy("insert rstream into W3 select rstream * from W2", path);

        env.sendEventBean(new SupportBean("E1", 1));
        env.sendEventBean(new SupportBean("E2", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("c1"), fields, new Object[][]{{"E1"}, {"E2"}});

        env.sendEventBean(new SupportBean("E3", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("c1"), fields, new Object[][]{{"E2"}, {"E3"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("c2"), fields, new Object[][]{{"E1"}});

        env.sendEventBean(new SupportBean("E4", 1));
        env.sendEventBean(new SupportBean("E5", 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("c1"), fields, new Object[][]{{"E4"}, {"E5"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("c2"), fields, new Object[][]{{"E2"}, {"E3"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("c3"), fields, new Object[][]{{"E1"}});

        env.undeployAll();
    }
}