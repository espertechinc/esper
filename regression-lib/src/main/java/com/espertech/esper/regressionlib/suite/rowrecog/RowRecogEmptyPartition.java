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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

public class RowRecogEmptyPartition implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String[] fields = "value".split(",");
        String text = "@name('s0') select * from SupportRecogBean#length(10) " +
            "match_recognize (" +
            "  partition by value" +
            "  measures E1.value as value" +
            "  pattern (E1 E2 | E2 E1 ) " +
            "  define " +
            "    E1 as E1.theString = 'A', " +
            "    E2 as E2.theString = 'B' " +
            ")";

        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A", 1));

        env.milestone(0);

        env.sendEventBean(new SupportRecogBean("B", 1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1});

        env.milestone(1);

        env.sendEventBean(new SupportRecogBean("B", 2));

        env.milestone(2);

        env.sendEventBean(new SupportRecogBean("A", 2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2});

        env.milestone(3);

        env.sendEventBean(new SupportRecogBean("B", 3));
        env.sendEventBean(new SupportRecogBean("A", 4));
        env.sendEventBean(new SupportRecogBean("A", 3));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3});

        env.milestone(4);

        env.sendEventBean(new SupportRecogBean("B", 4));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4});

        env.milestone(5);

        env.sendEventBean(new SupportRecogBean("A", 6));
        env.sendEventBean(new SupportRecogBean("B", 7));
        env.sendEventBean(new SupportRecogBean("B", 8));
        env.sendEventBean(new SupportRecogBean("A", 7));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{7});

        /**
         * Comment-in for testing partition removal.
         */
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportRecogBean("A", i));
            //System.out.println(i);
            //env.sendEventBean(new SupportRecogBean("B", i));
            //EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[] {i});
        }

        env.undeployAll();
    }
}
