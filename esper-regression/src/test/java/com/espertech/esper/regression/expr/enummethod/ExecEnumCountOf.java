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

public class ExecEnumCountOf implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCountOfEvents(epService);
        runAssertionCountOfScalar(epService);
    }

    private void runAssertionCountOfEvents(EPServiceProvider epService) {

        String[] fields = new String[]{"val0", "val1"};
        String eplFragment = "select " +
                "contained.countof(x=> x.p00 = 9) as val0, " +
                "contained.countof() as val1 " +
                " from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E2,9"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{2, 3});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(new String[0]));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{0, 0});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,9"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{1, 1});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{0, 1});

        stmtFragment.destroy();
    }

    private void runAssertionCountOfScalar(EPServiceProvider epService) {

        String[] fields = new String[]{"val0", "val1"};
        String eplFragment = "select " +
                "strvals.countof() as val0, " +
                "strvals.countof(x => x = 'E1') as val1 " +
                " from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, Integer.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, 1});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E1,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, 2});

        stmtFragment.destroy();
    }
}
