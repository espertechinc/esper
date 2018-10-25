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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ExprEnumAverage {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumAverageEvents());
        execs.add(new ExprEnumAverageScalar());
        execs.add(new ExprEnumInvalid());
        return execs;
    }

    private static class ExprEnumAverageEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3".split(",");
            String eplFragment = "@name('s0') select " +
                "beans.average(x => intBoxed) as val0," +
                "beans.average(x => doubleBoxed) as val1," +
                "beans.average(x => longBoxed) as val2," +
                "beans.average(x => bigDecimal) as val3 " +
                "from SupportBean_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Double.class, Double.class, Double.class, BigDecimal.class});

            env.sendEventBean(new SupportBean_Container(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.sendEventBean(new SupportBean_Container(Collections.emptyList()));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            List<SupportBean> list = new ArrayList<>();
            list.add(make(2, 3d, 4L, 5));
            env.sendEventBean(new SupportBean_Container(list));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2d, 3d, 4d, new BigDecimal(5.0d)});

            list.add(make(4, 6d, 8L, 10));
            env.sendEventBean(new SupportBean_Container(list));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{(2 + 4) / 2d, (3d + 6d) / 2d, (4L + 8L) / 2d, new BigDecimal((5 + 10) / 2d)});

            env.undeployAll();
        }
    }

    private static class ExprEnumAverageScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "intvals.average() as val0," +
                "bdvals.average() as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Double.class, BigDecimal.class});

            env.sendEventBean(SupportCollection.makeNumeric("1,2,3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2d, new BigDecimal(2d)});

            env.sendEventBean(SupportCollection.makeNumeric("1,null,3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2d, new BigDecimal(2d)});

            env.sendEventBean(SupportCollection.makeNumeric("4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{4d, new BigDecimal(4d)});
            env.undeployAll();

            // test average with lambda
            String[] fieldsLambda = "val0,val1".split(",");
            String eplLambda = "@name('s0') select " +
                "strvals.average(v => extractNum(v)) as val0, " +
                "strvals.average(v => extractBigDecimal(v)) as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplLambda).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fieldsLambda, new Class[]{Double.class, BigDecimal.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{(2 + 1 + 5 + 4) / 4d, new BigDecimal((2 + 1 + 5 + 4) / 4d)});

            env.sendEventBean(SupportCollection.makeString("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{1d, new BigDecimal(1)});

            env.sendEventBean(SupportCollection.makeString(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

            env.sendEventBean(SupportCollection.makeString(""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

            env.undeployAll();
        }
    }

    private static class ExprEnumInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select strvals.average() from SupportCollection";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'strvals.average()': Invalid input for built-in enumeration method 'average' and 0-parameter footprint, expecting collection of numeric values as input, received collection of String [select strvals.average() from SupportCollection]");

            epl = "select beans.average() from SupportBean_Container";
            SupportMessageAssertUtil.tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'beans.average()': Invalid input for built-in enumeration method 'average' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean.class.getName() + "'");
        }
    }

    private static SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        return bean;
    }
}
