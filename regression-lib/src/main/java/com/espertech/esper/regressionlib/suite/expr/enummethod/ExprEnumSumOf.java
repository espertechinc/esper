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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

public class ExprEnumSumOf {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumSumEvents());
        execs.add(new ExprEnumSumOfScalar());
        execs.add(new ExprEnumInvalid());
        execs.add(new ExprEnumSumOfArray());
        return execs;
    }

    private static class ExprEnumSumOfArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "{1d, 2d}.sumOf() as c0," +
                "{BigInteger.valueOf(1), BigInteger.valueOf(2)}.sumOf() as c1, " +
                "{1L, 2L}.sumOf() as c2, " +
                "{1L, 2L, null}.sumOf() as c3 " +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[]{3d, BigInteger.valueOf(3), 3L, 3L});

            env.undeployAll();
        }
    }

    private static class ExprEnumSumEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1,val2,val3,val4".split(",");
            String eplFragment = "@name('s0') select " +
                "beans.sumOf(x => intBoxed) as val0," +
                "beans.sumOf(x => doubleBoxed) as val1," +
                "beans.sumOf(x => longBoxed) as val2," +
                "beans.sumOf(x => bigDecimal) as val3, " +
                "beans.sumOf(x => bigInteger) as val4 " +
                "from SupportBean_Container";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, Double.class, Long.class, BigDecimal.class, BigInteger.class});

            env.sendEventBean(new SupportBean_Container(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            env.sendEventBean(new SupportBean_Container(Collections.emptyList()));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

            List<SupportBean> list = new ArrayList<>();
            list.add(make(2, 3d, 4L, 5, 6));
            env.sendEventBean(new SupportBean_Container(list));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, 3d, 4L, new BigDecimal(5), new BigInteger("6")});

            list.add(make(4, 6d, 8L, 10, 12));
            env.sendEventBean(new SupportBean_Container(list));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2 + 4, 3d + 6d, 4L + 8L, new BigDecimal(5 + 10), new BigInteger("18")});

            env.undeployAll();
        }
    }

    private static class ExprEnumSumOfScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "val0,val1".split(",");
            String eplFragment = "@name('s0') select " +
                "intvals.sumOf() as val0, " +
                "bdvals.sumOf() as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class, BigDecimal.class});

            env.sendEventBean(SupportCollection.makeNumeric("1,4,5"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1 + 4 + 5, new BigDecimal(1 + 4 + 5)});

            env.sendEventBean(SupportCollection.makeNumeric("3,4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3 + 4, new BigDecimal(3 + 4)});

            env.sendEventBean(SupportCollection.makeNumeric("3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3, new BigDecimal(3)});

            env.sendEventBean(SupportCollection.makeNumeric(""));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.sendEventBean(SupportCollection.makeNumeric(null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null});

            env.undeployAll();

            // test average with lambda
            // lambda with string-array input
            String[] fieldsLambda = "val0,val1".split(",");
            String eplLambda = "@name('s0') select " +
                "strvals.sumOf(v => extractNum(v)) as val0, " +
                "strvals.sumOf(v => extractBigDecimal(v)) as val1 " +
                "from SupportCollection";
            env.compileDeploy(eplLambda).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fieldsLambda, new Class[]{Integer.class, BigDecimal.class});

            env.sendEventBean(SupportCollection.makeString("E2,E1,E5,E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{2 + 1 + 5 + 4, new BigDecimal(2 + 1 + 5 + 4)});

            env.sendEventBean(SupportCollection.makeString("E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsLambda, new Object[]{1, new BigDecimal(1)});

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

            epl = "select beans.sumof() from SupportBean_Container";
            tryInvalidCompile(env, epl, "Failed to validate select-clause expression 'beans.sumof()': Invalid input for built-in enumeration method 'sumof' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '");
        }
    }

    private static SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal, int bigInteger) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        bean.setBigInteger(new BigInteger(Integer.toString(bigInteger)));
        return bean;
    }
}
