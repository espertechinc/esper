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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportContextInitEventWLength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewParameterizedByContext {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewParameterizedByContextLengthWindow());
        execs.add(new ViewParameterizedByContextDocSample());
        execs.add(new ViewParameterizedByContextMoreWindows());
        return execs;
    }

    private static class ViewParameterizedByContextMoreWindows implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            runAssertionWindow(env, "length_batch(context.miewl.intSize)", milestone);
            runAssertionWindow(env, "time(context.miewl.intSize)", milestone);
            runAssertionWindow(env, "ext_timed(longPrimitive, context.miewl.intSize)", milestone);
            runAssertionWindow(env, "time_batch(context.miewl.intSize)", milestone);
            runAssertionWindow(env, "ext_timed_batch(longPrimitive, context.miewl.intSize)", milestone);
            runAssertionWindow(env, "time_length_batch(context.miewl.intSize, context.miewl.intSize)", milestone);
            runAssertionWindow(env, "time_accum(context.miewl.intSize)", milestone);
            runAssertionWindow(env, "firstlength(context.miewl.intSize)", milestone);
            runAssertionWindow(env, "firsttime(context.miewl.intSize)", milestone);
            runAssertionWindow(env, "sort(context.miewl.intSize, intPrimitive)", milestone);
            runAssertionWindow(env, "rank(theString, context.miewl.intSize, theString)", milestone);
            runAssertionWindow(env, "time_order(longPrimitive, context.miewl.intSize)", milestone);
        }
    }

    private static class ViewParameterizedByContextLengthWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "create context CtxInitToTerm initiated by SupportContextInitEventWLength as miewl terminated after 1 year;\n" +
                "@name('s0') context CtxInitToTerm select context.miewl.id as id, count(*) as cnt from SupportBean(theString=context.miewl.id)#length(context.miewl.intSize)";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "id,cnt".split(",");

            sendInitEvent(env, "P1", 2);
            sendInitEvent(env, "P2", 4);
            sendInitEvent(env, "P3", 3);
            sendValueEvent(env, "P2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"P1", 0L}, {"P2", 1L}, {"P3", 0L}});

            env.milestone(0);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"P1", 0L}, {"P2", 1L}, {"P3", 0L}});

            for (int i = 0; i < 10; i++) {
                sendValueEvent(env, "P1");
                sendValueEvent(env, "P2");
                sendValueEvent(env, "P3");
            }

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"P1", 2L}, {"P2", 4L}, {"P3", 3L}});

            env.milestone(1);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"P1", 2L}, {"P2", 4L}, {"P3", 3L}});
            sendValueEvent(env, "P1");
            sendValueEvent(env, "P2");
            sendValueEvent(env, "P3");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"P1", 2L}, {"P2", 4L}, {"P3", 3L}});

            env.undeployAll();
        }

        private void sendValueEvent(RegressionEnvironment env, String id) {
            env.sendEventBean(new SupportBean(id, -1));
        }

        private void sendInitEvent(RegressionEnvironment env, String id, int intSize) {
            env.sendEventBean(new SupportContextInitEventWLength(id, intSize));
        }
    }

    private static void runAssertionWindow(RegressionEnvironment env, String window, AtomicInteger milestone) {
        String epl = "create context CtxInitToTerm initiated by SupportContextInitEventWLength as miewl terminated after 1 year;\n" +
            "context CtxInitToTerm select * from SupportBean#" + window;
        env.compileDeploy(epl);
        env.sendEventBean(new SupportContextInitEventWLength("P1", 2));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportContextInitEventWLength("P2", 20));

        env.undeployAll();
    }

    private static class ViewParameterizedByContextDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context CtxInitToTerm initiated by SupportContextInitEventWLength as miewl terminated after 1 year;\n" +
                "@name('s0') context CtxInitToTerm select context.miewl.id as id, count(*) as cnt from SupportBean(theString=context.miewl.id)#length(context.miewl.intSize);\n";
            env.compileDeploy(epl).milestone(0);

            env.sendEventBean(new SupportContextInitEventWLength("P1", 2));
            env.sendEventBean(new SupportContextInitEventWLength("P2", 4));
            env.sendEventBean(new SupportContextInitEventWLength("P3", 3));

            env.milestone(1);

            for (int i = 0; i < 10; i++) {
                env.sendEventBean(new SupportBean("P1", 0));
                env.sendEventBean(new SupportBean("P2", 0));
                env.sendEventBean(new SupportBean("P3", 0));
            }

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), "id,cnt".split(","), new Object[][]{{"P1", 2L}, {"P2", 4L}, {"P3", 3L}});

            env.undeployAll();
        }
    }
}
