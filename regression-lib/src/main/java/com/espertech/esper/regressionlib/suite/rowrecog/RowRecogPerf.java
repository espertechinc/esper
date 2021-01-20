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

import static com.espertech.esper.regressionlib.framework.RegressionFlag.EXCLUDEWHENINSTRUMENTED;
import static com.espertech.esper.regressionlib.framework.RegressionFlag.PERFORMANCE;
import static org.junit.Assert.assertTrue;

public class RowRecogPerf implements RegressionExecution {

    @Override
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(EXCLUDEWHENINSTRUMENTED, PERFORMANCE);
    }

    public void run(RegressionEnvironment env) {

        String text = "@name('s0') select * from SupportRecogBean " +
            "match_recognize (" +
            "  partition by value " +
            "  measures A.theString as a_string, C.theString as c_string " +
            "  all matches " +
            "  pattern (A B*? C) " +
            "  define A as A.cat = '1'," +
            "         B as B.cat = '2'," +
            "         C as C.cat = '3'" +
            ")";
        // When testing aggregation:
        //"  measures A.string as a_string, count(B.string) as cntb, C.string as c_string " +

        env.compileDeploy(text).addListener("s0");

        long start = System.currentTimeMillis();

        for (int partition = 0; partition < 2; partition++) {
            env.sendEventBean(new SupportRecogBean("E1", "1", partition));
            for (int i = 0; i < 25000; i++) {
                env.sendEventBean(new SupportRecogBean("E2_" + i, "2", partition));
            }
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportRecogBean("E3", "3", partition));
            env.assertListenerInvoked("s0");
        }

        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 2000);

        env.undeployAll();
    }
}