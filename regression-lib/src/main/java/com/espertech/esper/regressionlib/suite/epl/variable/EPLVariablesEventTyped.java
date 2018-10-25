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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.client.variable.VariableValueException;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class EPLVariablesEventTyped {
    public final static NonSerializable NON_SERIALIZABLE = new EPLVariablesEventTyped.NonSerializable("abc");

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLVariableEventTypedSceneOne());
        execs.add(new EPLVariableEventTypedSceneTwo());
        execs.add(new EPLVariableConfig());
        execs.add(new EPLVariableEventTypedSetProp());
        execs.add(new EPLVariableInvalid());
        return execs;
    }

    private static class EPLVariableInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            try {
                env.runtime().getVariableService().setVariableValue(null, "vars0_A", new SupportBean_S1(1));
                fail();
            } catch (VariableValueException ex) {
                assertEquals("Variable 'vars0_A' of declared event type 'SupportBean_S0' underlying type '" + SupportBean_S0.class.getName() + "' cannot be assigned a value of type '" + SupportBean_S1.class.getName() + "'", ex.getMessage());
            }

            tryInvalidCompile(env, "on SupportBean_S0 arrival set vars1_A = arrival",
                "Variable 'vars1_A' of declared event type '" + SupportBean_S1.class.getName() + "' underlying type '" + SupportBean_S1.class.getName() + "' cannot be assigned a value of type '" + SupportBean_S0.class.getName() + "'");

            tryInvalidCompile(env, "on SupportBean_S0 arrival set vars0_A = 1",
                "Variable 'vars0_A' of declared event type 'SupportBean_S0' underlying type '" + SupportBean_S0.class.getName() + "' cannot be assigned a value of type 'int'");
        }
    }

    private static class EPLVariableEventTypedSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String vars = "@name('vars') create variable " + SupportBean.class.getName() + " varbeannull;\n" +
                "create variable " + SupportBean.class.getName() + " varbean;\n" +
                "create variable SupportBean_S0 vars0;\n" +
                "create variable long varobj;\n" +
                "create variable long varobjnull;\n";
            env.compileDeploy(vars, path);
            String deploymentId = env.deploymentId("vars");

            String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
            String stmtSelectText = "@Name('Select') select varbean.theString as c0,varbean.intPrimitive as c1,vars0.id as c2,vars0.p00 as c3,varobj as c4,varbeannull.theString as c5, varobjnull as c6 from SupportBean_A";
            env.compileDeploy(stmtSelectText, path).addListener("Select");

            env.sendEventBean(new SupportBean_A("A1"));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, null});

            // update via API
            env.runtime().getVariableService().setVariableValue(deploymentId, "varobj", 101L);
            env.runtime().getVariableService().setVariableValue(deploymentId, "vars0", new SupportBean_S0(1, "S01"));
            env.runtime().getVariableService().setVariableValue(deploymentId, "varbean", new SupportBean("E1", -1));

            env.milestone(0);

            env.sendEventBean(new SupportBean_A("A2"));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{"E1", -1, 1, "S01", 101L, null, null});

            env.milestone(1);

            // update properties via on-set
            String stmtUpdateText = "@Name('Update') on SupportBean_B set varbean.theString = 'EX', varbean.intPrimitive = -999";
            env.compileDeploy(stmtUpdateText, path);
            assertEquals(StatementType.ON_SET, env.statement("Update").getProperty(StatementProperty.STATEMENTTYPE));
            env.sendEventBean(new SupportBean_B("B1"));

            env.milestone(2);

            env.sendEventBean(new SupportBean_A("A3"));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{"EX", -999, 1, "S01", 101L, null, null});

            // update full bean via on-set
            stmtUpdateText = "@Name('Update2') on SupportBean(intPrimitive = 0) as sb set varbean = sb";
            env.compileDeploy(stmtUpdateText, path);

            SupportBean bean = new SupportBean("E2", 0);
            env.sendEventBean(bean);

            env.milestone(3);

            env.sendEventBean(new SupportBean_A("A4"));
            EPAssertionUtil.assertProps(env.listener("Select").assertOneGetNewAndReset(), fields, new Object[]{"E2", 0, 1, "S01", 101L, null, null});

            env.undeployAll();
        }
    }

    private static class EPLVariableConfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertEquals(10, ((SupportBean_S0) env.runtime().getVariableService().getVariableValue(null, "vars0_A")).getId());
            assertEquals(20, ((SupportBean_S1) env.runtime().getVariableService().getVariableValue(null, "vars1_A")).getId());
            assertEquals(123, env.runtime().getVariableService().getVariableValue(null, "varsobj1"));
            Object value = env.runtime().getVariableService().getVariableValue(null, "myNonSerializable");
            if (!env.isHA()) {
                assertSame(NON_SERIALIZABLE, value);
            } else {
                assertEquals(NON_SERIALIZABLE, value);
            }

            env.milestone(0);

            assertEquals(30, ((SupportBean_S2) env.runtime().getVariableService().getVariableValue(null, "vars2")).getId());
            assertEquals(40, ((SupportBean_S3) env.runtime().getVariableService().getVariableValue(null, "vars3")).getId());
            assertEquals("ABC", env.runtime().getVariableService().getVariableValue(null, "varsobj2"));

            env.compileDeploy("@name('create') create variable object varsobj3=222");
            assertEquals(222, env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "varsobj3"));

            env.undeployAll();
        }
    }

    private static class EPLVariableEventTypedSetProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create variable SupportBean varbean", path);

            String[] fields = "varbean.theString,varbean.intPrimitive,varbean.getTheString()".split(",");
            env.compileDeploy("@name('s0') select varbean.theString,varbean.intPrimitive,varbean.getTheString() from SupportBean_S0", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

            env.compileDeploy("@name('set') on SupportBean_A set varbean.theString = 'A', varbean.intPrimitive = 1", path);
            env.addListener("set");
            env.sendEventBean(new SupportBean_A("E1"));
            env.listener("set").reset();

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

            SupportBean setBean = new SupportBean();
            env.runtime().getVariableService().setVariableValue(env.deploymentId("create"), "varbean", setBean);
            env.sendEventBean(new SupportBean_A("E2"));
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), "varbean.theString,varbean.intPrimitive".split(","), new Object[]{"A", 1});
            EPAssertionUtil.assertProps(env.iterator("set").next(), "varbean.theString,varbean.intPrimitive".split(","), new Object[]{"A", 1});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 1, "A"});
            assertNotSame(setBean, env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "varbean"));
            assertEquals(1, ((SupportBean) env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "varbean")).getIntPrimitive());

            // test self evaluate
            env.undeployModuleContaining("set");
            env.compileDeploy("@name('set') on SupportBean_A set varbean.theString = SupportBean_A.id, varbean.theString = '>'||varbean.theString||'<'", path);
            env.addListener("set");
            env.sendEventBean(new SupportBean_A("E3"));
            assertEquals(">E3<", ((SupportBean) env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "varbean")).getTheString());
            env.undeployModuleContaining("set");

            // test widen
            env.compileDeploy("@name('set') on SupportBean_A set varbean.longPrimitive = 1", path);
            env.addListener("set");
            env.sendEventBean(new SupportBean_A("E4"));
            assertEquals(1, ((SupportBean) env.runtime().getVariableService().getVariableValue(env.deploymentId("create"), "varbean")).getLongPrimitive());

            env.undeployAll();
        }
    }

    private static class EPLVariableEventTypedSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('v0') create variable Object varobject = null", path);
            env.compileDeploy("@name('v1') create variable " + SupportBean_A.class.getName() + " varbean = null", path);
            env.compileDeploy("@name('v2') create variable SupportBean_S0 vartype = null", path);
            String depIdVarobject = env.deploymentId("v0");
            String depIdVarbean = env.deploymentId("v1");
            String depIdVartype = env.deploymentId("v2");

            String[] fields = "varobject,varbean,varbean.id,vartype,vartype.id".split(",");
            env.compileDeploy("@name('s0') select varobject, varbean, varbean.id, vartype, vartype.id from SupportBean", path);
            env.addListener("s0");

            // test null
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            env.milestone(0);

            // test objects
            SupportBean_A a1objectOne = new SupportBean_A("A1");
            SupportBean_S0 s0objectOne = new SupportBean_S0(1);
            env.runtime().getVariableService().setVariableValue(depIdVarobject, "varobject", "abc");
            env.runtime().getVariableService().setVariableValue(depIdVarbean, "varbean", a1objectOne);
            env.runtime().getVariableService().setVariableValue(depIdVartype, "vartype", s0objectOne);

            env.milestone(1);

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"abc", a1objectOne, a1objectOne.getId(), s0objectOne, s0objectOne.getId()});

            // test on-set for Object and EventType
            String[] fieldsTop = "varobject,vartype,varbean".split(",");
            env.compileDeploy("@name('set') on SupportBean_S0(p00='X') arrival set varobject=1, vartype=arrival, varbean=null", path);
            env.addListener("set");

            SupportBean_S0 s0objectTwo = new SupportBean_S0(2, "X");
            env.sendEventBean(s0objectTwo);
            assertEquals(1, env.runtime().getVariableService().getVariableValue(depIdVarobject, "varobject"));
            assertEquals(s0objectTwo, env.runtime().getVariableService().getVariableValue(depIdVartype, "vartype"));
            assertEquals(s0objectTwo, env.runtime().getVariableService().getVariableValue(Collections.singleton(new DeploymentIdNamePair(depIdVartype, "vartype"))).get(new DeploymentIdNamePair(depIdVartype, "vartype")));
            EPAssertionUtil.assertProps(env.listener("set").assertOneGetNewAndReset(), fieldsTop, new Object[]{1, s0objectTwo, null});
            EPAssertionUtil.assertProps(env.iterator("set").next(), fieldsTop, new Object[]{1, s0objectTwo, null});

            // set via API to null
            Map<DeploymentIdNamePair, Object> newValues = new HashMap<>();
            newValues.put(new DeploymentIdNamePair(depIdVarobject, "varobject"), null);
            newValues.put(new DeploymentIdNamePair(depIdVartype, "vartype"), null);
            newValues.put(new DeploymentIdNamePair(depIdVarbean, "varbean"), null);
            env.runtime().getVariableService().setVariableValue(newValues);
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            // set via API to values
            newValues.put(new DeploymentIdNamePair(depIdVarobject, "varobject"), 10L);
            newValues.put(new DeploymentIdNamePair(depIdVartype, "vartype"), s0objectTwo);
            newValues.put(new DeploymentIdNamePair(depIdVarbean, "varbean"), a1objectOne);
            env.runtime().getVariableService().setVariableValue(newValues);
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10L, a1objectOne, a1objectOne.getId(), s0objectTwo, s0objectTwo.getId()});

            // test on-set for Bean class
            env.compileDeploy("@name('set-two') on SupportBean_A(id='Y') arrival set varobject=null, vartype=null, varbean=arrival", path);
            env.addListener("set-two");
            SupportBean_A a1objectTwo = new SupportBean_A("Y");
            env.sendEventBean(new SupportBean_A("Y"));
            assertEquals(null, env.runtime().getVariableService().getVariableValue(depIdVarobject, "varobject"));
            assertEquals(null, env.runtime().getVariableService().getVariableValue(depIdVartype, "vartype"));
            assertEquals(a1objectTwo, env.runtime().getVariableService().getVariableValue(Collections.singleton(new DeploymentIdNamePair(depIdVarbean, "varbean"))).get(new DeploymentIdNamePair(depIdVarbean, "varbean")));
            EPAssertionUtil.assertProps(env.listener("set-two").assertOneGetNewAndReset(), fieldsTop, new Object[]{null, null, a1objectTwo});
            EPAssertionUtil.assertProps(env.iterator("set-two").next(), fieldsTop, new Object[]{null, null, a1objectTwo});

            env.undeployAll();
        }
    }

    public static class NonSerializable {
        private final String myString;

        public NonSerializable(String myString) {
            this.myString = myString;
        }

        public String getMyString() {
            return myString;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NonSerializable that = (NonSerializable) o;

            return myString.equals(that.myString);
        }

        public int hashCode() {
            return myString.hashCode();
        }
    }
}
