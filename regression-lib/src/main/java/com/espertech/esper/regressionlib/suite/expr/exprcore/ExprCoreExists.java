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
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ExprCoreExists {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreExistsSimple());
        executions.add(new ExprCoreExistsInner());
        executions.add(new ExprCoreCastDoubleAndNullOM());
        executions.add(new ExprCoreCastStringAndNullCompile());
        return executions;
    }

    private static class ExprCoreExistsSimple implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select exists(theString) as t0, " +
                " exists(intBoxed?) as t1, " +
                " exists(dummy?) as t2, " +
                " exists(intPrimitive?) as t3, " +
                " exists(intPrimitive) as t4 " +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 5; i++) {
                TestCase.assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("t" + i));
            }

            SupportBean bean = new SupportBean("abc", 100);
            bean.setFloatBoxed(9.5f);
            bean.setIntBoxed(3);
            env.sendEventBean(bean);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new boolean[]{true, true, false, true, true});

            env.undeployAll();
        }
    }

    private static class ExprCoreExistsInner implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select exists(item?.id) as t0, " +
                " exists(item?.id?) as t1, " +
                " exists(item?.item.intBoxed) as t2, " +
                " exists(item?.indexed[0]?) as t3, " +
                " exists(item?.mapped('keyOne')?) as t4, " +
                " exists(item?.nested?) as t5, " +
                " exists(item?.nested.nestedValue?) as t6, " +
                " exists(item?.nested.nestedNested?) as t7, " +
                " exists(item?.nested.nestedNested.nestedNestedValue?) as t8, " +
                " exists(item?.nested.nestedNested.nestedNestedValue.dummy?) as t9, " +
                " exists(item?.nested.nestedNested.dummy?) as t10 " +
                " from SupportMarkerInterface";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 11; i++) {
                TestCase.assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("t" + i));
            }

            // cannot exists if the inner is null
            env.sendEventBean(new SupportBeanDynRoot(null));
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new boolean[]{false, false, false, false, false, false, false, false, false, false, false});

            // try nested, indexed and mapped
            env.sendEventBean(new SupportBeanDynRoot(SupportBeanComplexProps.makeDefaultBean()));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new boolean[]{false, false, false, true, true, true, true, true, true, false, false});

            // try nested, indexed and mapped
            env.sendEventBean(new SupportBeanDynRoot(SupportBeanComplexProps.makeDefaultBean()));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new boolean[]{false, false, false, true, true, true, true, true, true, false, false});

            // try a boxed that returns null but does exists
            env.sendEventBean(new SupportBeanDynRoot(new SupportBeanDynRoot(new SupportBean())));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new boolean[]{false, false, true, false, false, false, false, false, false, false, false});

            env.sendEventBean(new SupportBeanDynRoot(new SupportBean_A("10")));
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertResults(theEvent, new boolean[]{true, true, false, false, false, false, false, false, false, false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreCastDoubleAndNullOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "select exists(item?.intBoxed) as t0 from SupportMarkerInterface";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.existsProperty("item?.intBoxed"), "t0"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarkerInterface.class.getSimpleName())));
            model = SerializableObjectCopier.copyMayFail(model);
            TestCase.assertEquals(stmtText, model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));

            env.compileDeploy(model).addListener("s0");

            assertStringAndNull(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreCastStringAndNullCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select exists(item?.intBoxed) as t0 from SupportMarkerInterface";
            env.eplToModelCompileDeploy(epl).addListener("s0").milestone(0);

            assertStringAndNull(env);

            env.undeployAll();
        }
    }

    private static void assertStringAndNull(RegressionEnvironment env) {
        TestCase.assertEquals(Boolean.class, env.statement("s0").getEventType().getPropertyType("t0"));

        env.sendEventBean(new SupportBeanDynRoot(new SupportBean()));
        TestCase.assertEquals(true, env.listener("s0").assertOneGetNewAndReset().get("t0"));

        env.sendEventBean(new SupportBeanDynRoot(null));
        TestCase.assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("t0"));

        env.sendEventBean(new SupportBeanDynRoot("abc"));
        TestCase.assertEquals(false, env.listener("s0").assertOneGetNewAndReset().get("t0"));
    }

    private static void assertResults(EventBean theEvent, boolean[] result) {
        for (int i = 0; i < result.length; i++) {
            TestCase.assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
