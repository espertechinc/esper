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

public class ExecEnumDistinct implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDistinctEvents(epService);
        runAssertionDistinctScalar(epService);
    }

    private void runAssertionDistinctEvents(EPServiceProvider epService) {

        String[] fields = "val0".split(",");
        String eplFragment = "select " +
                "contained.distinctOf(x => p00) as val0 " +
                " from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,2", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1,E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E3,1", "E2,2", "E4,1", "E1,2"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E3,E2");
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

    private void runAssertionDistinctScalar(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.distinctOf() as val0, " +
                "strvals.distinctOf(v => extractNum(v)) as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E2,E2"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E2", "E1");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E2", "E1");
        listener.reset();

        LambdaAssertionUtil.assertSingleAndEmptySupportColl(epService, listener, fields);
        stmtFragment.destroy();
    }
}
