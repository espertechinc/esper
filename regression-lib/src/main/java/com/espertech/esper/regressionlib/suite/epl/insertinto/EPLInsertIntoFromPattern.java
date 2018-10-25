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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLInsertIntoFromPattern {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoPropsWildcard());
        execs.add(new EPLInsertIntoProps());
        execs.add(new EPLInsertIntoNoProps());
        execs.add(new EPLInsertIntoFromPatternNamedWindow());
        return execs;
    }

    private static class EPLInsertIntoPropsWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtText = "insert into MyThirdStream(es0id, es1id) " +
                "select es0.id, es1.id " +
                "from " +
                "pattern [every (es0=SupportBean_S0" +
                " or es1=SupportBean_S1)]";
            env.compileDeploy(stmtText, path);

            String stmtTwoText = "@name('s0') select * from MyThirdStream";
            env.compileDeploy(stmtTwoText, path).addListener("s0");

            sendEventsAndAssert(env);

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtText = "insert into MySecondStream(s0, s1) " +
                "select es0, es1 " +
                "from " +
                "pattern [every (es0=SupportBean_S0" +
                " or es1=SupportBean_S1)]";
            env.compileDeploy(stmtText, path);

            String stmtTwoText = "@name('s0') select s0.id as es0id, s1.id as es1id from MySecondStream";
            env.compileDeploy(stmtTwoText, path).addListener("s0");

            sendEventsAndAssert(env);

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNoProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtText = "insert into MyStream " +
                "select es0, es1 " +
                "from " +
                "pattern [every (es0=SupportBean_S0" +
                " or es1=SupportBean_S1)]";
            env.compileDeploy(stmtText, path);

            String stmtTwoText = "@name('s0') select es0.id as es0id, es1.id as es1id from MyStream#length(10)";
            env.compileDeploy(stmtTwoText, path).addListener("s0");

            sendEventsAndAssert(env);

            env.undeployAll();
        }
    }

    public static class EPLInsertIntoFromPatternNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window PositionW.win:time(1 hour).std:unique(intPrimitive) as select * from SupportBean", path);
            env.compileDeploy("insert into PositionW select * from SupportBean", path);
            env.compileDeploy("@name('s1') insert into Foo select * from pattern[every a = PositionW -> every b = PositionW]", path);
            env.addListener("s1").milestone(0);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));
            assertTrue(env.listener("s1").isInvoked());

            env.undeployAll();
        }
    }

    private static void sendEventsAndAssert(RegressionEnvironment env) {
        sendEventS1(env, 10, "");
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertNull(theEvent.get("es0id"));
        assertEquals(10, theEvent.get("es1id"));

        env.milestone(0);

        sendEventS0(env, 20, "");
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals(20, theEvent.get("es0id"));
        assertNull(theEvent.get("es1id"));
    }

    private static void sendEventS0(RegressionEnvironment env, int id, String p00) {
        SupportBean_S0 theEvent = new SupportBean_S0(id, p00);
        env.sendEventBean(theEvent);
    }

    private static void sendEventS1(RegressionEnvironment env, int id, String p10) {
        SupportBean_S1 theEvent = new SupportBean_S1(id, p10);
        env.sendEventBean(theEvent);
    }
}
