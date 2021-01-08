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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

import java.util.EnumSet;

public class RowRecogEmptyPartition implements RegressionExecution {

    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.EXCLUDEWHENINSTRUMENTED);
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
        env.assertPropsNew("s0", fields, new Object[]{1});

        env.milestone(1);

        env.sendEventBean(new SupportRecogBean("B", 2));

        env.milestone(2);

        env.sendEventBean(new SupportRecogBean("A", 2));
        env.assertPropsNew("s0", fields, new Object[]{2});

        env.milestone(3);

        env.sendEventBean(new SupportRecogBean("B", 3));
        env.sendEventBean(new SupportRecogBean("A", 4));
        env.sendEventBean(new SupportRecogBean("A", 3));
        env.assertPropsNew("s0", fields, new Object[]{3});

        env.milestone(4);

        env.sendEventBean(new SupportRecogBean("B", 4));
        env.assertPropsNew("s0", fields, new Object[]{4});

        env.milestone(5);

        env.sendEventBean(new SupportRecogBean("A", 6));
        env.sendEventBean(new SupportRecogBean("B", 7));
        env.sendEventBean(new SupportRecogBean("B", 8));
        env.sendEventBean(new SupportRecogBean("A", 7));
        env.assertPropsNew("s0", fields, new Object[]{7});

        /**
         * Comment-in for testing partition removal.
         */
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportRecogBean("A", i));
            //System.out.println(i);
            //env.sendEventBean(new SupportRecogBean("B", i));
            //env.assertPropsListenerNew("s0", fields, new Object[] {i});
        }

        env.undeployAll();
    }
}
