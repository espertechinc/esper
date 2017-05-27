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

public class ExecEnumWhere implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionWhereEvents(epService);
        runAssertionWhereString(epService);
    }

    private void runAssertionWhereEvents(EPServiceProvider epService) {

        String epl = "select " +
                "contained.where(x => p00 = 9) as val0," +
                "contained.where((x, i) => x.p00 = 9 and i >= 1) as val1 from Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0,val1".split(","), new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,9", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,9", "E2,1", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,9"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E3");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        LambdaAssertionUtil.assertST0Id(listener, "val0", null);
        LambdaAssertionUtil.assertST0Id(listener, "val1", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionWhereString(EPServiceProvider epService) {

        String[] fields = "val0,val1".split(",");
        String eplFragment = "select " +
                "strvals.where(x => x not like '%1%') as val0, " +
                "strvals.where((x, i) => x not like '%1%' and i > 1) as val1 " +
                "from SupportCollection";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E2", "E3");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E4,E2,E1"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E4", "E2");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", new String[0]);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", new String[0]);
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", new String[0]);
        listener.reset();

        stmtFragment.destroy();

        // test boolean
        eplFragment = "select " +
                "boolvals.where(x => x) as val0 " +
                "from SupportCollection";
        stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeBoolean("true,true,false"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", true, true);
        listener.reset();

        stmtFragment.destroy();
    }
}
