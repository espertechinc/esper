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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphParamProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.dataflow.SupportDataFlowAssertionUtil;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.fail;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EPLDataflowOpEPStatementSource {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowAllTypes());
        execs.add(new EPLDataflowStmtNameDynamic());
        execs.add(new EPLDataflowStatementFilter());
        execs.add(new EPLDataflowInvalid());
        return execs;
    }

    private static class EPLDataflowStmtNameDynamic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "create map schema SingleProp (id string), " +
                "EPStatementSource -> thedata<SingleProp> {" +
                "  statementDeploymentId : 'MyDeploymentId'," +
                "  statementName : 'MyStatement'," +
                "} " +
                "DefaultSupportCaptureOp(thedata) {}");

            DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(captureOp));

            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            assertNull(df.getUserObject());
            assertNull(df.getInstanceId());
            df.start();

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(0, captureOp.getCurrent().length);

            String epl = "@Name('MyStatement') select theString as id from SupportBean";
            EPCompiled compiled = env.compile(epl);
            try {
                env.deployment().deploy(compiled, new DeploymentOptions().setDeploymentId("MyDeploymentId"));
            } catch (EPDeployException e) {
                fail(e.getMessage());
            }

            env.sendEventBean(new SupportBean("E2", 2));
            captureOp.waitForInvocation(100, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"E2"});

            env.undeployModuleContaining("MyStatement");

            env.sendEventBean(new SupportBean("E3", 3));
            assertEquals(0, captureOp.getCurrent().length);

            try {
                env.deployment().deploy(compiled, new DeploymentOptions().setDeploymentId("MyDeploymentId"));
            } catch (EPDeployException e) {
                fail(e.getMessage());
            }

            env.sendEventBean(new SupportBean("E4", 4));
            captureOp.waitForInvocation(100, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"E4"});

            env.undeployModuleContaining("MyStatement");

            env.sendEventBean(new SupportBean("E5", 5));
            assertEquals(0, captureOp.getCurrent().length);

            compiled = env.compile("@Name('MyStatement') select 'X'||theString||'X' as id from SupportBean");
            try {
                env.deployment().deploy(compiled, new DeploymentOptions().setDeploymentId("MyDeploymentId"));
            } catch (EPDeployException e) {
                fail(e.getMessage());
            }

            env.sendEventBean(new SupportBean("E6", 6));
            captureOp.waitForInvocation(100, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"XE6X"});

            df.cancel();
            env.undeployAll();
        }
    }

    private static class EPLDataflowAllTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionStatementNameExists(env, DefaultSupportGraphEventUtil.EVENTTYPENAME, DefaultSupportGraphEventUtil.getPOJOEvents());
            runAssertionStatementNameExists(env, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
            runAssertionStatementNameExists(env, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
            runAssertionStatementNameExists(env, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());

            // test doc samples
            String epl = "@name('flow') create dataflow MyDataFlow\n" +
                "  create schema SampleSchema(tagId string, locX double),\t// sample type\t\t\t\n" +
                "\t\t\t\n" +
                "  // Consider only the statement named MySelectStatement when it exists.\n" +
                "  EPStatementSource -> stream.one<eventbean<?>> {\n" +
                "    statementDeploymentId : 'MyDeploymentABC',\n" +
                "    statementName : 'MySelectStatement'\n" +
                "  }\n" +
                "  \n" +
                "  // Consider all statements that match the filter object provided.\n" +
                "  EPStatementSource -> stream.two<eventbean<?>> {\n" +
                "    statementFilter : {\n" +
                "      class : '" + MyFilter.class.getName() + "'\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  // Consider all statements that match the filter object provided.\n" +
                "  // With collector that performs transformation.\n" +
                "  EPStatementSource -> stream.two<SampleSchema> {\n" +
                "    collector : {\n" +
                "      class : '" + MyCollector.class.getName() + "'\n" +
                "    },\n" +
                "    statementFilter : {\n" +
                "      class : '" + MyFilter.class.getName() + "'\n" +
                "    }\n" +
                "  }";
            env.compileDeploy(epl);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow");

            env.undeployAll();
        }
    }

    private static class EPLDataflowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test no statement name or statement filter provided
            String epl = "create dataflow DF1 " +
                "create schema AllObjects as java.lang.Object," +
                "EPStatementSource -> thedata<AllObjects> {} " +
                "DefaultSupportCaptureOp(thedata) {}";
            SupportDataFlowAssertionUtil.tryInvalidInstantiate(env, "DF1", epl,
                "Failed to instantiate data flow 'DF1': Failed to obtain operator instance for 'EPStatementSource': Failed to find required 'statementName' or 'statementFilter' parameter");

            // invalid: no output stream
            tryInvalidCompile(env, "create dataflow DF1 EPStatementSource { statementName : 'abc' }",
                "Failed to obtain operator 'EPStatementSource': EPStatementSource operator requires one output stream but produces 0 streams");

            // invalid: no statement deployment id
            tryInvalidCompile(env, "create dataflow DF1 EPStatementSource ->abc { statementName : 'abc' }",
                "Failed to obtain operator 'EPStatementSource': Both 'statementDeploymentId' and 'statementName' are required when either of these are specified");
        }
    }

    private static class EPLDataflowStatementFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // one statement exists before the data flow
            env.compileDeploy("select id from SupportBean_B");

            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "create schema AllObjects as java.lang.Object," +
                "EPStatementSource -> thedata<AllObjects> {} " +
                "DefaultSupportCaptureOp(thedata) {}");

            DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
            MyFilter myFilter = new MyFilter();
            options.parameterProvider(new DefaultSupportGraphParamProvider(Collections.<String, Object>singletonMap("statementFilter", myFilter)));
            options.operatorProvider(new DefaultSupportGraphOpProvider(captureOp));
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            df.start();

            env.sendEventBean(new SupportBean_B("B1"));
            captureOp.waitForInvocation(200, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"B1"});

            env.compileDeploy("select theString, intPrimitive from SupportBean");
            env.sendEventBean(new SupportBean("E1", 1));
            captureOp.waitForInvocation(200, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "theString,intPrimitive".split(","), new Object[]{"E1", 1});

            env.compileDeploy("@name('s2') select id from SupportBean_A");
            env.sendEventBean(new SupportBean_A("A1"));
            captureOp.waitForInvocation(200, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"A1"});

            env.undeployModuleContaining("s2");

            env.sendEventBean(new SupportBean_A("A2"));
            sleep(50);
            assertEquals(0, captureOp.getCurrent().length);

            env.compileDeploy("@name('s2') select id from SupportBean_A");

            env.sendEventBean(new SupportBean_A("A3"));
            captureOp.waitForInvocation(200, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"A3"});

            env.sendEventBean(new SupportBean_B("B2"));
            captureOp.waitForInvocation(200, 1);
            EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"B2"});

            df.cancel();

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean_A("A1"));
            env.sendEventBean(new SupportBean_B("B3"));
            assertEquals(0, captureOp.getCurrent().length);

            env.undeployAll();
        }
    }

    private static void runAssertionStatementNameExists(RegressionEnvironment env, String typeName, Object[] events) {
        env.compileDeploy("@Name('MyStatement') select * from " + typeName);

        env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
            "create schema AllObject java.lang.Object," +
            "EPStatementSource -> thedata<AllObject> {" +
            "  statementDeploymentId : '" + env.deploymentId("MyStatement") + "'," +
            "  statementName : 'MyStatement'," +
            "} " +
            "DefaultSupportCaptureOp(thedata) {}");

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>(2);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(captureOp));

        EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
        df.start();

        EventSender sender = env.eventService().getEventSender(typeName);
        for (Object event : events) {
            sender.sendEvent(event);
        }

        try {
            captureOp.get(1, TimeUnit.SECONDS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        EPAssertionUtil.assertEqualsExactOrder(events, captureOp.getCurrent());

        df.cancel();
        env.undeployAll();
    }

    public static class MyFilter implements EPDataFlowEPStatementFilter {
        public boolean pass(EPDataFlowEPStatementFilterContext statement) {
            return true;
        }
    }

    public static class MyCollector implements EPDataFlowIRStreamCollector {
        public void collect(EPDataFlowIRStreamCollectorContext data) {
        }
    }
}
