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
package com.espertech.esper.regressionlib.suite.client.stage;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.*;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.*;

public class ClientStageSendEvent {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageSendEventFilter());
        execs.add(new ClientStageSendEventNamedWindow());
        execs.add(new ClientStageSendEventPatternWFollowedBy());
        execs.add(new ClientStageSendEventSubquery());
        execs.add(new ClientStageSendEventContextCategory());
        execs.add(new ClientStageSendEventContextKeyed());
        execs.add(new ClientStageSendEventContextKeyedWithInitiated());
        execs.add(new ClientStageSendEventContextKeyedWithTerminated());
        execs.add(new ClientStageSendEventContextHash());
        execs.add(new ClientStageSendEventContextStartNoEnd());
        execs.add(new ClientStageSendEventContextStartWithEndFilter());
        execs.add(new ClientStageSendEventContextStartWithEndPattern());
        execs.add(new ClientStageSendEventContextInitiatedNoTerminated());
        execs.add(new ClientStageSendEventContextNestedCategoryOverKeyed());
        execs.add(new ClientStageSendEventContextNestedPartitionedOverStart());
        execs.add(new ClientStageSendEventContextNestedInitiatedOverKeyed());
        execs.add(new ClientStageSendEventContextNestedHashOverHash());
        execs.add(new ClientStageSendEventPatternWAnd());
        execs.add(new ClientStageSendEventPatternWEvery());
        execs.add(new ClientStageSendEventPatternWEveryDistinct());
        execs.add(new ClientStageSendEventPatternWOr());
        execs.add(new ClientStageSendEventPatternWGuard());
        execs.add(new ClientStageSendEventPatternWNot());
        execs.add(new ClientStageSendEventPatternWUntil());
        execs.add(new ClientStageSendEventUpdateIStream());
        return execs;
    }

    private static class ClientStageSendEventUpdateIStream implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "update istream SupportBean set intPrimitive = -1;\n" +
                "@name('s0') select * from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEvent(env, null, "E1", 10);
            env.assertEqualsNew("s0", "intPrimitive", -1);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEvent(env, "ST", "E2", 20);
            assertEquals(-1, env.listenerStage("ST", "s0").assertOneGetNewAndReset().get("intPrimitive"));

            unstageIt(env, "ST", deploymentId);

            env.milestone(1);

            sendEvent(env, null, "E3", 30);
            env.assertEqualsNew("s0", "intPrimitive", -1);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventSubquery implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select (select sum(id) from SupportBean_S0) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEventS0(env, null, 10);
            sendEventAssertSum(env, null, "E1", 0, 10);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEventAssertSum(env, "ST", "E2", 0, 10);
            sendEventS0(env, "ST", 20);
            sendEventS0(env, null, 21);
            sendEventAssertSum(env, "ST", "E3", 0, 10 + 20);

            unstageIt(env, "ST", deploymentId);

            env.milestone(1);

            sendEventAssertSum(env, null, "E4", 0, 10 + 20);
            sendEventS0(env, "ST", 30);
            sendEventS0(env, null, 31);
            sendEventAssertSum(env, null, "E5", 0, 10 + 20 + 31);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextStartWithEndPattern implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext start SupportBean_S0 end pattern [SupportBean_S1(id=100) -> SupportBean_S1(id=200)];\n" +
                    "@name('s0') context MyContext select sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEventS0(env, null, 10);
            sendEventS1(env, null, 100);
            sendEventAssertSum(env, null, "E1", 10, 10);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEventS1(env, null, 200);
            sendEventAssertSum(env, "ST", "E2", 20, 10 + 20);
            sendEventS1(env, "ST", 200);
            sendEventAssertNoOutput(env, "ST", "E3", -1);

            env.milestone(1);

            sendEventAssertNoOutput(env, "ST", "E4", -1);
            sendEventS0(env, "ST", 20);
            sendEventAssertSum(env, "ST", "E4", 30, 30);

            unstageIt(env, "ST", deploymentId);

            env.milestone(2);

            sendEventAssertSum(env, null, "E4", 40, 30 + 40);
            sendEventS1(env, null, 100);
            sendEventS1(env, "ST", 200);
            sendEventAssertSum(env, null, "E5", 41, 30 + 40 + 41);
            sendEventS1(env, null, 200);
            sendEventAssertNoOutput(env, null, "E6", -1);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextKeyedWithTerminated implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext partition by theString from SupportBean, p00 from SupportBean_S0 " +
                    "terminated by SupportBean_S0;\n" +
                    "@name('s0') context MyContext select sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEventAssertSum(env, null, "A", 10, 10);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEventAssertSum(env, "ST", "A", 20, 10 + 20);
            env.sendEventBeanStage("ST", new SupportBean_S0(100, "A"));
            sendEventAssertSum(env, "ST", "A", 21, 21);

            env.milestone(1);

            sendEventAssertSum(env, "ST", "B", 30, 30);
            env.sendEventBeanStage("ST", new SupportBean_S0(101, "B"));
            sendEventAssertSum(env, "ST", "A", 31, 21 + 31);

            unstageIt(env, "ST", deploymentId);

            env.milestone(2);

            sendEventAssertSum(env, null, "A", 40, 21 + 31 + 40);
            sendEventAssertSum(env, null, "B", 41, 41);
            env.sendEventBeanStage(null, new SupportBean_S0(102, "A"));
            sendEventAssertSum(env, null, "A", 42, 42);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextKeyedWithInitiated implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext partition by theString from SupportBean initiated by SupportBean(intPrimitive=1);\n" +
                    "@name('s0') context MyContext select sum(longPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEventAssertSum(env, null, "A", 1, 10, 10);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEventAssertSum(env, "ST", "A", 0, 20, 10 + 20);
            sendEventAssertNoOutput(env, "ST", "B", 0);

            env.milestone(1);

            sendEventAssertSum(env, "ST", "A", 0, 30, 10 + 20 + 30);
            sendEventAssertSum(env, "ST", "B", 1, 31, 31);
            sendEventAssertNoOutput(env, "ST", "C", 0);

            unstageIt(env, "ST", deploymentId);

            env.milestone(2);

            sendEventAssertSum(env, null, "B", 0, 40, 31 + 40);
            sendEventAssertSum(env, null, "A", 0, 41, 10 + 20 + 30 + 41);
            sendEventAssertNoOutput(env, null, "C", 0);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextStartWithEndFilter implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext start SupportBean_S0 end SupportBean_S1;\n" +
                    "@name('s0') context MyContext select sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEventS0(env, null, 100);
            sendEventAssertSum(env, null, "E1", 10, 10);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEventAssertSum(env, "ST", "E2", 20, 10 + 20);
            sendEventS1(env, null, 101);
            sendEventAssertSum(env, "ST", "E3", 21, 10 + 20 + 21);
            sendEventS1(env, "ST", 102);
            sendEventAssertNoOutput(env, "ST", "E4", 22);

            env.milestone(1);

            sendEventAssertNoOutput(env, "ST", "E5", 30);
            sendEventS0(env, null, 103);
            sendEventAssertNoOutput(env, "ST", "E6", 31);
            sendEventS0(env, "ST", 104);
            sendEventAssertSum(env, "ST", "E7", 32, 32);

            unstageIt(env, "ST", deploymentId);

            env.milestone(2);

            sendEventAssertSum(env, null, "E8", 40, 32 + 40);
            sendEventS1(env, null, 105);
            sendEventAssertNoOutput(env, "ST", null, "E9", 41, 0);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextNestedHashOverHash implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext \n" +
                    "  context MyContextA coalesce by consistent_hash_crc32(theString) from SupportBean granularity 16,\n" +
                    "  context MyContextB coalesce by consistent_hash_crc32(intPrimitive) from SupportBean granularity 16;\n" +
                    "@name('s0') context MyContext select sum(longPrimitive) as thesum from SupportBean group by theString, intPrimitive;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            sendEventAssertSum(env, null, "A", 1, 10, 10);

            stageIt(env, "ST", deploymentId);

            env.milestone(0);

            sendEventAssertSum(env, "ST", "A", 2, 11, 11);
            sendEventAssertSum(env, "ST", "B", 1, 12, 12);
            sendEventAssertSum(env, "ST", "A", 1, 13, 10 + 13);

            env.milestone(2);

            sendEventAssertSum(env, "ST", "A", 2, 20, 11 + 20);
            sendEventAssertSum(env, "ST", "B", 1, 21, 12 + 21);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEventAssertSum(env, null, "A", 1, 30, 10 + 13 + 30);
            sendEventAssertSum(env, null, "A", 2, 31, 11 + 20 + 31);
            sendEventAssertSum(env, null, "B", 1, 32, 12 + 21 + 32);
            sendEventAssertSum(env, null, "C", 3, 33, 33);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextNestedInitiatedOverKeyed implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext \n" +
                    "  context MyContextA initiated by SupportBean_S0 as e1,\n" +
                    "  context MyContextB partition by theString from SupportBean;\n" +
                    "@name('s0') context MyContext select context.MyContextA.e1.id as c0, context.MyContextB.key1 as c1," +
                    "  sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");
            String[] fields = "c0,c1,thesum".split(",");

            env.milestone(0);

            sendEventS0(env, null, 1000);
            sendEvent(env, null, "A", 10);
            assertProps(env.listenerStage(null, "s0").assertOneGetNewAndReset(), fields, new Object[]{1000, "A", 10});

            stageIt(env, "ST", deploymentId);

            env.milestone(1);

            sendEvent(env, "ST", "A", 20);
            assertPropsPerRow(env.listenerStage("ST", "s0").getAndResetLastNewData(), fields, new Object[][]{{1000, "A", 10 + 20}});
            sendEvent(env, "ST", "B", 21);
            assertPropsPerRow(env.listenerStage("ST", "s0").getAndResetLastNewData(), fields, new Object[][]{{1000, "B", 21}});

            env.milestone(2);

            sendEventS0(env, "ST", 2000);
            sendEvent(env, "ST", "A", 30);
            assertPropsPerRow(env.listenerStage("ST", "s0").getAndResetLastNewData(), fields, new Object[][]{{1000, "A", 10 + 20 + 30}, {2000, "A", 30}});

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEvent(env, null, "A", 40);
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{1000, "A", 10 + 20 + 30 + 40}, {2000, "A", 30 + 40}});

            sendEventS0(env, null, 3000);

            sendEvent(env, null, "B", 41);
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{1000, "B", 21 + 41}, {2000, "B", 41}, {3000, "B", 41}});

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextNestedPartitionedOverStart implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext \n" +
                    "  context MyContextCategory partition by theString from SupportBean,\n" +
                    "  context MyContextPartitioned start SupportBean(intPrimitive=1);\n" +
                    "@name('s0') context MyContext select sum(longPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            env.milestone(0);

            sendEventAssertSum(env, null, "A", 1, 10, 10);
            sendEventAssertNoOutput(env, null, null, "B", 0, 99);

            stageIt(env, "ST", deploymentId);

            sendEventAssertSum(env, "ST", "A", 0, 20, 30);
            sendEventAssertNoOutput(env, "ST", "ST", "C", 0, 99);
            sendEventAssertSum(env, "ST", "B", 1, 21, 21);
            sendEventAssertNoOutput(env, null, "ST", "D", 1, 99);

            env.milestone(1);

            sendEventAssertSum(env, "ST", "B", 0, 30, 21 + 30);
            sendEventAssertSum(env, "ST", "A", 0, 31, 10 + 20 + 31);

            env.milestone(2);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEventAssertNoOutput(env, "ST", null, "C", 1, -1);
            sendEventAssertNoOutput(env, null, null, "D", 0, 99);
            sendEventAssertSum(env, null, "A", 0, 41, 10 + 20 + 31 + 41);
            sendEventAssertSum(env, null, "B", 0, 42, 21 + 30 + 42);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextNestedCategoryOverKeyed implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext \n" +
                    "  context MyContextCategory group by theString = 'A' as grp1, group by theString = 'B' as grp2 from SupportBean,\n" +
                    "  context MyContextPartitioned partition by intPrimitive from SupportBean;\n" +
                    "@name('s0') context MyContext select sum(longPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            env.milestone(0);

            sendEventAssertSum(env, null, "A", 1000, 10, 10);
            sendEventAssertNoOutput(env, null, "C", 99);

            stageIt(env, "ST", deploymentId);

            sendEventAssertSum(env, "ST", "A", 1000, 11, 21);
            sendEventAssertNoOutput(env, "ST", "C", 1000);
            sendEventAssertSum(env, "ST", "B", 2000, 12, 12);
            sendEventAssertSum(env, "ST", "B", 2001, 13, 13);

            env.milestone(1);

            sendEventAssertSum(env, "ST", "B", 2000, 14, 26);

            env.milestone(2);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEventAssertNoOutput(env, "ST", null, "A", 1000, -1);
            sendEventAssertSum(env, null, "A", 1000, 20, 41);
            sendEventAssertNoOutput(env, null, "C", 1000);
            sendEventAssertSum(env, null, "B", 2000, 21, 12 + 14 + 21);
            sendEventAssertSum(env, null, "B", 2001, 22, 13 + 22);
            sendEventAssertSum(env, null, "A", 1001, 23, 23);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextInitiatedNoTerminated implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext initiated by SupportBean(theString='init') as sb;\n" +
                    "@name('s0') context MyContext select context.sb.intPrimitive as c0, sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            String[] fields = new String[]{"c0", "thesum"};

            sendEvent(env, null, "init", 100);
            env.assertPropsNew("s0", fields, new Object[]{100, 100});

            sendEvent(env, null, "x", 101);
            env.assertPropsNew("s0", fields, new Object[]{100, 201});

            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentId);

            sendEvent(env, "ST", "y", 200);
            assertProps(env.listenerStage("ST", "s0").assertOneGetNewAndReset(), fields, new Object[]{100, 401});

            env.milestone(0);

            sendEvent(env, "ST", "init", 300);
            assertPropsPerRowAnyOrder(env.listenerStage("ST", "s0").getAndResetLastNewData(), fields, new Object[][]{{100, 701}, {300, 300}});

            env.milestone(2);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEvent(env, null, "z", 400);
            env.assertPropsPerRowLastNewAnyOrder("s0", fields, new Object[][]{{100, 1101}, {300, 700}});

            sendEvent(env, null, "init", 401);
            env.assertPropsPerRowLastNewAnyOrder("s0", fields, new Object[][]{{100, 1101 + 401}, {300, 700 + 401}, {401, 401}});

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextStartNoEnd implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext start SupportBean(theString='start');\n" +
                    "@name('s0') context MyContext select sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            sendEventAssertNoOutput(env, null, "E1", 1);

            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentId);

            sendEventAssertNoOutput(env, null, "ST", "E2", 1, 0);

            env.milestone(0);

            sendEventAssertSum(env, "ST", "start", 10, 10);
            sendEventAssertSum(env, "ST", "E3", 11, 21);

            env.milestone(2);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEventAssertSum(env, null, "E4", 12, 33);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextHash implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext coalesce by consistent_hash_crc32(theString) from SupportBean granularity 16;\n" +
                    "@name('s0') context MyContext select theString, sum(intPrimitive) as thesum from SupportBean group by theString;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            sendEventAssertSum(env, null, "A", 10, 10);

            env.milestone(0);

            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentId);

            env.milestone(1);

            sendEventAssertSum(env, "ST", "A", 11, 21);
            sendEventAssertSum(env, "ST", "B", 12, 12);

            env.milestone(2);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEventAssertSum(env, null, "A", 13, 34);
            sendEventAssertSum(env, null, "B", 14, 26);
            sendEventAssertSum(env, null, "C", 15, 15);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextCategory implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create context MyContext group by theString='A' as grp1, group by theString='B' as grp2 from SupportBean;\n" +
                    "@name('s0') context MyContext select sum(intPrimitive) as thesum from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            sendEventAssertSum(env, null, "A", 10, 10);

            env.milestone(0);

            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentId);

            env.milestone(1);

            sendEvent(env, null, "A", 11);
            sendEventAssertSum(env, "ST", "A", 12, 22);
            sendEventAssertSum(env, "ST", "B", 13, 13);

            env.milestone(2);

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            sendEvent(env, "ST", "A", 14);
            sendEvent(env, null, "C", 15);
            sendEventAssertSum(env, null, "A", 16, 38);
            sendEventAssertSum(env, null, "B", 17, 30);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventContextKeyed implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('context') @public create context MyContext partition by theString from SupportBean", path);
            env.compileDeploy("@name('s0') context MyContext select sum(intPrimitive) as thesum from SupportBean", path).addListener("s0");
            String deploymentIdContext = env.deploymentId("context");
            String deploymentIdStmt = env.deploymentId("s0");

            sendEventAssertSum(env, null, "A", 1, 1);
            sendEventAssertSum(env, null, "A", 2, 3);

            env.milestone(0);

            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentIdContext, deploymentIdStmt);

            env.milestone(1);

            sendEvent(env, null, "A", 3);
            sendEventAssertSum(env, "ST", "A", 4, 1 + 2 + 4);
            sendEventAssertSum(env, "ST", "B", 10, 10);

            env.milestone(2);

            unstageIt(env, "ST", deploymentIdContext, deploymentIdStmt);

            env.milestone(3);

            sendEvent(env, "ST", "A", 5);
            sendEventAssertSum(env, null, "A", 6, 1 + 2 + 4 + 6);
            sendEventAssertSum(env, null, "C", 20, 20);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventPatternWEvery implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[every SupportBean]";
            runAssertionPatternEvery(env, epl);
        }
    }

    private static class ClientStageSendEventPatternWEveryDistinct implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[every-distinct(a.theString) a=SupportBean]";
            runAssertionPatternEvery(env, epl);
        }
    }

    private static class ClientStageSendEventPatternWOr implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[SupportBean(theString='a') or SupportBean(theString='b')]";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            stageIt(env, "ST", deploymentId);

            sendEvent(env, "ST", "a");
            env.listenerStage("ST", "s0").assertOneGetNewAndReset();

            unstageIt(env, "ST", deploymentId);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventPatternWNot implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[SupportBean(theString='a') and not SupportBean(theString='b')]";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            stageIt(env, "ST", deploymentId);

            sendEvent(env, "ST", "b");
            sendEvent(env, "ST", "a");
            assertFalse(env.listenerStage("ST", "s0").getAndClearIsInvoked());

            unstageIt(env, "ST", deploymentId);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventPatternWUntil implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[SupportBean(theString='a') until SupportBean(theString='b')]";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            stageIt(env, "ST", deploymentId);

            sendEvent(env, "ST", "b");
            sendEvent(env, "ST", "a");
            assertTrue(env.listenerStage("ST", "s0").getAndClearIsInvoked());

            unstageIt(env, "ST", deploymentId);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventPatternWGuard implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[SupportBean(theString='a') where timer:within(10 sec)]";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");

            stageIt(env, "ST", deploymentId);

            sendEvent(env, "ST", "a");
            env.listenerStage("ST", "s0").assertOneGetNewAndReset();

            unstageIt(env, "ST", deploymentId);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventPatternWAnd implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[SupportBean(theString='a') and SupportBean(theString='b')]";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            sendEvent(env, null, "a");
            env.stageService().getStage("ST");

            env.milestone(0);

            stageIt(env, "ST", deploymentId);
            sendEvent(env, "ST", "b");
            env.listenerStage("ST", "s0").assertOneGetNewAndReset();

            unstageIt(env, "ST", deploymentId);

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventPatternWFollowedBy implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[SupportBean(theString='a') -> SupportBean(theString='b') ->" +
                "SupportBean(theString='c') -> SupportBean(theString='d') -> SupportBean(theString='e')]";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            sendEvent(env, null, "a");
            env.stageService().getStage("ST");

            env.milestone(0);

            stageIt(env, "ST", deploymentId);
            sendEvent(env, "ST", "b");

            env.milestone(1);

            unstageIt(env, "ST", deploymentId);

            env.milestone(2);

            sendEvent(env, null, "c");

            env.milestone(3);

            stageIt(env, "ST", deploymentId);
            sendEvent(env, "ST", "d");

            env.milestone(4);

            unstageIt(env, "ST", deploymentId);

            env.milestone(5);

            sendEvent(env, null, "e");
            env.assertEventNew("s0", event -> {
            });

            env.undeployAll();
        }
    }

    private static class ClientStageSendEventNamedWindow implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create window MyWindow#keepall as SupportBean;\n" +
                    "insert into MyWindow select * from SupportBean;\n" +
                    "@name('s0') select sum(intPrimitive) as c0 from MyWindow;\n";
            runAssertionSimple(env, epl);
        }
    }

    private static class ClientStageSendEventFilter implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionSimple(env, "@name('s0') select sum(intPrimitive) as c0 from SupportBean");
        }
    }

    private static void runAssertionSimple(RegressionEnvironment env, String epl) {
        env.compileDeploy(epl).addListener("s0");
        String deploymentId = env.deploymentId("s0");

        sendEvent(env, null, "E1", 10);
        assertTotal(env, null, 10);

        env.milestone(0);

        env.stageService().getStage("P1");
        stageIt(env, "P1", deploymentId);

        env.milestone(1);

        assertNull(env.deployment().getDeployment(deploymentId));
        assertNotNull(env.stageService().getExistingStage("P1").getDeploymentService().getDeployment(deploymentId));
        assertEqualsAnyOrder(new String[]{deploymentId}, env.stageService().getExistingStage("P1").getDeploymentService().getDeployments());
        sendEvent(env, null, "E3", 21);
        sendEvent(env, "P1", "E4", 22);
        assertTotal(env, "P1", 10 + 22);

        env.milestone(2);

        unstageIt(env, "P1", deploymentId);

        env.milestone(3);

        assertNotNull(env.deployment().getDeployment(deploymentId));
        assertNull(env.stageService().getExistingStage("P1").getDeploymentService().getDeployment(deploymentId));
        sendEvent(env, null, "E5", 31);
        sendEvent(env, "P1", "E6", 32);
        assertTotal(env, null, 10 + 22 + 31);
        env.assertThat(() -> {
            SupportListener listener = env.listener("s0");

            env.undeployAll();

            sendEvent(env, null, "end", 99);
            assertFalse(listener.getAndClearIsInvoked());
        });
        env.undeployAll();
    }

    private static void runAssertionPatternEvery(RegressionEnvironment env, String epl) {
        env.compileDeploy(epl).addListener("s0");
        String deploymentId = env.deploymentId("s0");
        env.stageService().getStage("ST");

        stageIt(env, "ST", deploymentId);

        sendEvent(env, "ST", "E1");
        env.listenerStage("ST", "s0").assertOneGetNewAndReset();

        unstageIt(env, "ST", deploymentId);

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, String stageUri, String theString) {
        sendEvent(env, stageUri, theString, -1);
    }

    private static void sendEvent(RegressionEnvironment env, String stageUri, String theString, int intPrimitive) {
        sendEvent(env, stageUri, theString, intPrimitive, -1);
    }

    private static void sendEvent(RegressionEnvironment env, String stageUri, String theString, int intPrimitive, long longPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBeanStage(stageUri, sb);
    }

    private static void sendEventS0(RegressionEnvironment env, String stageUri, int id) {
        env.sendEventBeanStage(stageUri, new SupportBean_S0(id));
    }

    private static void sendEventS1(RegressionEnvironment env, String stageUri, int id) {
        env.sendEventBeanStage(stageUri, new SupportBean_S1(id));
    }

    private static void sendEventAssertNoOutput(RegressionEnvironment env, String stageUri, String theString, int intPrimitive) {
        sendEvent(env, stageUri, theString, intPrimitive);
        assertFalse(env.listenerStage(stageUri, "s0").getAndClearIsInvoked());
    }

    private static void sendEventAssertNoOutput(RegressionEnvironment env, String stageSendEvent, String stageListener, String theString, int intPrimitive, long longPrimitive) {
        sendEvent(env, stageSendEvent, theString, intPrimitive, longPrimitive);
        assertFalse(env.listenerStage(stageListener, "s0").getAndClearIsInvoked());
    }

    private static void assertTotal(RegressionEnvironment env, String stageUri, int total) {
        assertProps(env.listenerStage(stageUri, "s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{total});
    }

    private static void sendEventAssertSum(RegressionEnvironment env, String stageUri, String theString, int intPrimitive, int expected) {
        env.sendEventBeanStage(stageUri, new SupportBean(theString, intPrimitive));
        assertEquals(expected, env.listenerStage(stageUri, "s0").assertOneGetNewAndReset().get("thesum"));
    }

    private static void sendEventAssertSum(RegressionEnvironment env, String stageUri, String theString, int intPrimitive, long longPrimitive, long expected) {
        sendEvent(env, stageUri, theString, intPrimitive, longPrimitive);
        assertEquals(expected, env.listenerStage(stageUri, "s0").assertOneGetNewAndReset().get("thesum"));
    }
}
