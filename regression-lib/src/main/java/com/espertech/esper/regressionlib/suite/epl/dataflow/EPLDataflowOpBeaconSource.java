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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLDataflowOpBeaconSource {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowBeaconWithBeans());
        execs.add(new EPLDataflowBeaconVariable());
        execs.add(new EPLDataflowBeaconFields());
        execs.add(new EPLDataflowBeaconNoType());
        return execs;
    }

    private static class EPLDataflowBeaconWithBeans implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPLDataflowOpBeaconSource.MyLegacyEvent resultLegacy = (EPLDataflowOpBeaconSource.MyLegacyEvent) runAssertionBeans(env, "MyLegacyEvent");
            assertEquals("abc", resultLegacy.getMyfield());

            MyEventNoDefaultCtor resultNoDefCtor = (MyEventNoDefaultCtor) runAssertionBeans(env, "MyEventNoDefaultCtor");
            assertEquals("abc", resultNoDefCtor.getMyfield());
        }
    }

    private static class EPLDataflowBeaconVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create Schema SomeEvent()", path);
            env.compileDeploy("create variable int var_iterations=3", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream<SomeEvent> {" +
                "  iterations : var_iterations" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}", path);

            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>(3);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            df.start();
            Object[] output;
            try {
                output = future.get(2, TimeUnit.SECONDS);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            assertEquals(3, output.length);
            env.undeployAll();
        }
    }

    private static class EPLDataflowBeaconFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : new EventRepresentationChoice[]{EventRepresentationChoice.AVRO}) {
                runAssertionFields(env, rep, true);
                runAssertionFields(env, rep, false);
            }

            // test doc samples
            String epl = "@name('flow') create dataflow MyDataFlow\n" +
                "  create schema SampleSchema(tagId string, locX double, locY double)," +
                "  " +
                "  // BeaconSource that produces empty object-array events without delay or interval\n" +
                "  // until cancelled.\n" +
                "  BeaconSource -> stream.one {}\n" +
                "  \n" +
                "  // BeaconSource that produces one RFIDSchema event populating event properties\n" +
                "  // from a user-defined function \"generateTagId\" and values.\n" +
                "  BeaconSource -> stream.two<SampleSchema> {\n" +
                "    iterations : 1,\n" +
                "    tagId : generateTagId(),\n" +
                "    locX : 10,\n" +
                "    locY : 20 \n" +
                "  }\n" +
                "  \n" +
                "  // BeaconSource that produces 10 object-array events populating the price property \n" +
                "  // with a random value.\n" +
                "  BeaconSource -> stream.three {\n" +
                "    iterations : 1,\n" +
                "    interval : 10, // every 10 seconds\n" +
                "    initialDelay : 5, // start after 5 seconds\n" +
                "    price : Math.random() * 100,\n" +
                "  }";
            env.compileDeploy(epl);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow");
            env.undeployAll();

            // test options-provided beacon field
            String eplMinimal = "@name('flow') create dataflow MyGraph " +
                "BeaconSource -> outstream<SupportBean> {iterations:1} " +
                "EventBusSink(outstream) {}";
            env.compileDeploy(eplMinimal);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
            options.addParameterURI("BeaconSource/theString", "E1");
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyGraph", options);

            env.compileDeploy("@name('s0') select * from SupportBean").addListener("s0");
            instance.run();
            sleep(200);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

            // invalid: no output stream
            tryInvalidCompile(env, "create dataflow DF1 BeaconSource {}",
                "Failed to obtain operator 'BeaconSource': BeaconSource operator requires one output stream but produces 0 streams");

            env.undeployAll();
        }
    }

    private static void runAssertionFields(RegressionEnvironment env, EventRepresentationChoice representationEnum, boolean eventbean) {
        EPDataFlowInstantiationOptions options;

        RegressionPath path = new RegressionPath();
        env.compileDeploy(representationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEvent.class) + "create schema MyEvent(p0 string, p1 long, p2 double)", path);
        env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
            "" +
            "BeaconSource -> BeaconStream<" + (eventbean ? "EventBean<MyEvent>" : "MyEvent") + "> {" +
            "  iterations : 3," +
            "  p0 : 'abc'," +
            "  p1 : Math.round(Math.random() * 10) + 1," +
            "  p2 : 1d," +
            "}" +
            "DefaultSupportCaptureOp(BeaconStream) {}", path);

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>(3);
        options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
        df.start();
        Object[] output;
        try {
            output = future.get(2, TimeUnit.SECONDS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        assertEquals(3, output.length);
        for (int i = 0; i < 3; i++) {

            if (!eventbean) {
                if (representationEnum.isObjectArrayEvent()) {
                    Object[] row = (Object[]) output[i];
                    assertEquals("abc", row[0]);
                    long val = (Long) row[1];
                    assertTrue("val=" + val, val >= 0 && val <= 11);
                    assertEquals(1d, row[2]);
                } else if (representationEnum.isMapEvent()) {
                    Map row = (Map) output[i];
                    assertEquals("abc", row.get("p0"));
                    long val = (Long) row.get("p1");
                    assertTrue("val=" + val, val >= 0 && val <= 11);
                    assertEquals(1d, row.get("p2"));
                } else {
                    GenericData.Record row = (GenericData.Record) output[i];
                    assertEquals("abc", row.get("p0"));
                    long val = (Long) row.get("p1");
                    assertTrue("val=" + val, val >= 0 && val <= 11);
                    assertEquals(1d, row.get("p2"));
                }
            } else {
                EventBean row = (EventBean) output[i];
                assertEquals("abc", row.get("p0"));
            }
        }

        env.undeployAll();
    }

    private static class EPLDataflowBeaconNoType implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPDataFlowInstantiationOptions options;
            Object[] output;

            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

            int countExpected = 10;
            DefaultSupportCaptureOp<Object> futureAtLeast = new DefaultSupportCaptureOp<>(countExpected);
            options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureAtLeast));
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            df.start();
            try {
                output = futureAtLeast.get(1, TimeUnit.SECONDS);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            assertTrue(countExpected <= output.length);
            df.cancel();
            env.undeployAll();

            // BeaconSource with given number of iterations
            env.compileDeploy("@name('flow') create dataflow MyDataFlowTwo " +
                "BeaconSource -> BeaconStream {" +
                "  iterations: 5" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

            DefaultSupportCaptureOp<Object> futureExactTwo = new DefaultSupportCaptureOp<>(5);
            options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureExactTwo));
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowTwo", options).start();
            try {
                output = futureExactTwo.get(1, TimeUnit.SECONDS);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            assertEquals(5, output.length);
            env.undeployAll();

            // BeaconSource with delay
            env.compileDeploy("@name('flow') create dataflow MyDataFlowThree " +
                "BeaconSource -> BeaconStream {" +
                "  iterations: 2," +
                "  initialDelay: 0.5" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

            DefaultSupportCaptureOp<Object> futureExactThree = new DefaultSupportCaptureOp<>(2);
            options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureExactThree));
            long start = System.currentTimeMillis();
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowThree", options).start();
            try {
                output = futureExactThree.get(1, TimeUnit.SECONDS);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            long end = System.currentTimeMillis();
            assertEquals(2, output.length);
            assertTrue("delta=" + (end - start), end - start > 490);
            env.undeployAll();

            // BeaconSource with period
            env.compileDeploy("@name('flow') create dataflow MyDataFlowFour " +
                "BeaconSource -> BeaconStream {" +
                "  interval: 0.5" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");
            DefaultSupportCaptureOp<Object> futureFour = new DefaultSupportCaptureOp<>(2);
            options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureFour));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowFour", options);
            instance.start();
            try {
                output = futureFour.get(2, TimeUnit.SECONDS);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            assertEquals(2, output.length);
            instance.cancel();
            env.undeployAll();

            // test Beacon with define typed
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create objectarray schema MyTestOAType(p1 string)", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowFive " +
                "BeaconSource -> BeaconStream<MyTestOAType> {" +
                "  interval: 0.5," +
                "  p1 : 'abc'" +
                "}", path);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowFive");
            env.undeployAll();
        }
    }

    private static Object runAssertionBeans(RegressionEnvironment env, String typeName) {
        env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
            "" +
            "BeaconSource -> BeaconStream<" + typeName + "> {" +
            "  myfield : 'abc', iterations : 1" +
            "}" +
            "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
        df.start();
        Object[] output = new Object[0];
        try {
            output = future.get(2, TimeUnit.SECONDS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        assertEquals(1, output.length);
        env.undeployAll();
        return output[0];
    }

    public static String generateTagId() {
        return "";
    }

    public static class MyEventNoDefaultCtor {
        private String myfield;

        public MyEventNoDefaultCtor(String someOtherfield, int someOtherValue) {
        }

        public String getMyfield() {
            return myfield;
        }

        public void setMyfield(String myfield) {
            this.myfield = myfield;
        }
    }

    public static class MyLegacyEvent implements Serializable {
        private String myfield;

        public MyLegacyEvent() {
        }

        public String getMyfield() {
            return myfield;
        }

        public void setMyfield(String myfield) {
            this.myfield = myfield;
        }
    }

    public static class MyLocalJsonProvidedMyEvent implements Serializable {
        public String p0;
        public long p1;
        public double p2;
    }
}
