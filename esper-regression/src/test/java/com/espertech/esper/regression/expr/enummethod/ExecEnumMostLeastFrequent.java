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
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecEnumMostLeastFrequent implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMostLeastEvents(epService);
        runAssertionScalar(epService);
    }

    private void runAssertionMostLeastEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "contained.mostFrequent(x => p00) as val0," +
                "contained.leastFrequent(x => p00) as val1 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        SupportBean_ST0_Container bean = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2", "E3,12");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{12, 11});

        bean = SupportBean_ST0_Container.make2Value("E1,12");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{12, 12});

        bean = SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2", "E3,12", "E1,12", "E2,11", "E3,11");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{12, 2});

        bean = SupportBean_ST0_Container.make2Value("E2,11", "E1,12", "E2,15", "E3,12", "E1,12", "E2,11", "E3,11");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 15});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
    }

    private void runAssertionScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.mostFrequent() as val0, " +
                "strvals.leastFrequent() as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E2,E1,E3,E3,E4,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", "E4"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
        stmtFragment.destroy();

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.mostFrequent(v => extractNum(v)) as val0, " +
                "strvals.leastFrequent(v => extractNum(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E2,E1,E3,E3,E4,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, 4});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, 1});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
    }
}
