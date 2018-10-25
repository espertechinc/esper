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
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ExprCoreMinMaxNonAgg {
    private final static String EPL = "select max(longBoxed,intBoxed) as myMax, " +
        "max(longBoxed,intBoxed,shortBoxed) as myMaxEx, " +
        "min(longBoxed,intBoxed) as myMin, " +
        "min(longBoxed,intBoxed,shortBoxed) as myMinEx" +
        " from SupportBean#length(3)";


    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExecCoreMinMax());
        execs.add(new ExecCoreMinMaxOM());
        execs.add(new ExecCoreMinMaxCompile());
        return execs;
    }

    private static class ExecCoreMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setUpMinMax(env);
            EventType type = env.statement("s0").getEventType();
            Assert.assertEquals(Long.class, type.getPropertyType("myMax"));
            Assert.assertEquals(Long.class, type.getPropertyType("myMin"));
            Assert.assertEquals(Long.class, type.getPropertyType("myMinEx"));
            Assert.assertEquals(Long.class, type.getPropertyType("myMaxEx"));

            tryMinMaxWindowStats(env);

            env.undeployAll();
        }
    }

    private static class ExecCoreMinMaxOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create()
                .add(Expressions.max("longBoxed", "intBoxed"), "myMax")
                .add(Expressions.max(Expressions.property("longBoxed"), Expressions.property("intBoxed"), Expressions.property("shortBoxed")), "myMaxEx")
                .add(Expressions.min("longBoxed", "intBoxed"), "myMin")
                .add(Expressions.min(Expressions.property("longBoxed"), Expressions.property("intBoxed"), Expressions.property("shortBoxed")), "myMinEx")
            );
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName()).addView("length", Expressions.constant(3))));
            model = SerializableObjectCopier.copyMayFail(model);
            Assert.assertEquals(EPL, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryMinMaxWindowStats(env);

            env.undeployAll();
        }
    }

    private static class ExecCoreMinMaxCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.eplToModelCompileDeploy("@name('s0') " + EPL).addListener("s0");
            tryMinMaxWindowStats(env);
            env.undeployAll();
        }
    }

    private static void tryMinMaxWindowStats(RegressionEnvironment env) {
        sendEvent(env, 10, 20, (short) 4);
        EventBean received = env.listener("s0").getAndResetLastNewData()[0];
        Assert.assertEquals(20L, received.get("myMax"));
        Assert.assertEquals(10L, received.get("myMin"));
        Assert.assertEquals(4L, received.get("myMinEx"));
        Assert.assertEquals(20L, received.get("myMaxEx"));

        sendEvent(env, -10, -20, (short) -30);
        received = env.listener("s0").getAndResetLastNewData()[0];
        Assert.assertEquals(-10L, received.get("myMax"));
        Assert.assertEquals(-20L, received.get("myMin"));
        Assert.assertEquals(-30L, received.get("myMinEx"));
        Assert.assertEquals(-10L, received.get("myMaxEx"));
    }

    private static void setUpMinMax(RegressionEnvironment env) {
        env.compileDeploy("@name('s0')  " + EPL).addListener("s0");
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        sendBoxedEvent(env, longBoxed, intBoxed, shortBoxed);
    }

    private static void sendBoxedEvent(RegressionEnvironment env, Long longBoxed, Integer intBoxed, Short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }
}
