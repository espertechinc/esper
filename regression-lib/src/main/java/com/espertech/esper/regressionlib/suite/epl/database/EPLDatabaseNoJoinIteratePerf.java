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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertTrue;

public class EPLDatabaseNoJoinIteratePerf implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create variable boolean queryvar_bool", path);
        env.compileDeploy("create variable int lower", path);
        env.compileDeploy("create variable int upper", path);
        env.compileDeploy("on SupportBean set queryvar_bool=boolPrimitive, lower=intPrimitive,upper=intBoxed", path);

        String stmtText = "@name('s0') select * from sql:MyDBWithLRU100000 ['select mybigint, mybool from mytesttable where ${queryvar_bool} = mytesttable.mybool and myint between ${lower} and ${upper} order by mybigint']";
        String[] fields = new String[]{"mybigint", "mybool"};
        env.compileDeploy(stmtText, path);
        sendSupportBeanEvent(env, true, 20, 60);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{4L, true}});
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);

        env.undeployAll();
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, boolean boolPrimitive, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setBoolPrimitive(boolPrimitive);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }
}
