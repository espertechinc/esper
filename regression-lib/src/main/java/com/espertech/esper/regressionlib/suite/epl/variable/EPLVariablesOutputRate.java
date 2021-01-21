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
package com.espertech.esper.regressionlib.suite.epl.variable;

import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EPLVariablesOutputRate {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariableOutputRateEventsAll());
        execs.add(new EPLVariableOutputRateEventsAllOM());
        execs.add(new EPLVariableOutputRateEventsAllCompile());
        execs.add(new EPLVariableOutputRateTimeAll());
        return execs;
    }

    private static class EPLVariableOutputRateEventsAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtimeSetVariable(null, "var_output_limit", 3L);
            String stmtTextSelect = "@name('s0') select count(*) as cnt from SupportBean output last every var_output_limit events";
            env.compileDeploy(stmtTextSelect).addListener("s0");

            tryAssertionOutputRateEventsAll(env);

            env.undeployAll();
        }
    }

    private static class EPLVariableOutputRateEventsAllOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtimeSetVariable(null, "var_output_limit", 3L);
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.countStar(), "cnt"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));
            model.setOutputLimitClause(OutputLimitClause.create(OutputLimitSelector.LAST, "var_output_limit"));
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));

            String stmtTextSelect = "@name('s0') select count(*) as cnt from SupportBean output last every var_output_limit events";
            assertEquals(stmtTextSelect, model.toEPL());
            env.compileDeploy(model, new RegressionPath()).addListener("s0");

            tryAssertionOutputRateEventsAll(env);

            env.undeployAll();
        }
    }

    private static class EPLVariableOutputRateEventsAllCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtimeSetVariable(null, "var_output_limit", 3L);

            String stmtTextSelect = "@name('s0') select count(*) as cnt from SupportBean output last every var_output_limit events";
            env.eplToModelCompileDeploy(stmtTextSelect).addListener("s0");

            tryAssertionOutputRateEventsAll(env);

            env.undeployAll();
        }
    }

    private static class EPLVariableOutputRateTimeAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[] {"cnt"};
            env.runtimeSetVariable(null, "var_output_limit", 3L);
            sendTimer(env, 0);

            String stmtTextSelect = "@name('s0') select count(*) as cnt from SupportBean output snapshot every var_output_limit seconds";
            env.compileDeploy(stmtTextSelect).addListener("s0");

            sendSupportBeans(env, "E1", "E2");   // varargs: sends 2 events
            sendTimer(env, 2999);
            env.assertListenerNotInvoked("s0");

            env.milestone(0);

            sendTimer(env, 3000);
            env.assertPropsNew("s0", fields, new Object[]{2L});

            // set output limit to 5
            String stmtTextSet = "on SupportMarketDataBean set var_output_limit = volume";
            env.compileDeploy(stmtTextSet);
            sendSetterBean(env, 5L);

            // set output limit to 1 second
            sendSetterBean(env, 1L);

            env.milestone(1);

            sendTimer(env, 3200);
            sendSupportBeans(env, "E3", "E4");
            sendTimer(env, 3999);
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 4000);
            env.assertPropsNew("s0", fields, new Object[]{4L});

            // set output limit to 4 seconds (takes effect next time rescheduled, and is related to reference point which is 0)
            sendSetterBean(env, 4L);

            env.milestone(2);

            sendTimer(env, 4999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 5000);
            env.assertPropsNew("s0", fields, new Object[]{4L});

            sendTimer(env, 7999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 8000);
            env.assertPropsNew("s0", fields, new Object[]{4L});

            sendSupportBeans(env, "E5", "E6");   // varargs: sends 2 events

            env.milestone(3);

            sendTimer(env, 11999);
            env.assertListenerNotInvoked("s0");
            sendTimer(env, 12000);
            env.assertPropsNew("s0", fields, new Object[]{6L});

            sendTimer(env, 13000);
            // set output limit to 2 seconds (takes effect next time event received, and is related to reference point which is 0)
            sendSetterBean(env, 2L);
            sendSupportBeans(env, "E7", "E8");   // varargs: sends 2 events
            env.assertListenerNotInvoked("s0");

            sendTimer(env, 13999);
            env.assertListenerNotInvoked("s0");
            // set output limit to null : should stay at 2 seconds
            sendSetterBean(env, null);
            try {
                sendTimer(env, 14000);
                fail();
            } catch (RuntimeException ex) {
                // expected
            }
            env.undeployAll();
        }
    }

    private static void tryAssertionOutputRateEventsAll(RegressionEnvironment env) {
        String[] fields = new String[] {"cnt"};
        sendSupportBeans(env, "E1", "E2");   // varargs: sends 2 events
        env.assertListenerNotInvoked("s0");

        sendSupportBeans(env, "E3");
        env.assertPropsNew("s0", fields, new Object[]{3L});

        // set output limit to 5
        String stmtTextSet = "on SupportMarketDataBean set var_output_limit = volume";
        env.compileDeploy(stmtTextSet);
        sendSetterBean(env, 5L);

        sendSupportBeans(env, "E4", "E5", "E6", "E7"); // send 4 events
        env.assertListenerNotInvoked("s0");

        sendSupportBeans(env, "E8");
        env.assertPropsNew("s0", fields, new Object[]{8L});

        // set output limit to 2
        sendSetterBean(env, 2L);

        sendSupportBeans(env, "E9"); // send 1 events
        env.assertListenerNotInvoked("s0");

        sendSupportBeans(env, "E10");
        env.assertPropsNew("s0", fields, new Object[]{10L});

        // set output limit to 1
        sendSetterBean(env, 1L);

        sendSupportBeans(env, "E11");
        env.assertPropsNew("s0", fields, new Object[]{11L});

        sendSupportBeans(env, "E12");
        env.assertPropsNew("s0", fields, new Object[]{12L});

        // set output limit to null -- this continues at the current rate
        sendSetterBean(env, null);

        sendSupportBeans(env, "E13");
        env.assertPropsNew("s0", fields, new Object[]{13L});
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendSupportBeans(RegressionEnvironment env, String... strings) {
        for (String theString : strings) {
            sendSupportBean(env, theString);
        }
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
    }

    private static void sendSetterBean(RegressionEnvironment env, Long longValue) {
        SupportMarketDataBean bean = new SupportMarketDataBean("", 0, longValue, "");
        env.sendEventBean(bean);
    }
}
