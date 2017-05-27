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

public class ExecEnumAggregate implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAggregateEvents(epService);
        runAssertionAggregateScalar(epService);
    }

    private void runAssertionAggregateEvents(EPServiceProvider epService) {

        String[] fields = new String[]{"val0", "val1", "val2"};
        String eplFragment = "select " +
                "contained.aggregate(0, (result, item) => result + item.p00) as val0, " +
                "contained.aggregate('', (result, item) => result || ', ' || item.id) as val1, " +
                "contained.aggregate('', (result, item) => result || (case when result='' then '' else ',' end) || item.id) as val2 " +
                " from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Integer.class, String.class, String.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E2,2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{25, ", E1, E2, E2", "E1,E2,E2"});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(new String[0]));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{0, "", ""});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{12, ", E1", "E1"});

        stmtFragment.destroy();
    }

    private void runAssertionAggregateScalar(EPServiceProvider epService) {

        String[] fields = "val0".split(",");
        String eplFragment = "select " +
                "strvals.aggregate('', (result, item) => result || '+' || item) as val0 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{String.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"+E1+E2+E3"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"+E1"});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{""});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null});

        stmtFragment.destroy();
    }
}
