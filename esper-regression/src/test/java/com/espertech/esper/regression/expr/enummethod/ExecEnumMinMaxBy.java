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
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecEnumMinMaxBy implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String[] fields = "val0,val1,val2,val3".split(",");
        String eplFragment = "select " +
                "contained.minBy(x => p00) as val0," +
                "contained.maxBy(x => p00) as val1," +
                "contained.minBy(x => p00).id as val2," +
                "contained.maxBy(x => p00).p00 as val3 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{SupportBean_ST0.class, SupportBean_ST0.class, String.class, Integer.class});

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{bean.getContained().get(2), bean.getContained().get(0), "E2", 12});

        bean = SupportBean_ST0_Container.make2Value("E1,12");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{bean.getContained().get(0), bean.getContained().get(0), "E1", 12});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{null, null, null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{null, null, null, null});
        stmtFragment.destroy();

        // test scalar-coll with lambda
        String[] fieldsLambda = "val0,val1".split(",");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.minBy(v => extractNum(v)) as val0, " +
                "strvals.maxBy(v => extractNum(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fieldsLambda, new Class[]{String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{"E1", "E5"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{"E1", "E1"});
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsLambda, new Object[]{null, null});
    }
}
