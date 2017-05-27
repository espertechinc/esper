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
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

public class ExecEnumOrderBy implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOrderByEvents(epService);
        runAssertionOrderByScalar(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionOrderByEvents(EPServiceProvider epService) {

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        String eplFragment = "select " +
                "contained.orderBy(x => p00) as val0," +
                "contained.orderBy(x => 10 - p00) as val1," +
                "contained.orderBy(x => 0) as val2," +
                "contained.orderByDesc(x => p00) as val3," +
                "contained.orderByDesc(x => 10 - p00) as val4," +
                "contained.orderByDesc(x => 0) as val5" +
                " from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class, Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,2"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E2,E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "E2,E1");
        LambdaAssertionUtil.assertST0Id(listener, "val4", "E1,E2");
        LambdaAssertionUtil.assertST0Id(listener, "val5", "E1,E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E3,1", "E2,2", "E4,1", "E1,2"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E3,E4,E2,E1");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E2,E1,E3,E4");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E3,E2,E4,E1");
        LambdaAssertionUtil.assertST0Id(listener, "val3", "E2,E1,E3,E4");
        LambdaAssertionUtil.assertST0Id(listener, "val4", "E3,E4,E2,E1");
        LambdaAssertionUtil.assertST0Id(listener, "val5", "E3,E2,E4,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        for (String field : fields) {
            LambdaAssertionUtil.assertST0Id(listener, field, null);
        }
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        for (String field : fields) {
            LambdaAssertionUtil.assertST0Id(listener, field, "");
        }
        listener.reset();

        stmtFragment.destroy();
    }

    private void runAssertionOrderByScalar(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.orderBy() as val0, " +
                "strvals.orderByDesc() as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2", "E4", "E5");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E5", "E4", "E2", "E1");
        listener.reset();

        LambdaAssertionUtil.assertSingleAndEmptySupportColl(epService, listener, fields);
        stmtFragment.destroy();

        // test scalar-coll with lambda
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.orderBy(v => extractNum(v)) as val0, " +
                "strvals.orderByDesc(v => extractNum(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2", "E4", "E5");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E5", "E4", "E2", "E1");
        listener.reset();

        LambdaAssertionUtil.assertSingleAndEmptySupportColl(epService, listener, fields);

        stmtLambda.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "select contained.orderBy() from Bean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'contained.orderBy()': Invalid input for built-in enumeration method 'orderBy' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean_ST0.class.getName() + "' [select contained.orderBy() from Bean]");
    }
}
