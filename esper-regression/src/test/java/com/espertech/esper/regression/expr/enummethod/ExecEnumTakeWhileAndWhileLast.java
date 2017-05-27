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

public class ExecEnumTakeWhileAndWhileLast implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTakeWhileEvents(epService);
        runAssertionTakeWhileScalar(epService);
    }

    private void runAssertionTakeWhileEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3".split(",");
        String epl = "select " +
                "contained.takeWhile(x => x.p00 > 0) as val0," +
                "contained.takeWhile( (x, i) => x.p00 > 0 and i<2) as val1," +
                "contained.takeWhileLast(x => x.p00 > 0) as val2," +
                "contained.takeWhileLast( (x, i) => x.p00 > 0 and i<2) as val3" +
                " from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,3"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1,E2,E3");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1,E2,E3");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "E2,E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,0", "E2,2", "E3,3"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E2,E3");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "E2,E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,0", "E3,3"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E3");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,0"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "E1");
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

    private void runAssertionTakeWhileScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3".split(",");
        String epl = "select " +
                "strvals.takeWhile(x => x != 'E1') as val0," +
                "strvals.takeWhile( (x, i) => x != 'E1' and i<2) as val1," +
                "strvals.takeWhileLast(x => x != 'E1') as val2," +
                "strvals.takeWhileLast( (x, i) => x != 'E1' and i<2) as val3" +
                " from SupportCollection";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", "E2", "E3", "E4");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val3", "E3", "E4");
        listener.reset();

        stmt.destroy();
    }
}
