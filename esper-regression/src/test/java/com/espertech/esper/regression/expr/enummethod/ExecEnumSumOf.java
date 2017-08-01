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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

public class ExecEnumSumOf implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType("Bean", SupportBean_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSumEvents(epService);
        runAssertionSumOfScalar(epService);
        runAssertionInvalid(epService);
        runAssertionSumOfArray(epService);
    }

    private void runAssertionSumOfArray(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "{1d, 2d}.sumOf() as c0," +
                "{BigInteger.valueOf(1), BigInteger.valueOf(2)}.sumOf() as c1, " +
                "{1L, 2L}.sumOf() as c2, " +
                "{1L, 2L, null}.sumOf() as c3 " +
                " from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3".split(","), new Object[] {3d, BigInteger.valueOf(3), 3L, 3L});

        stmt.destroy();
    }

    private void runAssertionSumEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4".split(",");
        String eplFragment = "select " +
                "beans.sumOf(x => intBoxed) as val0," +
                "beans.sumOf(x => doubleBoxed) as val1," +
                "beans.sumOf(x => longBoxed) as val2," +
                "beans.sumOf(x => bigDecimal) as val3, " +
                "beans.sumOf(x => bigInteger) as val4 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Double.class, Long.class, BigDecimal.class, BigInteger.class});

        epService.getEPRuntime().sendEvent(new SupportBean_Container(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_Container(Collections.emptyList()));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

        List<SupportBean> list = new ArrayList<>();
        list.add(make(2, 3d, 4L, 5, 6));
        epService.getEPRuntime().sendEvent(new SupportBean_Container(list));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 3d, 4L, new BigDecimal(5), new BigInteger("6")});

        list.add(make(4, 6d, 8L, 10, 12));
        epService.getEPRuntime().sendEvent(new SupportBean_Container(list));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2 + 4, 3d + 6d, 4L + 8L, new BigDecimal(5 + 10), new BigInteger("18")});

        stmtFragment.destroy();
    }

    private void runAssertionSumOfScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "intvals.sumOf() as val0, " +
                "bdvals.sumOf() as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("1,4,5"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1 + 4 + 5, new BigDecimal(1 + 4 + 5)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("3,4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3 + 4, new BigDecimal(3 + 4)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, new BigDecimal(3)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        stmtFragment.destroy();

        // test average with lambda
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractBigDecimal", ExecEnumMinMax.MyService.class.getName(), "extractBigDecimal");

        // lambda with string-array input
        String[] fieldsLambda = "val0,val1".split(",");
        String eplLambda = "select " +
                "strvals.sumOf(v => extractNum(v)) as val0, " +
                "strvals.sumOf(v => extractBigDecimal(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fieldsLambda, new Class[]{Integer.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{2 + 1 + 5 + 4, new BigDecimal(2 + 1 + 5 + 4)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{1, new BigDecimal(1)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

        stmtLambda.destroy();
    }

    private SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal, int bigInteger) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        bean.setBigInteger(new BigInteger(Integer.toString(bigInteger)));
        return bean;
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "select beans.sumof() from Bean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'beans.sumof()': Invalid input for built-in enumeration method 'sumof' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type 'SupportBean' [select beans.sumof() from Bean]");
    }
}
