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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExprCoreInstanceOf {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreInstanceofSimple());
        executions.add(new ExprCoreInstanceofStringAndNullOM());
        executions.add(new ExprCoreInstanceofStringAndNullCompile());
        executions.add(new ExprCoreDynamicPropertyJavaTypes());
        executions.add(new ExprCoreDynamicSuperTypeAndInterface());
        return executions;
    }

    private static class ExprCoreInstanceofSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select instanceof(theString, string) as t0, " +
                " instanceof(intBoxed, int) as t1, " +
                " instanceof(floatBoxed, java.lang.Float) as t2, " +
                " instanceof(theString, java.lang.Float, char, byte) as t3, " +
                " instanceof(intPrimitive, java.lang.Integer) as t4, " +
                " instanceof(intPrimitive, long) as t5, " +
                " instanceof(intPrimitive, long, long, java.lang.Number) as t6, " +
                " instanceof(floatBoxed, long, float) as t7 " +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 7; i++) {
                Assert.assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("t" + i));
            }

            SupportBean bean = new SupportBean("abc", 100);
            bean.setFloatBoxed(100F);
            env.sendEventBean(bean);
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{true, false, true, false, true, false, true, true});

            bean = new SupportBean(null, 100);
            bean.setFloatBoxed(null);
            env.sendEventBean(bean);
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, false, false, false, true, false, true, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreInstanceofStringAndNullOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select instanceof(theString,string) as t0, " +
                "instanceof(theString,float,string,int) as t1 " +
                "from SupportBean";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create()
                .add(Expressions.instanceOf("theString", "string"), "t0")
                .add(Expressions.instanceOf(Expressions.property("theString"), "float", "string", "int"), "t1"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));
            model = (EPStatementObjectModel) SerializableObjectCopier.copyMayFail(model);
            Assert.assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);

            env.sendEventBean(new SupportBean("abc", 100));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertTrue((Boolean) theEvent.get("t0"));
            assertTrue((Boolean) theEvent.get("t1"));

            env.sendEventBean(new SupportBean(null, 100));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertFalse((Boolean) theEvent.get("t0"));
            assertFalse((Boolean) theEvent.get("t1"));

            env.undeployAll();
        }
    }

    private static class ExprCoreInstanceofStringAndNullCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select instanceof(theString,string) as t0, " +
                "instanceof(theString,float,string,int) as t1 " +
                "from SupportBean";
            env.eplToModelCompileDeploy(epl).addListener("s0").milestone(0);

            env.sendEventBean(new SupportBean("abc", 100));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertTrue((Boolean) theEvent.get("t0"));
            assertTrue((Boolean) theEvent.get("t1"));

            env.sendEventBean(new SupportBean(null, 100));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertFalse((Boolean) theEvent.get("t0"));
            assertFalse((Boolean) theEvent.get("t1"));

            env.undeployAll();
        }
    }

    private static class ExprCoreDynamicPropertyJavaTypes implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select instanceof(item?, string) as t0, " +
                " instanceof(item?, int) as t1, " +
                " instanceof(item?, java.lang.Float) as t2, " +
                " instanceof(item?, java.lang.Float, char, byte) as t3, " +
                " instanceof(item?, java.lang.Integer) as t4, " +
                " instanceof(item?, long) as t5, " +
                " instanceof(item?, long, long, java.lang.Number) as t6, " +
                " instanceof(item?, long, float) as t7 " +
                " from SupportBeanDynRoot";

            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanDynRoot("abc"));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{true, false, false, false, false, false, false, false});

            env.sendEventBean(new SupportBeanDynRoot(100f));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, false, true, true, false, false, true, true});

            env.sendEventBean(new SupportBeanDynRoot(null));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, false, false, false, false, false, false, false});

            env.sendEventBean(new SupportBeanDynRoot(10));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, true, false, false, true, false, true, false});

            env.sendEventBean(new SupportBeanDynRoot(99L));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, false, false, false, false, true, true, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreDynamicSuperTypeAndInterface implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select instanceof(item?, " + SupportMarkerInterface.class.getName() + ") as t0, " +
                " instanceof(item?, " + ISupportA.class.getName() + ") as t1, " +
                " instanceof(item?, " + ISupportBaseAB.class.getName() + ") as t2, " +
                " instanceof(item?, " + ISupportBaseABImpl.class.getName() + ") as t3, " +
                " instanceof(item?, " + ISupportA.class.getName() + ", " + ISupportB.class.getName() + ") as t4, " +
                " instanceof(item?, " + ISupportBaseAB.class.getName() + ", " + ISupportB.class.getName() + ") as t5, " +
                " instanceof(item?, " + ISupportAImplSuperG.class.getName() + ", " + ISupportB.class.getName() + ") as t6, " +
                " instanceof(item?, " + ISupportAImplSuperGImplPlus.class.getName() + ", " + SupportBeanAtoFBase.class.getName() + ") as t7 " +
                " from SupportBeanDynRoot";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanDynRoot(new SupportBeanDynRoot("abc")));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{true, false, false, false, false, false, false, false});

            env.sendEventBean(new SupportBeanDynRoot(new ISupportAImplSuperGImplPlus()));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, true, true, false, true, true, true, true});

            env.sendEventBean(new SupportBeanDynRoot(new ISupportAImplSuperGImpl("", "", "")));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, true, true, false, true, true, true, false});

            env.sendEventBean(new SupportBeanDynRoot(new ISupportBaseABImpl("")));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, false, true, true, false, true, false, false});

            env.sendEventBean(new SupportBeanDynRoot(new ISupportBImpl("", "")));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, false, true, false, true, true, true, false});

            env.sendEventBean(new SupportBeanDynRoot(new ISupportAImpl("", "")));
            assertResults(env.listener("s0").assertOneGetNewAndReset(), new boolean[]{false, true, true, false, true, true, false, false});

            env.undeployAll();
        }
    }

    private static void assertResults(EventBean theEvent, boolean[] result) {
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
