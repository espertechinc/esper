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

public class ExecEnumTakeAndTakeLast implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTakeEvents(epService);
        runAssertionTakeScalar(epService);
    }

    private void runAssertionTakeEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        String epl = "select " +
                "contained.take(2) as val0," +
                "contained.take(1) as val1," +
                "contained.take(0) as val2," +
                "contained.take(-1) as val3," +
                "contained.takeLast(2) as val4," +
                "contained.takeLast(1) as val5" +
                " from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class, Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,3"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "");
        LambdaAssertionUtil.assertST0Id(listener, "val4", "E2,E3");
        LambdaAssertionUtil.assertST0Id(listener, "val5", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "");
        LambdaAssertionUtil.assertST0Id(listener, "val4", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val5", "E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "");
        LambdaAssertionUtil.assertST0Id(listener, "val4", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val5", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        for (String field : fields) {
            LambdaAssertionUtil.assertST0Id(listener, field, "");
        }
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        for (String field : fields) {
            LambdaAssertionUtil.assertST0Id(listener, field, null);
        }
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionTakeScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3".split(",");
        String epl = "select " +
                "strvals.take(2) as val0," +
                "strvals.take(1) as val1," +
                "strvals.takeLast(2) as val2," +
                "strvals.takeLast(1) as val3" +
                " from SupportCollection";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E1");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", "E2", "E3");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val3", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E1");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", "E1", "E2");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val3", "E2");
        listener.reset();

        LambdaAssertionUtil.assertSingleAndEmptySupportColl(epService, listener, fields);

        stmt.destroy();
    }
}
