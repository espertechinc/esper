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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExprCoreCurrentTimestamp {
    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreCurrentTimestampGet());
        executions.add(new ExprCoreCurrentTimestampOM());
        executions.add(new ExprCoreCurrentTimestampCompile());
        return executions;
    }

    private static class ExprCoreCurrentTimestampGet implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select current_timestamp(), " +
                " current_timestamp as t0, " +
                " current_timestamp() as t1, " +
                " current_timestamp + 1 as t2 " +
                " from SupportBean";
            env.compileDeploy(stmtText).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(Long.class, type.getPropertyType("current_timestamp()"));
                assertEquals(Long.class, type.getPropertyType("t0"));
                assertEquals(Long.class, type.getPropertyType("t1"));
                assertEquals(Long.class, type.getPropertyType("t2"));
            });

            sendTimer(env, 100);
            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", event -> assertResults(event, new Object[]{100L, 100L, 101L}));

            sendTimer(env, 999);
            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", theEvent -> {
                assertResults(theEvent, new Object[]{999L, 999L, 1000L});
                assertEquals(theEvent.get("current_timestamp()"), theEvent.get("t0"));
            });

            env.undeployAll();
        }
    }

    private static class ExprCoreCurrentTimestampOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "select current_timestamp() as t0 from SupportBean";

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().add(Expressions.currentTimestamp(), "t0"));
            model.setFromClause(FromClause.create().add(FilterStream.create(SupportBean.class.getSimpleName())));
            model = (EPStatementObjectModel) SerializableObjectCopier.copyMayFail(model);
            assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);

            env.assertStmtType("s0", "t0", EPTypePremade.LONGBOXED.getEPType());

            sendTimer(env, 777);
            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", event -> assertResults(event, new Object[]{777L}));

            env.undeployAll();
        }
    }

    private static class ExprCoreCurrentTimestampCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String stmtText = "@name('s0') select current_timestamp() as t0 from SupportBean";
            env.eplToModelCompileDeploy(stmtText).addListener("s0").milestone(0);

            env.assertStmtType("s0", "t0", EPTypePremade.LONGBOXED.getEPType());

            sendTimer(env, 777);
            env.sendEventBean(new SupportBean());
            env.assertEventNew("s0", event -> assertResults(event, new Object[]{777L}));

            env.undeployAll();
        }
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void assertResults(EventBean theEvent, Object[] result) {
        for (int i = 0; i < result.length; i++) {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
