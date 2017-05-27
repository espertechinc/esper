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
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecEnumAverage implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAverageEvents(epService);
        runAssertionAverageScalar(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionAverageEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3".split(",");
        String eplFragment = "select " +
                "beans.average(x => intBoxed) as val0," +
                "beans.average(x => doubleBoxed) as val1," +
                "beans.average(x => longBoxed) as val2," +
                "beans.average(x => bigDecimal) as val3 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Double.class, Double.class, Double.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(new SupportBean_Container(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_Container(Collections.emptyList()));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        List<SupportBean> list = new ArrayList<>();
        list.add(make(2, 3d, 4L, 5));
        epService.getEPRuntime().sendEvent(new SupportBean_Container(list));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2d, 3d, 4d, new BigDecimal(5.0d)});

        list.add(make(4, 6d, 8L, 10));
        epService.getEPRuntime().sendEvent(new SupportBean_Container(list));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{(2 + 4) / 2d, (3d + 6d) / 2d, (4L + 8L) / 2d, new BigDecimal((5 + 10) / 2d)});

        stmtFragment.destroy();
    }

    private void runAssertionAverageScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "intvals.average() as val0," +
                "bdvals.average() as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Double.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("1,2,3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2d, new BigDecimal(2d)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("1,null,3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2d, new BigDecimal(2d)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4d, new BigDecimal(4d)});
        stmtFragment.destroy();

        // test average with lambda
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractBigDecimal", ExecEnumMinMax.MyService.class.getName(), "extractBigDecimal");

        String[] fieldsLambda = "val0,val1".split(",");
        String eplLambda = "select " +
                "strvals.average(v => extractNum(v)) as val0, " +
                "strvals.average(v => extractBigDecimal(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fieldsLambda, new Class[]{Double.class, BigDecimal.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{(2 + 1 + 5 + 4) / 4d, new BigDecimal((2 + 1 + 5 + 4) / 4d)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{1d, new BigDecimal(1)});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});

        stmtLambda.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "select strvals.average() from SupportCollection";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'strvals.average()': Invalid input for built-in enumeration method 'average' and 0-parameter footprint, expecting collection of numeric values as input, received collection of String [select strvals.average() from SupportCollection]");

        epl = "select beans.average() from Bean";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'beans.average()': Invalid input for built-in enumeration method 'average' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean.class.getName() + "'");
    }

    private SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        return bean;
    }
}
