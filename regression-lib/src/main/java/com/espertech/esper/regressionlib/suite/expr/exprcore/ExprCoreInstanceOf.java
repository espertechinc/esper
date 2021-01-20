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
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;
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
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "instanceof(theString, string)")
                .expression(fields[1], "instanceof(intBoxed, int)")
                .expression(fields[2], "instanceof(floatBoxed, java.lang.Float)")
                .expression(fields[3], "instanceof(theString, java.lang.Float, char, byte)")
                .expression(fields[4], "instanceof(intPrimitive, java.lang.Integer)")
                .expression(fields[5], "instanceof(intPrimitive, long)")
                .expression(fields[6], "instanceof(intPrimitive, long, long, java.lang.Number)")
                .expression(fields[7], "instanceof(floatBoxed, long, float)");

            builder.statementConsumer(stmt -> {
                for (int i = 0; i < fields.length; i++) {
                    Assert.assertEquals(Boolean.class, stmt.getEventType().getPropertyType(fields[i]));
                }
            });

            SupportBean bean = new SupportBean("abc", 100);
            bean.setFloatBoxed(100F);
            builder.assertion(bean).expect(fields, true, false, true, false, true, false, true, true);

            bean = new SupportBean(null, 100);
            bean.setFloatBoxed(null);
            builder.assertion(bean).expect(fields, false, false, false, false, true, false, true, false);

            builder.run(env);
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
            env.assertEventNew("s0", theEvent -> {
                assertTrue((Boolean) theEvent.get("t0"));
                assertTrue((Boolean) theEvent.get("t1"));
            });

            env.sendEventBean(new SupportBean(null, 100));
            env.assertEventNew("s0", theEvent -> {
                assertFalse((Boolean) theEvent.get("t0"));
                assertFalse((Boolean) theEvent.get("t1"));
            });

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
            env.assertEventNew("s0", theEvent -> {
                assertTrue((Boolean) theEvent.get("t0"));
                assertTrue((Boolean) theEvent.get("t1"));
            });

            env.sendEventBean(new SupportBean(null, 100));
            env.assertEventNew("s0", theEvent -> {
                assertFalse((Boolean) theEvent.get("t0"));
                assertFalse((Boolean) theEvent.get("t1"));
            });

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
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{true, false, false, false, false, false, false, false}));

            env.sendEventBean(new SupportBeanDynRoot(100f));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, false, true, true, false, false, true, true}));

            env.sendEventBean(new SupportBeanDynRoot(null));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, false, false, false, false, false, false, false}));

            env.sendEventBean(new SupportBeanDynRoot(10));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, true, false, false, true, false, true, false}));

            env.sendEventBean(new SupportBeanDynRoot(99L));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, false, false, false, false, true, true, true}));

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
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{true, false, false, false, false, false, false, false}));

            env.sendEventBean(new SupportBeanDynRoot(new ISupportAImplSuperGImplPlus()));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, true, true, false, true, true, true, true}));

            env.sendEventBean(new SupportBeanDynRoot(new ISupportAImplSuperGImpl("", "", "")));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, true, true, false, true, true, true, false}));

            env.sendEventBean(new SupportBeanDynRoot(new ISupportBaseABImpl("")));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, false, true, true, false, true, false, false}));

            env.sendEventBean(new SupportBeanDynRoot(new ISupportBImpl("", "")));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, false, true, false, true, true, true, false}));

            env.sendEventBean(new SupportBeanDynRoot(new ISupportAImpl("", "")));
            env.assertEventNew("s0", event -> assertResults(event, new boolean[]{false, true, true, false, true, true, false, false}));

            env.undeployAll();
        }
    }

    private static void assertResults(EventBean theEvent, boolean[] result) {
        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
