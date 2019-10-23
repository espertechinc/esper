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
package com.espertech.esper.regressionlib.suite.expr.define;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum.NAME;
import static com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum.TYPE;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExprDefineValueParameter {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprDefineValueParameterV());
        execs.add(new ExprDefineValueParameterVV());
        execs.add(new ExprDefineValueParameterVVV());
        execs.add(new ExprDefineValueParameterEV());
        execs.add(new ExprDefineValueParameterVEV());
        execs.add(new ExprDefineValueParameterVEVE());
        execs.add(new ExprDefineValueParameterEVE());
        execs.add(new ExprDefineValueParameterEVEVE());
        execs.add(new ExprDefineValueParameterInvalid());
        execs.add(new ExprDefineValueParameterCache());
        execs.add(new ExprDefineValueParameterVariable());
        execs.add(new ExprDefineValueParameterSubquery());
        return execs;
    }

    private static class ExprDefineValueParameterSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') expression cc { (v1, v2) -> v1 || v2} " +
                "select cc((select p00 from SupportBean_S0#lastevent), (select p01 from SupportBean_S0#lastevent)) as c0 from SupportBean_S1";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S1(0));
            assertNull(env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression returnsSame {v -> v} select returnsSame(1) as c0 from SupportBean").addListener("s0");
            String[] fields = "c0".split(",");
            assertTypeExpected(env, Integer.class);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1});

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterVV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression cc { (v1, v2) -> v1 || v2} select cc(p00, p01) as c0 from SupportBean_S0").addListener("s0");
            assertTypeExpected(env, String.class);

            sendAssert(env, "AB", "A", "B");
            sendAssert(env, null, "A", null);
            sendAssert(env, null, null, "B");
            sendAssert(env, "CD", "C", "D");

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterVVV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression cc { (v1, v2, v3) -> v1 || v2 || v3} select cc(p00, p01, p02) as c0 from SupportBean_S0").addListener("s0");
            assertTypeExpected(env, String.class);

            sendAssert(env, "ABC", "A", "B", "C");
            sendAssert(env, null, "A", null, "C");
            sendAssert(env, "DEF", "D", "E", "F");

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterEV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression cc { (e,v) -> e.p00 || v} select cc(e, p01) as c0 from SupportBean_S0 as e").addListener("s0");
            assertTypeExpected(env, String.class);

            sendAssert(env, "AB", "A", "B");
            sendAssert(env, "BC", "B", "C");

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterVEV implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') expression cc { (v1,e,v2) -> v1 || e.p01 || v2} select cc(p00, e, p02) as c0 from SupportBean_S0 as e").addListener("s0");
            assertTypeExpected(env, String.class);

            sendAssert(env, "ABC", "A", "B", "C");
            sendAssert(env, null, null, "B", null);
            sendAssert(env, "BCD", "B", "C", "D");

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterVEVE implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "@name('s0') expression cc { (v1,e1,v2,e2) -> v1 || e1.p01 || v2 || e2.p11} " +
                "select cc(e1.p00, e1, e2.p10, e2) as c0 from SupportBean_S0#lastevent as e1, SupportBean_S1#lastevent as e2";
            assertJoin(env, epl);

            epl = "@name('s0') expression cc { (v1,e1,v2,e2) -> v1 || e1.p01 || v2 || e2.p11} " +
                "select cc(e1.p00, e1, e2.p10, e2) as c0 from SupportBean_S1#lastevent as e2, SupportBean_S0#lastevent as e1";
            assertJoin(env, epl);
        }

        private void assertJoin(RegressionEnvironment env, String epl) {
            env.compileDeploy(epl).addListener("s0");
            assertTypeExpected(env, String.class);

            env.sendEventBean(new SupportBean_S0(1, "A", "B"));
            env.sendEventBean(new SupportBean_S1(2, "X", "Y"));
            assertEquals("ABXY", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean_S1(2, "Z", "P"));
            assertEquals("ABZP", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean_S0(1, "D", "E"));
            assertEquals("DEZP", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterEVE implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') expression cc { (e1,v,e2) -> e1.p00 || v || e2.p10} " +
                "select cc(e2, 'x', e1) as c0 from SupportBean_S1#lastevent as e1, SupportBean_S0#lastevent as e2";
            env.compileDeploy(epl).addListener("s0");
            assertTypeExpected(env, String.class);

            env.sendEventBean(new SupportBean_S0(1, "A"));
            env.sendEventBean(new SupportBean_S1(2, "1"));
            assertEquals("Ax1", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean_S1(2, "2"));
            assertEquals("Ax2", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.sendEventBean(new SupportBean_S0(1, "B"));
            assertEquals("Bx2", env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterEVEVE implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            String expression = "@public create expression cc { (a,v1,b,v2,c) -> a.p00 || v1 || b.p00 || v2 || c.p00}";
            env.compileDeploy(expression, path);

            String epl =
                "@name('s0') select cc(e2, 'x', e3, 'y', e1) as c0 from \n" +
                    "SupportBean_S0(id=1)#lastevent as e1, SupportBean_S0(id=2)#lastevent as e2, SupportBean_S0(id=3)#lastevent as e3;\n" +
                    "@name('s1') select cc(e2, 'x', e3, 'y', e1) as c0 from \n" +
                    "SupportBean_S0(id=1)#lastevent as e3, SupportBean_S0(id=2)#lastevent as e2, SupportBean_S0(id=3)#lastevent as e1;\n" +
                    "@name('s2') select cc(e1, 'x', e2, 'y', e3) as c0 from \n" +
                    "SupportBean_S0(id=1)#lastevent as e3, SupportBean_S0(id=2)#lastevent as e2, SupportBean_S0(id=3)#lastevent as e1;\n";
            env.compileDeploy(epl, path).addListener("s0").addListener("s1").addListener("s2");
            assertTypeExpected(env, String.class);

            env.sendEventBean(new SupportBean_S0(1, "A"));
            env.sendEventBean(new SupportBean_S0(3, "C"));
            env.sendEventBean(new SupportBean_S0(2, "B"));
            assertEquals("BxCyA", env.listener("s0").assertOneGetNewAndReset().get("c0"));
            assertEquals("BxAyC", env.listener("s1").assertOneGetNewAndReset().get("c0"));
            assertEquals("CxByA", env.listener("s2").assertOneGetNewAndReset().get("c0"));

            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "expression cc{(v1,v2) -> v1 || v2} select cc(1, 2) from SupportBean",
                "Failed to validate select-clause expression 'cc(1,2)': Error validating expression declaration 'cc': Failed to validate declared expression body expression 'v1||v2': Implicit conversion from datatype 'Integer' to string is not allowed");
        }
    }

    private static class ExprDefineValueParameterCache implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variable ExprDefineLocalService myService = new ExprDefineLocalService();\n" +
                "create expression doit {v -> myService.calc(v)};\n" +
                "@name('s0') select doit(theString) as c0 from SupportBean;\n";
            ExprDefineLocalService.services.clear();
            env.compileDeploy(epl).addListener("s0");
            ExprDefineLocalService service = ExprDefineLocalService.services.get(0);

            env.sendEventBean(new SupportBean("E10", -1));
            assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            assertEquals(1, service.getCalculations().size());

            env.sendEventBean(new SupportBean("E10", -1));
            assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("c0"));
            assertEquals(2, service.getCalculations().size());

            ExprDefineLocalService.services.clear();
            env.undeployAll();
        }
    }

    private static class ExprDefineValueParameterVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema A (value1 double, value2 double);\n" +
                "create variable double C=1.2;\n" +
                "create variable double D=1.5;\n" +
                "\n" +
                "create expression E {(V1,V2)=>max(V1,V2)};\n" +
                "\n" +
                "@name('s0') select E(value1,value2) as c0, E(value1,C) as c1, E(C,D) as c2 from A;\n";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1,c2".split(",");

            env.sendEventMap(CollectionUtil.buildMap("value1", 1d, "value2", 1.5d), "A");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[] {1.5d, 1.2d, 1.5d});

            env.runtime().getVariableService().setVariableValue(env.deploymentId("s0"), "D", 1.1d);

            env.sendEventMap(CollectionUtil.buildMap("value1", 1.8d, "value2", 1.5d), "A");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[] {1.8d, 1.8d, 1.2d});

            env.undeployAll();
        }
    }

    private static void assertTypeExpected(RegressionEnvironment env, Class clazz) {
        Object[][] expectedColTypes = new Object[][]{
            {"c0", clazz},
        };
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedColTypes, env.statement("s0").getEventType(), NAME, TYPE);
    }

    private static void sendAssert(RegressionEnvironment env, String expected, String p00, String p01) {
        sendAssert(env, expected, p00, p01, null);
    }

    private static void sendAssert(RegressionEnvironment env, String expected, String p00, String p01, String p02) {
        String[] fields = "c0".split(",");
        env.sendEventBean(new SupportBean_S0(0, p00, p01, p02));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
    }

    public static class ExprDefineLocalService {
        static List<ExprDefineLocalService> services = new ArrayList<>();
        private List<String> calculations = new ArrayList<>();

        public ExprDefineLocalService() {
            services.add(this);
        }

        public int calc(String value) {
            calculations.add(value);
            return Integer.parseInt(value.substring(1));
        }

        public List<String> getCalculations() {
            return calculations;
        }
    }
}
