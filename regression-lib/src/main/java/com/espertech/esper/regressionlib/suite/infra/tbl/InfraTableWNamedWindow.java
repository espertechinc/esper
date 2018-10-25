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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableWNamedWindow implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy("@Name('var') create table varagg (key string primary key, total sum(int))", path);
        env.compileDeploy("@Name('win') create window MyWindow#keepall as SupportBean", path);
        env.compileDeploy("@Name('insert') insert into MyWindow select * from SupportBean", path);
        env.compileDeploy("@Name('populate') into table varagg select sum(intPrimitive) as total from MyWindow group by theString", path);
        env.compileDeploy("@Name('s0') on SupportBean_S0 select theString, varagg[p00].total as c0 from MyWindow where theString = p00", path).addListener("s0");
        String[] fields = "theString,c0".split(",");

        env.sendEventBean(new SupportBean("E1", 10));

        env.milestone(0);

        env.sendEventBean(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});

        env.undeployAll();
    }
}
