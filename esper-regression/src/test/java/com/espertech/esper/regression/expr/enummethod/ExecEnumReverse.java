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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;

public class ExecEnumReverse implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionReverseEvents(epService);
        runAssertionReverseScalar(epService);
    }

    private void runAssertionReverseEvents(EPServiceProvider epService) {

        String epl = "select contained.reverse() as val from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val", "E3,E2,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E2,9", "E1,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val", "E1,E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        LambdaAssertionUtil.assertST0Id(listener, "val", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        LambdaAssertionUtil.assertST0Id(listener, "val", "");
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionReverseScalar(EPServiceProvider epService) {

        String[] fields = "val0".split(",");
        String eplFragment = "select " +
                "strvals.reverse() as val0 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E4", "E5", "E1", "E2");
        listener.reset();

        LambdaAssertionUtil.assertSingleAndEmptySupportColl(epService, listener, fields);

        stmtFragment.destroy();
    }
}
