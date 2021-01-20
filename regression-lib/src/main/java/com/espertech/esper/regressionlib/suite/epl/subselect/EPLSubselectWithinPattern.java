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
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

public class EPLSubselectWithinPattern {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectInvalid());
        execs.add(new EPLSubselectCorrelated());
        execs.add(new EPLSubselectAggregation());
        execs.add(new EPLSubselectSubqueryAgainstNamedWindowInUDFInPattern());
        execs.add(new EPLSubselectFilterPatternNamedWindowNoAlias());
        return execs;
    }

    private static class EPLSubselectInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindowInvalid#lastevent as select * from SupportBean_S0", path);

            env.tryInvalidCompile("select * from SupportBean_S0(exists (select * from SupportBean_S1))",
                "Failed to validate subquery number 1 querying SupportBean_S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window [select * from SupportBean_S0(exists (select * from SupportBean_S1))]");

            env.tryInvalidCompile(path, "select * from SupportBean_S0(exists (select * from MyWindowInvalid#lastevent))",
                "Failed to validate subquery number 1 querying MyWindowInvalid: Consuming statements to a named window cannot declare a data window view onto the named window [select * from SupportBean_S0(exists (select * from MyWindowInvalid#lastevent))]");

            env.tryInvalidCompile(path, "select * from SupportBean_S0(id in ((select p00 from MyWindowInvalid)))",
                "Failed to validate filter expression 'id in (subselect_1)': Implicit conversion not allowed: Cannot coerce types Integer and String [select * from SupportBean_S0(id in ((select p00 from MyWindowInvalid)))]");

            env.undeployAll();
        }
    }

    private static class EPLSubselectSubqueryAgainstNamedWindowInUDFInPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create window MyWindowSNW#unique(p00)#keepall as SupportBean_S0;\n" +
                "@name('s0') select * from pattern[SupportBean_S1(supportSingleRowFunction((select * from MyWindowSNW)))];\n";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean_S1(1));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class EPLSubselectFilterPatternNamedWindowNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // subselect in pattern
            String stmtTextOne = "@name('s0') select s.id as myid from pattern [every s=SupportBean_S0(p00 in (select p10 from SupportBean_S1#lastevent))]";
            env.compileDeployAddListenerMileZero(stmtTextOne, "s0");

            tryAssertion(env);
            env.undeployAll();

            // subselect in filter
            String stmtTextTwo = "@name('s0') select id as myid from SupportBean_S0(p00 in (select p10 from SupportBean_S1#lastevent))";
            env.compileDeployAddListenerMile(stmtTextTwo, "s0", 1);
            tryAssertion(env);
            env.undeployAll();

            // subselect in filter with named window
            String epl = "create window MyS1Window#lastevent as select * from SupportBean_S1;\n" +
                "insert into MyS1Window select * from SupportBean_S1;\n" +
                "@name('s0') select id as myid from SupportBean_S0(p00 in (select p10 from MyS1Window))";
            env.compileDeployAddListenerMile(epl, "s0", 2);
            tryAssertion(env);
            env.undeployAll();

            // subselect in pattern with named window
            epl = "create window MyS1Window#lastevent as select * from SupportBean_S1;\n" +
                "insert into MyS1Window select * from SupportBean_S1;\n" +
                "@name('s0') select s.id as myid from pattern [every s=SupportBean_S0(p00 in (select p10 from MyS1Window))];\n";
            env.compileDeployAddListenerMile(epl, "s0", 3);
            tryAssertion(env);
            env.undeployAll();
        }
    }

    private static class EPLSubselectCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "@name('s0') select sp1.id as myid from pattern[every sp1=SupportBean_S0(exists (select * from SupportBean_S1#keepall as stream1 where stream1.p10 = sp1.p00))]";
            env.compileDeployAddListenerMile(epl, "s0", 0);
            tryAssertionCorrelated(env);
            env.undeployAll();

            epl = "@name('s0') select id as myid from SupportBean_S0(exists (select stream1.id from SupportBean_S1#keepall as stream1 where stream1.p10 = stream0.p00)) as stream0";
            env.compileDeployAddListenerMile(epl, "s0", 1);
            tryAssertionCorrelated(env);
            env.undeployAll();

            epl = "@name('s0') select sp0.p00||'+'||sp1.p10 as myid from pattern[" +
                "every sp0=SupportBean_S0 -> sp1=SupportBean_S1(p11 = (select stream2.p21 from SupportBean_S2#keepall as stream2 where stream2.p20 = sp0.p00))]";
            env.compileDeployAddListenerMile(epl, "s0", 2);

            env.sendEventBean(new SupportBean_S2(21, "X", "A"));
            env.sendEventBean(new SupportBean_S2(22, "Y", "B"));
            env.sendEventBean(new SupportBean_S2(23, "Z", "C"));

            env.sendEventBean(new SupportBean_S0(1, "A"));
            env.sendEventBean(new SupportBean_S0(2, "Y"));
            env.sendEventBean(new SupportBean_S0(3, "C"));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S1(4, "B", "B"));
            env.assertEqualsNew("s0", "myid", "Y+B");

            env.sendEventBean(new SupportBean_S1(4, "B", "C"));
            env.sendEventBean(new SupportBean_S1(5, "C", "B"));
            env.sendEventBean(new SupportBean_S1(6, "X", "A"));
            env.sendEventBean(new SupportBean_S1(7, "A", "C"));
            env.assertListenerNotInvoked("s0");

            env.undeployAll();
        }
    }

    private static class EPLSubselectAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String stmtText = "@name('s0') select * from SupportBean_S0(id = (select sum(id) from SupportBean_S1#length(2)))";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            env.sendEventBean(new SupportBean_S0(1));
            env.sendEventBean(new SupportBean_S1(1));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S0(1));
            env.assertListenerInvoked("s0");

            env.sendEventBean(new SupportBean_S1(3));  // now at 4
            env.sendEventBean(new SupportBean_S0(3));
            env.sendEventBean(new SupportBean_S0(5));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S0(4));
            env.assertListenerInvoked("s0");

            env.sendEventBean(new SupportBean_S1(10));  // now at 13 (length window 2)
            env.sendEventBean(new SupportBean_S0(10));
            env.sendEventBean(new SupportBean_S0(3));
            env.assertListenerNotInvoked("s0");

            env.sendEventBean(new SupportBean_S0(13));
            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static void tryAssertionCorrelated(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean_S0(1, "A"));
        env.sendEventBean(new SupportBean_S1(2, "A"));
        env.sendEventBean(new SupportBean_S0(3, "B"));
        env.sendEventBean(new SupportBean_S1(4, "C"));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean_S0(5, "C"));
        env.assertEqualsNew("s0", "myid", 5);

        env.sendEventBean(new SupportBean_S0(6, "A"));
        env.assertEqualsNew("s0", "myid", 6);

        env.sendEventBean(new SupportBean_S0(7, "D"));
        env.sendEventBean(new SupportBean_S1(8, "E"));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean_S0(9, "C"));
        env.assertEqualsNew("s0", "myid", 9);
    }

    private static void tryAssertion(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean_S0(1, "A"));
        env.sendEventBean(new SupportBean_S1(2, "A"));
        env.sendEventBean(new SupportBean_S0(3, "B"));
        env.sendEventBean(new SupportBean_S1(4, "C"));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean_S0(5, "C"));
        env.assertEqualsNew("s0", "myid", 5);

        env.sendEventBean(new SupportBean_S0(6, "A"));
        env.sendEventBean(new SupportBean_S0(7, "D"));
        env.sendEventBean(new SupportBean_S1(8, "E"));
        env.sendEventBean(new SupportBean_S0(9, "C"));
        env.assertListenerNotInvoked("s0");

        env.sendEventBean(new SupportBean_S0(10, "E"));
        env.assertEqualsNew("s0", "myid", 10);
    }

    public static boolean supportSingleRowFunction(Object... v) {
        return true;
    }
}