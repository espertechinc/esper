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

public class ExecEnumAllOfAnyOf implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAllOfAnyOfEvents(epService);
        runAssertionAllOfAnyOfScalar(epService);
    }

    private void runAssertionAllOfAnyOfEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "contained.allof(x => p00 = 12) as val0," +
                "contained.anyof(x => p00 = 12) as val1 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Boolean.class, Boolean.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,12", "E2,2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12", "E2,12", "E2,12"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,0", "E2,0", "E2,0"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        stmtFragment.destroy();
    }

    private void runAssertionAllOfAnyOfScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.allof(x => x='E2') as val0," +
                "strvals.anyof(x => x='E2') as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Boolean.class, Boolean.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        stmtFragment.destroy();
    }
}
