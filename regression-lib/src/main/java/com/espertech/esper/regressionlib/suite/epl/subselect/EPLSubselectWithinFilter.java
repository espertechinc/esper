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

import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

public class EPLSubselectWithinFilter {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectWithinFilterExistsWhereAndUDF());
        execs.add(new EPLSubselectWithinFilterRowWhereAndUDF());
        return execs;
    }

    private static class EPLSubselectWithinFilterExistsWhereAndUDF implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') " +
                "inlined_class \"\"\"\n" +
                "  public class MyUtil { public static boolean compareIt(String one, String two) { return one.equals(two); } }\n" +
                "\"\"\" \n" +
                "select * from SupportBean_S0(exists (select * from SupportBean_S1#keepall where MyUtil.compareIt(s0.p00,p10))) as s0;\n";
            env.compileDeploy(epl).addListener("s0");

            sendS0Assert(env, 1, "a", false);
            env.sendEventBean(new SupportBean_S1(10, "x"));
            sendS0Assert(env, 2, "a", false);
            env.sendEventBean(new SupportBean_S1(11, "a"));
            sendS0Assert(env, 3, "a", true);
            sendS0Assert(env, 4, "x", true);

            env.undeployAll();
        }
    }

    private static class EPLSubselectWithinFilterRowWhereAndUDF implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') " +
                "inlined_class \"\"\"\n" +
                "  public class MyUtil { public static boolean compareIt(String one, String two) { return one.equals(two); } }\n" +
                "\"\"\" \n" +
                "select * from SupportBean_S0('abc' = (select p11 from SupportBean_S1#keepall where MyUtil.compareIt(s0.p00,p10))) as s0;\n";
            env.compileDeploy(epl).addListener("s0");

            sendS0Assert(env, 1, "a", false);
            env.sendEventBean(new SupportBean_S1(10, "x", "abc"));
            sendS0Assert(env, 1, "a", false);
            env.sendEventBean(new SupportBean_S1(11, "a", "abc"));
            sendS0Assert(env, 3, "a", true);
            sendS0Assert(env, 4, "x", true);
            sendS0Assert(env, 5, "y", false);

            env.undeployAll();
        }
    }

    private static void sendS0Assert(RegressionEnvironment env, int id, String p00, boolean expected) {
        env.sendEventBean(new SupportBean_S0(id, p00));
        env.assertListenerInvokedFlag("s0", expected);
    }
}
